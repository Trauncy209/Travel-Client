package com.travelclient.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    
    public ConfigManager() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("travelclient.json");
    }
    
    public void save() {
        try {
            JsonObject json = new JsonObject();
            JsonObject modules = new JsonObject();
            
            for (Module module : TravelClient.moduleManager.getModules()) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("keybind", module.getKeybind());
                modules.add(module.getName(), moduleJson);
            }
            
            json.add("modules", modules);
            
            // Save target if exists
            if (TravelClient.moduleManager.hasTarget()) {
                JsonObject target = new JsonObject();
                target.addProperty("x", TravelClient.moduleManager.getTargetX());
                target.addProperty("z", TravelClient.moduleManager.getTargetZ());
                json.add("target", target);
            }
            
            Files.writeString(configPath, GSON.toJson(json));
            TravelClient.LOGGER.info("Config saved");
            
        } catch (IOException e) {
            TravelClient.LOGGER.error("Failed to save config", e);
        }
    }
    
    public void load() {
        if (!Files.exists(configPath)) {
            TravelClient.LOGGER.info("No config file found, using defaults");
            return;
        }
        
        try {
            String content = Files.readString(configPath);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            
            if (json.has("modules")) {
                JsonObject modules = json.getAsJsonObject("modules");
                
                for (Module module : TravelClient.moduleManager.getModules()) {
                    if (modules.has(module.getName())) {
                        JsonObject moduleJson = modules.getAsJsonObject(module.getName());
                        
                        if (moduleJson.has("enabled") && moduleJson.get("enabled").getAsBoolean()) {
                            module.setEnabled(true);
                        }
                        
                        if (moduleJson.has("keybind")) {
                            int kb = moduleJson.get("keybind").getAsInt();
                            if (kb > 0) {
                                module.setKeybind(kb);
                            }
                        }
                    }
                }
            }
            
            if (json.has("target")) {
                JsonObject target = json.getAsJsonObject("target");
                double x = target.get("x").getAsDouble();
                double z = target.get("z").getAsDouble();
                TravelClient.moduleManager.setTargetCoords(x, z);
            }
            
            TravelClient.LOGGER.info("Config loaded");
            
        } catch (IOException e) {
            TravelClient.LOGGER.error("Failed to load config", e);
        }
    }
}

package com.travelclient;

import com.travelclient.gui.ClickGui;
import com.travelclient.gui.NBTEditorGui;
import com.travelclient.module.ModuleManager;
import com.travelclient.util.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TravelClient implements ClientModInitializer {
    public static final String MOD_ID = "travelclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public static MinecraftClient mc;
    public static ModuleManager moduleManager;
    public static ConfigManager configManager;
    public static ClickGui clickGui;
    
    private static KeyBinding openGuiKey;
    private static KeyBinding nbtEditorKey;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Travel Client initializing...");
        
        mc = MinecraftClient.getInstance();
        configManager = new ConfigManager();
        moduleManager = new ModuleManager();
        
        // Register GUI keybind (Right Shift)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.travelclient.open_gui",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.travelclient"
        ));
        
        // Register NBT Editor keybind (N key)
        nbtEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.travelclient.nbt_editor",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.travelclient"
        ));
        
        // Tick event for modules and keybinds
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            // Check GUI keybind
            while (openGuiKey.wasPressed()) {
                if (clickGui == null) clickGui = new ClickGui();
                client.setScreen(clickGui);
            }
            
            // Check NBT Editor keybind
            while (nbtEditorKey.wasPressed()) {
                client.setScreen(new NBTEditorGui());
            }
            
            // Check module keybinds (only when not in GUI)
            if (client.currentScreen == null) {
                long window = client.getWindow().getHandle();
                for (var module : moduleManager.getModules()) {
                    int key = module.getKeybind();
                    if (key > 0) {
                        if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                            if (!module.keyHeld) {
                                module.toggle();
                                module.keyHeld = true;
                            }
                        } else {
                            module.keyHeld = false;
                        }
                    }
                }
            }
            
            // Tick all enabled modules
            moduleManager.onTick();
        });
        
        // Load config
        configManager.load();
        
        LOGGER.info("Truancy Client v1.0.0 loaded! Press RIGHT SHIFT for GUI, N for NBT Editor, # for commands");
    }
    
    public static void setTargetCoords(double x, double z) {
        moduleManager.setTargetCoords(x, z);
    }
}

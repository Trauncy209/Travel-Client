package com.travelclient.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();
    
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;
    private int keyBind = 0;
    public boolean keyHeld = false; // Track if key is being held
    
    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }
    
    public void toggle() {
        enabled = !enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            toggle();
        }
    }
    
    public abstract void onTick();
    
    public void onEnable() {}
    
    public void onDisable() {}
    
    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public int getKeybind() { return keyBind; }
    public void setKeybind(int keyBind) { this.keyBind = keyBind; }
    
    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        PLAYER("Player"),
        RENDER("Render"),
        WORLD("World");
        
        private final String name;
        
        Category(String name) {
            this.name = name;
        }
        
        public String getName() { return name; }
    }
}

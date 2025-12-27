package com.travelclient.module.player;

import com.travelclient.module.Module;
import net.minecraft.text.Text;

public class Spammer extends Module {
    
    private String message = "Truancy Client!";
    private int delay = 60; // Ticks (3 seconds default)
    private int timer = 0;
    private boolean randomSuffix = true;
    private int suffixCounter = 0;
    
    public Spammer() {
        super("Spammer", "Spams chat messages", Category.PLAYER);
    }
    
    @Override
    public void onEnable() {
        timer = 0;
        suffixCounter = (int)(Math.random() * 1000);
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[Spammer] Started"), false);
            mc.player.sendMessage(Text.of("§7Message: §f" + message), false);
            mc.player.sendMessage(Text.of("§7Delay: §f" + delay + " ticks (" + (delay / 20.0) + "s)"), false);
        }
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.player.networkHandler == null) return;
        if (message.isEmpty()) return;
        
        timer++;
        if (timer >= delay) {
            timer = 0;
            sendSpam();
        }
    }
    
    private void sendSpam() {
        String toSend = message;
        
        // Add suffix to bypass dupe detection - using only safe characters
        // No brackets, no special chars - just a period and numbers
        if (randomSuffix) {
            suffixCounter++;
            if (suffixCounter > 9999) suffixCounter = 1;
            toSend = message + " " + suffixCounter;
        }
        
        // Strip any potentially problematic characters
        toSend = sanitizeMessage(toSend);
        
        try {
            mc.player.networkHandler.sendChatMessage(toSend);
        } catch (Exception e) {
            // Ignore send errors
        }
    }
    
    /**
     * Remove characters that might get you kicked from strict servers
     */
    private String sanitizeMessage(String msg) {
        StringBuilder sb = new StringBuilder();
        for (char c : msg.toCharArray()) {
            // Only allow basic printable ASCII and spaces
            if ((c >= 32 && c <= 126)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§c[Spammer] Stopped"), false);
        }
    }
    
    // Settings
    public String getMessage() { return message; }
    public void setMessage(String msg) { 
        this.message = sanitizeMessage(msg);
        if (mc.player != null && isEnabled()) {
            mc.player.sendMessage(Text.of("§a[Spammer] Message set: §f" + this.message), false);
        }
    }
    
    public int getDelay() { return delay; }
    public void setDelay(int d) { 
        this.delay = Math.max(20, d); // Minimum 1 second
        if (mc.player != null && isEnabled()) {
            mc.player.sendMessage(Text.of("§a[Spammer] Delay: §f" + delay + " ticks (" + (delay/20.0) + "s)"), false);
        }
    }
    
    public boolean getRandomSuffix() { return randomSuffix; }
    public void setRandomSuffix(boolean r) { this.randomSuffix = r; }
}

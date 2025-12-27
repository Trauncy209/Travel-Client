package com.travelclient.module.player;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class DeathCoords extends Module {
    
    private final List<DeathLocation> deathHistory = new ArrayList<>();
    private boolean wasAlive = true;
    private static final int MAX_HISTORY = 20;
    
    public DeathCoords() {
        super("DeathCoords", "Saves and displays coordinates when you die", Category.PLAYER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        boolean onDeathScreen = mc.currentScreen instanceof DeathScreen;
        
        if (onDeathScreen && wasAlive) {
            // Just died
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            String dimension = getDimensionName();
            
            DeathLocation death = new DeathLocation(x, y, z, dimension, System.currentTimeMillis());
            deathHistory.add(0, death); // Add to front
            
            // Trim history
            while (deathHistory.size() > MAX_HISTORY) {
                deathHistory.remove(deathHistory.size() - 1);
            }
            
            // Announce in chat
            String message = String.format("§c[Death] §fCoords: §e%.0f, %.0f, %.0f §7(%s)", x, y, z, dimension);
            mc.player.sendMessage(Text.literal(message), false);
            
            TravelClient.LOGGER.info("Death recorded: {}, {}, {} in {}", x, y, z, dimension);
            
            wasAlive = false;
        } else if (!onDeathScreen) {
            wasAlive = true;
        }
    }
    
    private String getDimensionName() {
        if (mc.world == null) return "Unknown";
        String dimKey = mc.world.getRegistryKey().getValue().toString();
        if (dimKey.contains("overworld")) return "Overworld";
        if (dimKey.contains("nether")) return "Nether";
        if (dimKey.contains("end")) return "End";
        return dimKey;
    }
    
    public List<DeathLocation> getDeathHistory() {
        return deathHistory;
    }
    
    public DeathLocation getLastDeath() {
        return deathHistory.isEmpty() ? null : deathHistory.get(0);
    }
    
    public static class DeathLocation {
        public final double x, y, z;
        public final String dimension;
        public final long timestamp;
        
        public DeathLocation(double x, double y, double z, String dimension, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return String.format("%.0f, %.0f, %.0f (%s)", x, y, z, dimension);
        }
    }
}

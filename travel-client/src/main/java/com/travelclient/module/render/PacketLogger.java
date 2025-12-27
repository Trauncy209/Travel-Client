package com.travelclient.module.render;

import com.travelclient.module.Module;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacketLogger extends Module {
    
    private boolean logOutgoing = true;   // Packets YOU send to server
    private boolean logIncoming = false;  // Packets SERVER sends to you
    private boolean logMovement = false;  // Position packets (very spammy)
    private boolean logChat = true;
    private boolean logInventory = true;
    private boolean logEntities = true;
    private boolean logWorld = true;
    
    private List<String> recentLogs = new ArrayList<>();
    private static final int MAX_LOGS = 100;
    
    // Packet explanations for beginners
    private static final Map<String, String> PACKET_EXPLANATIONS = new HashMap<>();
    
    static {
        // === OUTGOING (Client -> Server) ===
        // Movement
        PACKET_EXPLANATIONS.put("PlayerMoveC2SPacket", "You moved/looked around");
        PACKET_EXPLANATIONS.put("PlayerMoveC2SPacket$Full", "You moved AND looked (position + rotation)");
        PACKET_EXPLANATIONS.put("PlayerMoveC2SPacket$PositionAndOnGround", "You moved (just position)");
        PACKET_EXPLANATIONS.put("PlayerMoveC2SPacket$LookAndOnGround", "You looked around (just rotation)");
        PACKET_EXPLANATIONS.put("VehicleMoveC2SPacket", "You moved while in a boat/minecart/horse");
        
        // Interaction
        PACKET_EXPLANATIONS.put("PlayerInteractBlockC2SPacket", "You RIGHT-CLICKED a block (place/use)");
        PACKET_EXPLANATIONS.put("PlayerInteractEntityC2SPacket", "You interacted with an entity (attack/use)");
        PACKET_EXPLANATIONS.put("PlayerInteractItemC2SPacket", "You RIGHT-CLICKED with an item in hand");
        PACKET_EXPLANATIONS.put("PlayerActionC2SPacket", "You did an action (start/stop mining, drop item, swap hands, etc)");
        PACKET_EXPLANATIONS.put("HandSwingC2SPacket", "You swung your arm (attack animation)");
        
        // Inventory
        PACKET_EXPLANATIONS.put("ClickSlotC2SPacket", "You clicked a slot in inventory/chest - THIS IS WHAT DUPES EXPLOIT");
        PACKET_EXPLANATIONS.put("CreativeInventoryActionC2SPacket", "You spawned/moved item in CREATIVE MODE - Dupes try to send this in survival");
        PACKET_EXPLANATIONS.put("UpdateSelectedSlotC2SPacket", "You changed hotbar slot (scroll wheel/number key)");
        PACKET_EXPLANATIONS.put("PickFromInventoryC2SPacket", "You middle-clicked to pick block");
        PACKET_EXPLANATIONS.put("CloseHandledScreenC2SPacket", "You closed inventory/chest/furnace");
        PACKET_EXPLANATIONS.put("ButtonClickC2SPacket", "You clicked a button (enchanting table, stonecutter, etc)");
        
        // Chat/Commands
        PACKET_EXPLANATIONS.put("ChatMessageC2SPacket", "You sent a CHAT MESSAGE");
        PACKET_EXPLANATIONS.put("CommandExecutionC2SPacket", "You ran a COMMAND (starts with /)");
        
        // Player state
        PACKET_EXPLANATIONS.put("ClientCommandC2SPacket", "You changed state (start sneak, start sprint, open inventory, START ELYTRA FLYING)");
        PACKET_EXPLANATIONS.put("ClientStatusC2SPacket", "You respawned or requested stats");
        PACKET_EXPLANATIONS.put("PlayerInputC2SPacket", "You sent movement input (WASD) - used when riding");
        
        // World interaction
        PACKET_EXPLANATIONS.put("UpdateSignC2SPacket", "You edited a sign");
        PACKET_EXPLANATIONS.put("BookUpdateC2SPacket", "You edited a book");
        PACKET_EXPLANATIONS.put("JigsawGeneratingC2SPacket", "You used a jigsaw block");
        PACKET_EXPLANATIONS.put("UpdateBeaconC2SPacket", "You set beacon effects");
        PACKET_EXPLANATIONS.put("RenameItemC2SPacket", "You renamed item in anvil");
        PACKET_EXPLANATIONS.put("SelectMerchantTradeC2SPacket", "You selected a villager trade");
        
        // Technical
        PACKET_EXPLANATIONS.put("KeepAliveC2SPacket", "Ping! Telling server you're still connected");
        PACKET_EXPLANATIONS.put("TeleportConfirmC2SPacket", "You confirmed server teleported you - EXPLOITS sometimes skip this");
        PACKET_EXPLANATIONS.put("AcknowledgeChunksC2SPacket", "You confirmed you received chunk data");
        PACKET_EXPLANATIONS.put("ClientOptionsC2SPacket", "You changed settings (render distance, chat visibility, etc)");
        
        // === INCOMING (Server -> Client) ===
        // Position/Movement
        PACKET_EXPLANATIONS.put("PlayerPositionLookS2CPacket", "Server FORCED your position/look - Used for teleports, anticheat corrections");
        PACKET_EXPLANATIONS.put("EntityPositionS2CPacket", "Server told you where an entity is");
        PACKET_EXPLANATIONS.put("EntityVelocityUpdateS2CPacket", "Server set entity velocity - KNOCKBACK comes from this");
        PACKET_EXPLANATIONS.put("VehicleMoveS2CPacket", "Server moved your vehicle");
        
        // Entity info
        PACKET_EXPLANATIONS.put("EntitySpawnS2CPacket", "A new entity spawned near you");
        PACKET_EXPLANATIONS.put("ExperienceOrbSpawnS2CPacket", "XP orb spawned");
        PACKET_EXPLANATIONS.put("PlayerSpawnS2CPacket", "Another PLAYER appeared");
        PACKET_EXPLANATIONS.put("EntitiesDestroyS2CPacket", "Entities were removed/despawned");
        PACKET_EXPLANATIONS.put("EntityStatusS2CPacket", "Entity status changed (death, taming, etc)");
        PACKET_EXPLANATIONS.put("EntityTrackerUpdateS2CPacket", "Entity data updated (health, armor, held item)");
        PACKET_EXPLANATIONS.put("EntityAttributesS2CPacket", "Entity attributes (speed, health, attack damage)");
        PACKET_EXPLANATIONS.put("EntityEquipmentUpdateS2CPacket", "Entity's equipment changed (armor, held item)");
        
        // Inventory
        PACKET_EXPLANATIONS.put("InventoryS2CPacket", "Server sent FULL inventory contents - after dupe attempt, check if items actually appeared");
        PACKET_EXPLANATIONS.put("ScreenHandlerSlotUpdateS2CPacket", "Server updated ONE slot - Server correcting your inventory = DUPE FAILED");
        PACKET_EXPLANATIONS.put("OpenScreenS2CPacket", "Server opened a GUI (chest, furnace, villager)");
        PACKET_EXPLANATIONS.put("CloseScreenS2CPacket", "Server closed your GUI");
        PACKET_EXPLANATIONS.put("ScreenHandlerPropertyUpdateS2CPacket", "GUI property changed (furnace progress, enchant options)");
        
        // World
        PACKET_EXPLANATIONS.put("ChunkDataS2CPacket", "Server sent chunk data (blocks in a 16x16 area)");
        PACKET_EXPLANATIONS.put("BlockUpdateS2CPacket", "A single block changed");
        PACKET_EXPLANATIONS.put("ChunkDeltaUpdateS2CPacket", "Multiple blocks changed in a chunk");
        PACKET_EXPLANATIONS.put("ExplosionS2CPacket", "Explosion happened - also includes KNOCKBACK velocity!");
        PACKET_EXPLANATIONS.put("WorldEventS2CPacket", "World event (door open sound, particles, etc)");
        PACKET_EXPLANATIONS.put("ParticleS2CPacket", "Particle effect spawned");
        PACKET_EXPLANATIONS.put("PlaySoundS2CPacket", "Sound played");
        
        // Player state
        PACKET_EXPLANATIONS.put("HealthUpdateS2CPacket", "Your HEALTH/HUNGER changed");
        PACKET_EXPLANATIONS.put("ExperienceBarUpdateS2CPacket", "Your XP changed");
        PACKET_EXPLANATIONS.put("PlayerAbilitiesS2CPacket", "Your abilities changed (fly mode, creative, invulnerable)");
        PACKET_EXPLANATIONS.put("GameStateChangeS2CPacket", "Game state changed (rain, gamemode, credits, bed)");
        PACKET_EXPLANATIONS.put("DeathMessageS2CPacket", "Someone died (death message)");
        PACKET_EXPLANATIONS.put("CooldownUpdateS2CPacket", "Item cooldown started (ender pearl, chorus fruit)");
        
        // Chat
        PACKET_EXPLANATIONS.put("GameMessageS2CPacket", "Server sent you a CHAT MESSAGE");
        PACKET_EXPLANATIONS.put("ChatMessageS2CPacket", "Player chat message");
        PACKET_EXPLANATIONS.put("OverlayMessageS2CPacket", "Actionbar message (above hotbar)");
        PACKET_EXPLANATIONS.put("TitleS2CPacket", "Title text on screen");
        PACKET_EXPLANATIONS.put("SubtitleS2CPacket", "Subtitle text");
        
        // Technical
        PACKET_EXPLANATIONS.put("KeepAliveS2CPacket", "Server ping - you must respond or get kicked");
        PACKET_EXPLANATIONS.put("DisconnectS2CPacket", "SERVER KICKED YOU - Check why!");
        PACKET_EXPLANATIONS.put("CommonPingS2CPacket", "Ping packet");
        PACKET_EXPLANATIONS.put("BundleS2CPacket", "Multiple packets bundled together");
    }
    
    public PacketLogger() {
        super("PacketLogger", "Logs network packets with explanations", Category.RENDER);
    }
    
    @Override
    public void onTick() {
        // PacketLogger doesn't need tick updates - logging happens via mixin
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        recentLogs.clear();
        
        mc.player.sendMessage(Text.of("§a[PacketLogger] §fEnabled!"), false);
        mc.player.sendMessage(Text.of("§7Logging: " + 
            (logOutgoing ? "§aOUT " : "§cOUT ") + 
            (logIncoming ? "§aIN " : "§cIN ") +
            (logMovement ? "§aMOVE " : "§cMOVE ")), false);
        mc.player.sendMessage(Text.of("§7Commands:"), false);
        mc.player.sendMessage(Text.of("§e  #packets out §7- Toggle outgoing"), false);
        mc.player.sendMessage(Text.of("§e  #packets in §7- Toggle incoming"), false);
        mc.player.sendMessage(Text.of("§e  #packets move §7- Toggle movement (spammy)"), false);
        mc.player.sendMessage(Text.of("§e  #packets clear §7- Clear log"), false);
        mc.player.sendMessage(Text.of("§e  #packets show §7- Show recent packets"), false);
    }
    
    public void logOutgoingPacket(Object packet) {
        if (!logOutgoing) return;
        
        String name = packet.getClass().getSimpleName();
        
        // Filter movement if disabled
        if (!logMovement && isMovementPacket(name)) return;
        
        String explanation = PACKET_EXPLANATIONS.getOrDefault(name, "Unknown outgoing packet");
        String details = getPacketDetails(packet);
        
        String log = "§b[OUT] §f" + name;
        if (!details.isEmpty()) {
            log += " §7" + details;
        }
        log += "\n§8  → " + explanation;
        
        addLog(log);
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(log), false);
        }
    }
    
    public void logIncomingPacket(Object packet) {
        if (!logIncoming) return;
        
        String name = packet.getClass().getSimpleName();
        
        // Filter movement if disabled  
        if (!logMovement && isMovementPacket(name)) return;
        
        // Filter very spammy packets
        if (isSpammyPacket(name)) return;
        
        String explanation = PACKET_EXPLANATIONS.getOrDefault(name, "Unknown incoming packet");
        String details = getPacketDetails(packet);
        
        String log = "§a[IN] §f" + name;
        if (!details.isEmpty()) {
            log += " §7" + details;
        }
        log += "\n§8  ← " + explanation;
        
        addLog(log);
        
        if (mc.player != null) {
            mc.player.sendMessage(Text.of(log), false);
        }
    }
    
    private boolean isMovementPacket(String name) {
        return name.contains("PlayerMove") || 
               name.contains("Position") || 
               name.contains("VehicleMove") ||
               name.contains("EntityPosition") ||
               name.contains("EntityVelocity") ||
               name.equals("EntityS2CPacket");
    }
    
    private boolean isSpammyPacket(String name) {
        return name.contains("Time") ||
               name.contains("Light") ||
               name.contains("ChunkData") ||
               name.contains("KeepAlive") ||
               name.contains("Particle") ||
               name.contains("Sound");
    }
    
    private String getPacketDetails(Object packet) {
        try {
            // Outgoing packets
            if (packet instanceof ClickSlotC2SPacket p) {
                return "slot=" + p.getSlot() + " button=" + p.getButton() + " action=" + p.getActionType();
            }
            if (packet instanceof PlayerActionC2SPacket p) {
                return "action=" + p.getAction().name();
            }
            if (packet instanceof ClientCommandC2SPacket p) {
                return "mode=" + p.getMode().name();
            }
            if (packet instanceof UpdateSelectedSlotC2SPacket p) {
                return "slot=" + p.getSelectedSlot();
            }
            if (packet instanceof PlayerInteractEntityC2SPacket) {
                return "(attacking or using entity)";
            }
            if (packet instanceof PlayerInteractBlockC2SPacket p) {
                return "pos=" + p.getBlockHitResult().getBlockPos().toShortString();
            }
            
            // Incoming packets
            if (packet instanceof ScreenHandlerSlotUpdateS2CPacket p) {
                return "slot=" + p.getSlot() + " item=" + p.getStack().getName().getString();
            }
            if (packet instanceof HealthUpdateS2CPacket p) {
                return "health=" + p.getHealth() + " food=" + p.getFood();
            }
            if (packet instanceof PlayerPositionLookS2CPacket p) {
                return "TELEPORT to " + String.format("%.1f, %.1f, %.1f", p.getX(), p.getY(), p.getZ());
            }
            if (packet instanceof EntityVelocityUpdateS2CPacket p) {
                return "velocity=" + p.getVelocityX() + "," + p.getVelocityY() + "," + p.getVelocityZ();
            }
            if (packet instanceof GameStateChangeS2CPacket p) {
                return "reason=" + p.getReason().toString();
            }
            if (packet instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket p) {
                return "reason=" + p.reason().getString();
            }
            if (packet instanceof PlayerAbilitiesS2CPacket p) {
                return "fly=" + p.isFlying() + " creative=" + p.isCreativeMode();
            }
            
        } catch (Exception e) {
            // Ignore
        }
        return "";
    }
    
    private void addLog(String log) {
        recentLogs.add(log);
        if (recentLogs.size() > MAX_LOGS) {
            recentLogs.remove(0);
        }
    }
    
    public void showRecentLogs() {
        if (mc.player == null) return;
        
        mc.player.sendMessage(Text.of("§6=== Last " + Math.min(10, recentLogs.size()) + " Packets ==="), false);
        
        int start = Math.max(0, recentLogs.size() - 10);
        for (int i = start; i < recentLogs.size(); i++) {
            mc.player.sendMessage(Text.of(recentLogs.get(i)), false);
        }
    }
    
    public void clearLogs() {
        recentLogs.clear();
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[PacketLogger] Cleared!"), false);
        }
    }
    
    public void toggleOutgoing() {
        logOutgoing = !logOutgoing;
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[PacketLogger] Outgoing: " + (logOutgoing ? "§aON" : "§cOFF")), false);
        }
    }
    
    public void toggleIncoming() {
        logIncoming = !logIncoming;
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[PacketLogger] Incoming: " + (logIncoming ? "§aON" : "§cOFF")), false);
        }
    }
    
    public void toggleMovement() {
        logMovement = !logMovement;
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§a[PacketLogger] Movement: " + (logMovement ? "§aON (spammy!)" : "§cOFF")), false);
        }
    }
    
    // Getters for settings
    public boolean getLogOutgoing() { return logOutgoing; }
    public void setLogOutgoing(boolean b) { logOutgoing = b; }
    public boolean getLogIncoming() { return logIncoming; }
    public void setLogIncoming(boolean b) { logIncoming = b; }
    public boolean getLogMovement() { return logMovement; }
    public void setLogMovement(boolean b) { logMovement = b; }
}

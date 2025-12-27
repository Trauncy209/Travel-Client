package com.travelclient.util;

import com.travelclient.TravelClient;
import com.travelclient.module.Module;
import com.travelclient.module.movement.ElytraFly;
import com.travelclient.module.player.AutoMine;
import com.travelclient.module.player.Dupe;
import com.travelclient.module.player.Spammer;
import com.travelclient.module.render.BlockESP;
import com.travelclient.module.render.ElytraDebugRenderer;
import com.travelclient.module.render.PacketLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CommandHandler {
    
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final String PREFIX = "#";
    
    /**
     * Check if a message is a command and handle it
     * @return true if the message was a command (should be cancelled)
     */
    public static boolean handleCommand(String message) {
        if (!message.startsWith(PREFIX)) {
            return false;
        }
        
        String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
        if (parts.length == 0) return false;
        
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "elytra":
                handleElytraCommand(parts);
                return true;
            case "goto":
                handleGotoCommand(parts);
                return true;
            case "help":
                showHelp();
                return true;
            case "toggle":
                handleToggleCommand(parts);
                return true;
            case "target":
                handleTargetCommand(parts);
                return true;
            case "stop":
                handleStopCommand();
                return true;
            case "mine":
                handleMineCommand(parts);
                return true;
            case "find":
                handleMineCommand(parts); // Alias for mine
                return true;
            case "spam":
                handleSpamCommand(parts);
                return true;
            case "spamdelay":
                handleSpamDelayCommand(parts);
                return true;
            case "give":
                handleGiveCommand(parts);
                return true;
            case "packets":
                handlePacketsCommand(parts);
                return true;
            case "dupe":
                handleDupeCommand(parts);
                return true;
            case "nbt":
                handleNbtCommand();
                return true;
            case "nbtedit":
                openNBTEditor();
                return true;
            case "editor":
                openNBTEditor();
                return true;
            case "nbtdetail":
                handleNbtDetailCommand();
                return true;
            case "newerchunks":
                handleNewerChunksCommand(parts);
                return true;
            case "chunks":
                handleNewerChunksCommand(parts); // alias
                return true;
            case "spawn":
                handleSpawnCommand(parts);
                return true;
            case "enchant":
                handleEnchantCommand(parts);
                return true;
            case "repair":
                handleRepairCommand();
                return true;
            default:
                // Try to toggle a module by name
                for (Module module : TravelClient.moduleManager.getModules()) {
                    if (module.getName().toLowerCase().equals(command)) {
                        module.toggle();
                        String status = module.isEnabled() ? "§aenabled" : "§cdisabled";
                        sendMessage("§e" + module.getName() + " §f" + status);
                        return true;
                    }
                }
                sendMessage("§cUnknown command: " + command + ". Use §e#help§c for commands.");
                return true;
        }
    }
    
    private static void handleMineCommand(String[] parts) {
        BlockESP blockESP = TravelClient.moduleManager.getModule(BlockESP.class);
        AutoMine autoMine = TravelClient.moduleManager.getModule(AutoMine.class);
        
        if (blockESP == null) {
            sendMessage("§cBlockESP module not found!");
            return;
        }
        
        if (parts.length < 2) {
            sendMessage("§eUsage: #mine <block_name>");
            sendMessage("§7Examples:");
            sendMessage("§7  #mine iron_ore");
            sendMessage("§7  #mine diamond");
            sendMessage("§7  #mine ancient_debris");
            sendMessage("§7  #mine clear §8- Clear all tracked blocks");
            return;
        }
        
        String blockName = parts[1].toLowerCase();
        
        if (blockName.equals("clear") || blockName.equals("stop") || blockName.equals("off")) {
            blockESP.clearTargets();
            if (blockESP.isEnabled()) blockESP.toggle();
            if (autoMine != null && autoMine.isEnabled()) autoMine.toggle();
            sendMessage("§cStopped mining");
            return;
        }
        
        // If multiple words, join them
        if (parts.length > 2) {
            StringBuilder sb = new StringBuilder(parts[1]);
            for (int i = 2; i < parts.length; i++) {
                sb.append("_").append(parts[i]);
            }
            blockName = sb.toString();
        }
        
        // Enable BlockESP to find the ores
        blockESP.setSearchBlock(blockName);
        
        // Enable AutoMine to mine them
        if (autoMine != null && !autoMine.isEnabled()) {
            autoMine.toggle();
        }
        
        sendMessage("§aMining enabled! Walk near ores to auto-mine them.");
    }
    
    private static void handleElytraCommand(String[] parts) {
        ElytraFly elytraFly = TravelClient.moduleManager.getModule(ElytraFly.class);
        
        if (elytraFly == null) {
            sendMessage("§cElytraFly module not found!");
            return;
        }
        
        // No arguments - start flying!
        if (parts.length < 2) {
            // Enable ElytraFly if not enabled
            if (!elytraFly.isEnabled()) {
                elytraFly.setEnabled(true);
                sendMessage("§aElytraFly enabled!");
            }
            
            // Auto-start flight
            elytraFly.autoStartFlight();
            sendMessage("§aStarting elytra flight... Jump to activate!");
            return;
        }
        
        String subCommand = parts[1].toLowerCase();
        
        switch (subCommand) {
            case "setspeed":
                if (parts.length < 3) {
                    sendMessage("§cUsage: #elytra setspeed <0.1-2.0>");
                    return;
                }
                try {
                    double speed = Double.parseDouble(parts[2]);
                    if (speed < 0.1) speed = 0.1;
                    if (speed > 2.0) speed = 2.0;
                    elytraFly.setTargetSpeed(speed);
                    sendMessage("§aElytra speed set to §e" + String.format("%.2f", speed));
                } catch (NumberFormatException e) {
                    sendMessage("§cInvalid speed value. Use a number between 0.1 and 2.0");
                }
                break;
                
            case "speed":
                sendMessage("§aCurrent elytra speed: §e" + String.format("%.2f", elytraFly.getTargetSpeed()));
                break;
                
            case "safety":
                if (parts.length < 3) {
                    sendMessage("§cUsage: #elytra safety <1-10>");
                    return;
                }
                try {
                    int safety = Integer.parseInt(parts[2]);
                    if (safety < 1) safety = 1;
                    if (safety > 10) safety = 10;
                    elytraFly.setSafetyMargin(safety);
                    sendMessage("§aSafety margin set to §e" + safety + " §a(higher = more cautious)");
                } catch (NumberFormatException e) {
                    sendMessage("§cInvalid safety value. Use a number between 1 and 10");
                }
                break;
                
            case "debug":
                // Toggle debug visualization
                ElytraDebugRenderer debugRenderer = TravelClient.moduleManager.getModule(ElytraDebugRenderer.class);
                if (debugRenderer != null) {
                    debugRenderer.toggle();
                    elytraFly.setDebugMode(debugRenderer.isEnabled());
                    String status = debugRenderer.isEnabled() ? "§aenabled" : "§cdisabled";
                    sendMessage("§eElytra debug visualization " + status);
                    if (debugRenderer.isEnabled()) {
                        sendMessage("§7Green lines = scanned clear paths");
                        sendMessage("§7Orange lines = blocked paths");
                        sendMessage("§7Red line = chosen path");
                        sendMessage("§7Red box = predicted position");
                        sendMessage("§7Blue line = current direction");
                    }
                } else {
                    sendMessage("§cDebug renderer not found!");
                }
                break;
                
            case "stop":
                elytraFly.setEnabled(false);
                sendMessage("§cElytraFly disabled");
                break;
                
            case "progress":
                sendMessage("§aTotal progress: §e" + String.format("%.0f", elytraFly.getTotalProgress()) + " blocks");
                break;
                
            default:
                sendMessage("§eElytra commands:");
                sendMessage("§7  #elytra §f- Start flying (auto jump + activate)");
                sendMessage("§7  #elytra setspeed <0.1-2.0> §f- Set flight speed");
                sendMessage("§7  #elytra speed §f- Show current speed");
                sendMessage("§7  #elytra safety <1-10> §f- Set safety margin");
                sendMessage("§7  #elytra debug §f- Toggle path visualization");
                sendMessage("§7  #elytra progress §f- Show distance traveled");
                sendMessage("§7  #elytra stop §f- Disable ElytraFly");
        }
    }
    
    private static void handleToggleCommand(String[] parts) {
        if (parts.length < 2) {
            sendMessage("§cUsage: #toggle <module name>");
            return;
        }
        
        String moduleName = parts[1].toLowerCase();
        
        for (Module module : TravelClient.moduleManager.getModules()) {
            if (module.getName().toLowerCase().equals(moduleName)) {
                module.toggle();
                String status = module.isEnabled() ? "§aenabled" : "§cdisabled";
                sendMessage("§e" + module.getName() + " §f" + status);
                return;
            }
        }
        
        sendMessage("§cModule not found: " + moduleName);
    }
    
    private static void handleTargetCommand(String[] parts) {
        if (parts.length < 3) {
            if (TravelClient.moduleManager.hasTarget()) {
                sendMessage("§aCurrent target: §e" + 
                    (int) TravelClient.moduleManager.getTargetX() + "§f, §e" +
                    (int) TravelClient.moduleManager.getTargetZ());
            } else {
                sendMessage("§eNo target set. Usage: #target <x> <z>");
            }
            return;
        }
        
        try {
            double x = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            TravelClient.setTargetCoords(x, z);
            sendMessage("§aTarget set to §e" + (int) x + "§f, §e" + (int) z);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates. Usage: #target <x> <z>");
        }
    }
    
    private static void handleGotoCommand(String[] parts) {
        if (parts.length < 3) {
            sendMessage("§cUsage: #goto <x> <z>");
            return;
        }
        
        try {
            double x = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            TravelClient.setTargetCoords(x, z);
            sendMessage("§aTarget set to §e" + (int) x + "§f, §e" + (int) z);
            
            // Enable navigation
            com.travelclient.module.movement.ElytraNavigate nav = 
                TravelClient.moduleManager.getModule(com.travelclient.module.movement.ElytraNavigate.class);
            if (nav != null) {
                nav.setEnabled(true);
            }
            
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid coordinates. Usage: #goto <x> <z>");
        }
    }
    
    private static void handleStopCommand() {
        // Disable all travel modules
        ElytraFly elytraFly = TravelClient.moduleManager.getModule(ElytraFly.class);
        if (elytraFly != null) elytraFly.setEnabled(false);
        
        com.travelclient.module.movement.ElytraNavigate nav = 
            TravelClient.moduleManager.getModule(com.travelclient.module.movement.ElytraNavigate.class);
        if (nav != null) nav.setEnabled(false);
        
        com.travelclient.module.movement.AutoFirework firework = 
            TravelClient.moduleManager.getModule(com.travelclient.module.movement.AutoFirework.class);
        if (firework != null) firework.setEnabled(false);
        
        TravelClient.moduleManager.clearTarget();
        sendMessage("§cStopped all elytra modules");
    }
    
    private static void handleSpamCommand(String[] parts) {
        Spammer spammer = TravelClient.moduleManager.getModule(Spammer.class);
        if (spammer == null) {
            sendMessage("§cSpammer module not found!");
            return;
        }
        
        if (parts.length < 2) {
            sendMessage("§eUsage: #spam <message>");
            sendMessage("§7Example: #spam Hello everyone!");
            sendMessage("§7Use #spamdelay <ticks> to set delay");
            return;
        }
        
        // Join all parts after command as message
        StringBuilder msg = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) msg.append(" ");
            msg.append(parts[i]);
        }
        
        spammer.setMessage(msg.toString());
        if (!spammer.isEnabled()) {
            spammer.toggle();
        }
    }
    
    private static void handleSpamDelayCommand(String[] parts) {
        Spammer spammer = TravelClient.moduleManager.getModule(Spammer.class);
        if (spammer == null) {
            sendMessage("§cSpammer module not found!");
            return;
        }
        
        if (parts.length < 2) {
            sendMessage("§eUsage: #spamdelay <ticks>");
            sendMessage("§7Current: " + spammer.getDelay() + " ticks");
            sendMessage("§720 ticks = 1 second");
            return;
        }
        
        try {
            int delay = Integer.parseInt(parts[1]);
            spammer.setDelay(delay);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid number: " + parts[1]);
        }
    }
    
    private static void handleGiveCommand(String[] parts) {
        Dupe dupe = TravelClient.moduleManager.getModule(Dupe.class);
        if (dupe == null) {
            sendMessage("§cDupe module not found!");
            return;
        }
        
        if (parts.length < 2) {
            sendMessage("§eUsage: #give <item> [count]");
            sendMessage("§7Examples:");
            sendMessage("§7  #give diamond_sword");
            sendMessage("§7  #give enchanted_golden_apple 64");
            return;
        }
        
        String itemName = parts[1];
        int count = 1;
        
        if (parts.length >= 3) {
            try {
                count = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                count = 1;
            }
        }
        
        dupe.giveItem(itemName, count);
    }
    
    private static void handlePacketsCommand(String[] parts) {
        PacketLogger logger = TravelClient.moduleManager.getModule(PacketLogger.class);
        if (logger == null) {
            sendMessage("§cPacketLogger not found!");
            return;
        }
        
        if (parts.length < 2) {
            // Toggle the module
            logger.toggle();
            sendMessage("PacketLogger " + (logger.isEnabled() ? "§aenabled" : "§cdisabled"));
            return;
        }
        
        String sub = parts[1].toLowerCase();
        switch (sub) {
            case "out":
                logger.toggleOutgoing();
                break;
            case "in":
                logger.toggleIncoming();
                break;
            case "move":
            case "movement":
                logger.toggleMovement();
                break;
            case "clear":
                logger.clearLogs();
                break;
            case "show":
            case "recent":
                logger.showRecentLogs();
                break;
            default:
                sendMessage("§eUsage: #packets [out|in|move|clear|show]");
        }
    }
    
    private static void handleDupeCommand(String[] parts) {
        Dupe dupe = TravelClient.moduleManager.getModule(Dupe.class);
        if (dupe == null) {
            sendMessage("§cDupe module not found!");
            return;
        }
        
        String method = parts.length >= 2 ? parts[1] : "all";
        dupe.tryDupe(method);
    }
    
    private static void handleNbtCommand() {
        com.travelclient.module.player.NBTViewer viewer = TravelClient.moduleManager.getModule(com.travelclient.module.player.NBTViewer.class);
        if (viewer == null) {
            sendMessage("§cNBTViewer module not found!");
            return;
        }
        
        viewer.toggle();
        String status = viewer.isEnabled() ? "§aenabled" : "§cdisabled";
        sendMessage("§eNBTViewer §f" + status);
        if (viewer.isEnabled()) {
            sendMessage("§7Hold an item to see its NBT data");
            sendMessage("§7Use #nbtedit to open the editor GUI");
        }
    }
    
    private static void openNBTEditor() {
        // Schedule for next tick so chat screen closes first
        new Thread(() -> {
            try {
                Thread.sleep(50); // 50ms delay
                mc.execute(() -> {
                    mc.setScreen(new com.travelclient.gui.NBTEditorGui());
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private static void handleNewerChunksCommand(String[] parts) {
        com.travelclient.module.render.NewerChunks nc = TravelClient.moduleManager.getModule(com.travelclient.module.render.NewerChunks.class);
        if (nc == null) {
            sendMessage("§cNewerChunks module not found!");
            return;
        }
        
        if (parts.length >= 2 && parts[1].equalsIgnoreCase("clear")) {
            nc.clearData();
            return;
        }
        
        nc.toggle();
        String status = nc.isEnabled() ? "§aenabled" : "§cdisabled";
        sendMessage("§eNewerChunks §f" + status);
    }
    
    private static void handleNbtDetailCommand() {
        com.travelclient.module.player.NBTViewer viewer = TravelClient.moduleManager.getModule(com.travelclient.module.player.NBTViewer.class);
        if (viewer == null) {
            sendMessage("§cNBTViewer module not found!");
            return;
        }
        
        viewer.toggleDetailed();
    }
    
    private static void handleSpawnCommand(String[] parts) {
        Dupe dupe = TravelClient.moduleManager.getModule(Dupe.class);
        if (dupe == null) {
            sendMessage("§cDupe module not found!");
            return;
        }
        
        if (parts.length < 2) {
            sendMessage("§eUsage: #spawn <item> [enchants]");
            sendMessage("§7Enchant format: name:level,name:level");
            sendMessage("§7Example: #spawn diamond_sword sharpness:255,unbreaking:255");
            sendMessage("§7Example: #spawn netherite_pickaxe efficiency:255,fortune:255");
            sendMessage("§7Special: unbreakable:1 makes item unbreakable");
            return;
        }
        
        String itemName = parts[1];
        String enchants = parts.length >= 3 ? parts[2] : "";
        
        // If more parts, combine them (in case user used spaces)
        if (parts.length > 3) {
            StringBuilder sb = new StringBuilder(parts[2]);
            for (int i = 3; i < parts.length; i++) {
                sb.append(parts[i]);
            }
            enchants = sb.toString();
        }
        
        dupe.spawnWithEnchants(itemName, enchants);
    }
    
    private static void handleEnchantCommand(String[] parts) {
        Dupe dupe = TravelClient.moduleManager.getModule(Dupe.class);
        if (dupe == null) {
            sendMessage("§cDupe module not found!");
            return;
        }
        
        if (parts.length < 3) {
            sendMessage("§eUsage: #enchant <name> <level>");
            sendMessage("§7Example: #enchant sharpness 255");
            sendMessage("§7Example: #enchant unbreakable 1");
            sendMessage("§7Names: sharpness, protection, efficiency, fortune, looting, etc.");
            return;
        }
        
        String enchantName = parts[1];
        int level;
        try {
            level = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            sendMessage("§cInvalid level: " + parts[2]);
            return;
        }
        
        dupe.enchantHeldItem(enchantName, level);
    }
    
    private static void handleRepairCommand() {
        Dupe dupe = TravelClient.moduleManager.getModule(Dupe.class);
        if (dupe == null) {
            sendMessage("§cDupe module not found!");
            return;
        }
        
        dupe.repairHeldItem();
    }
    
    private static void showHelp() {
        sendMessage("§6§l=== Truancy Client Commands ===");
        sendMessage("§e#goto <x> <z> §f- Fly to coordinates");
        sendMessage("§e#elytra §f- Start elytra flight");
        sendMessage("§e#mine <block> §f- Find & auto-mine");
        sendMessage("§e#give <item> [count] §f- Spawn items");
        sendMessage("§e#spawn <item> <enchants> §f- Custom enchants");
        sendMessage("§e#enchant <name> <level> §f- Enchant held item");
        sendMessage("§e#repair §f- Repair held item");
        sendMessage("§e#dupe [method] §f- Dupe (packet/desync/drop)");
        sendMessage("§e#nbt §f- Show item info");
        sendMessage("§e#<module> §f- Toggle module");
    }
    
    private static void sendMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("§7[§6TC§7] " + message), false);
        }
    }
}

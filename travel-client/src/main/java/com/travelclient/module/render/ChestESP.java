package com.travelclient.module.render;

import com.travelclient.module.Module;
import com.travelclient.util.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashSet;
import java.util.Set;

public class ChestESP extends Module {
    
    private double range = 100;
    private boolean tracers = false;
    private boolean box = true;
    private boolean alertValuable = true;
    
    // Track already-alerted positions so we don't spam
    private final Set<BlockPos> alertedPositions = new HashSet<>();
    
    // Track chests WE have opened (green = untouched, cyan = we opened)
    private static final Set<BlockPos> openedByUs = new HashSet<>();
    
    // Track chests that appear tampered (signs of player activity)
    private static final Set<BlockPos> tamperedChests = new HashSet<>();
    
    private int scanCooldown = 0;
    
    public ChestESP() {
        super("ChestESP", "Highlights containers - Color coded by status", Category.RENDER);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Periodically scan for valuable chests
        if (scanCooldown > 0) {
            scanCooldown--;
            return;
        }
        scanCooldown = 40; // Check every 2 seconds
        
        if (!alertValuable) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        int chunkRange = (int) (range / 16) + 1;
        ChunkPos playerChunk = new ChunkPos(playerPos);
        
        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                WorldChunk chunk = mc.world.getChunk(playerChunk.x + cx, playerChunk.z + cz);
                if (chunk == null) continue;
                
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    if (playerPos.getSquaredDistance(pos) > range * range) continue;
                    if (alertedPositions.contains(pos)) continue;
                    
                    if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) {
                        int valuableScore = getValuableScore(pos);
                        
                        // Check for tamper signs
                        if (hasTamperSigns(pos) && !tamperedChests.contains(pos)) {
                            tamperedChests.add(pos);
                        }
                        
                        if (valuableScore >= 3) {
                            alertedPositions.add(pos);
                            String type = be instanceof ShulkerBoxBlockEntity ? "Shulker" : 
                                         be instanceof BarrelBlockEntity ? "Barrel" : "Chest";
                            
                            mc.player.sendMessage(Text.of("§c§l[!] §eValuable " + type + " detected! §7(" + 
                                pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") §cScore: " + valuableScore), false);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Check if chest shows signs of being tampered/opened by players
     */
    private boolean hasTamperSigns(BlockPos pos) {
        // Check for signs of player activity around chest
        int playerSigns = 0;
        
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block block = mc.world.getBlockState(checkPos).getBlock();
                    
                    // Torches = player was here
                    if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || 
                        block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH) {
                        playerSigns += 2;
                    }
                    
                    // Cobblestone underground = mining
                    if (block == Blocks.COBBLESTONE && pos.getY() < 60) {
                        playerSigns++;
                    }
                    
                    // Rails = player transport
                    if (block == Blocks.RAIL || block == Blocks.POWERED_RAIL) {
                        playerSigns += 2;
                    }
                    
                    // Crafting/furnace = player base
                    if (block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || 
                        block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER) {
                        playerSigns += 3;
                    }
                    
                    // Beds = someone lived here
                    if (block == Blocks.RED_BED || block.toString().contains("_bed")) {
                        playerSigns += 3;
                    }
                }
            }
        }
        
        return playerSigns >= 3;
    }
    
    /**
     * Call this when player opens a chest to track it
     */
    public static void markAsOpened(BlockPos pos) {
        openedByUs.add(pos);
    }
    
    /**
     * Calculate how likely a chest is to contain good loot
     * Higher score = more likely valuable
     */
    private int getValuableScore(BlockPos pos) {
        int score = 0;
        
        // Check if it's a trapped chest (player placed, often has valuables)
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block == Blocks.TRAPPED_CHEST) {
            score += 4; // Definitely player-placed
        }
        
        // Check surrounding blocks for signs of player builds
        int obsidianCount = 0;
        int netheriteBlockCount = 0;
        int reinforcedBlocks = 0;
        boolean hasRedstone = false;
        boolean nearBedrock = false;
        boolean inEnd = mc.world.getRegistryKey().getValue().getPath().contains("end");
        boolean inNether = mc.world.getRegistryKey().getValue().getPath().contains("nether");
        
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block checkBlock = mc.world.getBlockState(checkPos).getBlock();
                    
                    if (checkBlock == Blocks.OBSIDIAN || checkBlock == Blocks.CRYING_OBSIDIAN) {
                        obsidianCount++;
                    }
                    if (checkBlock == Blocks.NETHERITE_BLOCK) {
                        netheriteBlockCount++;
                    }
                    if (checkBlock == Blocks.BEDROCK) {
                        nearBedrock = true;
                    }
                    if (checkBlock == Blocks.REDSTONE_BLOCK || checkBlock == Blocks.REPEATER || 
                        checkBlock == Blocks.COMPARATOR || checkBlock == Blocks.OBSERVER) {
                        hasRedstone = true;
                    }
                    if (checkBlock == Blocks.REINFORCED_DEEPSLATE || checkBlock == Blocks.END_STONE_BRICKS) {
                        reinforcedBlocks++;
                    }
                }
            }
        }
        
        // Scoring
        if (obsidianCount >= 3) score += 2; // Protected by obsidian
        if (obsidianCount >= 8) score += 2; // Heavily protected
        if (netheriteBlockCount > 0) score += 3; // Expensive blocks nearby
        if (hasRedstone) score += 1; // Has redstone contraption
        if (reinforcedBlocks >= 2) score += 2;
        if (nearBedrock && pos.getY() < 10) score += 1; // Near bedrock level
        
        // End dimension chests are often stash locations
        if (inEnd && pos.getY() > 100) score += 2;
        
        // Hidden underground in nether
        if (inNether && pos.getY() < 30) score += 1;
        
        // Shulker boxes are always valuable
        BlockEntity be = mc.world.getBlockEntity(pos);
        if (be instanceof ShulkerBoxBlockEntity) {
            score += 2;
        }
        
        // Check for named chest (custom name = player storage)
        if (be instanceof LockableContainerBlockEntity lockable) {
            if (lockable.getCustomName() != null) {
                score += 3;
            }
        }
        
        return score;
    }
    
    public void onRender(MatrixStack matrices) {
        if (mc.player == null || mc.world == null || !isEnabled()) return;
        
        RenderUtils.setup();
        
        BlockPos playerPos = mc.player.getBlockPos();
        int chunkRange = (int) (range / 16) + 1;
        
        ChunkPos playerChunk = new ChunkPos(playerPos);
        
        for (int cx = -chunkRange; cx <= chunkRange; cx++) {
            for (int cz = -chunkRange; cz <= chunkRange; cz++) {
                WorldChunk chunk = mc.world.getChunk(playerChunk.x + cx, playerChunk.z + cz);
                if (chunk == null) continue;
                
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    if (playerPos.getSquaredDistance(pos) > range * range) continue;
                    
                    float r, g, b;
                    
                    if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                        int score = getValuableScore(pos);
                        boolean weOpened = openedByUs.contains(pos);
                        boolean tampered = tamperedChests.contains(pos);
                        
                        if (weOpened) {
                            // CYAN = We opened this chest
                            r = 0.0f; g = 1.0f; b = 1.0f;
                        } else if (score >= 3) {
                            // RED = Valuable/suspicious
                            r = 1.0f; g = 0.0f; b = 0.0f;
                        } else if (tampered) {
                            // ORANGE = Signs of player activity
                            r = 1.0f; g = 0.5f; b = 0.0f;
                        } else if (score >= 1) {
                            // YELLOW = Somewhat suspicious
                            r = 1.0f; g = 1.0f; b = 0.0f;
                        } else {
                            // GREEN = Untouched/natural
                            r = 0.0f; g = 1.0f; b = 0.0f;
                        }
                    } else if (be instanceof EnderChestBlockEntity) {
                        r = 1.0f; g = 0.0f; b = 1.0f; // Magenta
                    } else if (be instanceof ShulkerBoxBlockEntity) {
                        boolean weOpened = openedByUs.contains(pos);
                        if (weOpened) {
                            r = 0.0f; g = 1.0f; b = 1.0f; // Cyan
                        } else {
                            int score = getValuableScore(pos);
                            if (score >= 4) {
                                r = 1.0f; g = 0.0f; b = 0.0f; // Red
                            } else {
                                r = 1.0f; g = 0.5f; b = 1.0f; // Pink
                            }
                        }
                    } else if (be instanceof HopperBlockEntity) {
                        r = 0.5f; g = 0.5f; b = 0.5f; // Gray
                    } else {
                        continue;
                    }
                    
                    Box bbox = new Box(pos);
                    
                    if (this.box) {
                        RenderUtils.drawBox(matrices, bbox, r, g, b, 1.0f);
                    }
                }
            }
        }
        
        RenderUtils.cleanup();
    }
    
    @Override
    public void onDisable() {
        alertedPositions.clear();
    }
    
    public void setRange(double r) { range = r; }
    public double getRange() { return range; }
    public void setTracers(boolean t) { tracers = t; }
    public void setBox(boolean b) { box = b; }
    public void setAlertValuable(boolean a) { alertValuable = a; }
    public boolean getAlertValuable() { return alertValuable; }
    
    // For other modules to access
    public static Set<BlockPos> getOpenedByUs() { return openedByUs; }
}

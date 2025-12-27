package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoCrystal extends Module {
    
    // Settings
    private double placeRange = 5.0;
    private double breakRange = 5.0;
    private double targetRange = 5.0; // Only target within 5 blocks
    private double minDamage = 4.0;
    private double maxSelfDamage = 8.0;
    private boolean targetPlayers = true;
    private boolean targetMobs = false;
    private boolean autoSurround = true;
    private boolean antiWeakness = true;
    
    // Timing - FAST
    private int placeDelay = 0; // Ticks between places (0 = every tick)
    private int breakDelay = 0; // Ticks between breaks (0 = every tick)
    private int placeTick = 0;
    private int breakTick = 0;
    
    // Surround blocks
    private static final BlockPos[] SURROUND_OFFSETS = {
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1)
    };
    
    public AutoCrystal() {
        super("AutoCrystal", "Crystal PvP with surround", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // === SURROUND FIRST (protect feet) ===
        if (autoSurround) {
            doSurround();
        }
        
        // === BREAK CRYSTALS (spam) ===
        if (breakTick <= 0) {
            breakCrystals();
            breakTick = breakDelay;
        } else {
            breakTick--;
        }
        
        // === PLACE CRYSTALS (spam) ===
        if (placeTick <= 0) {
            placeCrystals();
            placeTick = placeDelay;
        } else {
            placeTick--;
        }
    }
    
    /**
     * SURROUND - Place obsidian around feet to block crystal damage
     */
    private void doSurround() {
        BlockPos feetPos = mc.player.getBlockPos();
        
        for (BlockPos offset : SURROUND_OFFSETS) {
            BlockPos pos = feetPos.add(offset);
            
            // Check if air
            if (!mc.world.getBlockState(pos).isAir()) continue;
            
            // Find obsidian in hotbar
            int slot = findBlock(Blocks.OBSIDIAN);
            if (slot == -1) slot = findBlock(Blocks.ENDER_CHEST); // Backup
            if (slot == -1) continue;
            
            // Switch and place
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;
            
            BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(pos), 
                Direction.UP, 
                pos.down(), 
                false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            
            mc.player.getInventory().selectedSlot = prevSlot;
            return; // One block per tick to not flag anticheat
        }
    }
    
    /**
     * BREAK - Destroy all crystals that would hurt enemies
     */
    private void breakCrystals() {
        List<EndCrystalEntity> crystals = new ArrayList<>();
        
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity)) continue;
            if (mc.player.distanceTo(entity) > breakRange) continue;
            crystals.add((EndCrystalEntity) entity);
        }
        
        // Sort by distance to enemy (closest first)
        LivingEntity target = findTarget();
        if (target != null) {
            crystals.sort(Comparator.comparingDouble(c -> c.distanceTo(target)));
        }
        
        // Break ALL crystals we can (spam)
        for (EndCrystalEntity crystal : crystals) {
            double selfDmg = calculateDamage(crystal.getPos(), mc.player);
            
            // Skip if would kill us
            if (selfDmg > mc.player.getHealth()) continue;
            
            // Check if would hurt target
            if (target != null) {
                double targetDmg = calculateDamage(crystal.getPos(), target);
                if (targetDmg >= minDamage || selfDmg < targetDmg) {
                    mc.interactionManager.attackEntity(mc.player, crystal);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            } else {
                // No target - break crystals that are safe
                if (selfDmg < maxSelfDamage) {
                    mc.interactionManager.attackEntity(mc.player, crystal);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }
    
    /**
     * PLACE - Spam crystals near enemies
     */
    private void placeCrystals() {
        int crystalSlot = findItem(Items.END_CRYSTAL);
        if (crystalSlot == -1) return;
        
        LivingEntity target = findTarget();
        if (target == null) return;
        
        // Find best placement
        BlockPos bestPos = null;
        double bestDamage = minDamage;
        
        List<BlockPos> positions = getPlacePositions(target);
        
        for (BlockPos pos : positions) {
            double dist = mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));
            if (dist > placeRange * placeRange) continue;
            
            Vec3d crystalPos = Vec3d.ofCenter(pos.up());
            double targetDmg = calculateDamage(crystalPos, target);
            double selfDmg = calculateDamage(crystalPos, mc.player);
            
            // Skip bad placements
            if (targetDmg < minDamage) continue;
            if (selfDmg > maxSelfDamage) continue;
            if (selfDmg > mc.player.getHealth() - 2) continue; // Safety buffer
            
            // Best = most damage to target
            if (targetDmg > bestDamage) {
                bestDamage = targetDmg;
                bestPos = pos;
            }
        }
        
        if (bestPos != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = crystalSlot;
            
            BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(bestPos).add(0, 0.5, 0),
                Direction.UP,
                bestPos,
                false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
            
            mc.player.getInventory().selectedSlot = prevSlot;
        }
    }
    
    /**
     * Get all valid crystal placement positions near target
     */
    private List<BlockPos> getPlacePositions(LivingEntity target) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos center = target.getBlockPos();
        
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = center.add(x, y, z);
                    
                    // Must be obsidian or bedrock
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) continue;
                    
                    // Need 2 air blocks above
                    if (!mc.world.getBlockState(pos.up()).isAir()) continue;
                    if (!mc.world.getBlockState(pos.up(2)).isAir()) continue;
                    
                    // No entities in the way
                    Box box = new Box(pos.up());
                    boolean blocked = false;
                    for (Entity e : mc.world.getOtherEntities(null, box)) {
                        if (e instanceof EndCrystalEntity) continue; // Existing crystal ok
                        blocked = true;
                        break;
                    }
                    if (blocked) continue;
                    
                    positions.add(pos);
                }
            }
        }
        
        // Sort by distance to target
        Vec3d targetPos = target.getPos();
        positions.sort(Comparator.comparingDouble(p -> Vec3d.ofCenter(p).squaredDistanceTo(targetPos)));
        
        return positions;
    }
    
    /**
     * Calculate crystal explosion damage
     */
    private double calculateDamage(Vec3d crystalPos, LivingEntity target) {
        double dist = target.getPos().distanceTo(crystalPos);
        if (dist > 12) return 0;
        
        // Explosion damage formula
        double exposure = getExposure(crystalPos, target);
        double impact = (1.0 - (dist / 12.0)) * exposure;
        double damage = (impact * impact + impact) * 42 * 0.5 + 1; // Crystal = 6 power
        
        // Reduce for armor and resistance
        damage = applyArmor(damage, target);
        
        return damage;
    }
    
    private double getExposure(Vec3d source, Entity target) {
        Box box = target.getBoundingBox();
        double xStep = 1.0 / ((box.maxX - box.minX) * 2 + 1);
        double yStep = 1.0 / ((box.maxY - box.minY) * 2 + 1);
        double zStep = 1.0 / ((box.maxZ - box.minZ) * 2 + 1);
        
        if (xStep < 0 || yStep < 0 || zStep < 0) return 0;
        
        int hit = 0;
        int total = 0;
        
        for (double x = 0; x <= 1; x += xStep) {
            for (double y = 0; y <= 1; y += yStep) {
                for (double z = 0; z <= 1; z += zStep) {
                    Vec3d point = new Vec3d(
                        box.minX + (box.maxX - box.minX) * x,
                        box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z
                    );
                    
                    // Raycast check (simplified)
                    if (mc.world.raycast(new net.minecraft.world.RaycastContext(
                        source, point,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        mc.player
                    )).getType() == net.minecraft.util.hit.HitResult.Type.MISS) {
                        hit++;
                    }
                    total++;
                }
            }
        }
        
        return (double) hit / total;
    }
    
    private double applyArmor(double damage, LivingEntity target) {
        // Simplified armor calculation
        int armor = target.getArmor();
        double reduction = armor * 0.04; // ~4% per armor point
        return damage * (1 - Math.min(0.8, reduction)); // Max 80% reduction
    }
    
    /**
     * Find closest target
     */
    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDist = targetRange + 1;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof LivingEntity)) continue;
            if (!((LivingEntity) entity).isAlive()) continue;
            
            boolean valid = false;
            if (targetPlayers && entity instanceof PlayerEntity) valid = true;
            if (targetMobs && entity instanceof HostileEntity) valid = true;
            
            if (!valid) continue;
            
            double dist = mc.player.distanceTo(entity);
            if (dist < bestDist) {
                bestDist = dist;
                best = (LivingEntity) entity;
            }
        }
        
        return best;
    }
    
    private int findItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
    
    private int findBlock(Block block) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                BlockItem bi = (BlockItem) mc.player.getInventory().getStack(i).getItem();
                if (bi.getBlock() == block) return i;
            }
        }
        return -1;
    }
    
    // Settings getters/setters
    public void setRange(double r) { placeRange = r; breakRange = r; targetRange = r; }
    public double getRange() { return placeRange; }
    public void setMinDamage(double d) { minDamage = d; }
    public double getMinDamage() { return minDamage; }
    public void setMaxSelfDamage(double d) { maxSelfDamage = d; }
    public double getMaxSelfDamage() { return maxSelfDamage; }
    public void setPlayers(boolean b) { targetPlayers = b; }
    public boolean getPlayers() { return targetPlayers; }
    public void setMobs(boolean b) { targetMobs = b; }
    public boolean getMobs() { return targetMobs; }
    public void setAutoSurround(boolean b) { autoSurround = b; }
    public void setPlaceDelay(int d) { placeDelay = d; }
    public void setBreakDelay(int d) { breakDelay = d; }
}

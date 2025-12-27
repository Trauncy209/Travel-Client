package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Respawn Anchor aura - works in Nether!
 * Place anchor -> Charge with glowstone -> Explode
 */
public class AnchorAura extends Module {
    
    private double range = 5.0;
    private double minDamage = 6.0;
    private double maxSelfDamage = 10.0;
    
    private BlockPos placedAnchor = null;
    private int chargeState = 0; // 0 = place, 1 = charge, 2 = explode
    
    public AnchorAura() {
        super("AnchorAura", "Respawn anchor PvP", Category.COMBAT);
    }
    
    @Override
    public void onEnable() {
        placedAnchor = null;
        chargeState = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        
        // Only works outside of the End
        if (mc.world.getRegistryKey() == net.minecraft.world.World.END) return;
        
        LivingEntity target = findTarget();
        if (target == null) {
            placedAnchor = null;
            chargeState = 0;
            return;
        }
        
        switch (chargeState) {
            case 0: // Place anchor
                if (placeAnchor(target)) {
                    chargeState = 1;
                }
                break;
                
            case 1: // Charge with glowstone
                if (chargeAnchor()) {
                    chargeState = 2;
                }
                break;
                
            case 2: // Explode!
                if (explodeAnchor()) {
                    chargeState = 0;
                    placedAnchor = null;
                }
                break;
        }
    }
    
    private boolean placeAnchor(LivingEntity target) {
        int anchorSlot = findItem(Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) return false;
        
        // Find best position near target
        BlockPos best = null;
        double bestDamage = minDamage;
        
        BlockPos targetPos = target.getBlockPos();
        
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    
                    // Must be air
                    if (!mc.world.getBlockState(pos).isAir()) continue;
                    
                    // Must be in range
                    if (mc.player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos)) > range * range) continue;
                    
                    // Must have support below
                    if (mc.world.getBlockState(pos.down()).isAir()) continue;
                    
                    // Calculate damage
                    Vec3d explodePos = Vec3d.ofCenter(pos);
                    double targetDmg = estimateDamage(explodePos, target);
                    double selfDmg = estimateDamage(explodePos, mc.player);
                    
                    if (targetDmg < minDamage) continue;
                    if (selfDmg > maxSelfDamage) continue;
                    if (selfDmg > mc.player.getHealth() - 4) continue;
                    
                    if (targetDmg > bestDamage) {
                        bestDamage = targetDmg;
                        best = pos;
                    }
                }
            }
        }
        
        if (best != null) {
            int prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = anchorSlot;
            
            BlockHitResult hit = new BlockHitResult(
                Vec3d.ofCenter(best.down()).add(0, 0.5, 0),
                Direction.UP,
                best.down(),
                false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            
            mc.player.getInventory().selectedSlot = prevSlot;
            placedAnchor = best;
            return true;
        }
        
        return false;
    }
    
    private boolean chargeAnchor() {
        if (placedAnchor == null) return false;
        
        // Check anchor still exists
        if (mc.world.getBlockState(placedAnchor).getBlock() != Blocks.RESPAWN_ANCHOR) {
            placedAnchor = null;
            chargeState = 0;
            return false;
        }
        
        int glowstoneSlot = findItem(Items.GLOWSTONE);
        if (glowstoneSlot == -1) return false;
        
        int prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = glowstoneSlot;
        
        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(placedAnchor),
            Direction.UP,
            placedAnchor,
            false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        
        mc.player.getInventory().selectedSlot = prevSlot;
        return true;
    }
    
    private boolean explodeAnchor() {
        if (placedAnchor == null) return false;
        
        // Check anchor still exists
        if (mc.world.getBlockState(placedAnchor).getBlock() != Blocks.RESPAWN_ANCHOR) {
            placedAnchor = null;
            return true;
        }
        
        // Right click with empty hand or non-glowstone to explode
        int prevSlot = mc.player.getInventory().selectedSlot;
        
        // Find a slot without glowstone
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() != Items.GLOWSTONE) {
                mc.player.getInventory().selectedSlot = i;
                break;
            }
        }
        
        BlockHitResult hit = new BlockHitResult(
            Vec3d.ofCenter(placedAnchor),
            Direction.UP,
            placedAnchor,
            false
        );
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        
        mc.player.getInventory().selectedSlot = prevSlot;
        return true;
    }
    
    private double estimateDamage(Vec3d pos, LivingEntity target) {
        double dist = target.getPos().distanceTo(pos);
        if (dist > 10) return 0;
        
        // Anchor explosion is powerful
        double damage = (1 - dist / 10) * 50;
        damage *= 0.5; // Armor reduction estimate
        
        return damage;
    }
    
    private LivingEntity findTarget() {
        LivingEntity best = null;
        double bestDist = range + 5;
        
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!(entity instanceof PlayerEntity)) continue;
            if (!((LivingEntity) entity).isAlive()) continue;
            
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
    
    public void setRange(double r) { range = r; }
    public void setMinDamage(double d) { minDamage = d; }
}

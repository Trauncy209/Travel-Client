package com.travelclient.module.combat;

import com.travelclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class Criticals extends Module {
    
    public Criticals() {
        super("Criticals", "Always land critical hits", Category.COMBAT);
    }
    
    @Override
    public void onTick() {
        // Criticals work by sending fake jump packets right before attacking
        // The actual crit logic happens in the attack event
    }
    
    /**
     * Call this before attacking to ensure a crit
     */
    public void doCrit() {
        if (mc.player == null || !mc.player.isOnGround()) return;
        
        // Send fake position packets to simulate falling
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        
        // Jump up slightly then fall - this triggers crit
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
    }
}

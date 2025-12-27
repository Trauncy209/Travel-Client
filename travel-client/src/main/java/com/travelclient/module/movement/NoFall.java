package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    
    public NoFall() {
        super("NoFall", "Prevents fall damage by sending ground packets", Category.MOVEMENT);
    }
    
    @Override
    public void onTick() {
        if (mc.player == null) return;
        
        // If falling and about to take damage
        if (mc.player.fallDistance > 2.5f && !mc.player.isFallFlying()) {
            // Send packet saying we're on ground
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.OnGroundOnly(true)
            );
        }
    }
}

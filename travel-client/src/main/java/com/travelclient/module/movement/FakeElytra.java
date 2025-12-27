package com.travelclient.module.movement;

import com.travelclient.module.Module;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class FakeElytra extends Module {
    
    private double speed = 2.5;
    private double vertSpeed = 1.5;
    private double turnSpeed = 4.0;
    private int ticks = 0;
    
    public FakeElytra() {
        super("FakeElytra", "Elytra flight with full control - NO fireworks used", Category.MOVEMENT);
    }
    
    @Override
    public void onEnable() {
        if (mc.player == null) return;
        
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            mc.player.sendMessage(Text.of("§c[FakeElytra] Equip an elytra!"), false);
            toggle();
            return;
        }
        
        mc.player.sendMessage(Text.of("§a[FakeElytra] §fEnabled - No rockets needed!"), false);
        mc.player.sendMessage(Text.of("§7W/S = forward/back, A/D = strafe"), false);
        mc.player.sendMessage(Text.of("§7Space = up, Shift = down"), false);
        ticks = 0;
    }
    
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;
        
        ticks++;
        
        // Auto-deploy elytra
        if (!mc.player.isFallFlying()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                return;
            }
            mc.player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING)
            );
            return;
        }
        
        // Get input
        boolean forward = mc.options.forwardKey.isPressed();
        boolean back = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        boolean up = mc.options.jumpKey.isPressed();
        boolean down = mc.options.sneakKey.isPressed();
        
        // Get look direction
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        
        // Calculate movement vectors
        // Forward vector (where player is looking horizontally)
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        
        // Right vector (perpendicular to forward)
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        
        // Build velocity from input
        double velX = 0, velY = 0, velZ = 0;
        
        // W/S = forward/back
        if (forward) {
            velX += forwardX * speed;
            velZ += forwardZ * speed;
        }
        if (back) {
            velX -= forwardX * speed * 0.5;
            velZ -= forwardZ * speed * 0.5;
        }
        
        // A/D = strafe left/right (FIXED DIRECTION)
        if (left) {
            velX -= rightX * speed * 0.7;
            velZ -= rightZ * speed * 0.7;
        }
        if (right) {
            velX += rightX * speed * 0.7;
            velZ += rightZ * speed * 0.7;
        }
        
        // Space/Shift = up/down
        if (up) {
            velY = vertSpeed;
        } else if (down) {
            velY = -vertSpeed;
        } else {
            // Slight gravity when no vertical input
            velY = -0.05;
        }
        
        // Apply velocity
        mc.player.setVelocity(velX, velY, velZ);
        
        // Send position packets to reduce rubberbanding
        if (ticks % 2 == 0) {
            Vec3d pos = mc.player.getPos();
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.Full(
                    pos.x, pos.y, pos.z,
                    mc.player.getYaw(), mc.player.getPitch(),
                    false
                )
            );
        }
        
        // Anti-kick: send grounded packet occasionally
        if (ticks % 40 == 0) {
            Vec3d pos = mc.player.getPos();
            mc.player.networkHandler.sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, pos.y - 0.04, pos.z, false
                )
            );
        }
        
        // Prevent fall damage
        mc.player.fallDistance = 0;
        
        // Speed display
        if (ticks % 10 == 0) {
            double spd = mc.player.getVelocity().horizontalLength() * 20;
            mc.player.sendMessage(Text.of("§b[FakeElytra] §fSpeed: §a" + String.format("%.1f", spd) + " §7b/s"), true);
        }
    }
    
    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.sendMessage(Text.of("§c[FakeElytra] Disabled"), false);
        }
    }
    
    // Settings
    public double getSpeed() { return speed; }
    public void setSpeed(double s) { speed = Math.max(0.5, Math.min(10, s)); }
    public double getVertSpeed() { return vertSpeed; }
    public void setVertSpeed(double s) { vertSpeed = Math.max(0.5, Math.min(5, s)); }
    public double getTurnSpeed() { return turnSpeed; }
    public void setTurnSpeed(double t) { turnSpeed = t; }
    public double getFireworkDelay() { return 0; }
    public void setFireworkDelay(double d) { }
}

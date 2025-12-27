package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.render.PacketLogger;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class PacketMixin {
    
    // Log outgoing packets
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (TravelClient.moduleManager == null) return;
        
        PacketLogger logger = TravelClient.moduleManager.getModule(PacketLogger.class);
        if (logger != null && logger.isEnabled()) {
            logger.logOutgoingPacket(packet);
        }
    }
    
    // Log incoming packets
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static void onReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (TravelClient.moduleManager == null) return;
        
        PacketLogger logger = TravelClient.moduleManager.getModule(PacketLogger.class);
        if (logger != null && logger.isEnabled()) {
            logger.logIncomingPacket(packet);
        }
    }
}

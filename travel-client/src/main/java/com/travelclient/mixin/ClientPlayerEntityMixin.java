package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.movement.SafeWalk;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    
    @Inject(method = "shouldSlowDown", at = @At("HEAD"), cancellable = true)
    private void onShouldSlowDown(CallbackInfoReturnable<Boolean> cir) {
        if (TravelClient.moduleManager != null) {
            SafeWalk safeWalk = TravelClient.moduleManager.getModule(SafeWalk.class);
            if (safeWalk != null && safeWalk.shouldPreventFall()) {
                cir.setReturnValue(true);
            }
        }
    }
}

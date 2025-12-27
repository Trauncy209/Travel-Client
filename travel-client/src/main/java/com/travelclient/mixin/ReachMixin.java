package com.travelclient.mixin;

import com.travelclient.TravelClient;
import com.travelclient.module.combat.Reach;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ReachMixin {
    
    @Inject(method = "getReachDistance", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetReachDistance(CallbackInfoReturnable<Float> cir) {
        try {
            if (TravelClient.moduleManager != null) {
                Reach reach = TravelClient.moduleManager.getModule(Reach.class);
                if (reach != null && reach.isEnabled()) {
                    cir.setReturnValue((float) reach.getReachDistance());
                }
            }
        } catch (Exception ignored) {}
    }
    
    @Inject(method = "hasExtendedReach", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasExtendedReach(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (TravelClient.moduleManager != null) {
                Reach reach = TravelClient.moduleManager.getModule(Reach.class);
                if (reach != null && reach.isEnabled()) {
                    cir.setReturnValue(true);
                }
            }
        } catch (Exception ignored) {}
    }
}

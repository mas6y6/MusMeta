package com.example.musmeta.mixin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.mas6y6.musmeta.utils.Utils")
public abstract class UtilsMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("example-plugin.mixin");

    @Inject(method = "isRunningAsRoot", at = @At("RETURN"), remap = false)
    private static void example$afterIsRunningAsRoot(CallbackInfoReturnable<Boolean> cir) {
        LOGGER.info("Utils.isRunningAsRoot() intercepted by ExamplePlugin mixin (returned {})", cir.getReturnValue());
    }
}
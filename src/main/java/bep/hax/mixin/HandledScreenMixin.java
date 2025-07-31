package bep.hax.mixin;

import bep.hax.modules.ShulkerOverviewModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class HandledScreenMixin {

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ShulkerOverviewModule module = Modules.get().get(ShulkerOverviewModule.class);
        if (module == null || !module.isActive()) return;

        module.renderShulkerOverlay(context, slot.x, slot.y, slot.getStack());
    }
}

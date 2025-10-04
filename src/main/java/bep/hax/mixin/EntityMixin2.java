package bep.hax.mixin;

import bep.hax.util.CapeManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin2 {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof PlayerEntity player) {
            if (CapeManager.getInstance().shouldGlow(player)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        Entity entity = (Entity) (Object) this;

        if (entity instanceof PlayerEntity player) {
            CapeManager manager = CapeManager.getInstance();
            if (manager.shouldGlow(player)) {
                // Friend capes get red glow, regular capes get aqua
                if (manager.hasFriendCape(player)) {
                    cir.setReturnValue(Formatting.RED.getColorValue());
                } else {
                    cir.setReturnValue(Formatting.AQUA.getColorValue());
                }
            }
        }
    }
}

package bep.hax.mixin;

import bep.hax.util.CapeManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;

        if (CapeManager.getInstance().hasCape(player)) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures modified = new SkinTextures(
                original.texture(),
                original.textureUrl(),
                CapeManager.CAPE_TEXTURE,
                original.elytraTexture(),
                original.model(),
                original.secure()
            );
            cir.setReturnValue(modified);
        }
    }
}

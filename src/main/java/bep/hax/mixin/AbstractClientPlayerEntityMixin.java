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
        CapeManager manager = CapeManager.getInstance();

        Identifier capeTexture = null;

        // Check for friend cape first
        if (manager.hasFriendCape(player)) {
            capeTexture = CapeManager.FRIEND_CAPE_TEXTURE;
        }
        // Then check for regular cape
        else if (manager.hasCape(player)) {
            capeTexture = CapeManager.CAPE_TEXTURE;
        }

        if (capeTexture != null) {
            SkinTextures original = cir.getReturnValue();
            SkinTextures modified = new SkinTextures(
                original.texture(),
                original.textureUrl(),
                capeTexture,
                original.elytraTexture(),
                original.model(),
                original.secure()
            );
            cir.setReturnValue(modified);
        }
    }
}

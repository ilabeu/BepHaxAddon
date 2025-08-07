package bep.hax.mixin;

import bep.hax.modules.SwingModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {
    
    @Inject(
        method = "renderFirstPersonItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0)
    )
    private void modifyHandTransform(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipmentProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SwingModifier module = Modules.get().get(SwingModifier.class);
        if (module != null && module.isActive()) {
            // Apply Y offset transformation
            float yOffset = module.getY(hand);
            if (yOffset != 0) {
                matrices.translate(0, yOffset, 0);
            }
            
            // Apply custom swing transformation
            float customSwing = module.getSwing(hand);
            if (module.isSwinging(hand) && customSwing != swingProgress) {
                // Undo the original swing progress and apply our custom one
                float swingDiff = customSwing - swingProgress;
                matrices.translate(0, swingDiff * 0.1f, 0);
            }
        }
    }
}
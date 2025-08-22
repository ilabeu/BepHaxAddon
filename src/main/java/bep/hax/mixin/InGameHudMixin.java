package bep.hax.mixin;

import net.minecraft.text.Text;
import bep.hax.modules.AntiToS;
import bep.hax.modules.NoHurtCam;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.DrawContext;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import bep.hax.modules.ShulkerOverviewModule;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Arm;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    private ItemStack currentStack;

    // See AntiToS.java
    @Inject(
        method = "renderHeldItemTooltip",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z")
    )
    private void censorItemTooltip(DrawContext context, CallbackInfo ci, @Local LocalRef<MutableText> itemName) {
        if (this.currentStack.isEmpty()) return;

        Modules modules = Modules.get();
        if (modules == null) return;
        AntiToS antiToS = modules.get(AntiToS.class);
        if (!antiToS.isActive()) return;

        if (antiToS.containsBlacklistedText(itemName.get().getString())) {
            itemName.set(Text.empty().append(antiToS.censorText(itemName.get().getString())).formatted(this.currentStack.getRarity().getFormatting()));
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ShulkerOverviewModule module = Modules.get().get(ShulkerOverviewModule.class);
        if (module == null || !module.isActive()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();
        int center = scaledWidth / 2;

        // Hotbar slots 0-8
        int hotbarY = scaledHeight - 19;
        for (int i = 0; i < 9; i++) {
            int posX = center - 90 + i * 20 + 2;
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) continue;
            module.renderShulkerOverlay(context, posX, hotbarY, stack);
        }

        // Offhand slot
        ItemStack offhandStack = player.getOffHandStack();
        if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            int offY = scaledHeight - 20;
            int offX;
            if (player.getMainArm() == Arm.LEFT) {
                offX = center - 91 - 26;
            } else {
                offX = center + 94;
            }
            module.renderShulkerOverlay(context, offX, offY, offhandStack);
        }
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlay(DrawContext context, net.minecraft.util.Identifier texture, float opacity, CallbackInfo ci) {
        // This handles the red hurt overlay
        Modules modules = Modules.get();
        if (modules == null) return;
        
        NoHurtCam noHurtCam = modules.get(NoHurtCam.class);
        if (noHurtCam != null && noHurtCam.shouldDisableRedOverlay()) {
            // Cancel the red overlay rendering when hurt
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.player.hurtTime > 0) {
                ci.cancel();
            }
        }
    }
}

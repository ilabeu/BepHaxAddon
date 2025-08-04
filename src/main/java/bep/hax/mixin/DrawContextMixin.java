package bep.hax.mixin;

import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import bep.hax.modules.LoreLocator;
import bep.hax.modules.ItemSearchBar;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 **/
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {
    @Shadow
    public abstract void fill(int x1, int y1, int x2, int y2, int color);

    // See LoreLocator.java
    @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V", at = @At(value = "HEAD"))
    private void highlightNamedItems(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        
        // LoreLocator highlighting
        LoreLocator ll = modules.get(LoreLocator.class);
        if (ll.isActive() && ll.shouldHighlightSlot(stack)) {
            this.fill(x - 1, y - 1, x + 17, y + 17, ll.color.get().getPacked());
            return;
        }
        
        // ItemSearchBar highlighting
        ItemSearchBar isb = modules.get(ItemSearchBar.class);
        if (isb.isActive() && isb.shouldHighlightSlot(stack)) {
            this.fill(x - 1, y - 1, x + 17, y + 17, isb.highlightColor.get().getPacked());
        }
    }

    @Inject(method = "drawItemWithoutEntity(Lnet/minecraft/item/ItemStack;III)V", at = @At("HEAD"))
    private void highlightNamedItemsNoEntity(ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        
        // LoreLocator highlighting
        LoreLocator ll = modules.get(LoreLocator.class);
        if (ll.isActive() && ll.shouldHighlightSlot(stack)) {
            this.fill(x - 1, y - 1, x + 17, y + 17, ll.color.get().getPacked());
            return;
        }
        
        // ItemSearchBar highlighting
        ItemSearchBar isb = modules.get(ItemSearchBar.class);
        if (isb.isActive() && isb.shouldHighlightSlot(stack)) {
            this.fill(x - 1, y - 1, x + 17, y + 17, isb.highlightColor.get().getPacked());
        }
    }
}

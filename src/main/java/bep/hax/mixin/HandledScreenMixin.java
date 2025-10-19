package bep.hax.mixin;
import bep.hax.modules.ShulkerOverviewModule;
import bep.hax.modules.ItemSearchBar;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    protected HandledScreenMixin(Text title) {
        super(title);
    }
    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow public abstract ScreenHandler getScreenHandler();
    @Unique private TextFieldWidget itemSearchField;
    @Unique private ItemSearchBar itemSearchModule;
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        itemSearchModule = Modules.get().get(ItemSearchBar.class);
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        itemSearchField = new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            this.x + itemSearchModule.getOffsetX(), 
            this.y + itemSearchModule.getOffsetY(),
            itemSearchModule.getFieldWidth(), 
            itemSearchModule.getFieldHeight(),
            Text.of("Search items...")
        );
        itemSearchField.setPlaceholder(Text.of("Search items..."));
        itemSearchField.setMaxLength(100);
        itemSearchField.setChangedListener(text -> {
            if (itemSearchModule != null) {
                itemSearchModule.updateSearchQuery(text);
            }
        });
        itemSearchField.setFocused(false);
        itemSearchField.setEditable(true);
        itemSearchField.setVisible(true);
        this.addDrawableChild(itemSearchField);
    }
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        itemSearchField.setX(this.x + itemSearchModule.getOffsetX());
        itemSearchField.setY(this.y + itemSearchModule.getOffsetY());
        itemSearchField.render(context, mouseX, mouseY, delta);
    }
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        if (keyCode == 258) {
            itemSearchField.setFocused(true);
            cir.setReturnValue(true);
            return;
        }
        if (keyCode == 256 && itemSearchField.isFocused()) {
            itemSearchField.setFocused(false);
            cir.setReturnValue(true);
            return;
        }
        if (itemSearchField.isFocused()) {
            if (itemSearchField.keyPressed(keyCode, scanCode, modifiers)) {
                cir.setReturnValue(true);
                return;
            }
            if (keyCode != 256) {
                cir.setReturnValue(true);
            }
        }
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (itemSearchModule == null || !itemSearchModule.isActive() || !itemSearchModule.shouldShowSearchField()) return;
        if (itemSearchField == null) return;
        boolean clickedOnField = mouseX >= itemSearchField.getX() && 
                                mouseX < itemSearchField.getX() + itemSearchField.getWidth() &&
                                mouseY >= itemSearchField.getY() && 
                                mouseY < itemSearchField.getY() + itemSearchField.getHeight();
        if (clickedOnField) {
            itemSearchField.setFocused(true);
            if (itemSearchField.mouseClicked(mouseX, mouseY, button)) {
                cir.setReturnValue(true);
                return;
            }
        } else {
            itemSearchField.setFocused(false);
        }
    }
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (itemSearchModule != null && itemSearchModule.isActive() && itemSearchModule.shouldShowSearchField()) {
            if (itemSearchField != null && itemSearchField.isFocused()) {
                if (itemSearchField.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return super.charTyped(chr, modifiers);
    }
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ShulkerOverviewModule module = Modules.get().get(ShulkerOverviewModule.class);
        if (module == null || !module.isActive()) return;
        module.renderShulkerOverlay(context, slot.x, slot.y, slot.getStack());
    }
}

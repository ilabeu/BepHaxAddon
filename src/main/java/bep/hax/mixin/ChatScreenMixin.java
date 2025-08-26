package bep.hax.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.Modules;
import bep.hax.modules.WebChat;
import net.minecraft.client.gui.DrawContext;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected TextFieldWidget chatField;
    
    protected ChatScreenMixin(Text title) {
        super(title);
    }
    
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules != null) {
            WebChat webChat = modules.get(WebChat.class);
            if (webChat != null && webChat.isActive() && webChat.shouldHideInGameChat()) {
                // Only render the input field, not the chat history
                if (this.chatField != null) {
                    // Draw a semi-transparent background for the input field
                    context.fill(2, this.height - 14 - 2, this.width - 2, this.height - 2, 0x80000000);
                    
                    // Render only the chat input field
                    this.chatField.render(context, mouseX, mouseY, delta);
                }
                ci.cancel();
            }
        }
    }
    
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules != null) {
            WebChat webChat = modules.get(WebChat.class);
            if (webChat != null && webChat.isActive() && webChat.shouldHideInGameChat()) {
                // Make sure the chat field is properly positioned
                if (this.chatField != null) {
                    this.chatField.setY(this.height - 12);
                }
            }
        }
    }
}
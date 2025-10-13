package bep.hax.mixin;
import bep.hax.modules.AntiToS;
import bep.hax.modules.ElytraFlyPlusPlus;
import bep.hax.util.CapeManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class EntityMixin {
    @Mixin(Entity.class)
    public static class EntityHooks {
        @Shadow
        protected UUID uuid;
        ElytraFlyPlusPlus efly = Modules.get().get(ElytraFlyPlusPlus.class);
        @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/Entity;getPose()Lnet/minecraft/entity/EntityPose;", cancellable = true)
        private void getPose(CallbackInfoReturnable<EntityPose> cir) {
            if (efly != null && efly.enabled() && this.uuid == mc.player.getUuid()) {
                cir.setReturnValue(EntityPose.STANDING);
            }
        }
        @Inject(at = @At("HEAD"), method = "Lnet/minecraft/entity/Entity;isSprinting()Z", cancellable = true)
        private void isSprinting(CallbackInfoReturnable<Boolean> cir) {
            if (efly != null && efly.enabled() && this.uuid == mc.player.getUuid()) {
                cir.setReturnValue(true);
            }
        }
        @Inject(at = @At("HEAD"), method = "pushAwayFrom", cancellable = true)
        private void pushAwayFrom(Entity entity, CallbackInfo ci) {
            if (mc.player != null && this.uuid == mc.player.getUuid() && efly != null && efly.enabled() && !entity.getUuid().equals(this.uuid)) {
                ci.cancel();
            }
        }
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
                    if (manager.hasTradingCape(player)) {
                        cir.setReturnValue(Formatting.GREEN.getColorValue());
                    } else if (manager.hasFriendCape(player)) {
                        cir.setReturnValue(Formatting.RED.getColorValue());
                    } else {
                        cir.setReturnValue(Formatting.AQUA.getColorValue());
                    }
                }
            }
        }
    }
    @Mixin(EntityRenderer.class)
    public static abstract class EntityRendererHooks {
        @ModifyVariable(method = "renderLabelIfPresent", at = @At("HEAD"), argsOnly = true)
        private Text censorEntityName(Text name) {
            Modules modules = Modules.get();
            if (modules == null) return name;
            AntiToS antiToS = modules.get(AntiToS.class);
            if (!antiToS.isActive()) return name;
            if (!antiToS.containsBlacklistedText(name.getString())) return name;
            return Text.literal(antiToS.censorText(name.getString())).setStyle(name.getStyle());
        }
        @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
        private void shouldRender(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
            if (!(entity instanceof PlayerEntity player)) return;
            Modules mods = Modules.get();
            if (mods == null) return;
            NoRender noRender = mods.get(NoRender.class);
            if (!noRender.isActive()) return;
            var codySetting = noRender.settings.get("cody");
            if (codySetting != null && (boolean) codySetting.get() && player.getGameProfile().getName().equals("codysmile11")) {
                cir.setReturnValue(false);
                cir.cancel();
            }
        }
    }
}
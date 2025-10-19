package bep.hax.mixin.meteor;
import bep.hax.util.EnemyManager;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTab;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.jetbrains.annotations.Nullable;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import meteordevelopment.meteorclient.settings.*;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import meteordevelopment.meteorclient.systems.modules.render.Nametags;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import static meteordevelopment.meteorclient.MeteorClient.mc;
@Mixin(value = Nametags.class, remap = false)
public abstract class NametagsMixin extends Module {
    @Shadow
    @Final
    private SettingGroup sgGeneral;
    public NametagsMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Unique
    private @Nullable Setting<Boolean> forceDefaultFont = null;
    @Unique
    private @Nullable Setting<Boolean> bephax$pearlOwner = null;
    @Unique
    private @Nullable Setting<Keybind> bephax$pearlOwnerKeybind = null;
    @Unique
    private @Nullable Setting<Double> bephax$pearlMaxDistance = null;
    @Unique
    private @Nullable Setting<Double> bephax$pearlScale = null;
    @Unique
    private @Nullable Setting<SettingColor> bephax$pearlNameColor = null;
    @Unique
    private @Nullable Setting<SettingColor> bephax$pearlBackgroundColor = null;
    @Unique
    private @Nullable Setting<Boolean> bephax$pearlShowDistance = null;
    @Unique
    private @Nullable Setting<Boolean> bephax$pearlCulling = null;
    @Unique
    private @Nullable Setting<Double> bephax$pearlCullingDotValue = null;
    @Unique
    private final Vector3d bephax$pearlPos = new Vector3d();
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lmeteordevelopment/meteorclient/systems/modules/render/Nametags;scale:Lmeteordevelopment/meteorclient/settings/Setting;", shift = At.Shift.AFTER))
    private void addDefaultFontSettings(CallbackInfo ci) {
        forceDefaultFont = sgGeneral.add(
            new BoolSetting.Builder()
                .name("force-default-font")
                .description("Force nametags to render using the default font, even if a custom GUI font is selected in your Meteor config.")
                .defaultValue(false)
                .build()
        );
        bephax$pearlOwner = sgGeneral.add(
            new BoolSetting.Builder()
                .name("pearl-owner")
                .description("Displays the owner name on thrown ender pearls.")
                .defaultValue(true)
                .build()
        );
        bephax$pearlOwnerKeybind = sgGeneral.add(
            new KeybindSetting.Builder()
                .name("pearl-owner-keybind")
                .description("Keybind to toggle showing pearl owner names.")
                .defaultValue(Keybind.none())
                .action(() -> {
                    if (bephax$pearlOwner != null) {
                        bephax$pearlOwner.set(!bephax$pearlOwner.get());
                    }
                })
                .build()
        );
        SettingGroup sgPearlOwner = settings.createGroup("Pearl Owner");
        bephax$pearlMaxDistance = sgPearlOwner.add(new DoubleSetting.Builder()
            .name("max-distance")
            .description("Maximum distance to display pearl owner names.")
            .defaultValue(500.0)
            .min(0.0)
            .sliderRange(0.0, 1000.0)
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlScale = sgPearlOwner.add(new DoubleSetting.Builder()
            .name("pearl-scale")
            .description("The scale of the pearl owner nametag.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 5.0)
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlNameColor = sgPearlOwner.add(new ColorSetting.Builder()
            .name("pearl-name-color")
            .description("The color of the owner's name.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlBackgroundColor = sgPearlOwner.add(new ColorSetting.Builder()
            .name("pearl-background-color")
            .description("The color of the nametag background.")
            .defaultValue(new SettingColor(170, 0, 255, 49))
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlShowDistance = sgPearlOwner.add(new BoolSetting.Builder()
            .name("pearl-show-distance")
            .description("Show distance to the pearl.")
            .defaultValue(true)
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlCulling = sgPearlOwner.add(new BoolSetting.Builder()
            .name("pearl-culling")
            .description("Only render nametags when you're looking at the pearl.")
            .defaultValue(false)
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get())
            .build()
        );
        bephax$pearlCullingDotValue = sgPearlOwner.add(new DoubleSetting.Builder()
            .name("pearl-culling-dot-value")
            .description("Dot product value for culling.")
            .defaultValue(0.5)
            .min(-1.0)
            .max(1.0)
            .sliderRange(-1.0, 1.0)
            .visible(() -> bephax$pearlOwner != null && bephax$pearlOwner.get() && bephax$pearlCulling != null && bephax$pearlCulling.get())
            .build()
        );
    }
    @Inject(method = "renderNametagPlayer", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;get()Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;", shift = At.Shift.AFTER))
    private void injectDefaultFont(Render2DEvent event, PlayerEntity player, boolean shadow, CallbackInfo ci, @Local(ordinal = 0) LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }
    @Inject(method = "renderNametagItem", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;get()Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;", shift = At.Shift.AFTER))
    private void injectDefaultFontForItemNametags(net.minecraft.item.ItemStack stack, boolean shadow, CallbackInfo ci, @Local(ordinal = 0) LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }
    @Inject(method = "renderGenericNametag", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;get()Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;", shift = At.Shift.AFTER))
    private void injectDefaultFontForGenericNametags(net.minecraft.entity.Entity entity, boolean shadow, CallbackInfo ci, @Local(ordinal = 0) LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }
    @Inject(method = "renderTntNametag", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;get()Lmeteordevelopment/meteorclient/renderer/text/TextRenderer;", shift = At.Shift.AFTER))
    private void injectDefaultFontForTNTNametags(String fuseText, boolean shadow, CallbackInfo ci, @Local(ordinal = 0) LocalRef<TextRenderer> text) {
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            text.set(VanillaTextRenderer.INSTANCE);
        }
    }
    @Unique
    @EventHandler
    private void onRender2DPearls(Render2DEvent event) {
        if (!isActive() || mc.world == null || mc.player == null) return;
        if (bephax$pearlOwner == null || !bephax$pearlOwner.get()) return;
        if (bephax$pearlScale == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EnderPearlEntity pearl)) continue;
            double distance = mc.player.distanceTo(pearl);
            if (bephax$pearlMaxDistance != null && distance > bephax$pearlMaxDistance.get()) continue;

            if (bephax$pearlCulling != null && bephax$pearlCulling.get()) {
                Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
                Vec3d pearlPos = pearl.getPos();
                Vec3d cameraToEntity = pearlPos.subtract(cameraPos).normalize();
                Vec3d cameraDirection = Vec3d.fromPolar(mc.gameRenderer.getCamera().getPitch(), mc.gameRenderer.getCamera().getYaw()).normalize();
                double dot = cameraDirection.dotProduct(cameraToEntity);
                if (bephax$pearlCullingDotValue != null && dot < bephax$pearlCullingDotValue.get()) continue;
            }

            Entity owner = pearl.getOwner();
            if (owner == null) continue;

            String ownerName = owner.getName().getString();
            if (bephax$pearlShowDistance != null && bephax$pearlShowDistance.get()) {
                ownerName += String.format(" [%.1fm]", distance);
            }

            bephax$pearlPos.set(pearl.getX(), pearl.getY() + pearl.getHeight() + 0.5, pearl.getZ());
            bephax$renderPearlNametag(ownerName);
        }
    }

    @Unique
    private void bephax$renderPearlNametag(String text) {
        if (bephax$pearlNameColor == null || bephax$pearlBackgroundColor == null || bephax$pearlScale == null) return;

        TextRenderer textRenderer;
        boolean shadow;
        if (forceDefaultFont != null && forceDefaultFont.get()) {
            textRenderer = VanillaTextRenderer.INSTANCE;
            shadow = false;
        } else {
            textRenderer = TextRenderer.get();
            shadow = Config.get().customFont.get();
        }

        if (!NametagUtils.to2D(bephax$pearlPos, bephax$pearlScale.get())) return;

        NametagUtils.begin(bephax$pearlPos);

        double textWidth = textRenderer.getWidth(text, shadow);
        double textHeight = textRenderer.getHeight(shadow);
        double widthHalf = textWidth / 2;

        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(-widthHalf - 1, -textHeight, textWidth + 2, textHeight + 2, bephax$pearlBackgroundColor.get());
        Renderer2D.COLOR.render(null);

        textRenderer.beginBig();
        textRenderer.render(text, -widthHalf, -textHeight, bephax$pearlNameColor.get(), shadow);
        textRenderer.end();

        NametagUtils.end();
    }
}

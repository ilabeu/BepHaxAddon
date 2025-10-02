package bep.hax.mixin.xaero;

import xaero.common.HudMod;
import xaero.common.misc.Misc;
import xaero.common.effect.Effects;
import xaero.common.gui.IScreenBase;
import bep.hax.modules.Solitaire;
import bep.hax.modules.Meteorites;
import bep.hax.modules.Minesweeper;
import org.spongepowered.asm.mixin.Mixin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.gui.DrawContext;
import xaero.hud.minimap.module.MinimapSession;
import bep.hax.gui.screens.SolitaireScreen;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.module.MinimapRenderer;
import bep.hax.gui.screens.MeteoritesScreen;
import net.minecraft.client.gui.screen.ChatScreen;
import bep.hax.gui.screens.MinesweeperScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import xaero.hud.render.module.ModuleRenderContext;
import org.spongepowered.asm.mixin.injection.Inject;
import xaero.common.minimap.render.MinimapRendererHelper;
import static meteordevelopment.meteorclient.MeteorClient.mc;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Tas [0xTas] <root@0xTas.dev>
 *     Forces the minimap to remain rendered while playing in-client minigames.
 **/
@Mixin(value = MinimapRenderer.class, remap = false)
public class MinimapRendererMixin {
    @Unique
    private @Nullable Solitaire solitaire = null;

    @Unique
    private @Nullable Meteorites meteorites = null;

    @Unique
    private @Nullable Minesweeper minesweeper = null;

    @Inject(
        method = "render(Lxaero/hud/minimap/module/MinimapSession;Lxaero/hud/render/module/ModuleRenderContext;Lnet/minecraft/client/gui/DrawContext;F)V",
        at = @At("HEAD"), cancellable = true, remap = true
    )
    private void forceRenderMinimapDuringMinigames(MinimapSession session, ModuleRenderContext c, DrawContext guiGraphics, float partialTicks, CallbackInfo ci) {
        if (mc == null) return;
        if (Misc.hasEffect(mc.player, Effects.NO_MINIMAP) && Misc.hasEffect(mc.player, Effects.NO_MINIMAP_HARMFUL)) return;
        if (meteorites == null || minesweeper == null || solitaire == null) {
            Modules mods = Modules.get();
            if (mods == null) return;

            solitaire = mods.get(Solitaire.class);
            meteorites = mods.get(Meteorites.class);
            minesweeper = mods.get(Minesweeper.class);
            if (meteorites == null || minesweeper == null || solitaire == null) return;
        }

        boolean allowedByDefault = (!session.getHideMinimapUnderF3() || !mc.getDebugHud().shouldShowDebugHud())
            && (!session.getHideMinimapUnderScreen() || mc.currentScreen == null || mc.currentScreen instanceof IScreenBase
            || mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof DeathScreen);

        if (allowedByDefault) return;
        boolean force = mc.currentScreen instanceof MeteoritesScreen && meteorites.renderMap.get();
        if (mc.currentScreen instanceof SolitaireScreen && solitaire.renderMap.get()) force = true;
        if (mc.currentScreen instanceof MinesweeperScreen && minesweeper.renderMap.get()) force = true;

        if (force) {
            ci.cancel();
            MinimapRendererHelper.restoreDefaultShaderBlendState();
            session.getProcessor().onRender(
                guiGraphics, c.x, c.y,c.screenWidth, c.screenHeight, c.screenScale,
                session.getConfiguredWidth(), c.w, partialTicks,
                HudMod.INSTANCE.getHudRenderer().getCustomVertexConsumers()
            );
            MinimapRendererHelper.restoreDefaultShaderBlendState();
        }
    }
}

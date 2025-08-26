package bep.hax.modules;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import bep.hax.Bep;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.ArrayList;

public class ShulkerFrameESP extends Module {
    // Settings for colors and rendering options
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> fillColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("Color of sides.")
        .defaultValue(new SettingColor(152, 98, 43, 50))
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Color of lines.")
        .defaultValue(new SettingColor(85, 43, 19, 255))
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Tracer Color")
        .description("Color of tracer line.")
        .defaultValue(new SettingColor(166, 150, 101, 255))
        .build()
    );

    private final Setting<Boolean> renderFill = sgGeneral.add(new BoolSetting.Builder()
        .name("Render Sides")
        .description("Render sides of frames.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderOutline = sgGeneral.add(new BoolSetting.Builder()
        .name("Render Lines")
        .description("Render lines of frames.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderTracer = sgGeneral.add(new BoolSetting.Builder()
        .name("Tracers")
        .description("Add tracers to frames.")
        .defaultValue(true)
        .build()
    );

    public ShulkerFrameESP() {
        super(Bep.CATEGORY, "Shulker-Frame-ESP", "Highlights ItemFrames and GlowItemFrames containing any Shulkerbox.");
    }

    // Helper: Checks if the given ItemStack is any shulkerbox
    private boolean isShulkerBox(ItemStack stack) {
        if (stack == null) return false;
        // All vanilla shulkerboxes
        return stack.getItem() == Items.SHULKER_BOX
            || stack.getItem() == Items.WHITE_SHULKER_BOX
            || stack.getItem() == Items.ORANGE_SHULKER_BOX
            || stack.getItem() == Items.MAGENTA_SHULKER_BOX
            || stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX
            || stack.getItem() == Items.YELLOW_SHULKER_BOX
            || stack.getItem() == Items.LIME_SHULKER_BOX
            || stack.getItem() == Items.PINK_SHULKER_BOX
            || stack.getItem() == Items.GRAY_SHULKER_BOX
            || stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX
            || stack.getItem() == Items.CYAN_SHULKER_BOX
            || stack.getItem() == Items.PURPLE_SHULKER_BOX
            || stack.getItem() == Items.BLUE_SHULKER_BOX
            || stack.getItem() == Items.BROWN_SHULKER_BOX
            || stack.getItem() == Items.GREEN_SHULKER_BOX
            || stack.getItem() == Items.RED_SHULKER_BOX
            || stack.getItem() == Items.BLACK_SHULKER_BOX;
    }

    // Finds all ItemFrames/GlowItemFrames with a shulkerbox inside
    private List<Entity> getShulkerFrames() {
        List<Entity> result = new ArrayList<>();
        if (mc.world == null) return result;
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity || entity instanceof GlowItemFrameEntity) {
                ItemStack stack = ((ItemFrameEntity) entity).getHeldItemStack();
                if (isShulkerBox(stack)) result.add(entity);
            }
        }
        return result;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        List<Entity> frames = getShulkerFrames();
        if (frames.isEmpty()) return;

        meteordevelopment.meteorclient.renderer.ShapeMode shapeMode;
        if (renderFill.get() && renderOutline.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Both;
        else if (renderFill.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Sides;
        else if (renderOutline.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Lines;
        else return;

        for (Entity frame : frames) {
            Box box = frame.getBoundingBox();
            event.renderer.box(
                box,
                fillColor.get(),
                outlineColor.get(),
                shapeMode,
                0
            );
            if (renderTracer.get()) {
                // Use the same logic as OminousVaultESP for tracer start
                event.renderer.line(
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.x,
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.y,
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.z,
                    box.getCenter().x, box.getCenter().y, box.getCenter().z,
                    tracerColor.get()
                );
            }
        }
    }
}

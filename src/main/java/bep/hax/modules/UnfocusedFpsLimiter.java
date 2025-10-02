package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class UnfocusedFpsLimiter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> unfocusedFps = sgGeneral.add(new IntSetting.Builder()
        .name("unfocused-fps")
        .description("The FPS limit when the game window is not focused.")
        .defaultValue(10)
        .min(10)
        .max(260)
        .sliderRange(10, 60)
        .build()
    );

    private int originalFps = -1;
    private boolean wasFocused = true;
    private boolean hasStoredOriginal = false;

    public UnfocusedFpsLimiter() {
        super(Bep.STASH, "unfocused-fps", "Limits the FPS when the game is unfocused.");
    }

    @Override
    public void onActivate() {
        if (mc.options != null) {
            originalFps = mc.options.getMaxFps().getValue();
            hasStoredOriginal = true;
            wasFocused = mc.isWindowFocused();

            // If already unfocused on activation, apply the limit
            if (!wasFocused) {
                setFpsLimit(unfocusedFps.get());
            }
        }
    }

    @Override
    public void onDeactivate() {
        if (hasStoredOriginal && mc.options != null) {
            setFpsLimit(originalFps);
        }
        hasStoredOriginal = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.options == null || !hasStoredOriginal) return;

        boolean focused = mc.isWindowFocused();

        // Only update when focus state changes
        if (focused != wasFocused) {
            if (focused) {
                // Window regained focus - restore original FPS
                setFpsLimit(originalFps);
            } else {
                // Window lost focus - apply unfocused FPS limit
                // Store current FPS as original if window was focused
                if (wasFocused) {
                    originalFps = mc.options.getMaxFps().getValue();
                }
                setFpsLimit(unfocusedFps.get());
            }
            wasFocused = focused;
        }
    }

    private void setFpsLimit(int fps) {
        // Ensure FPS is within valid Minecraft range
        // Minecraft uses specific values: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, then up to 260
        int validFps = fps;

        // Clamp to valid range
        if (validFps < 10) validFps = 10;
        if (validFps > 260) validFps = 260;

        // Round to nearest valid increment
        if (validFps <= 120) {
            validFps = Math.round(validFps / 10.0f) * 10;
        }

        try {
            mc.options.getMaxFps().setValue(validFps);
        } catch (Exception e) {
            // Silently ignore if the value is rejected
        }
    }
}

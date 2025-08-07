package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;

/**
 * @author OLEPOSSU
 * SwingModifier - Modifies swing rendering with customizable animations
 * Ported from BlackOut, self-contained implementation
 */

public class SwingModifier extends Module {
    public SwingModifier() {
        super(Bep.CATEGORY, "Swing Modifier", "Modifies swing rendering.");
    }

    private final SettingGroup sgMainHand = settings.createGroup("Main Hand");
    private final SettingGroup sgOffHand = settings.createGroup("Off Hand");

    //--------------------Main-Hand--------------------//
    private final Setting<Double> mSpeed = sgMainHand.add(new DoubleSetting.Builder()
        .name("Main Speed")
        .description("Speed of swinging.")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> mStart = sgMainHand.add(new DoubleSetting.Builder()
        .name("Main Start Progress")
        .description("Starts swing at this progress.")
        .defaultValue(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> mEnd = sgMainHand.add(new DoubleSetting.Builder()
        .name("Main End Progress")
        .description("Swings until reaching this progress.")
        .defaultValue(1)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> myStart = sgMainHand.add(new DoubleSetting.Builder()
        .name("Main Start Y")
        .description("Hand Y value in the beginning.")
        .defaultValue(0)
        .sliderRange(-10, 10)
        .build()
    );
    private final Setting<Double> myEnd = sgMainHand.add(new DoubleSetting.Builder()
        .name("Main End Y")
        .description("Hand Y value in the end.")
        .defaultValue(0)
        .sliderRange(-10, 10)
        .build()
    );
    private final Setting<Boolean> mReset = sgMainHand.add(new BoolSetting.Builder()
        .name("Reset")
        .description("Resets swing when swinging again.")
        .defaultValue(false)
        .build()
    );

    //--------------------Off-Hand--------------------//
    private final Setting<Double> oSpeed = sgOffHand.add(new DoubleSetting.Builder()
        .name("Off Speed")
        .description("Speed of swinging for offhand")
        .defaultValue(1)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> oStart = sgOffHand.add(new DoubleSetting.Builder()
        .name("Off Start Progress")
        .description("Starts swing at this progress.")
        .defaultValue(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> oEnd = sgOffHand.add(new DoubleSetting.Builder()
        .name("Off End Progress")
        .description("Swings until reaching this progress.")
        .defaultValue(1)
        .sliderMax(10)
        .build()
    );
    private final Setting<Double> oyStart = sgOffHand.add(new DoubleSetting.Builder()
        .name("Off Start Y")
        .description("Start Y value for offhand.")
        .defaultValue(0)
        .sliderRange(-10, 10)
        .build()
    );
    private final Setting<Double> oyEnd = sgOffHand.add(new DoubleSetting.Builder()
        .name("Off End Y")
        .description("End Y value for offhand.")
        .defaultValue(0)
        .sliderRange(-10, 10)
        .build()
    );
    private final Setting<Boolean> oReset = sgOffHand.add(new BoolSetting.Builder()
        .name("Reset")
        .description("Resets swing when swinging again.")
        .defaultValue(false)
        .build()
    );

    // Swing state tracking - using static to maintain state across renders
    private static boolean mainSwinging = false;
    private static float mainProgress = 0;
    private static boolean offSwinging = false;
    private static float offProgress = 0;

    /**
     * Called when the player swings their hand
     * Triggered by ClientPlayerEntityMixin
     */
    public void startSwing(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            if (mReset.get() || !mainSwinging) {
                mainProgress = 0;
                mainSwinging = true;
            }
        } else {
            if (oReset.get() || !offSwinging) {
                offProgress = 0;
                offSwinging = true;
            }
        }
    }

    /**
     * Updates swing progress every render frame
     * Uses frameTime for smooth, frame-independent animation
     */
    @EventHandler
    public void onRender(Render3DEvent event) {
        // Update main hand swing progress
        if (mainSwinging) {
            if (mainProgress >= 1) {
                mainSwinging = false;
                mainProgress = 0;
            } else {
                mainProgress += event.frameTime * mSpeed.get();
                if (mainProgress > 1) mainProgress = 1;
            }
        }

        // Update off hand swing progress
        if (offSwinging) {
            if (offProgress >= 1) {
                offSwinging = false;
                offProgress = 0;
            } else {
                offProgress += event.frameTime * oSpeed.get();
                if (offProgress > 1) offProgress = 1;
            }
        }
    }

    /**
     * Gets the current swing progress for rendering
     * Called by MixinHeldItemRenderer
     */
    public float getSwing(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            // Return the interpolated swing progress
            return (float) (mStart.get() + (mEnd.get() - mStart.get()) * mainProgress);
        } else {
            // Return the interpolated swing progress
            return (float) (oStart.get() + (oEnd.get() - oStart.get()) * offProgress);
        }
    }
    
    /**
     * Gets modified swing progress without knowing the hand
     * Will apply to the hand that's currently swinging
     */
    public float getModifiedSwingProgress(float originalProgress) {
        // Check if either hand is swinging and return modified progress
        if (mainSwinging && mainProgress > 0) {
            return (float) (mStart.get() + (mEnd.get() - mStart.get()) * mainProgress);
        }
        if (offSwinging && offProgress > 0) {
            return (float) (oStart.get() + (oEnd.get() - oStart.get()) * offProgress);
        }
        return originalProgress;
    }
    
    /**
     * Check if a hand is currently swinging
     */
    public boolean isSwinging(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            return mainSwinging || mainProgress > 0;
        }
        return offSwinging || offProgress > 0;
    }
    
    /**
     * Whether to modify equipment progress
     */
    public boolean shouldModifyEquipProgress() {
        return false; // Can be extended later if needed
    }
    
    /**
     * Gets equipment progress for the hand
     */
    public float getEquipProgress(Hand hand, float originalProgress) {
        return originalProgress; // Can be extended later if needed
    }

    /**
     * Gets the Y offset for hand positioning
     * Called by MixinHeldItemRenderer
     */
    public float getY(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            return (float) (myStart.get() + (myEnd.get() - myStart.get()) * mainProgress) / -10f;
        }
        return (float) (oyStart.get() + (oyEnd.get() - oyStart.get()) * offProgress) / -10f;
    }
    
    @Override
    public void onActivate() {
        // Reset state when module is activated
        mainSwinging = false;
        mainProgress = 0;
        offSwinging = false;
        offProgress = 0;
    }
}
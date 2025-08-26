package bep.hax.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import bep.hax.Bep;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import java.util.List;

public class InventoryNotif extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");

    // General settings
    public final Setting<TriggerMode> triggerMode = sgGeneral.add(new EnumSetting.Builder<TriggerMode>()
        .name("trigger-mode")
        .description("When to play the sound.")
        .defaultValue(TriggerMode.CONTINUOUS)
        .build()
    );

    public final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Cooldown between sounds in seconds.")
        .defaultValue(5)
        .range(1, 30)
        .sliderRange(1, 30)
        .visible(() -> triggerMode.get() == TriggerMode.CONTINUOUS)
        .build()
    );

    // Sound settings
    public final Setting<List<SoundEvent>> sound = sgSound.add(new SoundEventListSetting.Builder()
        .name("sound")
        .description("Sound to play when inventory is full.")
        .defaultValue(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value())
        .build()
    );

    public final Setting<Integer> volume = sgSound.add(new IntSetting.Builder()
        .name("volume")
        .description("Volume of the sound.")
        .defaultValue(100)
        .range(0, 200)
        .sliderRange(0, 200)
        .build()
    );

    public final Setting<Integer> pitch = sgSound.add(new IntSetting.Builder()
        .name("pitch")
        .description("Pitch of the sound.")
        .defaultValue(100)
        .range(50, 200)
        .sliderRange(50, 200)
        .build()
    );

    private boolean wasInventoryFull = false;
    private int cooldownTicks = 0;

    public InventoryNotif() {
        super(Bep.CATEGORY, "Inventory Notifs", "Plays a sound when inventory is full.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        boolean isInventoryFull = isInventoryFull();

        switch (triggerMode.get()) {
            case CONTINUOUS -> handleContinuousMode(isInventoryFull);
            case ON_CHANGE -> handleOnChangeMode(isInventoryFull);
        }

        wasInventoryFull = isInventoryFull;
    }

    private void handleContinuousMode(boolean isInventoryFull) {
        if (!isInventoryFull) {
            cooldownTicks = 0;
            return;
        }

        if (cooldownTicks <= 0) {
            playSound();
            cooldownTicks = cooldown.get() * 20; // Convert seconds to ticks
        } else {
            cooldownTicks--;
        }
    }

    private void handleOnChangeMode(boolean isInventoryFull) {
        if (isInventoryFull && !wasInventoryFull) {
            playSound();
        }
    }

    // removed PICKUP_ONLY mode

    private boolean isInventoryFull() {
        // Check main inventory slots (0-35), excluding offhand and armor
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void playSound() {
        if (mc.world == null) return;

        List<SoundEvent> selectedSounds = sound.get();
        if (selectedSounds.isEmpty()) return;

        float volumeFloat = volume.get() / 100.0f;
        float pitchFloat = pitch.get() / 100.0f;

        mc.world.playSound(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            selectedSounds.get(0),
            SoundCategory.MASTER,
            volumeFloat,
            pitchFloat,
            false
        );
    }

    public enum TriggerMode {
        CONTINUOUS("Continuous"),
        ON_CHANGE("On Change");

        private final String title;
        TriggerMode(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }
}

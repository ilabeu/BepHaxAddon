package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class YawLock extends Module {
    private float lockedYaw;

    public YawLock() {
        super(Bep.CATEGORY, "yaw-lock", "Locks your yaw to the closest 45-degree increment.");
    }

    @Override
    public void onActivate() {
        float currentYaw = mc.player.getYaw();
        // Normalize yaw to 0-360
        float normalizedYaw = (currentYaw % 360 + 360) % 360;
        // Find closest 45-degree multiple
        int closestMultiple = Math.round(normalizedYaw / 45f);
        lockedYaw = (closestMultiple * 45) % 360;
        // Convert back to -180 to 180 range if necessary
        if (lockedYaw > 180) lockedYaw -= 360;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.player.setYaw(lockedYaw);
        if (mc.player.hasVehicle()) {
            mc.player.getVehicle().setYaw(lockedYaw);
        }
    }
}

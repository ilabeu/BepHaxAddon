package bep.hax.modules;

import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import bep.hax.util.PVPRotationManager;

import java.util.Random;

/**
 * Base class for all modules
 * Extends Meteor's Module class with PVP-specific enhancements
 */
public abstract class PVPModule extends Module {

    // Random instance for modules - thread-safe implementation
    protected static final Random RANDOM = new Random();

    protected PVPModule(Category category, String name, String description) {
        super(category, name, description);
    }

    /**
     * Formats module data for HUD display
     * Override in subclasses to provide custom formatting
     */
    @Override
    public String getInfoString() {
        return null; // Default: no info string
    }

    /**
     * Helper method to check if player is in valid state for module operations
     */
    protected boolean isValidPlayer() {
        return mc.player != null && mc.world != null && !mc.player.isRemoved();
    }

    /**
     * Helper method to check if we're in a valid world (not in menu/loading)
     */
    protected boolean isInWorld() {
        return mc.world != null && mc.player != null;
    }

    /**
     * Safely toggle module off with validation
     */
    protected void safeToggle() {
        if (isActive()) {
            toggle();
        }
    }

    // Rotation methods from original RotationModule
    protected void setRotation(float yaw, float pitch) {
        PVPRotationManager.getInstance().setRotationClient(yaw, pitch);
    }

    protected void setRotationSilent(float yaw, float pitch) {
        PVPRotationManager.getInstance().setRotationSilent(yaw, pitch);
    }

    protected void setRotationClient(float yaw, float pitch) {
        PVPRotationManager.getInstance().setRotationClient(yaw, pitch);
    }
}

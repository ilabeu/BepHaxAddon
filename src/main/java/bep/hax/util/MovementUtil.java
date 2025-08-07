package bep.hax.util;

import net.minecraft.client.input.Input;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Original PVP MovementUtil - exact implementation for 2b2t compatibility
 * Contains critical movement calculations and safewalk functionality
 */
public class MovementUtil {

    /**
     * Checks if movement keys are being pressed
     * @return true if any movement key is pressed
     */
    public static boolean isInputtingMovement() {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    /**
     * Checks if there's movement input from the player
     * @return true if player is providing movement input
     */
    public static boolean isMovingInput() {
        return mc.player.input.movementForward != 0.0f
                || mc.player.input.movementSideways != 0.0f;
    }

    /**
     * Checks if the player is actually moving
     * @return true if player position has changed
     */
    public static boolean isMoving() {
        double d = mc.player.getX() - mc.player.lastX;
        double e = mc.player.getY() - mc.player.lastBaseY;
        double f = mc.player.getZ() - mc.player.lastZ;
        return MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0e-4);
    }

    /**
     * Applies sneak movement modifier with Swift Sneak enchantment support
     */
    public static void applySneak() {
        final float modifier = MathHelper.clamp(
            0.3f + (meteordevelopment.meteorclient.utils.Utils.getEnchantmentLevel(
                mc.player.getEquippedStack(EquipmentSlot.FEET), Enchantments.SWIFT_SNEAK) * 0.15F),
            0.0f, 1.0f);
        mc.player.input.movementForward *= modifier;
        mc.player.input.movementSideways *= modifier;
    }

    /**
     * Critical safewalk implementation for 2b2t - prevents falling off edges
     * This is ESSENTIAL for survival on 2b2t and must be preserved exactly
     * @param motionX X motion
     * @param motionZ Z motion
     * @return Safe motion vector
     */
    public static Vec2f applySafewalk(final double motionX, final double motionZ) {
        final double offset = 0.05;

        double moveX = motionX;
        double moveZ = motionZ;

        float fallDist = -mc.player.getStepHeight();
        if (!mc.player.isOnGround()) {
            fallDist = -1.5f;
        }

        while (moveX != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(moveX, fallDist, 0.0))) {
            if (moveX < offset && moveX >= -offset) {
                moveX = 0.0;
            } else if (moveX > 0.0) {
                moveX -= offset;
            } else {
                moveX += offset;
            }
        }

        while (moveZ != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, fallDist, moveZ))) {
            if (moveZ < offset && moveZ >= -offset) {
                moveZ = 0.0;
            } else if (moveZ > 0.0) {
                moveZ -= offset;
            } else {
                moveZ += offset;
            }
        }

        while (moveX != 0.0 && moveZ != 0.0 && mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(moveX, fallDist, moveZ))) {
            if (moveX < offset && moveX >= -offset) {
                moveX = 0.0;
            } else if (moveX > 0.0) {
                moveX -= offset;
            } else {
                moveX += offset;
            }

            if (moveZ < offset && moveZ >= -offset) {
                moveZ = 0.0;
            } else if (moveZ > 0.0) {
                moveZ -= offset;
            } else {
                moveZ += offset;
            }
        }

        return new Vec2f((float) moveX, (float) moveZ);
    }

    /**
     * Gets the yaw offset based on input for movement calculations
     * @param input Player input
     * @param rotationYaw Current yaw
     * @return Adjusted yaw for movement
     */
    public static float getYawOffset(Input input, float rotationYaw) {
        if (input.movementForward < 0.0f) rotationYaw += 180.0f;

        float forward = 1.0f;
        if (input.movementForward < 0.0f) {
            forward = -0.5f;
        } else if (input.movementForward > 0.0f) {
            forward = 0.5f;
        }

        float strafe = input.movementSideways;
        if (strafe > 0.0f) rotationYaw -= 90.0f * forward;
        if (strafe < 0.0f) rotationYaw += 90.0f * forward;
        return rotationYaw;
    }
}

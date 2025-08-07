package bep.hax.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Rotation utility functions for combat modules - exact PVP implementation
 */
public class RotationUtils {

    /**
     * Calculates the rotation needed to look at a specific position.
     * Exact implementation from PVP RotationUtil.getRotationsTo()
     */
    public static float[] getRotationsTo(Vec3d src, Vec3d dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z,
                dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y,
                Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[] {
                MathHelper.wrapDegrees(yaw),
                MathHelper.wrapDegrees(pitch)
        };
    }

    /**
     * Gets the rotation to attack a specific entity at the optimal hit vector.
     */
    public static float[] getRotationsTo(Entity entity, HitVector hitVector) {
        Vec3d targetPos = getHitVector(entity, hitVector);
        return getRotationsTo(mc.player.getEyePos(), targetPos);
    }

    /**
     * Gets the optimal hit position on an entity based on the hit vector mode.
     */
    public static Vec3d getHitVector(Entity entity, HitVector hitVector) {
        Vec3d feetPos = entity.getPos();

        return switch (hitVector) {
            case FEET -> feetPos;
            case TORSO -> feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
            case EYES -> entity.getEyePos();
            case CLOSEST -> {
                Vec3d eyePos = mc.player.getEyePos();
                Vec3d torsoPos = feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
                Vec3d eyesPos = entity.getEyePos();

                double feetDist = eyePos.squaredDistanceTo(feetPos);
                double torsoDist = eyePos.squaredDistanceTo(torsoPos);
                double eyesDist = eyePos.squaredDistanceTo(eyesPos);

                if (feetDist <= torsoDist && feetDist <= eyesDist) {
                    yield feetPos;
                } else if (torsoDist <= eyesDist) {
                    yield torsoPos;
                } else {
                    yield eyesPos;
                }
            }
        };
    }

    /**
     * Smooths rotation transitions with rotation speed
     * Exact implementation from PVP RotationUtil.smooth()
     */
    public static float[] smooth(float[] target, float[] previous, float rotationSpeed) {
        float speed = (1.0f - (MathHelper.clamp(rotationSpeed / 100.0f, 0.1f, 0.9f))) * 10.0f;

        float[] rotations = new float[2];

        rotations[0] = previous[0] + (float) (-getAngleDifference(previous[0], target[0]) / speed);
        rotations[1] = previous[1] + (-(previous[1] - target[1]) / speed);

        // force pitch to be in between -90 and 90 (instant ac-ban on some acs)
        rotations[1] = MathHelper.clamp(rotations[1], -90.0f, 90.0f);

        return rotations;
    }

    /**
     * Gets angle difference between two yaw values
     * Exact implementation from PVP RotationUtil.getAngleDifference()
     */
    public static double getAngleDifference(float client, float yaw) {
        return ((client - yaw) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    /**
     * Gets the difference between two pitch angles
     * Exact implementation from PVP RotationUtil.getAnglePitchDifference()
     */
    public static double getAnglePitchDifference(float client, float pitch) {
        return ((client - pitch) % 180.0 + 270.0) % 180.0 - 90.0;
    }

    /**
     * Converts pitch and yaw to a direction vector.
     * Exact implementation from PVP RotationUtil.getRotationVector()
     */
    public static Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180.0f);
        float g = -yaw * ((float) Math.PI / 180.0f);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    /**
     * Checks if there's a clear line of sight to the target position.
     */
    public static boolean canSeePosition(Vec3d from, Vec3d to) {
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result == null || result.getBlockPos().equals(BlockPos.ofFloored(to));
    }

    /**
     * Checks if the target is within the specified field of view.
     */
    public static boolean isInFov(Vec3d from, Vec3d to, float fov) {
        if (fov >= 180.0f) return true;

        float[] rotations = getRotationsTo(from, to);
        float yawDiff = MathHelper.wrapDegrees(mc.player.getYaw() - rotations[0]);

        return Math.abs(yawDiff) <= fov;
    }

    /**
     * Wraps an angle to be within -180 to 180 degrees.
     */
    public static float wrapDegrees(float degrees) {
        return MathHelper.wrapDegrees(degrees);
    }

    public enum HitVector {
        FEET,
        TORSO,
        EYES,
        CLOSEST
    }
}

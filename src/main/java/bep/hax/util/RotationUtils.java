package bep.hax.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;
public class RotationUtils {
    public static float nextYaw(double current, double target, double step) {
        double i = yawAngle(current, target);

        if (step >= Math.abs(i)) {
            return (float) (current + i);
        } else {
            return (float) (current + (i < 0 ? -1 : 1) * step);
        }
    }

    public static double yawAngle(double current, double target) {
        double c = MathHelper.wrapDegrees(current) + 180, t = MathHelper.wrapDegrees(target) + 180;
        if (c > t) {
            return t + 360 - c < Math.abs(c - t) ? 360 - c + t : t - c;
        } else {
            return 360 - t + c < Math.abs(c - t) ? -(360 - t + c) : t - c;
        }
    }

    public static float nextPitch(double current, double target, double step) {
        double i = target - current;

        return (float) (Math.abs(i) <= step ? target : i >= 0 ? current + step : current - step);
    }

    public static double radAngle(Vec2f vec1, Vec2f vec2) {
        double p = vec1.x * vec2.x + vec1.y * vec2.y;
        p /= Math.sqrt(vec1.x * vec1.x + vec1.y * vec1.y);
        p /= Math.sqrt(vec2.x * vec2.x + vec2.y * vec2.y);
        return Math.acos(p);
    }

    // Updated to match Shoreline's getRotationsTo
    public static double getYaw(Vec3d start, Vec3d target) {
        return MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(target.getZ() - start.getZ(), target.getX() - start.getX())) - 90));
    }

    public static double getPitch(Vec3d start, Vec3d target) {
        double diffX = target.getX() - start.getX();
        double diffY = target.getY() - start.getY();
        double diffZ = target.getZ() - start.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));
    }

    // Smoothing method adapted from Shoreline's RotationUtil
    public static Vec2f smooth(Vec2f target, Vec2f previous, float rotationSpeed) {
        float speed = (1.0f - (MathHelper.clamp(rotationSpeed / 100.0f, 0.1f, 0.9f))) * 10.0f;

        float newYaw = previous.x + (float) (-getYawDifference(previous.x, target.x) / speed);
        float newPitch = previous.y + (-(previous.y - target.y) / speed);

        // force pitch to be in between -90 and 90
        newPitch = MathHelper.clamp(newPitch, -90.0f, 90.0f);

        return new Vec2f(newYaw, newPitch);
    }

    public static double getYawDifference(float client, float yaw) {
        return ((client - yaw) % 360.0 + 540.0) % 360.0 - 180.0;
    }

    public static double getPitchDifference(float client, float pitch) {
        return ((client - pitch) % 180.0 + 270.0) % 180.0 - 90.0;
    }

    public static Vec3d getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180.0f);
        float g = -yaw * ((float) Math.PI / 180.0f);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
}

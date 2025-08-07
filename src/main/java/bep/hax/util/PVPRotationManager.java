package bep.hax.util;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import meteordevelopment.meteorclient.MeteorClient;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * PVP rotation management system adapted for Meteor Client
 * Maintains the exact rotation behavior from original PVP client
 */
public class PVPRotationManager {
    private static PVPRotationManager INSTANCE;

    private final List<Rotation> requests = new CopyOnWriteArrayList<>();
    private float serverYaw, serverPitch, lastServerYaw, lastServerPitch;
    private float prevYaw, prevPitch;
    private boolean rotate;

    // Current active rotation
    private Rotation rotation;
    private int rotateTicks;

    // Movement fix settings
    private boolean movementFix = true;
    private boolean mouseSensFix = true;
    private int preserveTicks = 3;

    private PVPRotationManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public static PVPRotationManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PVPRotationManager();
        }
        return INSTANCE;
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;

        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook()) {
            float packetYaw = packet.getYaw(0.0f);
            float packetPitch = packet.getPitch(0.0f);
            serverYaw = packetYaw;
            serverPitch = packetPitch;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Increment rotation ticks for timing
        if (rotation != null) {
            rotateTicks++;
        }

        // Clear expired rotation requests and get the highest priority one
        requests.removeIf(req -> req == null);

        if (requests.isEmpty()) {
            if (isDoneRotating()) {
                rotation = null;
                rotate = false;
            }
            return;
        }

        Rotation request = getRotationRequest();
        if (request == null) {
            if (isDoneRotating()) {
                rotation = null;
                rotate = false;
                return;
            }
        } else {
            rotation = request;
            rotateTicks = 0;
            rotate = true;
        }
    }

    /**
     * Set rotation with priority system
     */
    public void setRotation(Rotation rotation) {
        if (mouseSensFix) {
            double fix = Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
            rotation.setYaw((float) (rotation.getYaw() - (rotation.getYaw() - serverYaw) % fix));
            rotation.setPitch((float) (rotation.getPitch() - (rotation.getPitch() - serverPitch) % fix));
        }

        if (rotation.getPriority() == Integer.MAX_VALUE) {
            this.rotation = rotation;
        }

        // Remove any existing requests with the same priority
        requests.removeIf(r -> r.getPriority() == rotation.getPriority());
        // Add the new rotation request
        requests.add(rotation);
    }

    /**
     * Set client rotation (visual only)
     */
    public void setRotationClient(float yaw, float pitch) {
        if (mc.player == null) return;

        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    /**
     * Set silent rotation with packet
     */
    public void setRotationSilent(float yaw, float pitch) {
        setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                yaw,
                pitch,
                mc.player.isOnGround(),
                false
            )
        );
    }

    /**
     * Silent sync rotation
     */
    public void setRotationSilentSync() {
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        setRotation(new Rotation(Integer.MAX_VALUE, yaw, pitch, true));
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.Full(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                yaw,
                pitch,
                mc.player.isOnGround()
                    ,false
            )
        );
    }

    public boolean removeRotation(Rotation request) {
        return requests.remove(request);
    }

    public void clearRotations() {
        requests.clear();
        rotation = null;
        rotate = false;
        rotateTicks = 0;
    }

    public void clearRotationsByPriority(int priority) {
        requests.removeIf(req -> req.getPriority() == priority);
        if (rotation != null && rotation.getPriority() == priority) {
            rotation = null;
            rotate = false;
        }
    }

    public boolean isRotationBlocked(int priority) {
        return rotation != null && priority < rotation.getPriority();
    }

    public boolean isDoneRotating() {
        return rotateTicks > preserveTicks;
    }

    public boolean isRotating() {
        return rotation != null;
    }

    public float getRotationYaw() {
        return rotation != null ? rotation.getYaw() : mc.player.getYaw();
    }

    public float getRotationPitch() {
        return rotation != null ? rotation.getPitch() : mc.player.getPitch();
    }

    public float getServerYaw() {
        return serverYaw;
    }

    public float getWrappedYaw() {
        return MathHelper.wrapDegrees(serverYaw);
    }

    public float getServerPitch() {
        return serverPitch;
    }

    /**
     * Movement input to velocity conversion with rotation
     */
    private Vec3d movementInputToVelocity(float yaw, Vec3d movementInput, float speed) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        }

        Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
        float f = MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE);
        float g = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);
        return new Vec3d(
            vec3d.x * (double) g - vec3d.z * (double) f,
            vec3d.y,
            vec3d.z * (double) g + vec3d.x * (double) f
        );
    }

    private Rotation getRotationRequest() {
        Rotation rotationRequest = null;
        int priority = 0;
        for (Rotation request : requests) {
            if (request.getPriority() > priority) {
                rotationRequest = request;
                priority = request.getPriority();
            }
        }
        return rotationRequest;
    }

    // Getters/Setters for configuration
    public boolean getMovementFix() { return movementFix; }
    public void setMovementFix(boolean movementFix) { this.movementFix = movementFix; }

    public boolean getMouseSensFix() { return mouseSensFix; }
    public void setMouseSensFix(boolean mouseSensFix) { this.mouseSensFix = mouseSensFix; }

    public int getPreserveTicks() { return preserveTicks; }
    public void setPreserveTicks(int preserveTicks) { this.preserveTicks = preserveTicks; }
}

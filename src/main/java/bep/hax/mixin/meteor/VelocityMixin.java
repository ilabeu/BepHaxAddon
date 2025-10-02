package bep.hax.mixin.meteor;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Velocity extension for 2b2t - Adds collision handling for unloaded chunks.
 *
 * Instead of rubber-banding when hitting unloaded chunks, this smoothly
 * reduces velocity to prevent collision, allowing chunks to load naturally.
 */
@Mixin(value = Velocity.class, remap = false)
public abstract class VelocityMixin extends Module {
    public VelocityMixin(Category category, String name, String description) {
        super(category, name, description);
    }

    @Shadow
    @Final
    private SettingGroup sgGeneral;

    @Unique
    private Setting<Boolean> unloadedChunkFix;
    @Unique
    private Setting<Integer> checkDistance;
    @Unique
    private Setting<Double> slowdownFactor;
    @Unique
    private Setting<Boolean> stopCompletely;

    @Unique
    private Vec3d lastSafeVelocity = Vec3d.ZERO;
    @Unique
    private int ticksSinceSpawn = 0;
    @Unique
    private Vec3d lastPlayerPos = Vec3d.ZERO;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        unloadedChunkFix = sgGeneral.add(new BoolSetting.Builder()
            .name("2b2t-collision-fix")
            .description("Prevents rubber-banding when flying into unloaded chunks on 2b2t.")
            .defaultValue(false)
            .build()
        );

        checkDistance = sgGeneral.add(new IntSetting.Builder()
            .name("check-distance")
            .description("How many blocks ahead to check for unloaded chunks.")
            .defaultValue(3)
            .min(1)
            .sliderRange(1, 8)
            .visible(unloadedChunkFix::get)
            .build()
        );

        slowdownFactor = sgGeneral.add(new DoubleSetting.Builder()
            .name("slowdown-factor")
            .description("How much to reduce velocity near unloaded chunks (0.0 = stop, 1.0 = no change).")
            .defaultValue(0.3)
            .min(0.0)
            .max(1.0)
            .sliderRange(0.0, 1.0)
            .visible(unloadedChunkFix::get)
            .build()
        );

        stopCompletely = sgGeneral.add(new BoolSetting.Builder()
            .name("stop-completely")
            .description("Stop all movement instead of slowing down (safer but more noticeable).")
            .defaultValue(false)
            .visible(unloadedChunkFix::get)
            .build()
        );
    }

    @Unique
    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        // Reset on world join to prevent spawn issues
        ticksSinceSpawn = 0;
        lastPlayerPos = Vec3d.ZERO;
        lastSafeVelocity = Vec3d.ZERO;
    }

    @Unique
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!Utils.canUpdate() || !unloadedChunkFix.get()) return;
        if (mc.player == null || mc.world == null) return;

        // Grace period after spawning to let chunks load (20 ticks = 1 second)
        if (ticksSinceSpawn < 20) {
            ticksSinceSpawn++;
            lastPlayerPos = mc.player.getPos();
            return;
        }

        Vec3d currentPos = mc.player.getPos();
        Vec3d velocity = mc.player.getVelocity();

        // Check if player has actually moved since last tick
        // This prevents interference when stationary
        if (currentPos.squaredDistanceTo(lastPlayerPos) < 0.001) {
            lastPlayerPos = currentPos;
            return;
        }
        lastPlayerPos = currentPos;

        // Only process if player is moving
        if (velocity.lengthSquared() < 0.001) {
            lastSafeVelocity = Vec3d.ZERO;
            return;
        }

        // Check if moving into unloaded chunks
        if (isMovingIntoUnloadedChunks(velocity)) {
            // Reduce or stop velocity
            if (stopCompletely.get()) {
                mc.player.setVelocity(Vec3d.ZERO);
            } else {
                // Gradually reduce velocity when approaching unloaded chunks
                Vec3d reducedVelocity = velocity.multiply(slowdownFactor.get());
                mc.player.setVelocity(reducedVelocity);
            }
        } else {
            // Safe to move, remember this velocity
            lastSafeVelocity = velocity;
        }
    }

    @Unique
    private boolean isMovingIntoUnloadedChunks(Vec3d velocity) {
        if (mc.player == null || mc.world == null) return false;

        Vec3d currentPos = mc.player.getPos();
        ClientChunkManager chunkManager = mc.world.getChunkManager();
        int distance = checkDistance.get();

        // Check multiple points along the movement vector
        for (int i = 1; i <= distance; i++) {
            Vec3d checkPos = currentPos.add(velocity.normalize().multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            ChunkPos chunkPos = new ChunkPos(blockPos);

            // Check if this chunk is loaded
            Chunk chunk = chunkManager.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);

            if (chunk == null) {
                // Found an unloaded chunk ahead
                return true;
            }
        }

        // Also check the exact destination chunk
        Vec3d futurePos = currentPos.add(velocity.multiply(distance));
        ChunkPos futureChunk = new ChunkPos(BlockPos.ofFloored(futurePos));
        Chunk chunk = chunkManager.getChunk(futureChunk.x, futureChunk.z, ChunkStatus.FULL, false);

        return chunk == null;
    }

    @Unique
    public boolean isCollisionFixEnabled() {
        return unloadedChunkFix != null && unloadedChunkFix.get();
    }
}

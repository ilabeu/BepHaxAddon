package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import bep.hax.util.PushEntityEvent;
import bep.hax.util.PushOutOfBlocksEvent;

/**
 * GrimV3 Velocity module - cancels knockback when phased in blocks
 * Properly handles movement to prevent rubberbanding
 */
public class Velo extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General Settings
    private final Setting<Boolean> knockback = sgGeneral.add(new BoolSetting.Builder()
        .name("knockback")
        .description("Removes player knockback velocity when phased")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> explosion = sgGeneral.add(new BoolSetting.Builder()
        .name("explosion")
        .description("Removes player explosion velocity when phased")
        .defaultValue(true)
        .build());

    // Push Prevention Settings
    private final Setting<Boolean> pushEntities = sgGeneral.add(new BoolSetting.Builder()
        .name("nopush-entities")
        .description("Prevents being pushed away from entities")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> pushBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("nopush-blocks")
        .description("Prevents being pushed out of blocks when phased")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> pushFishhook = sgGeneral.add(new BoolSetting.Builder()
        .name("nopush-fishhook")
        .description("Prevents being pulled by fishing rod hooks")
        .defaultValue(true)
        .build());

    // State tracking
    private boolean shouldCancelVelocity = false;
    private boolean velocityCancelled = false;
    private int velocityTimer = 0;

    public Velo() {
        super(Bep.CATEGORY, "velo", "GrimV3 velocity - cancels knockback when phased");
    }

    @Override
    public String getInfoString() {
        return "GrimV3";
    }

    @Override
    public void onDeactivate() {
        shouldCancelVelocity = false;
        velocityCancelled = false;
        velocityTimer = 0;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        // Handle knockback packets
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && knockback.get()) {
            if (packet.getEntityId() != mc.player.getId()) return;
            
            // Cancel velocity if player is phased in blocks
            if (isPhased()) {
                event.cancel();
                shouldCancelVelocity = true;
                velocityCancelled = false;
                velocityTimer = 0;
            }
        }
        // Handle explosion packets
        else if (event.packet instanceof ExplosionS2CPacket && explosion.get()) {
            if (isPhased()) {
                event.cancel();
                shouldCancelVelocity = true;
                velocityCancelled = false;
                velocityTimer = 0;
            }
        }
        // Handle damage packets when phased
        else if (event.packet instanceof EntityDamageS2CPacket packet) {
            // Check if damage is to the player and they are phased
            try {
                // EntityDamageS2CPacket structure may vary, using safe check
                if (isPhased()) {
                    shouldCancelVelocity = true;
                    velocityCancelled = false;
                    velocityTimer = 0;
                }
            } catch (Exception ignored) {
                // Ignore if packet structure is different
            }
        }
        // Handle fishing rod pull
        else if (event.packet instanceof EntityStatusS2CPacket packet
            && packet.getStatus() == EntityStatuses.PULL_HOOKED_ENTITY && pushFishhook.get()) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof FishingBobberEntity hook && hook.getHookedEntity() == mc.player) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle velocity cancellation timing
        if (shouldCancelVelocity) {
            velocityTimer++;
            
            // Send single correction after 1 tick to avoid spam
            if (velocityTimer == 1 && !velocityCancelled) {
                sendGrimBypass();
                velocityCancelled = true;
            }
            
            // Reset after 3 ticks
            if (velocityTimer >= 3) {
                shouldCancelVelocity = false;
                velocityCancelled = false;
                velocityTimer = 0;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) return;

        // Only intercept movement packets when we need to cancel velocity
        if (shouldCancelVelocity && !velocityCancelled && event.packet instanceof PlayerMoveC2SPacket) {
            // Let the packet go through but flag for correction
            if (isPhased()) {
                velocityCancelled = true;
            }
        }
    }

    private void sendGrimBypass() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Send minimal correction to bypass Grim without causing rubberbanding
        // Single ground state toggle
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(
            !mc.player.isOnGround(),
            false
        ));
        
        // Send stop action to signal we're not moving
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
            mc.player.getBlockPos(),
            Direction.DOWN
        ));
    }

    @EventHandler
    public void onPushEntity(PushEntityEvent event) {
        if (pushEntities.get() && event.getPushed().equals(mc.player)) {
            event.cancel();
        }
    }

    @EventHandler
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        // Only prevent push when actually phased
        if (pushBlocks.get() && isPhased()) {
            event.cancel();
        }
    }

    /**
     * Checks if player is phased inside blocks
     */
    private boolean isPhased() {
        if (mc.player == null || mc.world == null) return false;
        
        Box playerBox = mc.player.getBoundingBox();
        
        // Check all block positions that intersect with player's bounding box
        int minX = (int) Math.floor(playerBox.minX);
        int maxX = (int) Math.floor(playerBox.maxX);
        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.floor(playerBox.maxY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxZ = (int) Math.floor(playerBox.maxZ);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(x, y, z);
                    net.minecraft.block.BlockState state = mc.world.getBlockState(pos);
                    
                    // Check if block is solid and would collide with player
                    if (!state.isAir() && !state.getCollisionShape(mc.world, pos).isEmpty()) {
                        net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(mc.world, pos);
                        Box blockBox = shape.getBoundingBox().offset(pos);
                        
                        if (playerBox.intersects(blockBox)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
}
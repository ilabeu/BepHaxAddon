package bep.hax.mixin.meteor;
import bep.hax.util.RotationUtils;
import bep.hax.util.PushEntityEvent;
import bep.hax.util.PushOutOfBlocksEvent;
import bep.hax.util.InventoryManager.VelocityMode;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = Velocity.class, remap = false)
public abstract class VelocityMixin extends Module {
    public VelocityMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Shadow @Final private SettingGroup sgGeneral;
    @Shadow @Final public Setting<Boolean> knockback;
    @Shadow @Final public Setting<Double> knockbackHorizontal;
    @Shadow @Final public Setting<Double> knockbackVertical;
    @Shadow @Final public Setting<Boolean> explosions;
    @Shadow @Final public Setting<Double> explosionsHorizontal;
    @Shadow @Final public Setting<Double> explosionsVertical;
    @Unique private SettingGroup bephax$sgAdvancedModes;
    @Unique private Setting<VelocityMode> bephax$mode;
    @Unique private Setting<Boolean> bephax$wallsGroundOnly;
    @Unique private Setting<Boolean> bephax$pushEntities;
    @Unique private Setting<Boolean> bephax$pushBlocks;
    @Unique private Setting<Boolean> bephax$pushFishhook;
    @Unique private RotationUtils bephax$rotationManager;
    @Unique private boolean bephax$cancelVelocity = false;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        bephax$rotationManager = RotationUtils.getInstance();
        bephax$sgAdvancedModes = settings.createGroup("Advanced Modes");
        bephax$mode = bephax$sgAdvancedModes.add(new EnumSetting.Builder<VelocityMode>()
            .name("mode")
            .description("Velocity mode (NORMAL = Meteor default, WALLS = only when phased, GRIM/GRIM_V3 = 2b2t bypass)")
            .defaultValue(VelocityMode.NORMAL)
            .build()
        );
        bephax$wallsGroundOnly = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("ground-only")
            .description("Only applies velocity in walls while on ground")
            .defaultValue(false)
            .visible(() -> bephax$mode.get() == VelocityMode.WALLS)
            .build()
        );
        bephax$pushEntities = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-entities")
            .description("Prevents being pushed away from entities")
            .defaultValue(true)
            .build()
        );
        bephax$pushBlocks = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-blocks")
            .description("Prevents being pushed out of blocks")
            .defaultValue(true)
            .build()
        );
        bephax$pushFishhook = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-fishhook")
            .description("Prevents being pulled by fishing rod hooks")
            .defaultValue(true)
            .build()
        );
    }
    @Override
    public void onDeactivate() {
        if (bephax$cancelVelocity && bephax$mode.get() == VelocityMode.GRIM) {
            bephax$sendGrimBypass();
        }
        bephax$cancelVelocity = false;
    }
    @Inject(method = "onPacketReceive", at = @At("HEAD"), cancellable = true)
    private void cancelMeteorHandler(PacketEvent.Receive event, CallbackInfo ci) {
        if (bephax$mode.get() != VelocityMode.NORMAL) {
            ci.cancel();
        }
    }
    @Unique
    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet && knockback.get()) {
            if (packet.getEntityId() != mc.player.getId()) return;
            switch (bephax$mode.get()) {
                case NORMAL -> {
                    return;
                }
                case WALLS -> {
                    if (!bephax$isPhased()) return;
                    if (bephax$wallsGroundOnly.get() && !mc.player.isOnGround()) return;
                    if (knockbackHorizontal.get() == 0.0 && knockbackVertical.get() == 0.0) {
                        event.cancel();
                        return;
                    }
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setX((int) (packet.getVelocityX() * knockbackHorizontal.get()));
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setY((int) (packet.getVelocityY() * knockbackVertical.get()));
                    ((EntityVelocityUpdateS2CPacketAccessor) packet).setZ((int) (packet.getVelocityZ() * knockbackHorizontal.get()));
                }
                case GRIM -> {
                    event.cancel();
                    bephax$cancelVelocity = true;
                }
                case GRIM_V3 -> {
                    if (bephax$isPhased()) {
                        event.cancel();
                    }
                }
            }
        }
        else if (event.packet instanceof ExplosionS2CPacket && explosions.get()) {
            switch (bephax$mode.get()) {
                case NORMAL -> {
                    return;
                }
                case WALLS -> {
                    if (!bephax$isPhased()) return;
                    event.cancel();
                }
                case GRIM -> {
                    event.cancel();
                    bephax$cancelVelocity = true;
                }
                case GRIM_V3 -> {
                    if (bephax$isPhased()) {
                        event.cancel();
                    }
                }
            }
        }
        else if (event.packet instanceof EntityDamageS2CPacket packet
            && packet.entityId() == mc.player.getId()
            && bephax$mode.get() == VelocityMode.GRIM_V3
            && bephax$isPhased()) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false, false));
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
        }
        else if (event.packet instanceof EntityStatusS2CPacket packet
            && packet.getStatus() == EntityStatuses.PULL_HOOKED_ENTITY
            && bephax$pushFishhook.get()) {
            Entity entity = packet.getEntity(mc.world);
            if (entity instanceof FishingBobberEntity hook && hook.getHookedEntity() == mc.player) {
                event.cancel();
            }
        }
    }
    @Unique
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (bephax$cancelVelocity && bephax$mode.get() == VelocityMode.GRIM) {
            bephax$sendGrimBypass();
            bephax$cancelVelocity = false;
        }
    }
    @Unique
    @EventHandler
    private void onPushEntity(PushEntityEvent event) {
        if (bephax$pushEntities.get() && event.getPushed().equals(mc.player)) {
            event.cancel();
        }
    }
    @Unique
    @EventHandler
    private void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        if (bephax$pushBlocks.get()) {
            event.cancel();
        }
    }
    @Unique
    private void bephax$sendGrimBypass() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        float yaw = bephax$rotationManager.getServerYaw();
        float pitch = bephax$rotationManager.getServerPitch();
        if (bephax$rotationManager.isRotating()) {
            yaw = bephax$rotationManager.getRotationYaw();
            pitch = bephax$rotationManager.getRotationPitch();
        }
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            yaw,
            pitch,
            mc.player.isOnGround(),
            false
        ));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            mc.player.isCrawling() ? mc.player.getBlockPos() : mc.player.getBlockPos().up(),
            Direction.DOWN
        ));
    }
    @Unique
    private boolean bephax$isPhased() {
        if (mc.player == null || mc.world == null) return false;
        Box playerBox = mc.player.getBoundingBox();
        int minX = (int) Math.floor(playerBox.minX);
        int maxX = (int) Math.floor(playerBox.maxX);
        int minY = (int) Math.floor(playerBox.minY);
        int maxY = (int) Math.floor(playerBox.maxY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxZ = (int) Math.floor(playerBox.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!mc.world.getBlockState(pos).isReplaceable()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
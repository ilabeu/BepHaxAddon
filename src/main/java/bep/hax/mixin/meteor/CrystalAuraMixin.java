package bep.hax.mixin.meteor;
import bep.hax.util.InventoryManager.IPlayerInteractEntityC2SPacket;
import bep.hax.util.RotationUtils;
import bep.hax.util.RotationUtils;
import bep.hax.util.InventoryManager;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.EndCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = CrystalAura.class, remap = false)
public abstract class CrystalAuraMixin extends Module {
    public CrystalAuraMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Shadow @Final private SettingGroup sgGeneral;
    @Shadow @Final private SettingGroup sgPause;
    @Shadow @Final private Setting<Boolean> rotate;
    @Unique private SettingGroup bephax$sgRotation;
    @Unique private Setting<Boolean> bephax$grimRotate;
    @Unique private Setting<Boolean> bephax$yawStep;
    @Unique private Setting<Integer> bephax$yawStepLimit;
    @Unique private SettingGroup bephax$sgGrimSupport;
    @Unique private Setting<Boolean> bephax$grimSupport;
    @Unique private Setting<Boolean> bephax$pauseOnGapple;
    @Unique private RotationUtils bephax$rotationManager;
    @Unique private InventoryManager bephax$inventoryManager;
    @Unique private Vec3d bephax$targetRotation = null;
    @Unique private boolean bephax$rotated = true;
    @Unique private boolean bephax$settingSlot = false;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        bephax$rotationManager = RotationUtils.getInstance();
        bephax$inventoryManager = InventoryManager.getInstance();
        bephax$sgRotation = settings.createGroup("Grim Rotations");
        bephax$grimRotate = bephax$sgRotation.add(new BoolSetting.Builder()
            .name("grim-rotate")
            .description("Rotation system with yaw stepping")
            .defaultValue(false)
            .build()
        );
        bephax$yawStep = bephax$sgRotation.add(new BoolSetting.Builder()
            .name("yaw-step")
            .description("Rotates over multiple ticks (45-90° for GrimAC)")
            .defaultValue(true)
            .visible(bephax$grimRotate::get)
            .build()
        );
        bephax$yawStepLimit = bephax$sgRotation.add(new IntSetting.Builder()
            .name("yaw-step-limit")
            .description("Max yaw rotation per tick")
            .defaultValue(90)
            .min(1)
            .max(180)
            .sliderRange(1, 180)
            .visible(() -> bephax$grimRotate.get() && bephax$yawStep.get())
            .build()
        );
        bephax$sgGrimSupport = settings.createGroup("Grim Support");
        bephax$grimSupport = bephax$sgGrimSupport.add(new BoolSetting.Builder()
            .name("grim-support")
            .description("Uses GrimAirPlace exploit for support block placement (enable Meteor's Support too!)")
            .defaultValue(false)
            .build()
        );
        bephax$pauseOnGapple = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-gapple")
            .description("Pauses when holding golden apples")
            .defaultValue(true)
            .build()
        );
    }
    @Inject(method = "onActivate", at = @At("TAIL"))
    private void onActivateInject(CallbackInfo ci) {
        bephax$targetRotation = null;
        bephax$rotated = true;
    }
    @Inject(method = "onDeactivate", at = @At("TAIL"))
    private void onDeactivateInject(CallbackInfo ci) {
        bephax$targetRotation = null;
        bephax$rotated = true;
        if (bephax$inventoryManager != null) {
            bephax$inventoryManager.syncToClient();
        }
    }
    @Unique
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPreTickHighPriority(TickEvent.Pre event) {
        if (!isActive() || mc.player == null) {
            bephax$targetRotation = null;
            bephax$rotated = true;
            return;
        }
        ItemStack mainHand = mc.player.getMainHandStack();
        boolean holdingCrystal = mainHand.getItem() instanceof EndCrystalItem;
        boolean holdingObsidian = mainHand.getItem() == Items.OBSIDIAN && bephax$grimSupport.get();
        if (holdingCrystal || holdingObsidian) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            bephax$settingSlot = true;
            try {
                bephax$inventoryManager.setSlot(currentSlot);
            } finally {
                bephax$settingSlot = false;
            }
        }
        if (bephax$grimRotate.get()) {
            if (rotate.get()) rotate.set(false);
            if (bephax$targetRotation != null && !bephax$rotated && bephax$yawStep.get()) {
                bephax$continueYawStep();
            }
        }
    }
    @Inject(method = "isActive", at = @At("RETURN"), cancellable = true)
    private void onIsActive(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (mc.player == null) return;
        if (bephax$pauseOnGapple.get()) {
            ItemStack mainHand = mc.player.getMainHandStack();
            ItemStack offHand = mc.player.getOffHandStack();
            if (mainHand.getItem() == Items.GOLDEN_APPLE ||
                mainHand.getItem() == Items.ENCHANTED_GOLDEN_APPLE ||
                offHand.getItem() == Items.GOLDEN_APPLE ||
                offHand.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                cir.setReturnValue(false);
            }
        }
    }
    @Unique
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onSendPacketHighest(PacketEvent.Send event) {
        if (!isActive() || !Utils.canUpdate() || mc.player == null) return;
        if (bephax$settingSlot) return;
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet) {
            int targetSlot = packet.getSelectedSlot();
            ItemStack targetStack = mc.player.getInventory().getStack(targetSlot);
            boolean isCrystalSlot = targetStack.getItem() instanceof EndCrystalItem;
            boolean isObsidianSlot = targetStack.getItem() == Items.OBSIDIAN && bephax$grimSupport.get();
            if (isCrystalSlot || isObsidianSlot) {
                bephax$settingSlot = true;
                try {
                    bephax$inventoryManager.setSlot(targetSlot);
                } finally {
                    bephax$settingSlot = false;
                }
            } else {
                bephax$settingSlot = true;
                try {
                    bephax$inventoryManager.syncToClient();
                } finally {
                    bephax$settingSlot = false;
                }
            }
        }
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            Hand hand = packet.getHand();
            if (hand == null) return;
            ItemStack heldItem = mc.player.getStackInHand(hand);
            if (heldItem.getItem() instanceof EndCrystalItem) {
                if (bephax$grimRotate.get()) {
                    BlockPos pos = packet.getBlockHitResult().getBlockPos();
                    Vec3d crystalPos = Vec3d.ofCenter(pos).add(0.0, 1.5, 0.0);
                    bephax$applyRotation(crystalPos);
                }
            }
            else if (heldItem.getItem() == Items.OBSIDIAN && bephax$grimSupport.get()) {
                event.cancel();
                bephax$placeGrimSupport(packet);
            }
        }
        else if (bephax$grimRotate.get() && event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            int entityId = ((IPlayerInteractEntityC2SPacket) packet).getTargetEntityId();
            Entity entity = mc.world.getEntityById(entityId);
            if (entity instanceof EndCrystalEntity crystal) {
                bephax$applyRotation(crystal.getPos());
            }
        }
    }
    @Unique
    private void bephax$placeGrimSupport(PlayerInteractBlockC2SPacket packet) {
        BlockHitResult hitResult = packet.getBlockHitResult();
        if (bephax$grimRotate.get()) {
            Vec3d supportPos = Vec3d.ofCenter(hitResult.getBlockPos());
            bephax$applyRotation(supportPos);
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
            Hand.OFF_HAND, hitResult, packet.getSequence()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    @Unique
    private void bephax$applyRotation(Vec3d target) {
        if (mc.player == null) return;
        bephax$targetRotation = target;
        float[] rotation = RotationUtils.getRotationsTo(mc.player.getEyePos(), target);
        if (bephax$yawStep.get()) {
            float serverYaw = bephax$rotationManager.getWrappedYaw();
            float targetYaw = rotation[0];
            float diff = serverYaw - targetYaw;
            while (diff > 180.0f) diff -= 360.0f;
            while (diff < -180.0f) diff += 360.0f;
            float diff1 = Math.abs(diff);
            int stepLimit = bephax$yawStepLimit.get();
            if (diff1 > stepLimit) {
                float deltaYaw = diff > 0.0f ? -stepLimit : stepLimit;
                float yaw = serverYaw + deltaYaw;
                bephax$rotationManager.setRotationSilent(yaw, rotation[1]);
                bephax$rotated = false;
            } else {
                bephax$rotationManager.setRotationSilent(targetYaw, rotation[1]);
                bephax$rotated = true;
                bephax$targetRotation = null;
            }
        } else {
            bephax$rotationManager.setRotationSilent(rotation[0], rotation[1]);
            bephax$rotated = true;
            bephax$targetRotation = null;
        }
    }
    @Unique
    private void bephax$continueYawStep() {
        if (mc.player == null || bephax$targetRotation == null) {
            bephax$rotated = true;
            return;
        }
        float[] rotation = RotationUtils.getRotationsTo(mc.player.getEyePos(), bephax$targetRotation);
        float serverYaw = bephax$rotationManager.getWrappedYaw();
        float targetYaw = rotation[0];
        float diff = serverYaw - targetYaw;
        while (diff > 180.0f) diff -= 360.0f;
        while (diff < -180.0f) diff += 360.0f;
        float diff1 = Math.abs(diff);
        int stepLimit = bephax$yawStepLimit.get();
        if (diff1 > stepLimit) {
            float deltaYaw = diff > 0.0f ? -stepLimit : stepLimit;
            float yaw = serverYaw + deltaYaw;
            bephax$rotationManager.setRotationSilent(yaw, rotation[1]);
            bephax$rotated = false;
        } else {
            bephax$rotationManager.setRotationSilent(targetYaw, rotation[1]);
            bephax$rotated = true;
            bephax$targetRotation = null;
        }
    }
}
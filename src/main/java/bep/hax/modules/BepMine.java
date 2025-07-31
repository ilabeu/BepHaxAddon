package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BepMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General Settings
    private final Setting<SpeedmineMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<SpeedmineMode>()
        .name("mode")
        .description("The mining mode for speedmine")
        .defaultValue(SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Boolean> multitaskConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("multitask")
        .description("Allows mining while using items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    public final Setting<Boolean> doubleBreakConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("double-break")
        .description("Allows you to mine two blocks at once")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to mine blocks")
        .defaultValue(4.0)
        .min(0.1)
        .sliderRange(0.1, 6.0)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The speed to mine blocks")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 1.0)
        .build()
    );

    private final Setting<Boolean> instantConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Instantly mines already broken blocks")
        .defaultValue(false)
        .build()
    );

    private final Setting<Swap> swapConfig = sgGeneral.add(new EnumSetting.Builder<Swap>()
        .name("auto-swap")
        .description("Swaps to the best tool once the mining is complete")
        .defaultValue(Swap.SILENT)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates when mining the block")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Boolean> switchResetConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-reset")
        .description("Resets mining after switching items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("grim")
        .description("Uses grim block breaking speeds")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> grimNewConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("grim-v3")
        .description("Uses new grim block breaking speeds")
        .defaultValue(false)
        .visible(grimConfig::get)
        .build()
    );

    private final Setting<Boolean> miningFix = sgGeneral.add(new BoolSetting.Builder()
        .name("mining-fix")
        .description("Mining fix for grim v3")
        .defaultValue(true)
        .visible(() -> grimConfig.get() && grimNewConfig.get())
        .build()
    );

    // Render Settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined")
        .defaultValue(true)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<Boolean> smoothColorConfig = sgRender.add(new BoolSetting.Builder()
        .name("color-smooth")
        .description("Interpolates from start to done color")
        .defaultValue(false)
        .build()
    );

    private final Setting<SettingColor> colorConfig = sgRender.add(new ColorSetting.Builder()
        .name("mine-color")
        .description("The mine render color")
        .defaultValue(new SettingColor(Color.RED))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<SettingColor> colorDoneConfig = sgRender.add(new ColorSetting.Builder()
        .name("done-color")
        .description("The done render color")
        .defaultValue(new SettingColor(Color.GREEN))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );

    private final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderRange(0, 1000)
        .visible(() -> false) // Hidden like in Shoreline
        .build()
    );

    // Animation system for fading
    private final Map<MiningData, Animation> fadeList = new HashMap<>();
    private FirstOutQueue<MiningData> miningQueue;
    private long lastBreak;

    public BepMine() {
        super(Bep.CATEGORY, "bep-mine", "Mines blocks faster");
    }

    // Getters for mixin access
    public Setting<Double> getSpeedConfig() {
        return speedConfig;
    }

    public Setting<SpeedmineMode> getModeConfig() {
        return modeConfig;
    }

    @Override
    public void onActivate() {
        if (doubleBreakConfig.get()) {
            miningQueue = new FirstOutQueue<>(2);
        } else {
            miningQueue = new FirstOutQueue<>(1);
        }
    }

    @Override
    public void onDeactivate() {
        if (miningQueue != null) {
            miningQueue.clear();
        }
        fadeList.clear();
    }

    @EventHandler
    public void onPlayerTick(final TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (modeConfig.get() == SpeedmineMode.DAMAGE) {
            // For damage mode, we'll use a different approach
            // The module will work by modifying the block breaking delta in the mixin
            return;
        }

        // AutoMine check would go here if implemented

        if (miningQueue.isEmpty()) {
            return;
        }

        List<MiningData> toRemove = new ArrayList<>();

        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                data.resetBreakTime();
            }
            if (isDataPacketMine(data) && (data.getState().isAir() || data.hasAttemptedBreak() && data.passedAttemptedBreakTime(500))) {
                // Sync inventory
                toRemove.add(data);
                continue;
            }

            final float damageDelta = calcBlockBreakingDelta(data.getState(), mc.world, data.getPos());
            data.damage(damageDelta);

            if (isDataPacketMine(data) && data.getBlockDamage() >= 1.0f && data.getSlot() != -1) {
                if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                    return;
                }

                if (!data.hasAttemptedBreak()) {
                    data.setAttemptedBreak(true);
                }
            }
        }

        miningQueue.removeAll(toRemove);

        MiningData miningData2 = miningQueue.getFirst();
        if (miningData2 == null) return;

        final double distance = mc.player.getEyePos().squaredDistanceTo(miningData2.getPos().toCenterPos());
        if (distance > rangeConfig.get() * rangeConfig.get()) {
            miningQueue.remove(miningData2);
            return;
        }

        if (miningData2.getState().isAir()) {
            return;
        }

        // Something went wrong, remove and remine
        if (miningData2.getBlockDamage() >= speedConfig.get() && miningData2.hasAttemptedBreak() && miningData2.passedAttemptedBreakTime(500)) {
            abortMining(miningData2);
            miningQueue.remove(miningData2);
        }

        if (miningData2.getBlockDamage() >= speedConfig.get()) {
            if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                return;
            }

            stopMining(miningData2);

            if (!instantConfig.get()) {
                miningQueue.remove(miningData2);
            }

            if (!miningData2.hasAttemptedBreak()) {
                miningData2.setAttemptedBreak(true);
            }
        }
    }

    @EventHandler
    public void onAttackBlock(final StartBreakingBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        // AutoMine check would go here

        event.cancel();

        // Do not try to break unbreakable blocks
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (blockState.getHardness(mc.world, event.blockPos) == -1.0f || blockState.isAir()) {
            return;
        }

        startManualMine(event.blockPos, event.direction);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet
            && packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
            && modeConfig.get() == SpeedmineMode.DAMAGE && grimConfig.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().up(500), packet.getDirection()));
        }

        if (event.packet instanceof UpdateSelectedSlotC2SPacket && switchResetConfig.get()
            && modeConfig.get() == SpeedmineMode.PACKET) {
            for (MiningData data : miningQueue) {
                data.resetDamage();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        // AutoMine check would go here

        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            handleBlockUpdatePacket(packet);
        } else if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
                if (packet1 instanceof BlockUpdateS2CPacket packet2) {
                    handleBlockUpdatePacket(packet2);
                }
            }
        }
    }

    private void handleBlockUpdatePacket(BlockUpdateS2CPacket packet) {
        if (!packet.getState().isAir()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
                data.setAttemptedBreak(false);
            }
        }
    }

    @EventHandler
    public void onRenderWorld(final Render3DEvent event) {
        if (mc.player.isCreative() || modeConfig.get() != SpeedmineMode.PACKET || !render.get()) {
            return;
        }

        // AutoMine check would go here

        // Add active mining data to fadeList if not present
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                continue;
            }
            if (!fadeList.containsKey(data)) {
                fadeList.put(data, new Animation(true, fadeTimeConfig.get()));
            }
        }

        // Update animation states
        for (Map.Entry<MiningData, Animation> entry : fadeList.entrySet()) {
            MiningData data = entry.getKey();
            boolean isActive = miningQueue.contains(data) && !data.getState().isAir();
            entry.getValue().setState(isActive);
        }

        // Render
        for (Map.Entry<MiningData, Animation> set : fadeList.entrySet()) {
            MiningData data = set.getKey();
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());

            int boxColor;
            int lineColor;
            if (smoothColorConfig.get()) {
                boxColor = data.getState().isAir() ? colorDoneConfig.get().getPacked() :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), colorDoneConfig.get(), colorConfig.get()).getPacked();
                lineColor = data.getState().isAir() ? colorDoneConfig.get().getPacked() :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), colorDoneConfig.get(), colorConfig.get()).getPacked();
            } else {
                boxColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
                lineColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
            }

            // Apply alpha
            boxColor = (boxColor & 0x00FFFFFF) | (boxAlpha << 24);
            lineColor = (lineColor & 0x00FFFFFF) | (lineAlpha << 24);

            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            net.minecraft.util.math.Vec3d center = render.getCenter();
            float total = isDataPacketMine(data) ? 1.0f : speedConfig.get().floatValue();
            float scale = data.getState().isAir() ? 1.0f : MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.tickDelta) / total, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);

            event.renderer.box(scaled.minX, scaled.minY, scaled.minZ, scaled.maxX, scaled.maxY, scaled.maxZ,
                new SettingColor(boxColor), new SettingColor(lineColor), shapeMode.get(), 0);
        }

        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
    }

    private void startManualMine(BlockPos pos, Direction direction) {
        clickMine(new MiningData(pos, direction));
    }

    public void clickMine(MiningData miningData) {
        int queueSize = miningQueue.size();
        if (queueSize <= 2) {
            queueMiningData(miningData);
        }
    }

    private void queueMiningData(MiningData data) {
        if (data.getState().isAir()) {
            return;
        }
        if (startMining(data)) {
            if (miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
                return;
            }
            miningQueue.addFirst(data);
        }
    }

    private boolean startMining(MiningData data) {
        if (data.isStarted()) {
            return false;
        }

        data.setStarted();
        if (grimNewConfig.get()) {
            if (!miningFix.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }

            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
        }

        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        return true;
    }

    private void abortMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    private void stopMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotations = getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if (grimConfig.get()) {
                Rotations.rotate(rotations[0], rotations[1]);
            } else {
                Rotations.rotate(rotations[0], rotations[1]);
            }
        }
        int slot = data.getSlot();
        boolean canSwap = slot != -1 && slot != mc.player.getInventory().selectedSlot;
        if (canSwap) {
            swapTo(slot);
        }
        stopMiningInternal(data);
        lastBreak = System.currentTimeMillis();
        if (canSwap) {
            swapSync(slot);
        }
    }

    private void swapTo(int slot) {
        switch (swapConfig.get()) {
            case NORMAL -> {
                mc.player.getInventory().selectedSlot = slot;
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
            case SILENT -> InvUtils.swap(slot, false);
            case SILENT_ALT -> InvUtils.swap(slot, false); // Meteor doesn't have ALT mode
        }
    }

    private void swapSync(int slot) {
        switch (swapConfig.get()) {
            case SILENT -> InvUtils.swapBack();
            case SILENT_ALT -> InvUtils.swap(slot, false);
        }
    }

    private void stopMiningInternal(MiningData data) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public boolean isBlockDelayGrim() {
        return System.currentTimeMillis() - lastBreak <= 280 && grimConfig.get();
    }

    private boolean isDataPacketMine(MiningData data) {
        return miningQueue.size() == 2 && data == miningQueue.getLast();
    }

    public float calcBlockBreakingDelta(BlockState state, BlockView world, BlockPos pos) {
        if (swapConfig.get() == Swap.OFF) {
            return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        }
        float f = state.getHardness(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    private float getBlockBreakingSpeed(BlockState block) {
        int tool = getBestTool(block);
        float f = mc.player.getInventory().getStack(tool).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getStack(tool);
            int i = 0;
            // Check for efficiency enchantment
            var enchantments = stack.getEnchantments();
            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.EFFICIENCY)) {
                    i = entry.getIntValue();
                    break;
                }
            }
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0f + (float) (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            // Check for Aqua Affinity
            boolean hasAquaAffinity = false;
            ItemStack helmet = mc.player.getInventory().getArmorStack(3); // Helmet slot
            if (!helmet.isEmpty()) {
                var enchantments = helmet.getEnchantments();
                for (var entry : enchantments.getEnchantmentEntries()) {
                    if (entry.getKey().matchesKey(Enchantments.AQUA_AFFINITY)) {
                        hasAquaAffinity = true;
                        break;
                    }
                }
            }
            if (!hasAquaAffinity) {
                f /= 5.0f;
            }
        }
        if (!mc.player.isOnGround()) {
            f /= 5.0f;
        }
        return f;
    }

    private boolean canHarvest(BlockState state) {
        if (state.isToolRequired()) {
            int tool = getBestTool(state);
            return mc.player.getInventory().getStack(tool).isSuitableFor(state);
        }
        return true;
    }

    private int getBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot == -1 ? mc.player.getInventory().selectedSlot : bestSlot;
    }

    public boolean isMining() {
        return !miningQueue.isEmpty();
    }

    // Utility methods
    private static float[] getRotationsTo(net.minecraft.util.math.Vec3d src, net.minecraft.util.math.Vec3d dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z,
            dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y,
            Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[] {
            MathHelper.wrapDegrees(yaw),
            MathHelper.wrapDegrees(pitch)
        };
    }

    private SettingColor interpolateColor(float value, SettingColor start, SettingColor end) {
        float sr = start.r / 255.0f;
        float sg = start.g / 255.0f;
        float sb = start.b / 255.0f;
        float sa = start.a / 255.0f;
        float er = end.r / 255.0f;
        float eg = end.g / 255.0f;
        float eb = end.b / 255.0f;
        float ea = end.a / 255.0f;
        return new SettingColor(
            (int)((sr * value + er * (1.0f - value)) * 255),
            (int)((sg * value + eg * (1.0f - value)) * 255),
            (int)((sb * value + eb * (1.0f - value)) * 255),
            (int)((sa * value + ea * (1.0f - value)) * 255)
        );
    }

    // Mining Data class
    public class MiningData {
        private boolean attemptedBreak;
        private long breakTime;
        private final BlockPos pos;
        private final Direction direction;
        private float lastDamage;
        private float blockDamage;
        private boolean started;

        public MiningData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
        }

        public void setAttemptedBreak(boolean attemptedBreak) {
            this.attemptedBreak = attemptedBreak;
            if (attemptedBreak) {
                resetBreakTime();
            }
        }

        public void resetBreakTime() {
            breakTime = System.currentTimeMillis();
        }

        public boolean hasAttemptedBreak() {
            return attemptedBreak;
        }

        public boolean passedAttemptedBreakTime(long time) {
            return System.currentTimeMillis() - breakTime >= time;
        }

        public float damage(final float dmg) {
            lastDamage = blockDamage;
            blockDamage += dmg;
            return blockDamage;
        }

        public void setDamage(float blockDamage) {
            this.blockDamage = blockDamage;
        }

        public void resetDamage() {
            started = false;
            blockDamage = 0.0f;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getSlot() {
            return getBestToolNoFallback(getState());
        }

        public BlockState getState() {
            return mc.world.getBlockState(pos);
        }

        public float getBlockDamage() {
            return blockDamage;
        }

        public float getLastDamage() {
            return lastDamage;
        }

        public boolean isStarted() {
            return started;
        }

        public void setStarted() {
            this.started = true;
        }

        private int getBestToolNoFallback(BlockState state) {
            int bestSlot = -1;
            float bestSpeed = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
            return bestSlot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MiningData that = (MiningData) o;
            return pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    // FirstOutQueue implementation
    private static class FirstOutQueue<T> extends java.util.ArrayList<T> {
        private final int maxSize;

        public FirstOutQueue(int maxSize) {
            this.maxSize = maxSize;
        }

        public void addFirst(T element) {
            add(0, element);
            while (size() > maxSize) {
                remove(size() - 1);
            }
        }

        public T getFirst() {
            return isEmpty() ? null : get(0);
        }

        public T getLast() {
            return isEmpty() ? null : get(size() - 1);
        }
    }

    // Animation class for fade effect
    private static class Animation {
        private boolean state;
        private long time;
        private final long duration;

        public Animation(boolean state, long duration) {
            this.state = state;
            this.duration = duration;
            this.time = System.currentTimeMillis();
        }

        public void setState(boolean state) {
            if (this.state != state) {
                this.state = state;
                this.time = System.currentTimeMillis();
            }
        }

        public float getFactor() {
            if (state) return 1.0f;
            long elapsed = System.currentTimeMillis() - time;
            float progress = Math.min(1.0f, elapsed / (float) duration);
            return 1.0f - progress;
        }
    }

    public enum SpeedmineMode {
        PACKET,
        DAMAGE
    }

    public enum Swap {
        NORMAL,
        SILENT,
        SILENT_ALT,
        OFF
    }
}

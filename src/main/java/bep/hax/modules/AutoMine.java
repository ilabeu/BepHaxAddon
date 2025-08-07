package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

import java.util.*;

public class AutoMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgCrystal = settings.createGroup("Crystal");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses while eating.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates when mining.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> enemyRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("enemy-range")
        .description("Range to search for enemies.")
        .defaultValue(5.0)
        .min(1.0)
        .sliderRange(1.0, 10.0)
        .build()
    );

    private final Setting<Boolean> avoidSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("avoid-self")
        .description("Avoids mining blocks that would affect you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> manualOverride = sgGeneral.add(new BoolSetting.Builder()
        .name("manual-override")
        .description("Manual mining overrides automatic targets.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> retargetDelay = sgGeneral.add(new IntSetting.Builder()
        .name("retarget-delay")
        .description("Delay in ticks before choosing a new target.")
        .defaultValue(10)
        .min(0)
        .sliderMax(40)
        .build()
    );

    // Targeting
    private final Setting<Priority> antiBurrowPriority = sgTargeting.add(new EnumSetting.Builder<Priority>()
        .name("anti-burrow")
        .description("Priority for mining burrow blocks.")
        .defaultValue(Priority.Highest)
        .build()
    );

    private final Setting<Priority> cevPriority = sgTargeting.add(new EnumSetting.Builder<Priority>()
        .name("cev-breaker")
        .description("Priority for cev breaking.")
        .defaultValue(Priority.High)
        .build()
    );

    private final Setting<Priority> trapCevPriority = sgTargeting.add(new EnumSetting.Builder<Priority>()
        .name("trap-cev")
        .description("Priority for trap cev.")
        .defaultValue(Priority.Normal)
        .build()
    );

    private final Setting<Priority> surroundPriority = sgTargeting.add(new EnumSetting.Builder<Priority>()
        .name("surround-miner")
        .description("Priority for mining enemy surround.")
        .defaultValue(Priority.Normal)
        .build()
    );

    private final Setting<Priority> cityPriority = sgTargeting.add(new EnumSetting.Builder<Priority>()
        .name("auto-city")
        .description("Priority for auto city.")
        .defaultValue(Priority.Normal)
        .build()
    );

    // Crystal
    private final Setting<Boolean> placeCrystals = sgCrystal.add(new BoolSetting.Builder()
        .name("place-crystals")
        .description("Places crystals for cev/city.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakCrystals = sgCrystal.add(new BoolSetting.Builder()
        .name("break-crystals")
        .description("Breaks crystals after placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> breakDelay = sgCrystal.add(new DoubleSetting.Builder()
        .name("break-delay")
        .description("Delay before breaking crystals.")
        .defaultValue(0.1)
        .min(0)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Double> breakTimeout = sgCrystal.add(new DoubleSetting.Builder()
        .name("break-timeout")
        .description("Stop trying to break crystal after this time.")
        .defaultValue(2.0)
        .min(0.5)
        .sliderRange(0.5, 5)
        .build()
    );

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders target info.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderAutoTarget = sgRender.add(new BoolSetting.Builder()
        .name("render-auto-target")
        .description("Renders automatic target selection.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
        .name("target-color")
        .description("Color for target rendering.")
        .defaultValue(new SettingColor(255, 0, 0, 100))
        .visible(render::get)
        .build()
    );

    // State
    private PlayerEntity currentTarget;
    public MineTarget currentMineTarget;
    private boolean isManualMining = false;
    private int retargetTimer = 0;
    private final Map<BlockPos, CrystalTask> crystalTasks = new HashMap<>();
    private final List<BlockPos> recentlyMined = new ArrayList<>();

    public AutoMine() {
        super(Bep.CATEGORY, "auto-mine", "Automatically targets blocks to mine for PvP.");
    }

    @Override
    public void onActivate() {
        currentTarget = null;
        currentMineTarget = null;
        isManualMining = false;
        retargetTimer = 0;
        crystalTasks.clear();
        recentlyMined.clear();
    }

    @Override
    public void onDeactivate() {
        crystalTasks.clear();
        recentlyMined.clear();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (pauseOnEat.get() && mc.player.isUsingItem()) return;

        // Update crystal tasks
        updateCrystalTasks();

        // Clean old mined positions
        recentlyMined.removeIf(pos -> !isRecentlyMined(pos));

        // Update target player
        updateTargetPlayer();

        // Check if we should retarget
        if (retargetTimer > 0) {
            retargetTimer--;
            return;
        }

        // Don't auto-target if manually mining
        if (isManualMining && manualOverride.get()) {
            // Check if manual mining is still active via BepMine
            BepMine bepMine = Modules.get().get(BepMine.class);
            if (bepMine == null || !bepMine.isActive() || !bepMine.isMining()) {
                isManualMining = false;
            } else {
                return;
            }
        }

        // Find and execute best mining target
        if (currentTarget != null) {
            MineTarget bestTarget = findBestTarget();
            if (bestTarget != null && (currentMineTarget == null || bestTarget.priority > currentMineTarget.priority)) {
                executeTarget(bestTarget);
                currentMineTarget = bestTarget;
                retargetTimer = retargetDelay.get();
            }
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        // Mark as manual mining
        isManualMining = true;
        currentMineTarget = null;

        // Add to recently mined so we don't auto-target it
        recentlyMined.add(event.blockPos);
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            BlockPos pos = packet.getPos();

            // Check if this was our target
            if (currentMineTarget != null && pos.equals(currentMineTarget.pos)) {
                if (!packet.getState().isSolid()) {
                    // Block was mined successfully
                    handleMinedBlock(currentMineTarget);
                    currentMineTarget = null;
                    retargetTimer = 0;
                }
            }

            // Update recently mined
            if (!packet.getState().isSolid()) {
                recentlyMined.add(pos);
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        // Render current target
        if (renderAutoTarget.get() && currentMineTarget != null) {
            event.renderer.box(currentMineTarget.pos, targetColor.get(), targetColor.get(), ShapeMode.Lines, 0);
        }

        // Render crystal positions
        for (CrystalTask task : crystalTasks.values()) {
            event.renderer.box(task.pos, targetColor.get(), targetColor.get(), ShapeMode.Lines, 0);
        }
    }

    private void updateTargetPlayer() {
        PlayerEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (Friends.get().isFriend(player)) continue;
            if (player.isDead() || player.getHealth() <= 0) continue;

            double distance = player.distanceTo(mc.player);
            if (distance > enemyRange.get()) continue;

            if (distance < bestDistance) {
                bestTarget = player;
                bestDistance = distance;
            }
        }

        currentTarget = bestTarget;
    }

    private MineTarget findBestTarget() {
        if (currentTarget == null) return null;

        List<MineTarget> potentialTargets = new ArrayList<>();

        // Check anti-burrow
        if (antiBurrowPriority.get() != Priority.Disabled) {
            MineTarget target = checkAntiBurrow();
            if (target != null) potentialTargets.add(target);
        }

        // Check cev
        if (cevPriority.get() != Priority.Disabled) {
            MineTarget target = checkCev();
            if (target != null) potentialTargets.add(target);
        }

        // Check trap cev
        if (trapCevPriority.get() != Priority.Disabled) {
            MineTarget target = checkTrapCev();
            if (target != null) potentialTargets.add(target);
        }

        // Check surround
        if (surroundPriority.get() != Priority.Disabled) {
            MineTarget target = checkSurround();
            if (target != null) potentialTargets.add(target);
        }

        // Check city
        if (cityPriority.get() != Priority.Disabled) {
            MineTarget target = checkCity();
            if (target != null) potentialTargets.add(target);
        }

        // Return highest priority target
        return potentialTargets.stream()
            .max(Comparator.comparingInt(t -> t.priority))
            .orElse(null);
    }

    private MineTarget checkAntiBurrow() {
        BlockPos feetPos = currentTarget.getBlockPos();
        if (isValidMiningTarget(feetPos) && !isRecentlyMined(feetPos)) {
            return new MineTarget(feetPos, MineType.ANTI_BURROW, antiBurrowPriority.get().value, null);
        }
        return null;
    }

    private MineTarget checkCev() {
        BlockPos headPos = currentTarget.getBlockPos().up(2);
        if (isValidMiningTarget(headPos) && !isRecentlyMined(headPos)) {
            BlockState state = mc.world.getBlockState(headPos);
            if (state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.ENDER_CHEST) {
                BlockPos crystalPos = headPos.up();
                if (canPlaceCrystal(crystalPos)) {
                    return new MineTarget(headPos, MineType.CEV, cevPriority.get().value, crystalPos);
                }
            }
        }
        return null;
    }

    private MineTarget checkTrapCev() {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = currentTarget.getBlockPos().up().offset(dir);
            if (isValidMiningTarget(pos) && !isRecentlyMined(pos)) {
                BlockState state = mc.world.getBlockState(pos);
                if (state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.ENDER_CHEST) {
                    BlockPos crystalPos = pos.up();
                    if (canPlaceCrystal(crystalPos)) {
                        return new MineTarget(pos, MineType.TRAP_CEV, trapCevPriority.get().value, crystalPos);
                    }
                }
            }
        }
        return null;
    }

    private MineTarget checkSurround() {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = currentTarget.getBlockPos().offset(dir);
            if (isValidMiningTarget(pos) && !isRecentlyMined(pos)) {
                if (!avoidSelf.get() || !wouldAffectPlayer(pos, mc.player)) {
                    return new MineTarget(pos, MineType.SURROUND, surroundPriority.get().value, null);
                }
            }
        }
        return null;
    }

    private MineTarget checkCity() {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos pos = currentTarget.getBlockPos().offset(dir);
            if (isValidMiningTarget(pos) && !isRecentlyMined(pos)) {
                if (!avoidSelf.get() || !wouldAffectPlayer(pos, mc.player)) {
                    BlockPos crystalPos = pos.offset(dir);
                    if (canPlaceCrystal(crystalPos)) {
                        return new MineTarget(pos, MineType.CITY, cityPriority.get().value, crystalPos);
                    }
                }
            }
        }
        return null;
    }

    private void executeTarget(MineTarget target) {
        // Start mining through StartBreakingBlockEvent
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(target.pos), Rotations.getPitch(target.pos), () -> {
                sendMineStart(target.pos);
            });
        } else {
            sendMineStart(target.pos);
        }
    }

    private void sendMineStart(BlockPos pos) {
        // This will trigger BepMine's onStartBreakingBlock handler
        mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void handleMinedBlock(MineTarget target) {
        if (target.crystalPos != null && placeCrystals.get()) {
            // Queue crystal placement
            crystalTasks.put(target.crystalPos, new CrystalTask(target.crystalPos, System.currentTimeMillis()));
        }
    }

    private void updateCrystalTasks() {
        Iterator<Map.Entry<BlockPos, CrystalTask>> iterator = crystalTasks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, CrystalTask> entry = iterator.next();
            CrystalTask task = entry.getValue();

            // Check timeout
            if (System.currentTimeMillis() - task.startTime > breakTimeout.get() * 1000) {
                iterator.remove();
                continue;
            }

            // Try to place crystal if not placed
            if (!task.placed) {
                if (placeCrystal(task.pos)) {
                    task.placed = true;
                    task.placeTime = System.currentTimeMillis();
                }
            }
            // Try to break crystal if placed
            else if (breakCrystals.get() && System.currentTimeMillis() - task.placeTime > breakDelay.get() * 1000) {
                EndCrystalEntity crystal = getCrystalAt(task.pos);
                if (crystal != null) {
                    attackCrystal(crystal);
                    iterator.remove();
                }
            }
        }
    }

    private boolean placeCrystal(BlockPos pos) {
        FindItemResult crystal = InvUtils.findInHotbar(Items.END_CRYSTAL);
        if (!crystal.found()) return false;

        BlockPos placePos = pos.down();
        if (!BlockUtils.canPlace(placePos)) return false;

        Hand hand = crystal.getHand();
        if (hand == null) {
            InvUtils.swap(crystal.slot(), false);
            hand = Hand.MAIN_HAND;
        }

        BlockHitResult result = new BlockHitResult(
            Vec3d.ofCenter(placePos),
            Direction.UP,
            placePos,
            false
        );

        mc.interactionManager.interactBlock(mc.player, hand, result);
        mc.player.swingHand(hand);

        if (crystal.getHand() == null) {
            InvUtils.swapBack();
        }

        return true;
    }

    private void attackCrystal(EndCrystalEntity crystal) {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(crystal), Rotations.getPitch(crystal), () -> {
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                mc.player.swingHand(Hand.MAIN_HAND);
            });
        } else {
            mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean isValidMiningTarget(BlockPos pos) {
        if (!BlockUtils.canBreak(pos)) return false;

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir() || state.getBlock() == Blocks.BEDROCK) return false;

        double distance = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        return distance <= 6.0;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockPos below = pos.down();
        BlockState belowState = mc.world.getBlockState(below);

        if (belowState.getBlock() != Blocks.OBSIDIAN && belowState.getBlock() != Blocks.BEDROCK) {
            return false;
        }

        Box crystalBox = new Box(pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);

        return !EntityUtils.intersectsWithEntity(crystalBox, entity -> !(entity instanceof EndCrystalEntity));
    }

    private EndCrystalEntity getCrystalAt(BlockPos pos) {
        Box searchBox = new Box(pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);

        for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
            if (entity instanceof EndCrystalEntity crystal) {
                return crystal;
            }
        }
        return null;
    }

    private boolean wouldAffectPlayer(BlockPos pos, PlayerEntity player) {
        Box playerBox = player.getBoundingBox();
        Box blockBox = new Box(pos);
        return playerBox.intersects(blockBox.expand(1));
    }

    private boolean isRecentlyMined(BlockPos pos) {
        // Check if block was mined in last 5 seconds
        return recentlyMined.contains(pos) && !mc.world.getBlockState(pos).isSolid();
    }

    // Data classes
    private static class MineTarget {
        public final BlockPos pos;
        public final MineType type;
        public final int priority;
        public final BlockPos crystalPos;

        public MineTarget(BlockPos pos, MineType type, int priority, BlockPos crystalPos) {
            this.pos = pos;
            this.type = type;
            this.priority = priority;
            this.crystalPos = crystalPos;
        }
    }

    private static class CrystalTask {
        public final BlockPos pos;
        public final long startTime;
        public boolean placed;
        public long placeTime;

        public CrystalTask(BlockPos pos, long startTime) {
            this.pos = pos;
            this.startTime = startTime;
            this.placed = false;
        }
    }

    public enum Priority {
        Disabled(-1),
        Lowest(0),
        Low(1),
        Normal(2),
        High(3),
        Highest(4);

        public final int value;

        Priority(int value) {
            this.value = value;
        }
    }

    public enum MineType {
        ANTI_BURROW,
        CEV,
        TRAP_CEV,
        SURROUND,
        CITY
    }

    // Public API methods for external modules to call
    public void onStart(BlockPos pos) {
        // Called when mining starts
        if (currentMineTarget != null && currentMineTarget.pos.equals(pos)) {
            // This is our target being mined
            info("Started mining " + currentMineTarget.type + " at " + pos);
        } else {
            // Manual mining started
            isManualMining = true;
            currentMineTarget = null;
            recentlyMined.add(pos);
        }
    }

    public void onAbort(BlockPos pos) {
        // Called when mining is aborted
        if (currentMineTarget != null && currentMineTarget.pos.equals(pos)) {
            info("Aborted mining " + currentMineTarget.type + " at " + pos);
            currentMineTarget = null;
            retargetTimer = 0;
        }
    }

    public void onStop(BlockPos pos) {
        // Called when mining completes
        if (currentMineTarget != null && currentMineTarget.pos.equals(pos)) {
            info("Completed mining " + currentMineTarget.type + " at " + pos);
            handleMinedBlock(currentMineTarget);
            currentMineTarget = null;
            retargetTimer = 0;
        }
        recentlyMined.add(pos);
    }

    // Public API to check if AutoMine is targeting a specific position
    public boolean isTargeting(BlockPos pos) {
        return currentMineTarget != null && currentMineTarget.pos.equals(pos);
    }

    // Public API to get current target info
    public MineType getCurrentTargetType() {
        return currentMineTarget != null ? currentMineTarget.type : null;
    }

    // Public API to check if we should place a crystal at a position
    public boolean shouldPlaceCrystal(BlockPos pos) {
        return crystalTasks.containsKey(pos);
    }
}
package bep.hax.modules;
import bep.hax.Bep;
import bep.hax.modules.PVPModule;
import bep.hax.util.InventoryManager;
import bep.hax.util.PlacementUtils;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.*;
public class Surround extends PVPModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-delay")
        .description("Ticks between placements")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 5)
        .build()
    );
    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
        .name("blocks-per-tick")
        .description("Blocks to place per tick")
        .defaultValue(8)
        .min(1)
        .sliderRange(1, 20)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Use silent rotations")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Placement range")
        .defaultValue(4.0)
        .min(0.0)
        .sliderRange(0.0, 6.0)
        .build()
    );
    private final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
        .name("attack-crystals")
        .description("Attacks crystals in the way")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> headLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("head-level")
        .description("Place blocks at Y+1 level")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> coverHead = sgGeneral.add(new BoolSetting.Builder()
        .name("cover-head")
        .description("Place block at Y+2")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> mineExtend = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-extend")
        .description("Extends if being mined")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> jumpDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables after jumping")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
        .name("multitask")
        .description("Place while using items")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> grimPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("grim-place")
        .description("Uses GrimAirPlace exploit for block placement (bypass anti-cheat)")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render placements")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Shape render mode")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Side color")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .visible(render::get)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Line color")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(render::get)
        .build()
    );
    private static final List<Block> RESISTANT_BLOCKS = Arrays.asList(
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.ENDER_CHEST
    );
    private final Map<BlockPos, Long> placementTimes = new HashMap<>();
    private final List<BlockPos> placements = new ArrayList<>();
    private double prevY;
    private int delayCounter = 0;
    private InventoryManager inventoryManager;
    private int lastSlot = -1;
    public Surround() {
        super(Bep.CATEGORY, "surround", "Surrounds feet with obsidian");
    }
    @Override
    public void onActivate() {
        if (mc.player == null) return;
        prevY = mc.player.getY();
        placementTimes.clear();
        placements.clear();
        delayCounter = 0;
        inventoryManager = InventoryManager.getInstance();
        lastSlot = -1;
    }
    @Override
    public void onDeactivate() {
        placementTimes.clear();
        placements.clear();
        delayCounter = 0;
        if (inventoryManager != null) {
            inventoryManager.syncToClient();
        }
        lastSlot = -1;
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        long currentTime = System.currentTimeMillis();
        placementTimes.entrySet().removeIf(entry ->
            !mc.world.getBlockState(entry.getKey()).isReplaceable() ||
            currentTime - entry.getValue() > 100
        );
        if (jumpDisable.get() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f)) {
            toggle();
            return;
        }
        if (!multitask.get() && mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) {
            placements.clear();
            return;
        }
        int slot = getResistantBlockSlot();
        if (slot == -1) {
            placements.clear();
            return;
        }
        List<BlockPos> surroundPositions = calculateSurround();
        if (surroundPositions.isEmpty()) {
            placements.clear();
            return;
        }
        if (attack.get()) {
            attackCrystals(surroundPositions);
        }
        placements.clear();
        for (BlockPos pos : surroundPositions) {
            if (mc.world.getBlockState(pos).isReplaceable() &&
                mc.player.squaredDistanceTo(pos.toCenterPos()) <= placeRange.get() * placeRange.get()) {
                placements.add(pos);
            }
        }
        if (placements.isEmpty()) return;
        placeBlocks(slot, currentTime);
    }
    private List<BlockPos> calculateSurround() {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int footY = playerPos.getY();
        Box box = mc.player.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX - 0.0001);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ - 0.0001);
        Set<BlockPos> footBlocks = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                footBlocks.add(new BlockPos(x, footY, z));
            }
        }
        for (BlockPos foot : footBlocks) {
            positions.add(foot.north());
            positions.add(foot.south());
            positions.add(foot.east());
            positions.add(foot.west());
        }
        positions.removeAll(footBlocks);
        if (headLevel.get()) {
            Set<BlockPos> headPositions = new HashSet<>();
            for (BlockPos foot : footBlocks) {
                BlockPos up = foot.up();
                headPositions.add(up.north());
                headPositions.add(up.south());
                headPositions.add(up.east());
                headPositions.add(up.west());
            }
            for (BlockPos foot : footBlocks) {
                headPositions.remove(foot.up());
            }
            positions.addAll(headPositions);
        }
        if (coverHead.get()) {
            for (BlockPos foot : footBlocks) {
                positions.add(foot.up(2));
            }
        }
        if (mineExtend.get()) {
            Set<BlockPos> extended = new HashSet<>();
            for (BlockPos pos : new ArrayList<>(positions)) {
                if (mc.world.getBlockState(pos).isReplaceable()) continue;
                if (mc.world.getBlockState(pos).getHardness(mc.world, pos) < 0) continue;
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    BlockPos extPos = pos.offset(dir);
                    if (!footBlocks.contains(extPos) && !positions.contains(extPos)) {
                        extended.add(extPos);
                    }
                }
            }
            positions.addAll(extended);
        }
        return new ArrayList<>(positions);
    }
    private void placeBlocks(int slot, long currentTime) {
        delayCounter++;
        if (delayCounter < placeDelay.get()) return;
        delayCounter = 0;
        if (lastSlot != slot) {
            inventoryManager.setSlot(slot);
            lastSlot = slot;
        }
        int placed = 0;
        for (BlockPos pos : placements) {
            if (placed >= blocksPerTick.get()) break;
            if (placementTimes.containsKey(pos)) continue;
            if (!mc.world.getBlockState(pos).isReplaceable()) continue;
            if (!mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, net.minecraft.block.ShapeContext.absent())) {
                continue;
            }
            boolean success;
            if (grimPlace.get()) {
                if (rotate.get()) {
                    float[] rot = bep.hax.util.RotationUtils.getRotationsTo(mc.player.getEyePos(), Vec3d.ofCenter(pos));
                    setRotationSilent(rot[0], rot[1]);
                }
                airPlace(pos);
                success = true;
            } else {
                success = normalPlace(pos);
            }
            if (success) {
                placementTimes.put(pos, currentTime);
                placed++;
            }
        }
    }
    private void airPlace(BlockPos pos) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ORIGIN,
            Direction.DOWN
        ));
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
            Hand.OFF_HAND,
            hit,
            mc.player.currentScreenHandler.getRevision() + 2
        ));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ORIGIN,
            Direction.DOWN
        ));
        mc.player.swingHand(Hand.MAIN_HAND);
    }
    private boolean normalPlace(BlockPos pos) {
        Direction side = PlacementUtils.getPlaceSide(pos);
        if (side == null) return false;
        BlockPos neighbor = pos.offset(side);
        Direction opposite = side.getOpposite();
        Vec3d hitPos = Vec3d.ofCenter(neighbor).add(Vec3d.of(opposite.getVector()).multiply(0.5));
        if (rotate.get()) {
            float[] rot = bep.hax.util.RotationUtils.getRotationsTo(mc.player.getEyePos(), hitPos);
            setRotationSilent(rot[0], rot[1]);
        }
        BlockHitResult hitResult = new BlockHitResult(hitPos, opposite, neighbor, false);
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
            Hand.MAIN_HAND,
            hitResult,
            0
        ));
        mc.player.swingHand(Hand.MAIN_HAND);
        return true;
    }
    private void attackCrystals(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            Entity crystal = mc.world.getOtherEntities(null, new Box(pos)).stream()
                .filter(e -> e instanceof EndCrystalEntity)
                .findFirst()
                .orElse(null);
            if (crystal != null) {
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                return;
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placements.isEmpty()) return;
        for (BlockPos pos : placements) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    private int getResistantBlockSlot() {
        for (Block block : RESISTANT_BLOCKS) {
            int slot = InvUtils.findInHotbar(stack ->
                stack.getItem() instanceof BlockItem item &&
                item.getBlock() == block
            ).slot();
            if (slot != -1) return slot;
        }
        return -1;
    }
}
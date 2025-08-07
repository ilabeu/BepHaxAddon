package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import bep.hax.util.PushOutOfBlocksEvent;
import bep.hax.util.PVPRotationManager;
import bep.hax.util.PlacementUtils;
import bep.hax.util.RotationUtils;

/**
 * Multi-mode phase module for clipping through blocks
 * Ported from PVP with optimizations for Meteor Client architecture
 * Supports Normal, Sand, Climb, Pearl, and Clip modes for various bypass techniques
 */
public class Phase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgClipping = settings.createGroup("Clipping");

    // Main settings
    private final Setting<PhaseMode> mode = sgGeneral.add(new EnumSetting.Builder<PhaseMode>()
        .name("mode")
        .description("The phase mode for clipping into blocks.")
        .defaultValue(PhaseMode.Normal)
        .build()
    );

    // Pearl mode settings
    private final Setting<Integer> pitch = sgPearl.add(new IntSetting.Builder()
        .name("pitch")
        .description("The pitch angle to throw pearls.")
        .defaultValue(85)
        .range(70, 90)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> swapAlternative = sgPearl.add(new BoolSetting.Builder()
        .name("swap-alternative")
        .description("Uses inventory swap for swapping to pearls.")
        .defaultValue(true)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> attack = sgPearl.add(new BoolSetting.Builder()
        .name("attack")
        .description("Attacks entities in the way of the pearl phase.")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> swing = sgPearl.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand when throwing pearls.")
        .defaultValue(true)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    private final Setting<Boolean> selfFill = sgPearl.add(new BoolSetting.Builder()
        .name("self-fill")
        .description("Automatically fills blocks you are phasing on.")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );

    // Clipping mode settings
    private final Setting<Double> blocks = sgClipping.add(new DoubleSetting.Builder()
        .name("blocks")
        .description("The block distance to phase clip.")
        .defaultValue(0.003)
        .range(0.001, 10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    private final Setting<Double> distance = sgClipping.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance to phase.")
        .defaultValue(0.2)
        .range(0.0, 10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    private final Setting<Boolean> autoClip = sgClipping.add(new BoolSetting.Builder()
        .name("auto-clip")
        .description("Automatically clips into the block.")
        .defaultValue(true)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );

    // State tracking
    private boolean wasPhasing = false;
    private int tickCounter = 0;

    public Phase() {
        super(Bep.CATEGORY, "phase", "Allows player to phase through solid blocks.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        // Handle pearl mode activation
        if (mode.get() == PhaseMode.Pearl) {
            performPearlPhase();
            toggle(); // Pearl mode is one-time use
            return;
        }

        // Handle clip mode activation
        if (mode.get() == PhaseMode.Clip) {
            performClipPhase();
            toggle(); // Clip mode is one-time use
            return;
        }

        // Auto-clip for Normal mode
        if (autoClip.get() && mode.get() == PhaseMode.Normal) {
            performAutoClip();
        }

        wasPhasing = false;
        tickCounter = 0;
    }

    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.noClip = false;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;

        // Handle Clip mode tick behavior
        if (mode.get() == PhaseMode.Clip && mc.player.isOnGround() && !mc.player.hasVehicle()) {
            performClipTick();
            toggle();
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.get()) {
            case Normal -> handleNormalMovement(event);
            case Sand -> handleSandMovement(event);
            case Climb -> handleClimbMovement(event);
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        // Cancel push out of blocks for all modes
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (packet.getEntityId() == mc.player.getId() && isActive()) {
                // Allow velocity but prevent push-out effects
                Vec3d velocity = new Vec3d(packet.getVelocityX() / 8000.0, packet.getVelocityY() / 8000.0, packet.getVelocityZ() / 8000.0);
                if (velocity.lengthSquared() < 0.1) { // Likely a push-out packet
                    event.cancel();
                }
            }
        }
    }

    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.player == null || mc.world == null) return;

        switch (mode.get()) {
            case Normal -> {
                if (event.shape != VoxelShapes.empty() &&
                    event.shape.getBoundingBox().maxY > mc.player.getBoundingBox().minY &&
                    mc.player.isSneaking()) {
                    event.cancel();
                    event.shape = VoxelShapes.empty();
                }
            }
            case Sand -> {
                event.cancel();
                event.shape = VoxelShapes.empty();
                mc.player.noClip = true;
            }
            case Climb -> {
                if (mc.player.horizontalCollision) {
                    event.cancel();
                    event.shape = VoxelShapes.empty();
                }
                if (mc.options.sneakKey.isPressed() || (mc.options.jumpKey.isPressed() && event.pos.getY() > mc.player.getY())) {
                    event.cancel();
                }
            }
        }
    }

    @EventHandler
    private void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        if (isActive()) {
            event.cancel();
        }
    }

    /**
     * Performs pearl-based phasing - exact PVP implementation
     */
    private void performPearlPhase() {
        int pearlSlot = PlacementUtils.getEnderPearlSlot();
        if (pearlSlot == -1 || mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL.getDefaultStack())) {
            return;
        }

        float prevYaw = mc.player.getYaw();
        float prevPitch = mc.player.getPitch();
        final Vec3d pearlTargetVec = new Vec3d(Math.floor(mc.player.getX()) + 0.5, 0.0, Math.floor(mc.player.getZ()) + 0.5);
        float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePos(), pearlTargetVec);
        float yaw = rotations[0] + 180.0f;  // normalize

        // Attack entities if enabled
        if (attack.get()) {
            handlePearlAttacks(yaw);
        }

        // Self-fill if enabled
        if (selfFill.get()) {
            handleSelfFill(yaw);
        }

        // Set client rotation for visual feedback
        PVPRotationManager rotationManager = PVPRotationManager.getInstance();
        rotationManager.setRotationClient(yaw, pitch.get());

        // Swap to pearl using exact PVP method
        if (swapAlternative.get()) {
            performInventorySwapPVP(pearlSlot);
        } else if (pearlSlot < 9) {
            // Use direct slot setting like PVP
            mc.player.getInventory().selectedSlot = pearlSlot;
        }

        // Set silent rotation for server
        rotationManager.setRotationSilent(yaw, pitch.get());

        // Throw pearl with exact PVP packet method
        mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, yaw, pitch.get()));

        if (swing.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        // Restore swap using exact PVP method
        if (swapAlternative.get()) {
            performInventorySwapPVP(pearlSlot);
        } else if (pearlSlot < 9) {
            // Sync inventory back
            mc.player.getInventory().selectedSlot = mc.player.getInventory().selectedSlot;
        }

        // Restore rotations using PVP method
        rotationManager.setRotationSilentSync();
        rotationManager.setRotationClient(prevYaw, prevPitch);
    }

    /**
     * Handles attacks for pearl phasing
     */
    private void handlePearlAttacks(float yaw) {
        // Attack item frames in the way
        BlockHitResult hitResult = (BlockHitResult) mc.player.raycast(3.0, 0, false);
        Box searchBox = Box.from(Vec3d.ofCenter(hitResult.getBlockPos())).expand(0.2);

        for (Entity entity : mc.world.getOtherEntities(null, searchBox)) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }

        // Break scaffolding blocks
        BlockState state = mc.world.getBlockState(mc.player.getBlockPos());
        if (state.getBlock() instanceof ScaffoldingBlock) {
            BlockPos pos = mc.player.getBlockPos();
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }

    /**
     * Handles self-fill placement for pearl phasing - exact PVP implementation
     */
    private void handleSelfFill(float yaw) {
        // Exact PVP directional placement logic
        float yaw1 = yaw % 360.0f;
        if (yaw1 < 0.0f) {
            yaw1 += 360.0f;
        }

        BlockPos blockPos = mc.player.getBlockPos();
        if (yaw1 >= 22.5 && yaw1 < 67.5) {
            blockPos = blockPos.south().west();
        } else if (yaw1 >= 67.5 && yaw1 < 112.5) {
            blockPos = blockPos.west();
        } else if (yaw1 >= 112.5 && yaw1 < 157.5) {
            blockPos = blockPos.north().west();
        } else if (yaw1 >= 157.5 && yaw1 < 202.5) {
            blockPos = blockPos.north();
        } else if (yaw1 >= 202.5 && yaw1 < 247.5) {
            blockPos = blockPos.north().east();
        } else if (yaw1 >= 247.5 && yaw1 < 292.5) {
            blockPos = blockPos.east();
        } else if (yaw1 >= 292.5 && yaw1 < 337.5) {
            blockPos = blockPos.south().east();
        } else {
            blockPos = blockPos.south();
        }

        // Find resistant block and place using exact PVP conditions
        FindItemResult resistantBlock = PlacementUtils.findResistantBlock();
        if (resistantBlock.found() && blockPos != null && !mc.world.getBlockState(blockPos.down()).isReplaceable()) {
            // Use rotation callback like original PVP
            PVPRotationManager rotationManager = PVPRotationManager.getInstance();
            PlacementUtils.placeBlock(blockPos, true, true, true); // strict direction, rotate, swing
        }
    }

    /**
     * Performs inventory swapping for pearls - exact PVP method
     */
    private void performInventorySwapPVP(int pearlSlot) {
        // Exact PVP inventory swap implementation
        mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    /**
     * Performs auto-clip movement
     */
    private void performAutoClip() {
        if (mc.player == null || mc.world == null) return;

        double cos = Math.cos(Math.toRadians(mc.player.getYaw() + 90.0f));
        double sin = Math.sin(Math.toRadians(mc.player.getYaw() + 90.0f));

        double newX = mc.player.getX() + (blocks.get() * cos);
        double newZ = mc.player.getZ() + (blocks.get() * sin);

        mc.player.setPosition(newX, mc.player.getY(), newZ);
    }

    /**
     * Performs clip phase tick behavior
     */
    private void performClipTick() {
        Vec3d center = mc.player.getBlockPos().toCenterPos();
        boolean flagX = (center.x - mc.player.getX()) > 0;
        boolean flagZ = (center.z - mc.player.getZ()) > 0;

        double x = center.x + 0.2 * (flagX ? -1 : 1);
        double z = center.z + 0.2 * (flagZ ? -1 : 1);

        mc.player.setPosition(x, mc.player.getY(), z);
    }

    /**
     * Performs simple clip phase
     */
    private void performClipPhase() {
        performClipTick();
    }

    /**
     * Handles normal phase movement
     */
    private void handleNormalMovement(PlayerMoveEvent event) {
        if (!mc.player.isSneaking() || !PlacementUtils.isPhasing()) return;

        float yaw = mc.player.getYaw();
        double offsetX = distance.get() * Math.cos(Math.toRadians(yaw + 90.0f));
        double offsetZ = distance.get() * Math.sin(Math.toRadians(yaw + 90.0f));

        Box newBB = mc.player.getBoundingBox().offset(offsetX, 0.0, offsetZ);
        mc.player.setBoundingBox(newBB);
    }

    /**
     * Handles sand phase movement (noclip)
     */
    private void handleSandMovement(PlayerMoveEvent event) {
        mc.player.noClip = true;

        double yMotion = 0.0;
        if (mc.options.jumpKey.isPressed()) {
            yMotion = 0.3;
        } else if (mc.options.sneakKey.isPressed()) {
            yMotion = -0.3;
        }

        event.movement = new Vec3d(event.movement.x, yMotion, event.movement.z);
    }

    /**
     * Handles climb phase movement
     */
    private void handleClimbMovement(PlayerMoveEvent event) {
        // Allow climbing when colliding horizontally
        if (mc.player.horizontalCollision) {
            double yMotion = event.movement.y;
            if (mc.options.jumpKey.isPressed()) {
                yMotion = 0.3;
            } else if (mc.options.sneakKey.isPressed()) {
                yMotion = -0.3;
            }
            event.movement = new Vec3d(event.movement.x, yMotion, event.movement.z);
        }
    }


    @Override
    public String getInfoString() {
        return mode.get().toString();
    }

    public enum PhaseMode {
        Normal("Normal"),
        Sand("Sand"),
        Climb("Climb"),
        Pearl("Pearl"),
        Clip("Clip");

        private final String title;

        PhaseMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}

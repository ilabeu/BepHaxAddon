package bep.hax.modules;

import bep.hax.Bep;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StashMover extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgInput = settings.createGroup("Input");
    private final SettingGroup sgPearl = settings.createGroup("Pearl Loading");
    private final SettingGroup sgGoBack = settings.createGroup("Go Back");
    private final SettingGroup sgResetPearl = settings.createGroup("Reset Pearl");
    private final SettingGroup sgDelays = settings.createGroup("Delays");
    private final SettingGroup sgRendering = settings.createGroup("Rendering");

    // General Settings
    private final Setting<Boolean> pauseOnLag = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pause when server is lagging")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> maxRetries = sgGeneral.add(new IntSetting.Builder()
        .name("max-retries")
        .description("Maximum retries for failed actions")
        .defaultValue(3)
        .min(1)
        .max(10)
        .build()
    );

    // Input Settings
    private final Setting<Boolean> onlyShulkers = sgInput.add(new BoolSetting.Builder()
        .name("only-shulkers")
        .description("Only take shulker boxes from input chests")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> breakEmptyContainers = sgInput.add(new BoolSetting.Builder()
        .name("break-empty")
        .description("Break empty containers after emptying them")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> fillEnderChest = sgInput.add(new BoolSetting.Builder()
        .name("fill-enderchest")
        .description("Fill ender chest to maximize movement")
        .defaultValue(true)
        .build()
    );

    // Pearl Loading Settings (Input to Output)
    private final Setting<String> pearlPlayerName = sgPearl.add(new StringSetting.Builder()
        .name("pearl-player")
        .description("Player name to message for pearl loading (Input→Output)")
        .defaultValue("Player")
        .build()
    );

    private final Setting<String> pearlCommand = sgPearl.add(new StringSetting.Builder()
        .name("pearl-command")
        .description("Command to send for pearl loading (Input→Output)")
        .defaultValue("!tp")
        .build()
    );

    private final Setting<Integer> pearlTimeout = sgPearl.add(new IntSetting.Builder()
        .name("pearl-timeout")
        .description("Timeout for pearl loading in seconds")
        .defaultValue(10)
        .min(5)
        .max(30)
        .build()
    );

    private final Setting<Integer> pearlRetryDelay = sgPearl.add(new IntSetting.Builder()
        .name("pearl-retry-delay")
        .description("Delay between pearl command retries in ticks")
        .defaultValue(100)
        .min(20)
        .max(200)
        .build()
    );

    // Go Back Settings
    private final Setting<GoBackMethod> goBackMethod = sgGoBack.add(new EnumSetting.Builder<GoBackMethod>()
        .name("go-back-method")
        .description("Method to go back to input area")
        .defaultValue(GoBackMethod.KILL)
        .build()
    );

    private final Setting<String> goBackPlayerName = sgGoBack.add(new StringSetting.Builder()
        .name("go-back-player")
        .description("Player name for go back pearl loading (Output→Input)")
        .defaultValue("PlayerName")
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    private final Setting<String> goBackCommand = sgGoBack.add(new StringSetting.Builder()
        .name("go-back-command")
        .description("Command for go back pearl loading (Output→Input)")
        .defaultValue("back")
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    // Pearl Settings - Output
    // Pickup position
    private final Setting<BlockPos> outputPearlPickupPos = sgResetPearl.add(new BlockPosSetting.Builder()
        .name("output-pickup-pos")
        .description("Position for pearl pickup at output")
        .defaultValue(new BlockPos(0, 64, 0))
        .build()
    );

    // Note: setOutputPickupHere button will be handled by getWidget() override

    // Throw position
    private final Setting<BlockPos> outputPearlThrowPos = sgResetPearl.add(new BlockPosSetting.Builder()
        .name("output-throw-pos")
        .description("Position for pearl throw at output")
        .defaultValue(new BlockPos(0, 64, 0))
        .build()
    );

    // Note: setOutputThrowHere button will be handled by getWidget() override

    private final Setting<Double> outputPearlThrowPitch = sgResetPearl.add(new DoubleSetting.Builder()
        .name("output-throw-pitch")
        .description("Pitch for throwing pearl at output (90 = straight down)")
        .defaultValue(90.0)
        .sliderRange(-90, 90)
        .build()
    );

    private final Setting<Double> outputPearlThrowYaw = sgResetPearl.add(new DoubleSetting.Builder()
        .name("output-throw-yaw")
        .description("Yaw for throwing pearl at output")
        .defaultValue(0.0)
        .sliderRange(-180, 180)
        .build()
    );

    // Pearl Settings - Input
    // Pickup position
    private final Setting<BlockPos> inputPearlPickupPos = sgResetPearl.add(new BlockPosSetting.Builder()
        .name("input-pickup-pos")
        .description("Position for pearl pickup at input")
        .defaultValue(new BlockPos(0, 64, 0))
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    // Note: setInputPickupHere button will be handled by getWidget() override and visible when PEARL method

    // Throw position
    private final Setting<BlockPos> inputPearlThrowPos = sgResetPearl.add(new BlockPosSetting.Builder()
        .name("input-throw-pos")
        .description("Position for pearl throw at input")
        .defaultValue(new BlockPos(0, 64, 0))
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    // Note: setInputThrowHere button will be handled by getWidget() override and visible when PEARL method

    private final Setting<Double> inputPearlThrowPitch = sgResetPearl.add(new DoubleSetting.Builder()
        .name("input-throw-pitch")
        .description("Pitch for throwing pearl at input (90 = straight down)")
        .defaultValue(90.0)
        .sliderRange(-90, 90)
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    private final Setting<Double> inputPearlThrowYaw = sgResetPearl.add(new DoubleSetting.Builder()
        .name("input-throw-yaw")
        .description("Yaw for throwing pearl at input")
        .defaultValue(0.0)
        .sliderRange(-180, 180)
        .visible(() -> goBackMethod.get() == GoBackMethod.PEARL)
        .build()
    );

    private final Setting<Integer> pearlWaitTime = sgResetPearl.add(new IntSetting.Builder()
        .name("pearl-wait-time")
        .description("Time to wait after throwing pearl (seconds)")
        .defaultValue(5)
        .min(1)
        .max(10)
        .build()
    );

    private final Setting<Double> positionTolerance = sgResetPearl.add(new DoubleSetting.Builder()
        .name("position-tolerance")
        .description("How close to target position before throwing pearl (blocks)")
        .defaultValue(0.3)
        .min(0.1)
        .max(2.0)
        .sliderRange(0.1, 2.0)
        .build()
    );

    private final Setting<Double> trapdoorEdgeDistance = sgResetPearl.add(new DoubleSetting.Builder()
        .name("trapdoor-edge-distance")
        .description("Distance from trapdoor edge when positioning (blocks)")
        .defaultValue(0.4)
        .min(0.2)
        .max(1.0)
        .sliderRange(0.2, 1.0)
        .build()
    );

    // Delay Settings
    private final Setting<Integer> openDelay = sgDelays.add(new IntSetting.Builder()
        .name("open-delay")
        .description("Delay after opening container in ticks")
        .defaultValue(10)
        .min(5)
        .max(30)
        .build()
    );

    private final Setting<Integer> transferDelay = sgDelays.add(new IntSetting.Builder()
        .name("transfer-delay")
        .description("Delay between item transfers in ticks")
        .defaultValue(2)
        .min(0)
        .max(10)
        .build()
    );

    private final Setting<Integer> closeDelay = sgDelays.add(new IntSetting.Builder()
        .name("close-delay")
        .description("Delay after closing container in ticks")
        .defaultValue(5)
        .min(0)
        .max(20)
        .build()
    );

    private final Setting<Integer> moveDelay = sgDelays.add(new IntSetting.Builder()
        .name("move-delay")
        .description("Delay between movements in ticks")
        .defaultValue(20)
        .min(5)
        .max(50)
        .build()
    );

    // Rendering Settings
    private final Setting<Boolean> renderSelection = sgRendering.add(new BoolSetting.Builder()
        .name("render-selection")
        .description("Render selection areas")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> outlineWidth = sgRendering.add(new IntSetting.Builder()
        .name("outline-width")
        .description("Width of area outlines")
        .defaultValue(2)
        .min(1)
        .max(5)
        .sliderRange(1, 5)
        .build()
    );

    private final Setting<SettingColor> inputAreaColor = sgRendering.add(new ColorSetting.Builder()
        .name("input-area-outline")
        .description("Outline color for input area")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .build()
    );

    private final Setting<SettingColor> outputAreaColor = sgRendering.add(new ColorSetting.Builder()
        .name("output-area-outline")
        .description("Outline color for output area")
        .defaultValue(new SettingColor(0, 100, 255, 255))
        .build()
    );

    private final Setting<SettingColor> inputContainerColor = sgRendering.add(new ColorSetting.Builder()
        .name("input-container-color")
        .description("Color for input containers (not empty)")
        .defaultValue(new SettingColor(0, 255, 0, 100))
        .build()
    );

    private final Setting<SettingColor> outputContainerColor = sgRendering.add(new ColorSetting.Builder()
        .name("output-container-color")
        .description("Color for output containers (not full)")
        .defaultValue(new SettingColor(0, 100, 255, 100))
        .build()
    );

    private final Setting<SettingColor> activeContainerColor = sgRendering.add(new ColorSetting.Builder()
        .name("active-container-color")
        .description("Color for currently active container")
        .defaultValue(new SettingColor(255, 255, 0, 150))
        .build()
    );

    private final Setting<SettingColor> emptyContainerColor = sgRendering.add(new ColorSetting.Builder()
        .name("empty-container-color")
        .description("Color for empty containers")
        .defaultValue(new SettingColor(128, 128, 128, 50))
        .build()
    );

    private final Setting<SettingColor> fullContainerColor = sgRendering.add(new ColorSetting.Builder()
        .name("full-container-color")
        .description("Color for full containers")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );

    // Enums
    public enum GoBackMethod {
        KILL("Kill"),
        PEARL("Pearl Loading");

        private final String name;

        GoBackMethod(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static enum SelectionMode {
        NONE,
        INPUT_FIRST,
        INPUT_SECOND,
        OUTPUT_FIRST,
        OUTPUT_SECOND
    }

    public enum ProcessState {
        IDLE,
        CHECKING_LOCATION,
        INPUT_PROCESS,
        LOADING_PEARL,
        RESET_PEARL_PICKUP,
        RESET_PEARL_PLACE_SHULKER,
        RESET_PEARL_APPROACH,
        RESET_PEARL_PREPARE,
        RESET_PEARL_THROW,
        RESET_PEARL_WAIT,
        OUTPUT_PROCESS,
        GOING_BACK,
        OPENING_CONTAINER,
        TRANSFERRING_ITEMS,
        CLOSING_CONTAINER,
        BREAKING_CONTAINER,
        MOVING_TO_CONTAINER,
        OPENING_ENDERCHEST,
        FILLING_ENDERCHEST,
        EMPTYING_ENDERCHEST,
        WAITING
    }

    // State Management
    private ProcessState currentState = ProcessState.IDLE;
    private int stateTimer = 0;
    private int retryCount = 0;
    private long lastActionTime = 0;
    private boolean isSelecting = false;

    // Area Selection - Static so they persist when module is toggled
    private static BlockPos inputAreaPos1 = null;
    private static BlockPos inputAreaPos2 = null;
    private static BlockPos outputAreaPos1 = null;
    private static BlockPos outputAreaPos2 = null;
    private static BlockPos selectionPos1 = null;
    private static SelectionMode selectionMode = SelectionMode.NONE;

    // Container Management
    private static final Set<ContainerInfo> inputContainers = ConcurrentHashMap.newKeySet();
    private static final Set<ContainerInfo> outputContainers = ConcurrentHashMap.newKeySet();
    private ContainerInfo currentContainer = null;
    private BlockPos enderChestPos = null;
    private Direction approachDirection = null;
    private BlockPos lastBaritoneGoal = null;
    private int containerOpenFailures = 0;

    // Pearl Loading
    private long lastPearlMessageTime = 0;
    private String lastRandomString = "";
    private boolean waitingForPearl = false;
    private int pearlRetryCount = 0;
    private Vec3d initialPlayerPos = null; // For teleport detection

    // Pearl tracking
    private boolean hasThrownPearl = false;
    private long pearlThrowTime = 0;
    private boolean hasPlacedShulker = false;
    private boolean isGoingToInput = false; // Track if we're resetting pearl for input or output
    private ItemStack offhandBackup = ItemStack.EMPTY;
    private int pearlFailRetries = 0; // Track pearl throw retry attempts
    private int previousSlot = -1;
    private int rotationStabilizationTimer = 0; // Timer to ensure rotation is recognized by server
    private boolean rotationSet = false; // Track if rotation has been set for throwing
    private BlockPos safeRetreatPos = null; // Safe air block position to retreat to after throwing

    // Kill command tracking
    private boolean waitingForRespawn = false;
    private long lastKillTime = 0;
    private int killRetryCount = 0;

    // Transfer tracking
    private int itemsTransferred = 0;
    private int containersProcessed = 0;
    private boolean inventoryFull = false;
    private boolean enderChestFull = false;
    private boolean enderChestHasItems = false;
    private boolean enderChestEmptied = false;

    // Container Info Class
    private static class ContainerInfo {
        public final BlockPos pos;
        public final ContainerType type;
        public boolean isEmpty = false;
        public boolean isFull = false;
        public int slotsFilled = 0;
        public int totalSlots = 27;

        public ContainerInfo(BlockPos pos, ContainerType type) {
            this.pos = pos;
            this.type = type;
            if (type == ContainerType.BARREL) {
                this.totalSlots = 27;
            } else if (type == ContainerType.DOUBLE_CHEST || type == ContainerType.DOUBLE_TRAPPED_CHEST) {
                this.totalSlots = 54;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ContainerInfo that = (ContainerInfo) o;
            return pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    private enum ContainerType {
        CHEST, DOUBLE_CHEST, TRAPPED_CHEST, DOUBLE_TRAPPED_CHEST, BARREL, ENDER_CHEST
    }

    private static StashMover INSTANCE;

    public StashMover() {
        super(Bep.CATEGORY, "stash-mover", "Automatically moves items between stash areas using pearl loading");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        // Reset state
        stateTimer = 0;
        retryCount = 0;
        itemsTransferred = 0;
        containersProcessed = 0;
        inventoryFull = false;
        enderChestFull = false;
        currentContainer = null;
        waitingForPearl = false;
        pearlRetryCount = 0;
        containerOpenFailures = 0;

        String prefix = meteordevelopment.meteorclient.systems.config.Config.get().prefix.get();

        info("StashMover activated");
        if (inputAreaPos1 != null && inputAreaPos2 != null) {
            info("§aInput area set with §f" + inputContainers.size() + "§a containers");
        } else {
            info("§7Use §f" + prefix + "setinput §7to select input area");
        }
        if (outputAreaPos1 != null && outputAreaPos2 != null) {
            info("§bOutput area set with §f" + outputContainers.size() + "§b containers");
        } else {
            info("§7Use §f" + prefix + "setoutput §7to select output area");
        }

        // Automatically start if areas are configured
        if (hasValidAreas()) {
            info("§eStarting automated transfer process...");
            currentState = ProcessState.CHECKING_LOCATION;
        } else {
            info("§cConfigure both input and output areas to start");
            currentState = ProcessState.IDLE;
        }
    }

    @Override
    public void onDeactivate() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.player.closeHandledScreen();
        }

        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }

        // Stop all movement
        mc.options.sneakKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);

        // Clear input
        if (mc.player != null && mc.player.input != null) {
            mc.player.input.movementForward = 0.0f;
            mc.player.input.movementSideways = 0.0f;
        }

        currentState = ProcessState.IDLE;
        info("StashMover deactivated");
    }


    public void handleBlockSelectionPublic(BlockPos pos) {
        handleBlockSelection(pos);
    }

    private void handleBlockSelection(BlockPos pos) {
        switch (selectionMode) {
            case INPUT_FIRST -> {
                selectionPos1 = pos;
                selectionMode = SelectionMode.INPUT_SECOND;
                info("§aInput area first corner set");
                info("§eLeft-click another block to set the second corner");
            }
            case INPUT_SECOND -> {
                if (pos.equals(selectionPos1)) {
                    warning("Second corner must be different from the first!");
                    return;
                }
                setInputArea(selectionPos1, pos);
                selectionMode = SelectionMode.NONE;
                selectionPos1 = null;
                info("§aInput area selection complete!");
            }
            case OUTPUT_FIRST -> {
                selectionPos1 = pos;
                selectionMode = SelectionMode.OUTPUT_SECOND;
                info("§bOutput area first corner set");
                info("§eLeft-click another block to set the second corner");
            }
            case OUTPUT_SECOND -> {
                if (pos.equals(selectionPos1)) {
                    warning("Second corner must be different from the first!");
                    return;
                }
                setOutputArea(selectionPos1, pos);
                selectionMode = SelectionMode.NONE;
                selectionPos1 = null;
                info("§bOutput area selection complete!");
            }
        }
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Only process module logic if module is active
        if (!isActive()) return;

        // State timer
        if (stateTimer > 0) {
            stateTimer--;
            return;
        }

        // Check lag
        if (pauseOnLag.get() && isServerLagging()) {
            return;
        }

        // Do not auto-start - wait for manual command

        // Process state
        handleCurrentState();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        // Only render if module is active or in selection mode
        if (!isActive() && selectionMode == SelectionMode.NONE) return;
        if (!renderSelection.get() && selectionMode == SelectionMode.NONE) return;

        // Render input area outline only
        if (inputAreaPos1 != null && inputAreaPos2 != null) {
            Box inputBox = new Box(
                inputAreaPos1.getX(), inputAreaPos1.getY(), inputAreaPos1.getZ(),
                inputAreaPos2.getX() + 1, inputAreaPos2.getY() + 1, inputAreaPos2.getZ() + 1
            );
            event.renderer.box(inputBox, inputAreaColor.get(), inputAreaColor.get(), ShapeMode.Lines, outlineWidth.get());
        }

        // Render output area outline only
        if (outputAreaPos1 != null && outputAreaPos2 != null) {
            Box outputBox = new Box(
                outputAreaPos1.getX(), outputAreaPos1.getY(), outputAreaPos1.getZ(),
                outputAreaPos2.getX() + 1, outputAreaPos2.getY() + 1, outputAreaPos2.getZ() + 1
            );
            event.renderer.box(outputBox, outputAreaColor.get(), outputAreaColor.get(), ShapeMode.Lines, outlineWidth.get());
        }

        // Only render containers if module is active
        if (isActive()) {
            // Render input containers (only non-empty ones)
            for (ContainerInfo container : inputContainers) {
                if (!container.isEmpty) {
                    SettingColor color = container == currentContainer ? activeContainerColor.get() : inputContainerColor.get();
                    renderContainer(event, container, color);
                }
            }

            // Render output containers (only non-full ones)
            for (ContainerInfo container : outputContainers) {
                if (!container.isFull) {
                    SettingColor color = container == currentContainer ? activeContainerColor.get() : outputContainerColor.get();
                    renderContainer(event, container, color);
                }
            }
        }

        // Render selection in progress
        if (selectionMode != SelectionMode.NONE && selectionPos1 != null) {
            // Get the block the player is looking at
            BlockPos currentPos = mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.BLOCK ?
                ((BlockHitResult)mc.crosshairTarget).getBlockPos() : mc.player.getBlockPos();

            Box selectionBox = new Box(
                Math.min(selectionPos1.getX(), currentPos.getX()),
                Math.min(selectionPos1.getY(), currentPos.getY()),
                Math.min(selectionPos1.getZ(), currentPos.getZ()),
                Math.max(selectionPos1.getX(), currentPos.getX()) + 1,
                Math.max(selectionPos1.getY(), currentPos.getY()) + 1,
                Math.max(selectionPos1.getZ(), currentPos.getZ()) + 1
            );

            SettingColor color = (selectionMode == SelectionMode.INPUT_FIRST || selectionMode == SelectionMode.INPUT_SECOND) ?
                new SettingColor(0, 255, 0, 100) : new SettingColor(0, 100, 255, 100);

            event.renderer.box(selectionBox, color, color, ShapeMode.Both, 0);

            // Render corner markers
            Box corner1 = new Box(
                selectionPos1.getX(), selectionPos1.getY(), selectionPos1.getZ(),
                selectionPos1.getX() + 1, selectionPos1.getY() + 1, selectionPos1.getZ() + 1
            );
            event.renderer.box(corner1, new SettingColor(255, 255, 0, 200),
                             new SettingColor(255, 255, 0, 100), ShapeMode.Both, 0);
        }
    }

    private void renderContainer(Render3DEvent event, ContainerInfo container, SettingColor color) {
        Box box = new Box(
            container.pos.getX(), container.pos.getY(), container.pos.getZ(),
            container.pos.getX() + 1, container.pos.getY() + 1, container.pos.getZ() + 1
        );

        // Expand box for double chests
        if (container.type == ContainerType.DOUBLE_CHEST ||
            container.type == ContainerType.DOUBLE_TRAPPED_CHEST) {

            BlockState state = mc.world.getBlockState(container.pos);
            if (state.contains(Properties.CHEST_TYPE)) {
                ChestType chestType = state.get(Properties.CHEST_TYPE);
                Direction facing = state.get(Properties.HORIZONTAL_FACING);

                if (chestType == ChestType.LEFT) {
                    BlockPos otherPos = container.pos.offset(facing.rotateYClockwise());
                    box = box.union(new Box(
                        otherPos.getX(), otherPos.getY(), otherPos.getZ(),
                        otherPos.getX() + 1, otherPos.getY() + 1, otherPos.getZ() + 1
                    ));
                } else if (chestType == ChestType.RIGHT) {
                    BlockPos otherPos = container.pos.offset(facing.rotateYCounterclockwise());
                    box = box.union(new Box(
                        otherPos.getX(), otherPos.getY(), otherPos.getZ(),
                        otherPos.getX() + 1, otherPos.getY() + 1, otherPos.getZ() + 1
                    ));
                }
            }
        }

        // Render with both fill and outline for visibility
        event.renderer.box(box, color, color, ShapeMode.Both, 1);
    }

    private void handleCurrentState() {
        switch (currentState) {
            case IDLE -> { /* Waiting */ }
            case CHECKING_LOCATION -> checkLocation();
            case INPUT_PROCESS -> handleInputProcess();
            case LOADING_PEARL -> handlePearlLoading();
            case RESET_PEARL_PICKUP -> handleResetPearlPickup();
            case RESET_PEARL_PLACE_SHULKER -> handleResetPearlPlaceShulker();
            case RESET_PEARL_APPROACH -> handleResetPearlApproach();
            case RESET_PEARL_PREPARE -> handleResetPearlPrepare();
            case RESET_PEARL_THROW -> handleResetPearlThrow();
            case RESET_PEARL_WAIT -> handleResetPearlWait();
            case OUTPUT_PROCESS -> handleOutputProcess();
            case GOING_BACK -> handleGoingBack();
            case OPENING_CONTAINER -> handleOpeningContainer();
            case TRANSFERRING_ITEMS -> handleTransferringItems();
            case CLOSING_CONTAINER -> handleClosingContainer();
            case BREAKING_CONTAINER -> handleBreakingContainer();
            case MOVING_TO_CONTAINER -> handleMovingToContainer();
            case OPENING_ENDERCHEST -> handleOpeningEnderChest();
            case FILLING_ENDERCHEST -> handleFillingEnderChest();
            case EMPTYING_ENDERCHEST -> handleEmptyingEnderChest();
            case WAITING -> handleWaiting();
        }
    }

    // Area selection methods
    public void startInputSelection() {
        selectionMode = SelectionMode.INPUT_FIRST;
        selectionPos1 = null;
        info("§aInput area selection started - §fLeft-click §afirst corner block");
    }

    public void startOutputSelection() {
        selectionMode = SelectionMode.OUTPUT_FIRST;
        selectionPos1 = null;
        info("§bOutput area selection started - §fLeft-click §bfirst corner block");
    }

    public void cancelSelection() {
        selectionMode = SelectionMode.NONE;
        selectionPos1 = null;
        info("§cSelection cancelled");
    }

    public void setInputArea(BlockPos pos1, BlockPos pos2) {
        inputAreaPos1 = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        inputAreaPos2 = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );

        detectContainersInArea(inputAreaPos1, inputAreaPos2, true);
        info("§aInput area set with §f" + inputContainers.size() + " §acontainers");
    }

    public void setOutputArea(BlockPos pos1, BlockPos pos2) {
        outputAreaPos1 = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        outputAreaPos2 = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );

        detectContainersInArea(outputAreaPos1, outputAreaPos2, false);
        info("§bOutput area set with §f" + outputContainers.size() + " §bcontainers");
    }

    private void detectContainersInArea(BlockPos pos1, BlockPos pos2, boolean isInput) {
        Set<ContainerInfo> containers = isInput ? inputContainers : outputContainers;
        containers.clear();
        Set<BlockPos> processedPositions = new HashSet<>();

        for (int x = pos1.getX(); x <= pos2.getX(); x++) {
            for (int y = pos1.getY(); y <= pos2.getY(); y++) {
                for (int z = pos1.getZ(); z <= pos2.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (processedPositions.contains(pos)) continue;

                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    ContainerInfo container = null;

                    if (block instanceof ChestBlock && !(block instanceof TrappedChestBlock)) {
                        // Always detect double chests properly
                        if (state.contains(Properties.CHEST_TYPE)) {
                            ChestType chestType = state.get(Properties.CHEST_TYPE);
                            if (chestType != ChestType.SINGLE) {
                                Direction facing = state.get(Properties.HORIZONTAL_FACING);
                                BlockPos otherPos = null;

                                if (chestType == ChestType.LEFT) {
                                    otherPos = pos.offset(facing.rotateYClockwise());
                                } else {
                                    otherPos = pos.offset(facing.rotateYCounterclockwise());
                                }

                                processedPositions.add(otherPos);
                                container = new ContainerInfo(pos, ContainerType.DOUBLE_CHEST);
                            } else {
                                container = new ContainerInfo(pos, ContainerType.CHEST);
                            }
                        } else {
                            container = new ContainerInfo(pos, ContainerType.CHEST);
                        }
                    } else if (block instanceof TrappedChestBlock) {
                        // Always include trapped chests and detect doubles
                        if (state.contains(Properties.CHEST_TYPE)) {
                            ChestType chestType = state.get(Properties.CHEST_TYPE);
                            if (chestType != ChestType.SINGLE) {
                                Direction facing = state.get(Properties.HORIZONTAL_FACING);
                                BlockPos otherPos = null;

                                if (chestType == ChestType.LEFT) {
                                    otherPos = pos.offset(facing.rotateYClockwise());
                                } else {
                                    otherPos = pos.offset(facing.rotateYCounterclockwise());
                                }

                                processedPositions.add(otherPos);
                                container = new ContainerInfo(pos, ContainerType.DOUBLE_TRAPPED_CHEST);
                            } else {
                                container = new ContainerInfo(pos, ContainerType.TRAPPED_CHEST);
                            }
                        } else {
                            container = new ContainerInfo(pos, ContainerType.TRAPPED_CHEST);
                        }
                    } else if (block instanceof BarrelBlock) {
                        // Always include barrels
                        container = new ContainerInfo(pos, ContainerType.BARREL);
                    }

                    if (container != null) {
                        containers.add(container);
                        processedPositions.add(pos);
                    }
                }
            }
        }
    }

    // Process control methods
    private void startProcess() {
        if (!hasValidAreas()) {
            error("Please set input and output areas first!");
            return;
        }

        currentState = ProcessState.CHECKING_LOCATION;
        info("Starting StashMover process...");
    }

    public void startProcessManually() {
        startProcess();
    }

    private void stopCurrentProcess() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.player.closeHandledScreen();
        }

        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        }

        currentState = ProcessState.IDLE;
        currentContainer = null;
        waitingForPearl = false;
        pearlRetryCount = 0;
        stateTimer = 0;
        retryCount = 0;
        waitingForRespawn = false;
        killRetryCount = 0;
        initialPlayerPos = null;
        hasThrownPearl = false;
        hasPlacedShulker = false;
        enderChestHasItems = false;
        enderChestFull = false;
        pearlFailRetries = 0;
        offhandBackup = ItemStack.EMPTY;

        info("Process stopped");
    }

    public void stopProcessManually() {
        stopCurrentProcess();
    }


    private void checkLocation() {
        if (isNearInputArea()) {
            info("Near input area, starting input process");
            currentState = ProcessState.INPUT_PROCESS;
            findNextInputContainer();
        } else if (isNearOutputArea()) {
            info("Near output area, resetting pearl first");
            // ALWAYS reset pearl when arriving at output to ensure one is in stasis
            currentState = ProcessState.RESET_PEARL_PICKUP;
            hasThrownPearl = false;
            hasPlacedShulker = false;
            isGoingToInput = false; // We're at output, pearl will be for going back to input
        } else {
            warning("Not near any configured area!");
            warning("Player pos: " + mc.player.getBlockPos());
            warning("Input area: " + inputAreaPos1 + " to " + inputAreaPos2);
            warning("Output area: " + outputAreaPos1 + " to " + outputAreaPos2);
            currentState = ProcessState.IDLE;
        }
    }

    // Input process methods
    private void handleInputProcess() {
        // First check if inventory is full
        if (isInventoryFull()) {
            // Use enderchest
            if (fillEnderChest.get() && !isEnderChestFull()) {
                info("Inventory full, checking enderchest...");
                findOrPlaceEnderChest();
                return;
            } else {
                // Can't use enderchest or it's full
                info("Inventory full and enderchest not available/full, starting pearl loading");
                currentState = ProcessState.LOADING_PEARL;
                return;
            }
        }

        // Find next container to process
        if (currentContainer == null) {
            findNextInputContainer();
        }
    }

    private void findNextInputContainer() {
        currentContainer = inputContainers.stream()
            .filter(c -> !c.isEmpty)
            .min(Comparator.comparingDouble(c -> mc.player.getPos().distanceTo(Vec3d.ofCenter(c.pos))))
            .orElse(null);

        if (currentContainer == null) {
            info("All input containers processed!");
            currentState = ProcessState.LOADING_PEARL;
        } else {
            moveToContainer(currentContainer);
        }
    }

    private void moveToContainer(ContainerInfo container) {
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(container.pos));

        if (distance > 4.5) {
            // Use Baritone to move to container
            GoalBlock goal = new GoalBlock(container.pos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
            currentState = ProcessState.MOVING_TO_CONTAINER;
            stateTimer = moveDelay.get();
        } else {
            // Close enough, open container
            currentState = ProcessState.OPENING_CONTAINER;
            stateTimer = 10; // Hold view for 10 ticks (half second)
        }
    }

    private void handleMovingToContainer() {
        if (currentContainer == null) {
            currentState = ProcessState.INPUT_PROCESS;
            return;
        }

        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(currentContainer.pos));

        if (distance <= 4.5) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            currentState = ProcessState.OPENING_CONTAINER;
            stateTimer = 10; // Hold view for 10 ticks (half second)
        }
    }

    private void handleOpeningContainer() {
        if (currentContainer == null) {
            currentState = ProcessState.INPUT_PROCESS;
            return;
        }

        // Look at container and HOLD the rotation for GrimAC
        Vec3d containerCenter = Vec3d.ofCenter(currentContainer.pos);
        double yaw = Rotations.getYaw(containerCenter);
        double pitch = Rotations.getPitch(containerCenter);

        // Apply rotation
        Rotations.rotate(yaw, pitch);

        // Set rotation directly
        mc.player.setYaw((float)yaw);
        mc.player.setPitch((float)pitch);

        // Hold view for 10 ticks
        if (stateTimer > 0) {
            stateTimer--;

            // Log when we're about to open
            if (stateTimer == 1) {
                info("Opening container");
            }
            return;
        }

        // After holding view, open container
        BlockHitResult hitResult = new BlockHitResult(
            containerCenter,
            Direction.UP,
            currentContainer.pos,
            false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        currentState = ProcessState.WAITING;
        stateTimer = openDelay.get();
    }

    private void handleWaiting() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            // Successfully opened container - reset failure counter
            containerOpenFailures = 0;
            currentState = ProcessState.TRANSFERRING_ITEMS;
            stateTimer = transferDelay.get();
        } else if (retryCount < maxRetries.get()) {
            retryCount++;
            containerOpenFailures++;

            // Check failures
            if (isNearOutputArea() && containerOpenFailures >= 10) {
                error("Failed to open output containers 10 times - disabling module");
                toggle(); // Turn off the module
                return;
            }

            currentState = ProcessState.OPENING_CONTAINER;
            stateTimer = 10;
        } else {
            warning("Failed to open container after " + maxRetries.get() + " retries");
            currentContainer.isEmpty = true;
            currentContainer = null;
            retryCount = 0;
            currentState = ProcessState.INPUT_PROCESS;
        }
    }

    private void handleTransferringItems() {
        // Check location
        if (isNearInputArea()) {
            handleInputTransferringItems();
        } else if (isNearOutputArea()) {
            handleOutputTransferringItems();
        } else {
            currentState = ProcessState.CHECKING_LOCATION;
        }
    }

    private void handleInputTransferringItems() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            // Container window was closed unexpectedly (attacked by endermite, etc.)
            // Check if we still have the container and should reopen it
            if (currentContainer != null && !currentContainer.isEmpty && !currentContainer.isFull) {
                // Check if inventory is not full - we still need to transfer items
                boolean inventoryHasSpace = false;
                for (int j = 0; j < 36; j++) {
                    if (mc.player.getInventory().getStack(j).isEmpty()) {
                        inventoryHasSpace = true;
                        break;
                    }
                }

                if (inventoryHasSpace) {
                    warning("Container window closed unexpectedly! Reopening...");
                    // Attempt to reopen the container
                    currentState = ProcessState.OPENING_CONTAINER;
                    stateTimer = 10;
                    containerOpenFailures++;

                    // If we've failed too many times, skip this container
                    if (containerOpenFailures > 3) {
                        warning("Failed to reopen container multiple times, skipping");
                        currentContainer = null;
                        containerOpenFailures = 0;
                        currentState = ProcessState.INPUT_PROCESS;
                    }
                    return;
                }
            }

            currentState = ProcessState.INPUT_PROCESS;
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            currentState = ProcessState.CLOSING_CONTAINER;
            return;
        }

        boolean transferredItem = false;
        boolean inventoryHasSpace = false;

        // Check inventory space
        for (int j = 0; j < 36; j++) {
            if (mc.player.getInventory().getStack(j).isEmpty()) {
                inventoryHasSpace = true;
                break;
            }
        }

        if (!inventoryHasSpace) {
            // Inventory full - check if we should drop non-shulkers
            if (onlyShulkers.get()) {
                // Drop non-shulker items to make room
                boolean droppedItem = false;
                for (int j = 0; j < 36; j++) {
                    ItemStack invStack = mc.player.getInventory().getStack(j);
                    // Only keep shulkers when onlyShulkers is enabled
                    if (!invStack.isEmpty() && !isShulkerBox(invStack.getItem())) {
                        // Close container first then drop the item
                        mc.player.closeHandledScreen();
                        // Use InvUtils to drop the correct item from inventory
                        InvUtils.drop().slot(j);
                        droppedItem = true;
                        info("Dropped non-shulker item: " + invStack.getItem().getName().getString());
                        // Set state to reopen container after dropping
                        currentState = ProcessState.OPENING_CONTAINER;
                        stateTimer = transferDelay.get();
                        return; // Drop one item at a time, will reopen container next tick
                    }
                }

                if (!droppedItem) {
                    // No non-shulker items to drop, inventory truly full
                    currentState = ProcessState.CLOSING_CONTAINER;
                    return;
                }
            } else {
                // Not in OnlyShulker mode, inventory is full
                currentState = ProcessState.CLOSING_CONTAINER;
                return;
            }
        }

        // Container to inventory
        // Container slots are indexed from 0 to totalSlots-1 in the handler
        for (int i = 0; i < currentContainer.totalSlots; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                // Filter - only take shulkers when onlyShulkers is enabled
                if (onlyShulkers.get() && !isShulkerBox(stack.getItem())) {
                    continue;
                }

                // Shift-click the item to transfer it to inventory
                mc.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0, // button (0 for left click)
                    SlotActionType.QUICK_MOVE, // shift-click
                    mc.player
                );

                transferredItem = true;
                itemsTransferred++;
                stateTimer = transferDelay.get();
                return; // Process one item at a time
            }
        }

        // Container is empty
        if (!transferredItem) {
            currentContainer.isEmpty = true;
            info("Container is now empty");
            currentState = ProcessState.CLOSING_CONTAINER;
        }
    }

    private void handleClosingContainer() {
        if (mc.currentScreen instanceof GenericContainerScreen) {
            mc.player.closeHandledScreen();
        }

        stateTimer = closeDelay.get();

        // Handle by location
        if (isNearOutputArea()) {
            // At OUTPUT area - handle depositing items

            // If OnlyShulker mode, drop any non-shulker items BEFORE processing
            if (onlyShulkers.get()) {
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    // Drop ALL non-shulker items
                    if (!stack.isEmpty() && !isShulkerBox(stack.getItem())) {
                        InvUtils.drop().slot(i);
                        info("Dropped non-shulker at output: " + stack.getItem().getName().getString());
                        stateTimer = 5;
                        return; // Drop one at a time
                    }
                }
            }

            // Check inventory
            boolean inventoryEmpty = !hasItemsToTransfer();

            if (inventoryEmpty && enderChestHasItems && fillEnderChest.get()) {
                // Need to get items from enderchest
                info("Getting items from enderchest to continue depositing");
                enderChestPos = findNearbyEnderChest();
                if (enderChestPos != null) {
                    currentState = ProcessState.OPENING_ENDERCHEST;
                    stateTimer = 10; // Hold view for 10 ticks (half second)
                } else {
                    // Try to place enderchest
                    FindItemResult enderChest = InvUtils.findInHotbar(Items.ENDER_CHEST);
                    if (enderChest.found()) {
                        BlockPos placePos = findSuitablePlacePos();
                        if (placePos != null) {
                            placeEnderChest(placePos, enderChest.slot());
                        }
                    }
                }
                return;
            }

            if (inventoryEmpty && !enderChestHasItems) {
                // Both inventory and enderchest are empty, go back
                info("All items deposited, going back to input");
                currentContainer = null;
                currentState = ProcessState.GOING_BACK;
                return;
            }

            // Still have items, find next output container
            currentContainer = null;
            currentState = ProcessState.OUTPUT_PROCESS;

        } else if (isNearInputArea()) {
            // At INPUT area - handle collecting items

            // If OnlyShulker mode, drop any non-shulker items
            if (onlyShulkers.get()) {
                boolean foundNonShulker = false;
                for (int i = 0; i < 36; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    // Drop ALL non-shulker items when onlyShulkers is enabled
                    if (!stack.isEmpty() && !isShulkerBox(stack.getItem())) {
                        // Drop non-shulker item using InvUtils for consistency
                        InvUtils.drop().slot(i);
                        info("Dropped non-shulker: " + stack.getItem().getName().getString());
                        stateTimer = 5;
                        foundNonShulker = true;
                        // After dropping, reopen container if it still has items
                        if (currentContainer != null && !currentContainer.isEmpty) {
                            currentState = ProcessState.OPENING_CONTAINER;
                        } else {
                            currentState = ProcessState.INPUT_PROCESS;
                        }
                        return; // Drop one at a time
                    }
                }

                // If we went through all slots and found no non-shulkers, continue
                if (!foundNonShulker) {
                    info("No non-shulker items to drop");
                }
            }

            // Break empty container
            if (currentContainer != null && currentContainer.isEmpty && breakEmptyContainers.get()) {
                currentState = ProcessState.BREAKING_CONTAINER;
                return;
            }

            // Check if full
            if (isInventoryFull()) {
                info("Inventory full");

                // Fill enderchest
                if (fillEnderChest.get() && !isEnderChestFull()) {
                    info("Checking enderchest...");
                    findOrPlaceEnderChest();
                } else {
                    // Both inventory and enderchest are full, start pearl loading
                    info("Inventory and enderchest full, starting pearl loading");
                    currentContainer = null;
                    currentState = ProcessState.LOADING_PEARL;
                }
            } else {
                // Inventory not full, continue with next container
                currentContainer = null;
                currentState = ProcessState.INPUT_PROCESS;
            }
        } else {
            // Not at either area, check location
            currentState = ProcessState.CHECKING_LOCATION;
        }
    }

    private void handleBreakingContainer() {
        if (currentContainer == null) {
            currentState = ProcessState.INPUT_PROCESS;
            return;
        }

        // Break the container
        mc.interactionManager.updateBlockBreakingProgress(currentContainer.pos, Direction.UP);

        // Check broken
        if (mc.world.getBlockState(currentContainer.pos).isAir()) {
            inputContainers.remove(currentContainer);
            containersProcessed++;
            currentContainer = null;
            currentState = ProcessState.INPUT_PROCESS;
            stateTimer = moveDelay.get();
        }
    }

    private void findOrPlaceEnderChest() {
        enderChestPos = findNearbyEnderChest();

        if (enderChestPos != null) {
            currentState = ProcessState.OPENING_ENDERCHEST;
            stateTimer = 10; // Hold view for 10 ticks (half second)
        } else {
            // Try to place enderchest
            FindItemResult enderChest = InvUtils.findInHotbar(Items.ENDER_CHEST);
            if (enderChest.found()) {
                BlockPos placePos = findSuitablePlacePos();
                if (placePos != null) {
                    placeEnderChest(placePos, enderChest.slot());
                }
            } else {
                currentContainer = null;
                currentState = ProcessState.INPUT_PROCESS;
            }
        }
    }

    private void handleOpeningEnderChest() {
        if (enderChestPos == null) {
            currentState = ProcessState.INPUT_PROCESS;
            return;
        }

        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(enderChestPos));

        if (distance > 4.5) {
            GoalBlock goal = new GoalBlock(enderChestPos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
            stateTimer = moveDelay.get();
            return;
        }

        // Look at enderchest and HOLD the rotation for GrimAC
        Vec3d echestCenter = Vec3d.ofCenter(enderChestPos);
        double yaw = Rotations.getYaw(echestCenter);
        double pitch = Rotations.getPitch(echestCenter);

        // Apply rotation
        Rotations.rotate(yaw, pitch);

        // Set rotation directly
        mc.player.setYaw((float)yaw);
        mc.player.setPitch((float)pitch);

        // Hold view for 10 ticks
        if (stateTimer > 0) {
            stateTimer--;

            // Log when we're about to open
            if (stateTimer == 1) {
                info("Opening enderchest");
            }
            return;
        }

        // After holding view, open enderchest
        BlockHitResult hitResult = new BlockHitResult(
            echestCenter,
            Direction.UP,
            enderChestPos,
            false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        // Reset failure counter on successful attempt
        containerOpenFailures = 0;

        // Determine next state
        if (isNearOutputArea()) {
            currentState = ProcessState.EMPTYING_ENDERCHEST;
        } else {
            currentState = ProcessState.FILLING_ENDERCHEST;
        }
        stateTimer = openDelay.get();
    }

    private void handleFillingEnderChest() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            // Enderchest closed, check what to do next
            checkNextStepAfterEnderChest();
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            mc.player.closeHandledScreen();
            checkNextStepAfterEnderChest();
            return;
        }

        // Inventory to enderchest
        boolean transferred = false;
        boolean enderChestHasSpace = false;

        // First check if enderchest has any space
        for (int j = 0; j < 27; j++) {
            if (handler.getSlot(j).getStack().isEmpty()) {
                enderChestHasSpace = true;
                break;
            }
        }

        if (!enderChestHasSpace) {
            enderChestFull = true;
            info("Enderchest is full");
            mc.player.closeHandledScreen();
            stateTimer = closeDelay.get();
            checkNextStepAfterEnderChest();
            return;
        }

        // Try to transfer items from inventory to enderchest
        // Enderchest has 27 slots (0-26)
        // Player inventory starts at slot 27
        for (int i = 27; i < 63; i++) { // Player inventory slots in enderchest GUI
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                // Only transfer shulkers when onlyShulkers is enabled
                if (onlyShulkers.get() && !isShulkerBox(stack.getItem())) {
                    continue;
                }

                // Shift-click to transfer to enderchest
                mc.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0, // button (0 for left click)
                    SlotActionType.QUICK_MOVE, // shift-click
                    mc.player
                );

                transferred = true;
                enderChestHasItems = true; // Mark that we have items in enderchest
                stateTimer = transferDelay.get();
                info("Transferred item to enderchest");
                return; // Process one item at a time
            }
        }

        if (!transferred) {
            // No more items to transfer or enderchest full
            mc.player.closeHandledScreen();
            stateTimer = closeDelay.get();
            checkNextStepAfterEnderChest();
        }
    }

    private void checkNextStepAfterEnderChest() {
        // Both full check
        if (isInventoryFull() && enderChestFull) {
            info("Both inventory and enderchest are full, starting pearl loading");
            currentContainer = null;
            currentState = ProcessState.LOADING_PEARL;
        } else {
            // Continue processing input containers
            currentContainer = null;
            currentState = ProcessState.INPUT_PROCESS;
        }
    }

    private void handleEmptyingEnderChest() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            // Enderchest closed, continue output process
            currentState = ProcessState.OUTPUT_PROCESS;
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            mc.player.closeHandledScreen();
            currentState = ProcessState.OUTPUT_PROCESS;
            return;
        }

        // Enderchest to inventory
        boolean transferred = false;
        boolean inventoryHasSpace = false;

        // Check space
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
                inventoryHasSpace = true;
                break;
            }
        }

        if (!inventoryHasSpace) {
            // Inventory is full, close enderchest and continue
            mc.player.closeHandledScreen();
            currentState = ProcessState.OUTPUT_PROCESS;
            return;
        }

        // Check if enderchest has any items first
        boolean enderChestIsEmpty = true;
        for (int i = 0; i < 27; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
                enderChestIsEmpty = false;
                break;
            }
        }

        if (enderChestIsEmpty) {
            // Enderchest is empty, we're done
            enderChestHasItems = false;
            enderChestEmptied = true;
            info("Enderchest is empty");
            mc.player.closeHandledScreen();
            stateTimer = closeDelay.get();
            currentState = ProcessState.OUTPUT_PROCESS;
            return;
        }

        // Try to transfer items from enderchest to inventory
        // Enderchest slots are 0-26
        for (int i = 0; i < 27; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                // Only transfer shulkers if in OnlyShulkers mode
                if (onlyShulkers.get() && !isShulkerBox(stack.getItem())) {
                    continue;
                }

                // Shift-click to transfer to inventory
                mc.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0, // button (0 for left click)
                    SlotActionType.QUICK_MOVE, // shift-click
                    mc.player
                );

                transferred = true;
                enderChestHasItems = true; // Mark that chest still has items
                stateTimer = transferDelay.get();
                info("Retrieved item from enderchest");
                return; // Process one item at a time
            }
        }

        // If no items transferred but enderchest not empty, inventory must be full or only non-shulkers left
        if (!transferred && !enderChestIsEmpty) {
            if (inventoryHasSpace && onlyShulkers.get()) {
                // Only non-shulkers left in enderchest
                info("Only non-shulkers left in enderchest");
                enderChestHasItems = false;
                mc.player.closeHandledScreen();
                currentState = ProcessState.OUTPUT_PROCESS;
            } else {
                // Inventory full, deposit first
                info("Inventory full, depositing items first");
                mc.player.closeHandledScreen();
                currentState = ProcessState.OUTPUT_PROCESS;
            }
        }
    }

    // Pearl loading methods
    private void handlePearlLoading() {
        if (!waitingForPearl) {
            sendPearlCommand();
            waitingForPearl = true;
            lastPearlMessageTime = System.currentTimeMillis();
            pearlRetryCount = 0;
            initialPlayerPos = mc.player.getPos();
        }

        // Check teleport
        Vec3d currentPos = mc.player.getPos();
        double distance = currentPos.distanceTo(initialPlayerPos);

        // If we moved more than 100 blocks, we likely teleported
        if (distance > 100) {
            // Near output
            if (isNearOutputArea()) {
                info("Successfully pearl loaded to output area!");
                waitingForPearl = false;

                // Go through pearl reset
                currentState = ProcessState.RESET_PEARL_PICKUP;
                hasThrownPearl = false;
                hasPlacedShulker = false;
                isGoingToInput = false; // We're going to output area
                return;
            } else if (!isNearInputArea()) {
                // We teleported but not to the expected area, retry
                warning("Teleported but not to output area, retrying...");
                waitingForPearl = false;
                currentState = ProcessState.LOADING_PEARL;
                return;
            }
        }

        // Timeout check
        if (System.currentTimeMillis() - lastPearlMessageTime > pearlTimeout.get() * 1000) {
            if (pearlRetryCount < maxRetries.get()) {
                pearlRetryCount++;
                info("Pearl loading timeout, retrying (attempt " + pearlRetryCount + "/" + maxRetries.get() + ")");
                sendPearlCommand();
                lastPearlMessageTime = System.currentTimeMillis();
            } else {
                error("Pearl loading failed after " + maxRetries.get() + " retries!");
                currentState = ProcessState.IDLE;
                waitingForPearl = false;
            }
        }
    }

    private void sendPearlCommand() {
        String randomSuffix = generateRandomString(8);
        String command = String.format("/msg %s %s %s",
            pearlPlayerName.get(),
            pearlCommand.get(),
            randomSuffix);

        ChatUtils.sendPlayerMsg(command);
        lastRandomString = randomSuffix;
        info("Sent pearl command: " + command);
    }

    // Pearl methods
    private void handleResetPearlPickup() {
        // Use correct coordinates based on where we are
        BlockPos pickupPos;
        if (isGoingToInput) {
            pickupPos = inputPearlPickupPos.get();
        } else {
            pickupPos = outputPearlPickupPos.get();
        }
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(pickupPos));

        if (distance > 3) {
            // Move to pickup location
            GoalBlock goal = new GoalBlock(pickupPos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
            stateTimer = moveDelay.get();
        } else {
            // At pickup location, place shulker in offhand
            currentState = ProcessState.RESET_PEARL_PLACE_SHULKER;
            stateTimer = 10;
        }
    }

    private void handleResetPearlPlaceShulker() {
        // Try to place shulker in offhand if available (optional)
        if (!hasPlacedShulker) {
            // Find shulker in hotbar slot 0
            ItemStack slot0 = mc.player.getInventory().getStack(0);

            if (isShulkerBox(slot0.getItem())) {
                // Backup what's currently in offhand
                offhandBackup = mc.player.getOffHandStack().copy();

                // Move shulker to offhand
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    45, // Offhand slot
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );

                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    36, // Hotbar slot 0
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );

                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    45, // Offhand slot
                    0,
                    SlotActionType.PICKUP,
                    mc.player
                );

                hasPlacedShulker = true;
                info("Placed shulker in offhand");
                stateTimer = 20;
            } else {
                // No shulker found, that's OK - mark as done and continue
                info("No shulker in slot 0, continuing without offhand shulker");
                hasPlacedShulker = true; // Mark as done so we don't keep trying
                stateTimer = 10;
            }
        } else {
            // Wait for pearl to be dispensed and pick it up
            // Check pearl
            FindItemResult pearl = InvUtils.find(Items.ENDER_PEARL);
            if (pearl.found()) {
                info("Pearl picked up, moving to throw location");
                currentState = ProcessState.RESET_PEARL_APPROACH;
                stateTimer = 10;
            } else {
                // Wait for pearl
                stateTimer = 10;
            }
        }
    }

    private void handleResetPearlApproach() {
        // Use correct coordinates based on where we are (trapdoor position)
        BlockPos throwPos;
        if (isGoingToInput) {
            throwPos = inputPearlThrowPos.get();
        } else {
            throwPos = outputPearlThrowPos.get();
        }

        // Calculate distance to throw position
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(throwPos));

        // Check if we're close enough to start manual approach
        if (distance <= 3.0) {
            // We're within 3 blocks, cancel Baritone and start manual approach
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            }
            lastBaritoneGoal = null;
            currentState = ProcessState.RESET_PEARL_PREPARE;
            stateTimer = 5;
            info("Starting precise positioning");
            return;
        }

        // Check if Baritone is already pathing to a goal
        if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
            // Let it continue pathing, don't set a new goal
            stateTimer = 10;
            return;
        }

        // Find a safe air block adjacent to the trapdoor to approach from
        BlockPos goalPos = null;
        safeRetreatPos = null;

        // Look for air blocks adjacent to the trapdoor
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos adjacent = throwPos.offset(dir);
            BlockState adjacentState = mc.world.getBlockState(adjacent);
            BlockState belowState = mc.world.getBlockState(adjacent.down());

            // Check if this is an air block with solid ground below
            if (adjacentState.isAir() && belowState.isSolidBlock(mc.world, adjacent.down())) {
                // Found a safe air block to approach from
                goalPos = adjacent;
                safeRetreatPos = adjacent; // Store this as our retreat position
                approachDirection = dir.getOpposite();
                info("Found safe approach position");
                break;
            }
        }

        // If no air block found, try 2 blocks away
        if (goalPos == null) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos candidate = throwPos.offset(dir, 2);
                BlockState state = mc.world.getBlockState(candidate);
                BlockState belowState = mc.world.getBlockState(candidate.down());

                if (state.isAir() && belowState.isSolidBlock(mc.world, candidate.down())) {
                    goalPos = candidate;
                    safeRetreatPos = throwPos.offset(dir); // Middle position for retreat
                    approachDirection = dir.getOpposite();
                    info("Using fallback approach position");
                    break;
                }
            }
        }

        if (goalPos == null) {
            // Final fallback - just try north
            goalPos = throwPos.offset(Direction.NORTH, 2);
            safeRetreatPos = throwPos.offset(Direction.NORTH);
            approachDirection = Direction.SOUTH;
            warning("Using fallback approach position");
        }

        // Only set goal if it's different from the last one
        if (lastBaritoneGoal == null || !lastBaritoneGoal.equals(goalPos)) {
            lastBaritoneGoal = goalPos;
            GoalBlock goal = new GoalBlock(goalPos);
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(goal);
            info("Pathing to approach position");
        }

        stateTimer = moveDelay.get();
    }

    private void handleResetPearlPrepare() {
        // Use correct coordinates based on where we are
        BlockPos throwPos;
        double throwYaw, throwPitch;
        if (isGoingToInput) {
            throwPos = inputPearlThrowPos.get();
            throwYaw = inputPearlThrowYaw.get();
            throwPitch = inputPearlThrowPitch.get();
        } else {
            throwPos = outputPearlThrowPos.get();
            throwYaw = outputPearlThrowYaw.get();
            throwPitch = outputPearlThrowPitch.get();
        }

        // ALWAYS sneak to prevent falling or water walking
        mc.options.sneakKey.setPressed(true);

        // Check if there's a trapdoor at the throw position
        BlockState throwState = mc.world.getBlockState(throwPos);
        boolean isTrapdoor = throwState.getBlock() instanceof TrapdoorBlock;

        // Store current position as safe retreat if not already set
        if (safeRetreatPos == null) {
            safeRetreatPos = mc.player.getBlockPos();
            info("Stored retreat position");
        }

        // Target the exact center of the water/trapdoor block
        double targetX = throwPos.getX() + 0.5;
        double targetZ = throwPos.getZ() + 0.5;

        // Calculate vector from player to target
        double dx = targetX - mc.player.getX();
        double dz = targetZ - mc.player.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Calculate the EXACT yaw needed to face the target from current position
        double requiredYaw = Math.toDegrees(Math.atan2(-dx, dz));

        // Store the approach yaw for backward movement later
        // This is the direction we're walking toward the pearl position
        if (approachDirection == null && horizontalDistance > 0.1) {
            // Determine which direction we're approaching from based on position
            double approachAngle = Math.toDegrees(Math.atan2(dx, -dz)); // Note: different order for approach
            // Store as a direction for clarity
            if (Math.abs(approachAngle) <= 45) {
                approachDirection = Direction.NORTH;
            } else if (Math.abs(approachAngle) >= 135) {
                approachDirection = Direction.SOUTH;
            } else if (approachAngle > 45 && approachAngle < 135) {
                approachDirection = Direction.EAST;
            } else {
                approachDirection = Direction.WEST;
            }
            info("Approach direction: " + approachDirection);
        }

        // Look slightly down at the water source
        double requiredPitch = 15.0; // Look slightly down, not too steep

        // Set and LOCK the rotation - this is critical
        Rotations.rotate(requiredYaw, requiredPitch);

        // Also set the player's actual rotation to ensure it sticks
        mc.player.setYaw((float)requiredYaw);
        mc.player.setPitch((float)requiredPitch);

        // Check if we're close enough
        boolean inPosition = horizontalDistance < positionTolerance.get();

        if (!inPosition) {
            // Clear ALL movement keys first to ensure clean movement
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);

            // Keep sneaking
            mc.options.sneakKey.setPressed(true);

            // Move ONLY forward - no strafing, no turning
            mc.options.forwardKey.setPressed(true);

            stateTimer++;

            // Log progress
            if (stateTimer % 20 == 0) {
                info(String.format("Approaching water (%.2f blocks away) Yaw: %.1f",
                    horizontalDistance, requiredYaw));
            }

            // Check for stuck condition (not making progress)
            if (stateTimer > 60 && horizontalDistance > 2.0) {
                // Try backing up briefly then continue
                if (stateTimer % 40 < 5) {
                    mc.options.forwardKey.setPressed(false);
                    mc.options.backKey.setPressed(true);
                    info("Backing up briefly to unstick");
                } else {
                    mc.options.backKey.setPressed(false);
                    mc.options.forwardKey.setPressed(true);
                }
            }

            // Timeout with more lenient position acceptance
            if (stateTimer > 120) {
                // Use a slightly larger tolerance after timeout to avoid getting stuck
                if (horizontalDistance < positionTolerance.get() * 1.5) {
                    info("Close enough after timeout");
                    inPosition = true;
                } else {
                    // Keep trying
                    info(String.format("Still approaching (%.2f blocks away)", horizontalDistance));
                }
            }

            return; // Keep moving until in position
        }

        if (inPosition) {
            // We're in position at the edge
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);

            // Keep sneaking to maintain position
            mc.options.sneakKey.setPressed(true);

            // NOW switch to the EXACT configured yaw and pitch for throwing
            // This is different from the yaw we used to walk here
            Rotations.rotate(throwYaw, throwPitch);

            // Set rotation directly
            mc.player.setYaw((float)throwYaw);
            mc.player.setPitch((float)throwPitch);

            info("Switching to throw angle - Yaw: " + String.format("%.3f", throwYaw) +
                 " Pitch: " + String.format("%.3f", throwPitch));

            info("In position, ready to throw");
            currentState = ProcessState.RESET_PEARL_THROW;
            stateTimer = 5; // Very short wait to avoid delays
        }
    }

    private void handleResetPearlThrow() {
        // Get throw parameters
        BlockPos throwPos;
        double throwYaw, throwPitch;
        if (isGoingToInput) {
            throwPos = inputPearlThrowPos.get();
            throwYaw = inputPearlThrowYaw.get();
            throwPitch = inputPearlThrowPitch.get();
        } else {
            throwPos = outputPearlThrowPos.get();
            throwYaw = outputPearlThrowYaw.get();
            throwPitch = outputPearlThrowPitch.get();
        }

        if (!hasThrownPearl) {
            // Make sure we're still sneaking at the edge
            mc.options.sneakKey.setPressed(true);

            // Check if we're dealing with a trapdoor
            BlockState throwState = mc.world.getBlockState(throwPos);
            boolean isTrapdoor = throwState.getBlock() instanceof TrapdoorBlock;

            // Set rotation if not already set
            if (!rotationSet) {
                // Use EXACT configured values for throwing - no calculations!
                Rotations.rotate(throwYaw, throwPitch);

                // Also set player rotation directly
                mc.player.setYaw((float)throwYaw);
                mc.player.setPitch((float)throwPitch);

                info("Set exact throw angle: Yaw=" + String.format("%.3f", throwYaw) +
                     " Pitch=" + String.format("%.3f", throwPitch));

                rotationSet = true;
                rotationStabilizationTimer = 10; // Wait 10 ticks (0.5 seconds) for rotation to stabilize
                return;
            }

            // Wait for rotation to stabilize (important for GrimAC)
            if (rotationStabilizationTimer > 0) {
                // Keep applying the EXACT configured rotation every tick
                Rotations.rotate(throwYaw, throwPitch);
                mc.player.setYaw((float)throwYaw);
                mc.player.setPitch((float)throwPitch);

                rotationStabilizationTimer--;
                if (rotationStabilizationTimer == 0) {
                    info("Rotation stabilized, ready to throw");
                }
                return;
            }

            // Select pearl and ensure it's in hand
            FindItemResult pearl = InvUtils.find(Items.ENDER_PEARL);
            if (pearl.found()) {
                // Only swap if we don't already have a pearl in hand
                if (mc.player.getMainHandStack().getItem() != Items.ENDER_PEARL) {
                    // Save current slot before switching
                    previousSlot = mc.player.getInventory().selectedSlot;
                    InvUtils.swap(pearl.slot(), false);
                    stateTimer = 3; // Wait for swap to register
                    return;
                }

                // Double-check we have pearl in hand
                if (mc.player.getMainHandStack().getItem() != Items.ENDER_PEARL) {
                    warning("Pearl not in hand, retrying swap");
                    return;
                }

                // Very short wait if timer is set
                if (stateTimer > 1) {
                    stateTimer--;
                    return;
                }

                // Store position BEFORE throwing pearl
                initialPlayerPos = mc.player.getPos();

                info("Throwing pearl");

                // Throw pearl
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

                hasThrownPearl = true;
                pearlThrowTime = System.currentTimeMillis();
                info("Threw ender pearl - MOVING BACK NOW!");

                // Calculate the yaw to face AWAY from the pearl (opposite of approach)
                // This ensures we walk straight back regardless of throw rotation
                double retreatYaw = 0;
                if (approachDirection != null) {
                    // Face opposite of approach direction
                    switch (approachDirection) {
                        case NORTH -> retreatYaw = 180; // Face south to walk back
                        case SOUTH -> retreatYaw = 0;   // Face north to walk back
                        case EAST -> retreatYaw = -90;  // Face west to walk back
                        case WEST -> retreatYaw = 90;   // Face east to walk back
                    }
                } else {
                    // Fallback: use current yaw + 180 to walk straight back
                    retreatYaw = mc.player.getYaw() + 180;
                }

                // Set rotation to face away from pearl
                Rotations.rotate(retreatYaw, 0);
                mc.player.setYaw((float)retreatYaw);
                mc.player.setPitch(0);

                // Start moving backward (forward in the retreat direction)
                mc.options.sneakKey.setPressed(true);
                mc.options.forwardKey.setPressed(true); // Walk forward in retreat direction
                mc.options.backKey.setPressed(false);
                mc.player.input.movementForward = 1.0f;
                mc.player.input.movementSideways = 0.0f;

                // No delay
                rotationStabilizationTimer = 0; // Don't wait, move NOW
                stateTimer = 10; // Move back for 10 ticks (0.5 seconds) - just enough to avoid pearl

                currentState = ProcessState.RESET_PEARL_WAIT;
            } else {
                error("No ender pearl found!");
                mc.options.sneakKey.setPressed(false);
                currentState = ProcessState.OUTPUT_PROCESS;
            }
        }
    }

    private void handleResetPearlWait() {
        // Move backward for reduced ticks
        if (stateTimer > 0) {
            // Keep sneaking
            mc.options.sneakKey.setPressed(true);

            // Keep the retreat rotation locked
            double retreatYaw = 0;
            if (approachDirection != null) {
                switch (approachDirection) {
                    case NORTH -> retreatYaw = 180; // Face south to walk back
                    case SOUTH -> retreatYaw = 0;   // Face north to walk back
                    case EAST -> retreatYaw = -90;  // Face west to walk back
                    case WEST -> retreatYaw = 90;   // Face east to walk back
                }
                Rotations.rotate(retreatYaw, 0);
                mc.player.setYaw((float)retreatYaw);
                mc.player.setPitch(0);
            }

            // Clear other movement
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.options.sprintKey.setPressed(false);

            // Force forward movement (in retreat direction)
            mc.options.forwardKey.setPressed(true);

            // Set input directly
            mc.player.input.movementForward = 1.0f;
            mc.player.input.movementSideways = 0.0f;

            stateTimer--;

            // Log progress
            if (stateTimer == 9) {
                info("Moving backward to avoid pearl!");
            }

            if (stateTimer == 0) {
                info("Safe distance reached");
                // Stop all movement
                mc.options.forwardKey.setPressed(false);
                mc.options.backKey.setPressed(false);
                mc.player.input.movementForward = 0.0f;
                // Stop sneaking to allow normal movement
                mc.options.sneakKey.setPressed(false);

                // CRITICAL: Unlock rotation here to allow free look for containers
                // Reset Rotations module state by setting to current rotation
                Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());

                // Store initial position for teleport detection
                if (initialPlayerPos == null) {
                    initialPlayerPos = mc.player.getPos();
                }
            }
            return;
        }

        // Stop all movement and sneaking
        mc.options.sneakKey.setPressed(false);
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);

        // Ensure rotation is unlocked for free look
        Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());

        // Wait to ensure pearl wasn't loaded
        if (System.currentTimeMillis() - pearlThrowTime > pearlWaitTime.get() * 1000) {
            // Check if we're still at the same location (pearl wasn't loaded)
            BlockPos throwPos;
            if (isGoingToInput) {
                throwPos = inputPearlThrowPos.get();
            } else {
                throwPos = outputPearlThrowPos.get();
            }

            // Use the position stored when we threw the pearl
            double distance = mc.player.getPos().distanceTo(initialPlayerPos);

            // Also check if we have pearls in inventory (we shouldn't if it went into stasis)
            FindItemResult pearlCheck = InvUtils.find(Items.ENDER_PEARL);
            boolean stillHasPearl = pearlCheck.found() && pearlCheck.count() > 0;

            if (distance < 5 && !stillHasPearl) {  // Tighter tolerance and check for pearl
                info("Pearl successfully placed in stasis (no pearl in inventory)");

                // Restore offhand item
                restoreOffhandItem();

                // Restore previous hotbar slot if it was saved
                if (previousSlot >= 0 && previousSlot < 9) {
                    mc.player.getInventory().selectedSlot = previousSlot;
                    previousSlot = -1;
                }

                // Reset pearl tracking
                hasThrownPearl = false;
                hasPlacedShulker = false;
                pearlFailRetries = 0;
                approachDirection = null;
                lastBaritoneGoal = null;
                rotationSet = false;
                rotationStabilizationTimer = 0;
                safeRetreatPos = null;

                // IMPORTANT: Final rotation unlock to ensure we can look at containers
                Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());

                // Check where we should continue based on location
                if (isNearInputArea()) {
                    info("Continuing to input process");
                    currentState = ProcessState.INPUT_PROCESS;
                    findNextInputContainer();
                } else if (isNearOutputArea()) {
                    info("Continuing to output process");
                    currentState = ProcessState.OUTPUT_PROCESS;
                    // Small delay before finding containers to ensure rotation is unlocked
                    stateTimer = 5;
                } else {
                    currentState = ProcessState.CHECKING_LOCATION;
                }
            } else {
                warning("Pearl was loaded! Teleportation detected");

                // Pearl failed, retry if we haven't exceeded max retries
                pearlFailRetries++;
                if (pearlFailRetries < maxRetries.get()) {
                    warning("Pearl throw failed, retrying (attempt " + pearlFailRetries + "/" + maxRetries.get() + ")");
                    // Restore offhand and retry pearl pickup
                    restoreOffhandItem();
                    hasThrownPearl = false;
                    hasPlacedShulker = false;
                    rotationSet = false;
                    rotationStabilizationTimer = 0;
                    currentState = ProcessState.RESET_PEARL_PICKUP;
                } else {
                    error("Pearl throw failed after " + maxRetries.get() + " attempts!");
                    restoreOffhandItem();
                    currentState = ProcessState.IDLE;
                }
            }
        }
    }

    private void restoreOffhandItem() {
        // First, move shulker from offhand back to slot 0 if there is one
        ItemStack offhandItem = mc.player.getOffHandStack();
        if (!offhandItem.isEmpty() && isShulkerBox(offhandItem.getItem())) {
            // Move shulker from offhand to hotbar slot 0
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                45, // Offhand slot
                0,
                SlotActionType.PICKUP,
                mc.player
            );

            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                36, // Hotbar slot 0
                0,
                SlotActionType.PICKUP,
                mc.player
            );

            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                45, // Offhand slot (put back what was in slot 0 if anything)
                0,
                SlotActionType.PICKUP,
                mc.player
            );

            info("Moved shulker back to hotbar slot 0");
        }

        // Then restore the original offhand item if there was one
        if (!offhandBackup.isEmpty()) {
            // Find the backup item in inventory
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (ItemStack.areEqual(stack, offhandBackup)) {
                    // Move it back to offhand
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        45, // Offhand slot
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                    );

                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        i < 9 ? i + 36 : i, // Convert inventory slot to handler slot
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                    );

                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        45, // Offhand slot
                        0,
                        SlotActionType.PICKUP,
                        mc.player
                    );

                    info("Restored original offhand item");
                    break;
                }
            }
        }
        offhandBackup = ItemStack.EMPTY;
    }

    // Output process methods
    private void handleOutputProcess() {
        // Small delay to ensure rotation is properly unlocked after pearl reset
        if (stateTimer > 0) {
            stateTimer--;
            // Keep resetting rotation to ensure it's unlocked
            Rotations.rotate(mc.player.getYaw(), mc.player.getPitch());
            return;
        }

        // Reset enderchest flag since we're now at output
        enderChestFull = false;

        // Check if we have items in inventory to deposit
        if (hasItemsToTransfer()) {
            // We have items, find a container to deposit them
            if (currentContainer == null) {
                findNextOutputContainer();
            } else {
                // Continue with current container
                moveToContainer(currentContainer);
            }
            return;
        }

        // Check if we need to get items from enderchest
        if (hasItemsInEnderChest()) {
            info("Inventory empty but enderchest has items, opening enderchest");
            // Find enderchest in output area
            enderChestPos = findNearbyEnderChest();
            if (enderChestPos != null) {
                currentState = ProcessState.OPENING_ENDERCHEST;
                stateTimer = 10; // Hold view for 10 ticks (half second)
            } else {
                warning("No enderchest found in output area!");
                currentState = ProcessState.GOING_BACK;
            }
            return;
        }

        // Both inventory and enderchest are empty
        info("Inventory and enderchest empty, going back to input");
        currentState = ProcessState.GOING_BACK;
    }

    private void findNextOutputContainer() {
        // Find containers that aren't full
        currentContainer = outputContainers.stream()
            .filter(c -> !c.isFull)
            .min(Comparator.comparingDouble(c -> mc.player.getPos().distanceTo(Vec3d.ofCenter(c.pos))))
            .orElse(null);

        if (currentContainer == null) {
            warning("All output containers are full!");
            currentState = ProcessState.GOING_BACK;
        } else {
            moveToContainer(currentContainer);
        }
    }

    private void handleOutputTransferringItems() {
        if (!(mc.currentScreen instanceof GenericContainerScreen)) {
            // Container window was closed unexpectedly (attacked by endermite, etc.)
            // Check if we still have the container and should reopen it
            if (currentContainer != null && !currentContainer.isFull) {
                // Check if we still have items to transfer
                boolean hasItems = false;
                for (int i = 0; i < 36; i++) {
                    if (!mc.player.getInventory().getStack(i).isEmpty()) {
                        hasItems = true;
                        break;
                    }
                }

                if (hasItems) {
                    warning("Container window closed unexpectedly! Reopening...");
                    // Attempt to reopen the container
                    currentState = ProcessState.OPENING_CONTAINER;
                    stateTimer = 10;
                    containerOpenFailures++;

                    // If we've failed too many times, skip this container
                    if (containerOpenFailures > 3) {
                        warning("Failed to reopen container multiple times, marking as full");
                        currentContainer.isFull = true;
                        currentContainer = null;
                        containerOpenFailures = 0;
                        currentState = ProcessState.OUTPUT_PROCESS;
                    }
                    return;
                }
            }

            currentState = ProcessState.OUTPUT_PROCESS;
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
            currentState = ProcessState.CLOSING_CONTAINER;
            return;
        }

        boolean transferredItem = false;
        boolean containerHasSpace = false;

        // Check if container has any empty slots
        for (int i = 0; i < currentContainer.totalSlots; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) {
                containerHasSpace = true;
                break;
            }
        }

        if (!containerHasSpace) {
            // Container is full
            currentContainer.isFull = true;
            info("Container is now full");
            currentState = ProcessState.CLOSING_CONTAINER;
            return;
        }

        // Check if we have any items to transfer
        boolean hasItems = false;
        for (int i = 0; i < 36; i++) { // Check player inventory (not including armor/offhand)
            if (!mc.player.getInventory().getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            // No items to transfer
            info("No items left to transfer");
            currentState = ProcessState.CLOSING_CONTAINER;
            return;
        }

        // Inventory to container
        // In a container GUI, player inventory slots start after container slots
        // Container slots: 0 to currentContainer.totalSlots-1
        // Player inventory slots: currentContainer.totalSlots to currentContainer.totalSlots+35
        int playerInventoryStart = currentContainer.totalSlots;

        for (int i = playerInventoryStart; i < playerInventoryStart + 36; i++) {
            Slot slot = handler.getSlot(i);
            ItemStack stack = slot.getStack();

            if (!stack.isEmpty()) {
                // Only transfer shulkers when onlyShulkers is enabled
                if (onlyShulkers.get() && !isShulkerBox(stack.getItem())) {
                    continue;
                }

                // Shift-click to transfer item to container
                mc.interactionManager.clickSlot(
                    handler.syncId,
                    slot.id,
                    0, // button (0 for left click)
                    SlotActionType.QUICK_MOVE, // shift-click
                    mc.player
                );

                transferredItem = true;
                itemsTransferred++;
                stateTimer = transferDelay.get();
                return; // Process one item at a time
            }
        }

        // No more items to transfer or container is full
        if (!transferredItem) {
            // Check if we actually have no items left
            boolean inventoryEmpty = true;
            for (int i = 0; i < 36; i++) {
                if (!mc.player.getInventory().getStack(i).isEmpty()) {
                    inventoryEmpty = false;
                    break;
                }
            }

            if (inventoryEmpty) {
                // Check if we have items in enderchest
                if (enderChestHasItems && fillEnderChest.get()) {
                    info("Inventory empty but enderchest has items, retrieving from enderchest");
                    currentState = ProcessState.CLOSING_CONTAINER;
                } else {
                    info("Inventory and enderchest empty");
                    currentState = ProcessState.CLOSING_CONTAINER;
                }
            } else {
                // Container must be full if we couldn't transfer
                currentContainer.isFull = true;
                info("Container is now full");
                currentState = ProcessState.CLOSING_CONTAINER;
            }
        }
    }

    private boolean hasItemsInEnderChest() {
        // Return true if we stored items and haven't emptied yet
        // This flag is set when we put items in and cleared when we empty
        return enderChestHasItems && !enderChestEmptied;
    }

    // Go back process methods
    private void handleGoingBack() {
        switch (goBackMethod.get()) {
            case KILL -> {
                if (!waitingForRespawn) {
                    String killCommand = "/kill";
                    if (killRetryCount > 0) {
                        // Add random string if we're retrying due to spam filter
                        killCommand = "/kill " + generateRandomString(6);
                    }
                    ChatUtils.sendPlayerMsg(killCommand);
                    info("Sent kill command: " + killCommand);
                    waitingForRespawn = true;
                    lastKillTime = System.currentTimeMillis();
                    initialPlayerPos = mc.player.getPos();
                }

                // Check if player is dead and send respawn packet
                if (mc.player.isDead() || mc.player.getHealth() <= 0) {
                    // Send respawn packet to respawn
                    mc.getNetworkHandler().sendPacket(new ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN));
                    info("Sent respawn packet");
                }

                // Check if we respawned (position changed significantly or health restored)
                Vec3d currentPos = mc.player.getPos();
                double distance = currentPos.distanceTo(initialPlayerPos);

                if ((distance > 100 || mc.player.getHealth() > 0) && System.currentTimeMillis() - lastKillTime > 1000) {
                    if (isNearInputArea()) {
                        info("Respawned at input area!");
                        waitingForRespawn = false;
                        killRetryCount = 0;
                        currentState = ProcessState.INPUT_PROCESS;
                        findNextInputContainer();
                    } else if (System.currentTimeMillis() - lastKillTime > 5000) {
                        // Retry if not respawned after 5 seconds (likely spam filtered)
                        killRetryCount++;
                        waitingForRespawn = false;
                        warning("Kill command may have been spam filtered, retrying with random suffix...");
                    }
                }
            }
            case PEARL -> {
                // Pearl loading method for going back
                if (!waitingForPearl) {
                    sendGoBackPearlCommand();
                    waitingForPearl = true;
                    lastPearlMessageTime = System.currentTimeMillis();
                    pearlRetryCount = 0;
                    initialPlayerPos = mc.player.getPos();
                }

                // Check teleport
                Vec3d currentPos = mc.player.getPos();
                double distance = currentPos.distanceTo(initialPlayerPos);

                // If we moved more than 100 blocks, we likely teleported
                if (distance > 100) {
                    // Check if we're back at input area
                    if (isNearInputArea()) {
                        info("Successfully returned to input area via pearl!");
                        waitingForPearl = false;

                        // Always reset pearl after GoBack pearl teleport (as per requirements)
                        currentState = ProcessState.RESET_PEARL_PICKUP;
                        hasThrownPearl = false;
                        hasPlacedShulker = false;
                        isGoingToInput = true; // We're going back to input area
                    } else if (!isNearOutputArea()) {
                        // We teleported but not to the expected area, retry
                        warning("Teleported but not to input area, retrying...");
                        waitingForPearl = false;
                        // Don't change state, let it retry
                    }
                } else {
                    // Timeout check and retry
                    if (System.currentTimeMillis() - lastPearlMessageTime > pearlTimeout.get() * 1000) {
                        if (pearlRetryCount < maxRetries.get()) {
                            pearlRetryCount++;
                            info("Go back pearl timeout, retrying (attempt " + pearlRetryCount + "/" + maxRetries.get() + ")");
                            sendGoBackPearlCommand();
                            lastPearlMessageTime = System.currentTimeMillis();
                        } else {
                            error("Go back pearl loading failed after " + maxRetries.get() + " retries!");
                            currentState = ProcessState.IDLE;
                            waitingForPearl = false;
                        }
                    }
                }
            }
        }
    }

    private void sendGoBackPearlCommand() {
        String randomSuffix = generateRandomString(8);
        String command = String.format("/msg %s %s %s",
            goBackPlayerName.get(),
            goBackCommand.get(),
            randomSuffix);

        ChatUtils.sendPlayerMsg(command);
        info("Sent go back command: " + command);
    }

    // Helper methods
    private boolean hasValidAreas() {
        return inputAreaPos1 != null && inputAreaPos2 != null &&
               outputAreaPos1 != null && outputAreaPos2 != null;
    }

    private boolean isNearInputArea() {
        if (inputAreaPos1 == null || inputAreaPos2 == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        return playerPos.getX() >= inputAreaPos1.getX() - 10 &&
               playerPos.getX() <= inputAreaPos2.getX() + 10 &&
               playerPos.getY() >= inputAreaPos1.getY() - 5 &&
               playerPos.getY() <= inputAreaPos2.getY() + 5 &&
               playerPos.getZ() >= inputAreaPos1.getZ() - 10 &&
               playerPos.getZ() <= inputAreaPos2.getZ() + 10;
    }

    private boolean isNearOutputArea() {
        if (outputAreaPos1 == null || outputAreaPos2 == null) return false;

        BlockPos playerPos = mc.player.getBlockPos();
        return playerPos.getX() >= outputAreaPos1.getX() - 10 &&
               playerPos.getX() <= outputAreaPos2.getX() + 10 &&
               playerPos.getY() >= outputAreaPos1.getY() - 5 &&
               playerPos.getY() <= outputAreaPos2.getY() + 5 &&
               playerPos.getZ() >= outputAreaPos1.getZ() - 10 &&
               playerPos.getZ() <= outputAreaPos2.getZ() + 10;
    }

    private boolean isServerLagging() {
        // Simple lag detection based on TPS or packet response time
        return false; // Implement actual lag detection if needed
    }

    private boolean isInventoryFull() {
        // Check main inventory slots (9-35) and hotbar (0-8), but NOT offhand
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return false;
            }
            // If we only want shulkers, non-shulker items don't count as taking space
            if (onlyShulkers.get() && !isShulkerBox(stack.getItem())) {
                return false; // This slot can be replaced by dropping the item
            }
        }
        // Also check offhand is empty (for pearl handling)
        ItemStack offhand = mc.player.getOffHandStack();
        if (!offhand.isEmpty() && !isShulkerBox(offhand.getItem())) {
            // Offhand should be empty or only have a shulker for pearl process
            return true;
        }
        return true;
    }

    private boolean isEnderChestFull() {
        return enderChestFull;
    }

    private boolean hasItemsToTransfer() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                if (!onlyShulkers.get() || isShulkerBox(stack.getItem())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isShulkerBox(Item item) {
        return item == Items.SHULKER_BOX ||
               item == Items.WHITE_SHULKER_BOX ||
               item == Items.ORANGE_SHULKER_BOX ||
               item == Items.MAGENTA_SHULKER_BOX ||
               item == Items.LIGHT_BLUE_SHULKER_BOX ||
               item == Items.YELLOW_SHULKER_BOX ||
               item == Items.LIME_SHULKER_BOX ||
               item == Items.PINK_SHULKER_BOX ||
               item == Items.GRAY_SHULKER_BOX ||
               item == Items.LIGHT_GRAY_SHULKER_BOX ||
               item == Items.CYAN_SHULKER_BOX ||
               item == Items.PURPLE_SHULKER_BOX ||
               item == Items.BLUE_SHULKER_BOX ||
               item == Items.BROWN_SHULKER_BOX ||
               item == Items.GREEN_SHULKER_BOX ||
               item == Items.RED_SHULKER_BOX ||
               item == Items.BLACK_SHULKER_BOX;
    }

    private boolean isContainerItem(Item item) {
        // Check if item is a container that we might want to keep
        return item == Items.CHEST ||
               item == Items.TRAPPED_CHEST ||
               item == Items.BARREL ||
               item == Items.ENDER_CHEST;
    }

    private BlockPos findNearbyEnderChest() {
        int searchRadius = 5;
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block instanceof EnderChestBlock) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    private BlockPos findSuitablePlacePos() {
        BlockPos playerPos = mc.player.getBlockPos();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = playerPos.add(x, 0, z);

                if (mc.world.getBlockState(pos).isAir() &&
                    mc.world.getBlockState(pos.down()).isSolidBlock(mc.world, pos.down())) {
                    return pos;
                }
            }
        }

        return null;
    }

    private void placeEnderChest(BlockPos pos, int slot) {
        InvUtils.swap(slot, false);

        BlockHitResult hitResult = new BlockHitResult(
            Vec3d.ofCenter(pos),
            Direction.UP,
            pos.down(),
            false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);

        enderChestPos = pos;
        currentState = ProcessState.OPENING_ENDERCHEST;
        stateTimer = openDelay.get();
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }


    // getWidget override for custom buttons

    // Public API methods for commands
    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public BlockPos getSelectionPos1() {
        return selectionPos1;
    }

    public boolean isSelecting() {
        return selectionMode != SelectionMode.NONE;
    }

    public ProcessState getCurrentState() {
        return currentState;
    }

    public int getItemsTransferred() {
        return itemsTransferred;
    }

    public int getContainersProcessed() {
        return containersProcessed;
    }

    public boolean hasInputArea() {
        return inputAreaPos1 != null && inputAreaPos2 != null;
    }

    public boolean hasOutputArea() {
        return outputAreaPos1 != null && outputAreaPos2 != null;
    }

    public int getInputContainerCount() {
        return inputContainers.size();
    }

    public int getOutputContainerCount() {
        return outputContainers.size();
    }

    public void clearAreas() {
        inputAreaPos1 = null;
        inputAreaPos2 = null;
        outputAreaPos1 = null;
        outputAreaPos2 = null;
        inputContainers.clear();
        outputContainers.clear();
        selectionMode = SelectionMode.NONE;
        info("All areas cleared");
    }

    public void renderAreas(Render3DEvent event) {
        // Render input area
        if (inputAreaPos1 != null && inputAreaPos2 != null) {
            Box inputBox = new Box(
                inputAreaPos1.getX(), inputAreaPos1.getY(), inputAreaPos1.getZ(),
                inputAreaPos2.getX() + 1, inputAreaPos2.getY() + 1, inputAreaPos2.getZ() + 1
            );
            SettingColor inputColor = new SettingColor(0, 255, 0, 50);
            event.renderer.box(inputBox, inputColor, inputColor, ShapeMode.Both, 0);
        }

        // Render output area
        if (outputAreaPos1 != null && outputAreaPos2 != null) {
            Box outputBox = new Box(
                outputAreaPos1.getX(), outputAreaPos1.getY(), outputAreaPos1.getZ(),
                outputAreaPos2.getX() + 1, outputAreaPos2.getY() + 1, outputAreaPos2.getZ() + 1
            );
            SettingColor outputColor = new SettingColor(0, 0, 255, 50);
            event.renderer.box(outputBox, outputColor, outputColor, ShapeMode.Both, 0);
        }
    }
}

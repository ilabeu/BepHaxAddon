package bep.hax.modules;
import bep.hax.Bep;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import java.util.ArrayList;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;

public class PearlLoader extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCoordinates = settings.createGroup("Coordinates");
    private final SettingGroup sgTrigger = settings.createGroup("Trigger");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // Coordinate settings for the walking loop
    private final Setting<BlockPos> walkPoint1 = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Walk Point 1")
        .description("First position for anti-AFK walking loop")
        .defaultValue(new BlockPos(0, 64, 0))
        .build()
    );

    private final Setting<BlockPos> walkPoint2 = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Walk Point 2")
        .description("Second position for anti-AFK walking loop")
        .defaultValue(new BlockPos(10, 64, 0))
        .build()
    );

    private final Setting<BlockPos> trapdoorPosition = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Trapdoor Position")
        .description("Position of the trapdoor for pearl loading")
        .defaultValue(new BlockPos(5, 64, 5))
        .build()
    );

    // Load mode selection
    public enum LoadMode {
        TRAPDOOR("Trapdoor - Interact with trapdoor to load pearl"),
        WALK_TO("Walk To - Walk to position and return");

        private final String description;

        LoadMode(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private final Setting<LoadMode> loadMode = sgGeneral.add(new EnumSetting.Builder<LoadMode>()
        .name("Load Mode")
        .description("Method to use for loading the pearl")
        .defaultValue(LoadMode.TRAPDOOR)
        .build()
    );

    // Trigger settings
    private final Setting<String> triggerKeyword = sgTrigger.add(new StringSetting.Builder()
        .name("Trigger Keyword")
        .description("Keyword in chat to trigger pearl loading")
        .defaultValue("!pearl")
        .build()
    );

    private final Setting<Boolean> enableTrigger = sgTrigger.add(new BoolSetting.Builder()
        .name("Enable Chat Trigger")
        .description("Enable pearl loading via chat messages")
        .defaultValue(true)
        .build()
    );

    // Whitelist settings
    private final Setting<Boolean> useWhitelist = sgWhitelist.add(new BoolSetting.Builder()
        .name("Use Whitelist")
        .description("Only accept triggers from whitelisted players")
        .defaultValue(true)
        .visible(enableTrigger::get)
        .build()
    );

    private final Setting<List<String>> whitelistedPlayers = sgWhitelist.add(new StringListSetting.Builder()
        .name("Whitelisted Players")
        .description("Players who can trigger pearl loading")
        .defaultValue(new ArrayList<>())
        .visible(() -> enableTrigger.get() && useWhitelist.get())
        .build()
    );

    // Timing settings
    private final Setting<Double> reachThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("Reach Threshold")
        .description("Fallback distance threshold if Baritone check fails")
        .defaultValue(0.5)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .build()
    );

    private final Setting<Double> trapdoorCloseTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("Trapdoor Close Time")
        .description("Seconds to keep trapdoor closed for pearl to load")
        .defaultValue(2.0)
        .min(0.5)
        .max(10.0)
        .sliderRange(0.5, 10.0)
        .visible(() -> loadMode.get() == LoadMode.TRAPDOOR)
        .build()
    );

    private final Setting<Double> standTime = sgGeneral.add(new DoubleSetting.Builder()
        .name("Stand Time")
        .description("Seconds to stand at load position")
        .defaultValue(1.0)
        .min(0.5)
        .max(3.0)
        .sliderRange(0.5, 3.0)
        .visible(() -> loadMode.get() == LoadMode.WALK_TO)
        .build()
    );

    private final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
        .name("Debug Mode")
        .description("Show detailed debug information")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> arrivalWaitTicks = sgGeneral.add(new IntSetting.Builder()
        .name("Arrival Wait Ticks")
        .description("Ticks to wait after arriving at position for lag")
        .defaultValue(20)
        .min(0)
        .max(100)
        .sliderRange(0, 100)
        .build()
    );

    // State management
    private enum State {
        WALKING_TO_POINT1,
        WALKING_TO_POINT2,
        WALKING_TO_TRAPDOOR,
        ARRIVED_AT_TRAPDOOR,
        ROTATING_TO_TRAPDOOR,
        CLOSING_TRAPDOOR,
        WAITING_CLOSED,
        OPENING_TRAPDOOR,
        WALKING_TO_LOAD_POSITION,
        ARRIVED_AT_LOAD_POSITION,
        STANDING_AT_LOAD,
        RETURNING_FROM_LOAD
    }

    private State currentState = State.WALKING_TO_POINT2;
    private boolean isActive = false;
    private boolean pearlLoadTriggered = false;
    private long stateStartTime = 0;
    private long lastInteractionTime = 0;
    private BlockPos currentTarget = null;
    private int rotationTicks = 0;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean trapdoorWasClosed = false;

    public PearlLoader() {
        super(Bep.CATEGORY, "PearlLoader", "Anti-AFK loop with pearl loading capability");
    }

    @Override
    public void onActivate() {
        isActive = true;
        currentState = State.WALKING_TO_POINT2;
        pearlLoadTriggered = false;
        stateStartTime = System.currentTimeMillis();
        currentTarget = walkPoint2.get();

        startPathing(currentTarget);

        info("Pearl Loader activated - Starting anti-AFK loop");
    }

    @Override
    public void onDeactivate() {
        isActive = false;
        stopPathing();
        info("Pearl Loader deactivated");
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive || !enableTrigger.get() || pearlLoadTriggered) return;

        String fullMessage = event.getMessage().getString();
        String messageLower = fullMessage.toLowerCase();
        String keyword = triggerKeyword.get().toLowerCase();

        // Check if message contains the keyword
        if (!messageLower.contains(keyword)) return;

        // Extract sender name if using whitelist
        if (useWhitelist.get()) {
            String sender = extractSenderName(fullMessage);
            if (sender == null || !isPlayerWhitelisted(sender)) {
                if (debugMode.get()) {
                    info("Trigger ignored - player not whitelisted: " + sender);
                }
                return;
            }
            info("Pearl load triggered by " + sender + " with keyword: " + triggerKeyword.get());
        } else {
            info("Pearl load triggered by keyword: " + triggerKeyword.get());
        }

        triggerPearlLoad();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isActive || mc.player == null || mc.world == null) return;

        // Handle pearl loading sequence
        if (pearlLoadTriggered) {
            handlePearlLoadingSequence();
            return;
        }

        // Handle normal walking loop
        handleWalkingLoop();
    }

    private void handleWalkingLoop() {
        if (currentTarget == null) return;

        if (isPathingDone()) {
            // Reached target, switch to next point
            if (currentState == State.WALKING_TO_POINT1) {
                currentState = State.WALKING_TO_POINT2;
                currentTarget = walkPoint2.get();
                if (debugMode.get()) info("Reached Point 1, walking to Point 2");
            } else if (currentState == State.WALKING_TO_POINT2) {
                currentState = State.WALKING_TO_POINT1;
                currentTarget = walkPoint1.get();
                if (debugMode.get()) info("Reached Point 2, walking to Point 1");
            }

            startPathing(currentTarget);
        }
    }

    private void handlePearlLoadingSequence() {
        switch (currentState) {
            case WALKING_TO_TRAPDOOR -> handleWalkingToTrapdoor();
            case ARRIVED_AT_TRAPDOOR -> handleArrivedAtTrapdoor();
            case WALKING_TO_LOAD_POSITION -> handleWalkingToLoadPosition();
            case ARRIVED_AT_LOAD_POSITION -> handleArrivedAtLoadPosition();
            case ROTATING_TO_TRAPDOOR -> handleRotatingToTrapdoor();
            case CLOSING_TRAPDOOR -> handleClosingTrapdoor();
            case WAITING_CLOSED -> handleWaitingClosed();
            case OPENING_TRAPDOOR -> handleOpeningTrapdoor();
            case STANDING_AT_LOAD -> handleStandingAtLoad();
            case RETURNING_FROM_LOAD -> handleReturningFromLoad();
        }
    }

    private void handleWalkingToTrapdoor() {
        BlockPos targetPos = getTrapdoorApproachPosition();
        double distance = getDistanceToTarget(targetPos);
        if (distance <= reachThreshold.get()) {
            stopPathing();
            currentState = State.ARRIVED_AT_TRAPDOOR;
            stateStartTime = System.currentTimeMillis();
            if (debugMode.get()) info("Within threshold of trapdoor approach position, waiting for settle");
        } else if (isPathingDone()) {
            startPathing(targetPos);
            if (debugMode.get()) info("Pathing done but not close enough, restarting pathing to approach position");
        }
    }

    private void handleArrivedAtTrapdoor() {
        long elapsed = System.currentTimeMillis() - stateStartTime;
        long waitTime = arrivalWaitTicks.get() * 50; // approx 50ms per tick

        BlockPos targetPos = getTrapdoorApproachPosition();
        double distance = getDistanceToTarget(targetPos);

        if (distance > reachThreshold.get()) {
            currentState = State.WALKING_TO_TRAPDOOR;
            startPathing(targetPos);
            if (debugMode.get()) info("Drifted away during wait, returning to walking");
            return;
        }

        if (elapsed >= waitTime) {
            currentState = State.ROTATING_TO_TRAPDOOR;
            stateStartTime = System.currentTimeMillis();
            rotationTicks = 0;
            if (debugMode.get()) info("Wait after arrival complete, proceeding to rotate");
        }
    }

    private void handleWalkingToLoadPosition() {
        BlockPos target = trapdoorPosition.get();
        double distance = getDistanceToTarget(target);
        if (distance <= reachThreshold.get()) {
            stopPathing();
            currentState = State.ARRIVED_AT_LOAD_POSITION;
            stateStartTime = System.currentTimeMillis();
            if (debugMode.get()) info("Within threshold of load position, waiting for settle");
        } else if (isPathingDone()) {
            startPathing(target);
            if (debugMode.get()) info("Pathing done but not close enough, restarting pathing to load position");
        }
    }

    private void handleArrivedAtLoadPosition() {
        long elapsed = System.currentTimeMillis() - stateStartTime;
        long waitTime = arrivalWaitTicks.get() * 50;

        BlockPos target = trapdoorPosition.get();
        double distance = getDistanceToTarget(target);

        if (distance > reachThreshold.get()) {
            currentState = State.WALKING_TO_LOAD_POSITION;
            startPathing(target);
            if (debugMode.get()) info("Drifted away during wait, returning to walking");
            return;
        }

        if (elapsed >= waitTime) {
            currentState = State.STANDING_AT_LOAD;
            stateStartTime = System.currentTimeMillis();
            if (debugMode.get()) info("Wait after arrival complete, starting stand time");
        }
    }

    private void handleRotatingToTrapdoor() {
        if (rotateToBlock(trapdoorPosition.get())) {
            // Check if block is actually a trapdoor
            BlockState state = mc.world.getBlockState(trapdoorPosition.get());
            if (!(state.getBlock() instanceof TrapdoorBlock)) {
                error("No trapdoor found at specified position!");
                resetToLoop();
                return;
            }

            boolean isOpen = state.get(TrapdoorBlock.OPEN);
            if (debugMode.get()) info("Rotation complete, trapdoor is currently " + (isOpen ? "OPEN" : "CLOSED") + ", proceeding to interact");

            currentState = State.CLOSING_TRAPDOOR;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private void handleClosingTrapdoor() {
        double distToTrapdoor = getDistanceToTarget(trapdoorPosition.get());
        if (distToTrapdoor > 5.0) {
            error("Too far from trapdoor to interact safely! Restarting approach.");
            currentState = State.WALKING_TO_TRAPDOOR;
            BlockPos approachPos = getTrapdoorApproachPosition();
            startPathing(approachPos);
            return;
        }

        BlockState state = mc.world.getBlockState(trapdoorPosition.get());
        boolean isOpen = state.get(TrapdoorBlock.OPEN);

        if (isOpen) {
            interactWithTrapdoor();
            trapdoorWasClosed = true;
            if (debugMode.get()) info("Closed trapdoor");
        } else {
            if (debugMode.get()) info("Trapdoor already closed");
            trapdoorWasClosed = false;
        }

        // Move to waiting state
        currentState = State.WAITING_CLOSED;
        stateStartTime = System.currentTimeMillis();
    }

    private void handleWaitingClosed() {
        long elapsed = System.currentTimeMillis() - stateStartTime;
        long waitTime = (long)(trapdoorCloseTime.get() * 1000);

        // Check if trapdoor is actually closed after a short delay (account for server lag)
        if (elapsed > 500) {
            BlockState state = mc.world.getBlockState(trapdoorPosition.get());
            boolean isOpen = state.get(TrapdoorBlock.OPEN);
            if (isOpen) {
                if (debugMode.get()) info("Trapdoor not closed properly, retrying close");
                currentState = State.CLOSING_TRAPDOOR;
                stateStartTime = System.currentTimeMillis();
                return;
            }
        }

        // Log progress periodically in debug mode
        if (debugMode.get() && elapsed % 1000 < 50) {
            info(String.format("Waiting with trapdoor closed: %.1f / %.1f seconds",
                elapsed / 1000.0, trapdoorCloseTime.get()));
        }

        // Wait for the configured time before opening
        if (elapsed >= waitTime) {
            currentState = State.OPENING_TRAPDOOR;
            stateStartTime = System.currentTimeMillis();
            if (debugMode.get()) info("Wait complete after " + (elapsed/1000.0) + " seconds, now opening trapdoor");
        }
    }

    private void handleOpeningTrapdoor() {
        double distToTrapdoor = getDistanceToTarget(trapdoorPosition.get());
        if (distToTrapdoor > 5.0) {
            error("Too far from trapdoor to interact safely! Restarting approach.");
            currentState = State.WALKING_TO_TRAPDOOR;
            BlockPos approachPos = getTrapdoorApproachPosition();
            startPathing(approachPos);
            return;
        }

        BlockState state = mc.world.getBlockState(trapdoorPosition.get());
        boolean isOpen = state.get(TrapdoorBlock.OPEN);

        if (!isOpen) {
            interactWithTrapdoor();
            if (debugMode.get()) info("Opened trapdoor");
        } else {
            if (debugMode.get()) info("Trapdoor already open");
        }

        info("Pearl loading complete");
        resetToLoop();
    }

    private void handleStandingAtLoad() {
        long elapsed = System.currentTimeMillis() - stateStartTime;
        long waitTime = (long)(standTime.get() * 1000);

        if (elapsed >= waitTime) {
            currentState = State.RETURNING_FROM_LOAD;
            currentTarget = walkPoint1.get();
            startPathing(currentTarget);
            if (debugMode.get()) info("Stand time complete, returning to loop");
        }
    }

    private void handleReturningFromLoad() {
        if (isPathingDone() || getDistanceToTarget(currentTarget) <= reachThreshold.get()) {
            info("Pearl loading complete");
            resetToLoop();
        }
    }

    private void triggerPearlLoad() {
        if (pearlLoadTriggered) return;

        pearlLoadTriggered = true;
        stopPathing();

        if (loadMode.get() == LoadMode.TRAPDOOR) {
            currentState = State.WALKING_TO_TRAPDOOR;
            BlockPos approachPos = getTrapdoorApproachPosition();
            startPathing(approachPos);
            if (debugMode.get()) info("Starting trapdoor pearl load sequence");
        } else {
            currentState = State.WALKING_TO_LOAD_POSITION;
            startPathing(trapdoorPosition.get());
            if (debugMode.get()) info("Starting walk-to pearl load sequence");
        }

        stateStartTime = System.currentTimeMillis();
    }

    private void resetToLoop() {
        pearlLoadTriggered = false;
        currentState = State.WALKING_TO_POINT1;
        currentTarget = walkPoint1.get();
        startPathing(currentTarget);
        stateStartTime = System.currentTimeMillis();
    }

    private boolean rotateToBlock(BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d playerEyes = mc.player.getEyePos();
        Vec3d lookVec = target.subtract(playerEyes);

        double dx = lookVec.x;
        double dy = lookVec.y;
        double dz = lookVec.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        targetPitch = (float) Math.toDegrees(Math.atan2(-dy, distance));
        targetPitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f);

        rotationTicks++;

        float yawDiff = wrapDegrees(targetYaw - mc.player.getYaw());
        float pitchDiff = targetPitch - mc.player.getPitch();

        // Smooth rotation
        float rotSpeed = 0.1f;
        mc.player.setYaw(mc.player.getYaw() + yawDiff * rotSpeed);
        mc.player.setPitch(mc.player.getPitch() + pitchDiff * rotSpeed);

        return Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f || rotationTicks > 50;
    }

    private void interactWithTrapdoor() {
        if (System.currentTimeMillis() - lastInteractionTime < 500) return;

        Vec3d hitVec = Vec3d.ofCenter(trapdoorPosition.get());
        Direction hitSide = getClosestSide(trapdoorPosition.get());

        BlockHitResult hitResult = new BlockHitResult(
            hitVec,
            hitSide,
            trapdoorPosition.get(),
            false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        mc.player.swingHand(Hand.MAIN_HAND);

        lastInteractionTime = System.currentTimeMillis();
    }

    private BlockPos getTrapdoorApproachPosition() {
        // Find best position 1 block away from trapdoor
        BlockPos trapPos = trapdoorPosition.get();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

        BlockPos bestPos = null;
        double minDist = Double.MAX_VALUE;

        for (Direction dir : dirs) {
            BlockPos checkPos = trapPos.offset(dir);
            BlockState state = mc.world.getBlockState(checkPos);
            BlockState below = mc.world.getBlockState(checkPos.down());

            if (state.isAir() && below.isSolidBlock(mc.world, checkPos.down())) {
                double dist = mc.player.getPos().distanceTo(Vec3d.ofCenter(checkPos));
                if (dist < minDist) {
                    minDist = dist;
                    bestPos = checkPos;
                }
            }
        }

        return bestPos != null ? bestPos : trapPos.north();
    }

    private Direction getClosestSide(BlockPos pos) {
        Vec3d playerPos = mc.player.getPos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = playerPos.subtract(blockCenter);

        if (Math.abs(diff.x) > Math.abs(diff.z)) {
            return diff.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private double getDistanceToTarget(BlockPos target) {
        if (mc.player == null || target == null) return Double.MAX_VALUE;
        return mc.player.getPos().distanceTo(Vec3d.ofCenter(target));
    }

    private void startPathing(BlockPos target) {
        if (target == null) return;

        try {
            Class.forName("baritone.api.BaritoneAPI");
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(target));
        } catch (ClassNotFoundException e) {
            error("Baritone not available!");
        }
    }

    private void stopPathing() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        } catch (ClassNotFoundException ignored) {}
    }

    private boolean isPathingDone() {
        try {
            return !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing();
        } catch (Exception e) {
            return false;
        }
    }

    private String extractSenderName(String message) {
        // Handle whispers: "PlayerName whispers: message"
        if (message.contains(" whispers: ")) {
            String beforeWhispers = message.substring(0, message.indexOf(" whispers: "));
            // Remove any color codes or formatting
            beforeWhispers = beforeWhispers.replaceAll("§[0-9a-fk-or]", "");
            String[] parts = beforeWhispers.split(" ");
            if (parts.length >= 1) {
                // Get the last word before "whispers:"
                String name = parts[parts.length - 1].trim();
                if (debugMode.get()) info("Extracted whisper sender: " + name);
                return name;
            }
        }
        // Handle public messages: "<PlayerName> message" or "[Rank] PlayerName: message"
        else if (message.contains(": ")) {
            String beforeColon = message.substring(0, message.indexOf(": "));
            // Remove color codes
            beforeColon = beforeColon.replaceAll("§[0-9a-fk-or]", "");

            // Handle <PlayerName> format
            if (beforeColon.contains("<") && beforeColon.contains(">")) {
                int start = beforeColon.lastIndexOf("<");
                int end = beforeColon.lastIndexOf(">");
                if (start < end) {
                    String name = beforeColon.substring(start + 1, end).trim();
                    if (debugMode.get()) info("Extracted public sender: " + name);
                    return name;
                }
            }

            // Handle regular format (last word before colon)
            String[] parts = beforeColon.split(" ");
            if (parts.length > 0) {
                String name = parts[parts.length - 1].replaceAll("[<>\\[\\]]", "").trim();
                if (debugMode.get()) info("Extracted sender: " + name);
                return name;
            }
        }

        if (debugMode.get()) warning("Could not extract sender from message: " + message);
        return null;
    }

    private boolean isPlayerWhitelisted(String playerName) {
        if (playerName == null) return false;
        for (String whitelisted : whitelistedPlayers.get()) {
            if (whitelisted.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    private float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WButton triggerButton = list.add(theme.button("Trigger Pearl Load")).widget();
        triggerButton.action = () -> {
            if (!pearlLoadTriggered && isActive) {
                info("Manually triggering pearl load");
                triggerPearlLoad();
            }
        };

        WButton testLoop = list.add(theme.button("Test Walking Loop")).widget();
        testLoop.action = () -> {
            if (isActive) {
                info("Testing walking loop");
                resetToLoop();
            }
        };

        return list;
    }
}

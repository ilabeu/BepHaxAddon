package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.*;
import java.util.List;
import java.util.Arrays;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class PearlLoader extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCoordinates = settings.createGroup("Coordinates");
    private final SettingGroup sgTrigger = settings.createGroup("Trigger");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    
    // Coordinate settings for the two walking points
    private final Setting<BlockPos> point1 = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Point 1")
        .description("First coordinate to walk between")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );
    
    private final Setting<BlockPos> point2 = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Point 2")
        .description("Second coordinate to walk between")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );
    
    private final Setting<BlockPos> trapdoorPos = sgCoordinates.add(new BlockPosSetting.Builder()
        .name("Trapdoor Position")
        .description("Position of the trapdoor to interact with")
        .defaultValue(new BlockPos(0, 0, 0))
        .build()
    );
    
    // General settings
    private final Setting<Double> reachDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("Reach Distance")
        .description("How close to get to a point before switching")
        .defaultValue(3.0)
        .min(1.0)
        .max(10.0)
        .sliderRange(1.0, 10.0)
        .build()
    );
    
    private final Setting<Double> trapdoorDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("Trapdoor Delay")
        .description("Delay in seconds between trapdoor interactions")
        .defaultValue(1.0)
        .min(0.1)
        .max(5.0)
        .sliderRange(0.1, 5.0)
        .build()
    );
    
    private final Setting<Double> trapdoorReach = sgGeneral.add(new DoubleSetting.Builder()
        .name("Trapdoor Reach")
        .description("Maximum distance to interact with trapdoor")
        .defaultValue(3.5)
        .min(2.0)
        .max(6.0)
        .sliderRange(2.0, 6.0)
        .build()
    );
    
    // Trigger settings
    private final Setting<String> triggerText = sgTrigger.add(new StringSetting.Builder()
        .name("Trigger Text")
        .description("Text that the message must contain to trigger pearl loading")
        .defaultValue("!tp")
        .build()
    );
    
    // Whitelist settings
    private final Setting<Boolean> useWhitelist = sgWhitelist.add(new BoolSetting.Builder()
        .name("Use Whitelist")
        .description("Only accept trigger from whitelisted players")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<List<String>> whitelistedPlayers = sgWhitelist.add(new StringListSetting.Builder()
        .name("Whitelisted Players")
        .description("Players who can trigger pearl loading (case-insensitive)")
        .defaultValue(Arrays.asList("Player1", "Player2"))
        .visible(useWhitelist::get)
        .build()
    );
    
    private final Setting<Boolean> autoWalk = sgGeneral.add(new BoolSetting.Builder()
        .name("Auto Walk")
        .description("Automatically walk between points when module is active")
        .defaultValue(true)
        .build()
    );
    
    // State variables
    private boolean isWalking = false;
    private boolean movingToPoint2 = true;
    private boolean isLoadingPearl = false;
    private BlockPos currentTarget = null;
    private long lastTrapdoorInteraction = 0;
    private int trapdoorInteractionCount = 0;
    private long pearlLoadStartTime = 0;
    private static final long PEARL_LOAD_TIMEOUT = 30000; // 30 second timeout
    private int stuckCheckCounter = 0;
    private BlockPos lastPlayerPos = null;
    private float targetYaw = 0;
    private float targetPitch = 0;
    private boolean isRotating = false;
    private int rotationTicks = 0;
    private boolean waitingForDelay = false;
    private long delayStartTime = 0;
    private boolean firstInteractionDone = false;
    
    public PearlLoader() {
        super(Bep.CATEGORY, "PearlLoader", "Automatically loads pearls using trapdoors when triggered");
    }
    
    @Override
    public void onActivate() {
        isWalking = autoWalk.get();
        movingToPoint2 = true;
        isLoadingPearl = false;
        currentTarget = null;
        trapdoorInteractionCount = 0;
        pearlLoadStartTime = 0;
        stuckCheckCounter = 0;
        lastPlayerPos = null;
        isRotating = false;
        rotationTicks = 0;
        waitingForDelay = false;
        delayStartTime = 0;
        firstInteractionDone = false;
        
        if (isWalking) {
            startWalking();
        }
        
        info("Pearl Loader activated");
    }
    
    @Override
    public void onDeactivate() {
        stopWalking();
        isWalking = false;
        isLoadingPearl = false;
        currentTarget = null;
        trapdoorInteractionCount = 0;
        pearlLoadStartTime = 0;
        stuckCheckCounter = 0;
        lastPlayerPos = null;
        isRotating = false;
        rotationTicks = 0;
        waitingForDelay = false;
        delayStartTime = 0;
        firstInteractionDone = false;
        
        info("Pearl Loader deactivated");
    }
    
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        
        // Button to manually trigger pearl loading
        WButton triggerLoad = list.add(theme.button("Trigger Pearl Load")).widget();
        triggerLoad.action = () -> {
            if (!isLoadingPearl) {
                info("Manually triggering pearl load");
                triggerPearlLoad();
            }
        };
        
        return list;
    }
    
    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        if (!isActive()) return;
        
        String message = event.getMessage().getString();
        
        // Check if this is a whisper message and extract the content
        // Format: "<time> PlayerName whispers: message"
        if (message.contains(" whispers: ")) {
            // Extract the player name and message content
            String beforeWhispers = message.substring(0, message.indexOf(" whispers: "));
            String[] beforeParts = beforeWhispers.split(" ");
            if (beforeParts.length < 2) return;
            
            // Get the player name (last part before "whispers:")
            String playerName = beforeParts[beforeParts.length - 1];
            
            // Check whitelist if enabled
            if (useWhitelist.get()) {
                boolean isWhitelisted = false;
                for (String whitelisted : whitelistedPlayers.get()) {
                    if (whitelisted.equalsIgnoreCase(playerName)) {
                        isWhitelisted = true;
                        break;
                    }
                }
                if (!isWhitelisted) {
                    return; // Player not whitelisted, ignore
                }
            }
            
            // Extract the part after "whispers: "
            String[] parts = message.split(" whispers: ", 2);
            if (parts.length > 1) {
                String whisperContent = parts[1];
                // Check if the whisper content contains our trigger text
                if (whisperContent.toLowerCase().contains(triggerText.get().toLowerCase())) {
                    info("Pearl load triggered by " + playerName);
                    triggerPearlLoad();
                }
            }
        } else {
            // For non-whisper messages, check the entire message
            // You might want to disable this or add different logic for non-whispers
            if (!useWhitelist.get() && message.toLowerCase().contains(triggerText.get().toLowerCase())) {
                info("Pearl load triggered");
                triggerPearlLoad();
            }
        }
    }
    
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        
        // Check for stuck state
        checkIfStuck();
        
        // Handle pearl loading sequence
        if (isLoadingPearl) {
            // Check for timeout
            if (System.currentTimeMillis() - pearlLoadStartTime > PEARL_LOAD_TIMEOUT) {
                error("Pearl loading timed out! Resetting...");
                resetPearlLoadingState();
                return;
            }
            handlePearlLoading();
            return;
        }
        
        // Handle walking between points
        if (isWalking && currentTarget != null) {
            handleWalking();
        }
    }
    
    private void startWalking() {
        try {
            // Set initial target
            currentTarget = movingToPoint2 ? point2.get() : point1.get();
            
            // Use Baritone goto command to path to the target
            Class.forName("baritone.api.BaritoneAPI");
            String gotoCommand = String.format("goto %d %d %d", currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
            baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(gotoCommand);
            
            isWalking = true;
        } catch (ClassNotFoundException e) {
            error("Baritone not available. Walking disabled.");
            isWalking = false;
        }
    }
    
    private void stopWalking() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        } catch (ClassNotFoundException ignored) {}
    }
    
    private void handleWalking() {
        if (mc.player == null || currentTarget == null) return;
        
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(currentTarget));
        
        if (distance <= reachDistance.get()) {
            // Switch target
            movingToPoint2 = !movingToPoint2;
            currentTarget = movingToPoint2 ? point2.get() : point1.get();
            
            // Update Baritone with new goto command
            try {
                Class.forName("baritone.api.BaritoneAPI");
                String gotoCommand = String.format("goto %d %d %d", currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
                baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(gotoCommand);
            } catch (ClassNotFoundException ignored) {}
        }
    }
    
    private void triggerPearlLoad() {
        if (isLoadingPearl) return;
        
        isLoadingPearl = true;
        pearlLoadStartTime = System.currentTimeMillis();
        trapdoorInteractionCount = 0;
        waitingForDelay = false;
        firstInteractionDone = false;
        delayStartTime = 0;
        
        // Stop walking temporarily
        if (isWalking) {
            stopWalking();
        }
        
        // Path to trapdoor using goto command
        try {
            Class.forName("baritone.api.BaritoneAPI");
            BlockPos trapPos = trapdoorPos.get();
            String gotoCommand = String.format("goto %d %d %d", trapPos.getX(), trapPos.getY(), trapPos.getZ());
            baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(gotoCommand);
        } catch (ClassNotFoundException e) {
            error("Baritone not available. Cannot path to trapdoor.");
            resetPearlLoadingState();
            return;
        }
    }
    
    private void handlePearlLoading() {
        if (mc.player == null || mc.world == null) return;
        
        BlockPos trapPos = trapdoorPos.get();
        double distance = mc.player.getPos().distanceTo(Vec3d.ofCenter(trapPos));
        
        // Check if within reach distance
        if (distance > trapdoorReach.get() + 0.5) {
            // Still pathing to trapdoor
            return;
        }
        
        // Stop Baritone pathing once we're close enough
        if (distance <= trapdoorReach.get()) {
            stopWalking();
        }
        
        BlockState state = mc.world.getBlockState(trapPos);
        Block block = state.getBlock();
        
        // Verify it's a trapdoor
        if (!(block instanceof TrapdoorBlock)) {
            error("Block at trapdoor position is not a trapdoor!");
            resetPearlLoadingState();
            return;
        }
        
        // Calculate rotation to look at trapdoor
        Vec3d trapdoorVec = Vec3d.ofCenter(trapPos);
        Vec3d playerEyes = mc.player.getEyePos();
        Vec3d lookVec = trapdoorVec.subtract(playerEyes);
        
        double dx = lookVec.x;
        double dy = lookVec.y;
        double dz = lookVec.z;
        double distance2D = Math.sqrt(dx * dx + dz * dz);
        
        targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        targetPitch = (float) Math.toDegrees(Math.atan2(-dy, distance2D));
        targetPitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f);
        
        // Start rotation if not already rotating
        if (!isRotating) {
            isRotating = true;
            rotationTicks = 0;
        }
        
        // Smoothly rotate towards target
        if (isRotating) {
            rotationTicks++;
            float rotationSpeed = 0.15f; // Adjust for smoother/faster rotation
            
            float yawDiff = wrapDegrees(targetYaw - mc.player.getYaw());
            float pitchDiff = targetPitch - mc.player.getPitch();
            
            // Apply smooth rotation
            mc.player.setYaw(mc.player.getYaw() + yawDiff * rotationSpeed);
            mc.player.setPitch(mc.player.getPitch() + pitchDiff * rotationSpeed);
            
            // Check if we're close enough to the target rotation
            if (Math.abs(yawDiff) < 5.0f && Math.abs(pitchDiff) < 5.0f) {
                isRotating = false;
            } else if (rotationTicks < 10) {
                // Still rotating, wait
                return;
            }
        }
        
        // Handle delay between first and second interaction
        if (waitingForDelay) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - delayStartTime < (long)(trapdoorDelay.get() * 1000)) {
                return; // Still waiting
            }
            waitingForDelay = false;
        }
        
        // Get current trapdoor state before interaction
        boolean isOpenBefore = state.get(TrapdoorBlock.OPEN);
        
        // Determine best hit direction based on player position
        Direction hitDirection = getOptimalHitDirection(trapPos);
        
        // Calculate precise hit vector on the trapdoor face
        Vec3d hitVec = calculateHitVector(trapPos, hitDirection);
        
        // Create hit result
        BlockHitResult hitResult = new BlockHitResult(
            hitVec,
            hitDirection,
            trapPos,
            false
        );
        
        // Send packet directly for more reliable server-side interaction
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
            Hand.MAIN_HAND,
            hitResult,
            0
        ));
        
        // Also do client-side interaction for immediate feedback
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        
        trapdoorInteractionCount++;
        lastTrapdoorInteraction = System.currentTimeMillis();
        
        // Log interaction
        info(String.format("Trapdoor interaction %d - State before: %s", trapdoorInteractionCount, isOpenBefore ? "open" : "closed"));
        
        if (trapdoorInteractionCount == 1) {
            // First interaction done, now wait before second
            firstInteractionDone = true;
            waitingForDelay = true;
            delayStartTime = System.currentTimeMillis();
            info(String.format("Waiting %.1f seconds before closing...", trapdoorDelay.get()));
        } else if (trapdoorInteractionCount >= 2) {
            // Second interaction done, pearl loading complete
            info("Pearl loading complete!");
            resetPearlLoadingState();
            
            // Resume walking if enabled
            if (autoWalk.get()) {
                startWalking();
            }
        }
    }
    
    private void resetPearlLoadingState() {
        isLoadingPearl = false;
        trapdoorInteractionCount = 0;
        pearlLoadStartTime = 0;
        isRotating = false;
        rotationTicks = 0;
        waitingForDelay = false;
        delayStartTime = 0;
        firstInteractionDone = false;
    }
    
    private float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) {
            degrees -= 360.0f;
        }
        if (degrees < -180.0f) {
            degrees += 360.0f;
        }
        return degrees;
    }
    
    private Direction getOptimalHitDirection(BlockPos pos) {
        // Get player position relative to block
        Vec3d playerPos = mc.player.getPos();
        Vec3d blockCenter = Vec3d.ofCenter(pos);
        Vec3d diff = playerPos.subtract(blockCenter);
        
        // Determine which face is closest to player
        double absX = Math.abs(diff.x);
        double absY = Math.abs(diff.y);
        double absZ = Math.abs(diff.z);
        
        // Prefer hitting from above if close enough
        if (absY > 0.5 && diff.y > 0) {
            return Direction.UP;
        }
        
        // Otherwise hit from the side we're facing
        if (absX > absZ) {
            return diff.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
    
    private Vec3d calculateHitVector(BlockPos pos, Direction direction) {
        // Calculate a realistic hit point on the block face
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        // Add small random offset for more realistic interaction
        double offset = 0.3;
        double randomOffset = 0.1;
        
        switch (direction) {
            case UP:
                y = pos.getY() + 1.0;
                x += (Math.random() - 0.5) * randomOffset;
                z += (Math.random() - 0.5) * randomOffset;
                break;
            case DOWN:
                y = pos.getY();
                x += (Math.random() - 0.5) * randomOffset;
                z += (Math.random() - 0.5) * randomOffset;
                break;
            case NORTH:
                z = pos.getZ() - offset;
                x += (Math.random() - 0.5) * randomOffset;
                y += (Math.random() - 0.5) * randomOffset;
                break;
            case SOUTH:
                z = pos.getZ() + 1.0 + offset;
                x += (Math.random() - 0.5) * randomOffset;
                y += (Math.random() - 0.5) * randomOffset;
                break;
            case WEST:
                x = pos.getX() - offset;
                y += (Math.random() - 0.5) * randomOffset;
                z += (Math.random() - 0.5) * randomOffset;
                break;
            case EAST:
                x = pos.getX() + 1.0 + offset;
                y += (Math.random() - 0.5) * randomOffset;
                z += (Math.random() - 0.5) * randomOffset;
                break;
        }
        
        return new Vec3d(x, y, z);
    }
    
    private void checkIfStuck() {
        if (mc.player == null) return;
        
        BlockPos currentPos = mc.player.getBlockPos();
        
        // Check every 20 ticks (1 second)
        if (stuckCheckCounter++ >= 20) {
            stuckCheckCounter = 0;
            
            if (lastPlayerPos != null && lastPlayerPos.equals(currentPos)) {
                // Player hasn't moved in 1 second
                if (isLoadingPearl && System.currentTimeMillis() - pearlLoadStartTime > 10000) {
                    // If stuck during pearl loading for more than 10 seconds
                    warning("Detected stuck state during pearl loading. Attempting recovery...");
                    
                    // Try to unstuck by canceling and restarting
                    stopWalking();
                    
                    // Give it a moment then try to path again
                    if (System.currentTimeMillis() - pearlLoadStartTime > 15000) {
                        resetPearlLoadingState();
                        if (autoWalk.get()) {
                            startWalking();
                        }
                    }
                }
            }
            
            lastPlayerPos = currentPos;
        }
    }
}
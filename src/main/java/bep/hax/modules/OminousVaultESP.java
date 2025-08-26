package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import bep.hax.Bep;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.orbit.EventHandler;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.util.math.ChunkPos;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class OminousVaultESP extends Module {
    public OminousVaultESP() {
        super(Bep.CATEGORY, "Ominous-Vault-ESP", "Highlights Ominous Vaults.");

        // Queue initial chunk scan on world join, paced over ~3.5s
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (mc.world == null || mc.player == null) return;
            initialScanQueue.clear();
            int chunkRadius = mc.options.getViewDistance().getValue();
            int totalChunks = (2 * chunkRadius + 1) * (2 * chunkRadius + 1);
            int durationTicks = 70; // ~3.5s at 20 TPS
            initialScanChunksPerTick = Math.max(1, (int) Math.ceil(totalChunks / (double) durationTicks));
            for (int cx = mc.player.getChunkPos().x - chunkRadius; cx <= mc.player.getChunkPos().x + chunkRadius; cx++) {
                for (int cz = mc.player.getChunkPos().z - chunkRadius; cz <= mc.player.getChunkPos().z + chunkRadius; cz++) {
                    initialScanQueue.addLast(new ChunkPos(cx, cz));
                }
            }
            initialScanActive = true;
        });
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> fillColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("Color of sides.")
        .defaultValue(new SettingColor(0, 120, 120, 50))
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("Color of lines.")
        .defaultValue(new SettingColor(31, 161, 159, 255))
        .build()
    );

    private final Setting<SettingColor> tracerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("Tracer Color")
        .description("Color of tracer line.")
        .defaultValue(new SettingColor(40, 200, 195, 255))
        .build()
    );

    private final Setting<Boolean> renderFill = sgGeneral.add(new BoolSetting.Builder()
        .name("Render Sides")
        .description("Render sides of Ominous Vaults.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderOutline = sgGeneral.add(new BoolSetting.Builder()
        .name("Render Lines")
        .description("Render lines of Ominous Vaults.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderTracer = sgGeneral.add(new BoolSetting.Builder()
        .name("Tracers")
        .description("Add tracers to Ominous Vaults.")
        .defaultValue(true)
        .build()
    );

    private final Set<BlockPos> ominousVaults = java.util.Collections.synchronizedSet(new HashSet<>());
    private final Map<ChunkPos, Set<BlockPos>> chunkVaults = new HashMap<>();
    private Set<ChunkPos> lastLoadedChunks = new HashSet<>();
    private long lastRecheckTime = 0;
    private final int recheckIntervalMs = 4000;
    private final Map<ChunkPos, Integer> pendingChunks = new HashMap<>();

    // Initial join scan pacing
    private final Deque<ChunkPos> initialScanQueue = new ArrayDeque<>();
    private boolean initialScanActive = false;
    private int initialScanChunksPerTick = 0;

    private long lastFullRescan = 0;
    private final int fullRescanIntervalMs = 3000;

    @EventHandler
    private void onTick(meteordevelopment.meteorclient.events.world.TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;
        // Process initial scan queue
        if (initialScanActive) {
            int processed = 0;
            int minY0 = mc.world.getBottomY();
            int maxY0 = mc.world.getHeight();
            while (processed < initialScanChunksPerTick && !initialScanQueue.isEmpty()) {
                ChunkPos cp = initialScanQueue.pollFirst();
                // Scan only if chunk is loaded as WorldChunk
                var chunk = mc.world.getChunk(cp.x, cp.z);
                if (chunk instanceof WorldChunk) {
                    scanChunkForVaults(cp, minY0, maxY0);
                }
                processed++;
            }
            if (initialScanQueue.isEmpty()) initialScanActive = false;
        }

        Set<ChunkPos> currentChunks = new HashSet<>();
        BlockPos playerPos = mc.player.getBlockPos();
        int chunkRadius = mc.options.getViewDistance().getValue();
        int minY = mc.world.getBottomY();
        int maxY = mc.world.getHeight();

        for (int cx = (playerPos.getX() >> 4) - chunkRadius; cx <= (playerPos.getX() >> 4) + chunkRadius; cx++) {
            for (int cz = (playerPos.getZ() >> 4) - chunkRadius; cz <= (playerPos.getZ() >> 4) + chunkRadius; cz++) {
                currentChunks.add(new ChunkPos(cx, cz));
            }
        }

        Set<ChunkPos> newChunks = new HashSet<>(currentChunks);
        newChunks.removeAll(lastLoadedChunks);
        Set<ChunkPos> unloadedChunks = new HashSet<>(lastLoadedChunks);
        unloadedChunks.removeAll(currentChunks);

        for (ChunkPos chunkPos : unloadedChunks) {
            Set<BlockPos> removed = chunkVaults.remove(chunkPos);
            if (removed != null) ominousVaults.removeAll(removed);
            pendingChunks.remove(chunkPos);
        }

        for (ChunkPos chunkPos : newChunks) {
            pendingChunks.put(chunkPos, 10);
        }

        Set<ChunkPos> toScan = new HashSet<>();
        for (Map.Entry<ChunkPos, Integer> entry : new HashMap<>(pendingChunks).entrySet()) {
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) toScan.add(entry.getKey());
            else pendingChunks.put(entry.getKey(), ticksLeft);
        }
        for (ChunkPos chunkPos : toScan) {
            scanChunkForVaults(chunkPos, minY, maxY);
            pendingChunks.remove(chunkPos);
        }

        // Periodically validate cached vaults
        long now = System.currentTimeMillis();
        if (now - lastRecheckTime >= recheckIntervalMs) {
            lastRecheckTime = now;
            Set<BlockPos> toRemoveVaults = new HashSet<>();
            for (BlockPos pos : ominousVaults) {
                BlockState state = mc.world.getBlockState(pos);
                Property<?> ominousProperty = null;
                for (Property<?> prop : state.getProperties()) {
                    if (prop.getName().equals("ominous")) {
                        ominousProperty = prop;
                        break;
                    }
                }
                if (ominousProperty == null || !Boolean.TRUE.equals(state.get(ominousProperty))) {
                    toRemoveVaults.add(pos);
                }
            }
            ominousVaults.removeAll(toRemoveVaults);
            for (Set<BlockPos> set : chunkVaults.values()) set.removeAll(toRemoveVaults);
        }

        if (now - lastFullRescan >= fullRescanIntervalMs) {
            lastFullRescan = now;
            for (ChunkPos chunkPos : lastLoadedChunks) {
                scanChunkForVaults(chunkPos, minY, maxY);
            }
        }

        lastLoadedChunks = currentChunks;
    }

    private boolean scanChunkForVaults(ChunkPos chunkPos, int minY, int maxY) {
        net.minecraft.world.chunk.Chunk chunk;
        try {
            chunk = mc.world.getChunk(chunkPos.x, chunkPos.z);
        } catch (Exception e) {
            return false;
        }
        if (chunk != null && chunk instanceof net.minecraft.world.chunk.WorldChunk) {
            Set<BlockPos> foundVaults = new HashSet<>();
            for (net.minecraft.block.entity.BlockEntity blockEntity : ((net.minecraft.world.chunk.WorldChunk) chunk).getBlockEntities().values()) {
                if (net.minecraft.registry.Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()) != null
                    && net.minecraft.registry.Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType()).getPath().equals("vault")) {
                    BlockPos pos = blockEntity.getPos();
                    BlockState state = mc.world.getBlockState(pos);
                    Property<?> ominousProperty = null;
                    for (Property<?> prop : state.getProperties()) {
                        if (prop.getName().equals("ominous")) {
                            ominousProperty = prop;
                            break;
                        }
                    }
                    if (ominousProperty != null && Boolean.TRUE.equals(state.get(ominousProperty))) {
                        foundVaults.add(pos);
                    }
                }
            }
            if (!foundVaults.isEmpty()) {
                chunkVaults.put(chunkPos, foundVaults);
                ominousVaults.addAll(foundVaults);
                return true;
            }
        }
        return false;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        meteordevelopment.meteorclient.renderer.ShapeMode shapeMode;
        if (renderFill.get() && renderOutline.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Both;
        else if (renderFill.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Sides;
        else if (renderOutline.get()) shapeMode = meteordevelopment.meteorclient.renderer.ShapeMode.Lines;
        else return;

        for (BlockPos pos : ominousVaults) {
            event.renderer.box(
                pos,
                fillColor.get(),
                outlineColor.get(),
                shapeMode,
                0
            );
            if (renderTracer.get()) {
                event.renderer.line(
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.x,
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.y,
                    meteordevelopment.meteorclient.utils.render.RenderUtils.center.z,
                    pos.toCenterPos().x, pos.toCenterPos().y, pos.toCenterPos().z,
                    tracerColor.get()
                );
            }
        }
    }
}

package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.HangingSignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.*;

public class SignRender extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgClustering = settings.createGroup("Clustering");
    private final SettingGroup sgOptimization = settings.createGroup("Optimization");

    // General settings
    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance to render signs (blocks).")
        .defaultValue(512.0)
        .min(16.0)
        .max(1024.0)
        .sliderRange(16.0, 1024.0)
        .build()
    );

    private final Setting<Integer> maxSigns = sgGeneral.add(new IntSetting.Builder()
        .name("max-signs")
        .description("Maximum number of signs to render.")
        .defaultValue(200)
        .min(5)
        .max(500)
        .sliderRange(5, 1000)
        .build()
    );

    private final Setting<Boolean> filterEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("filter-empty")
        .description("Hide empty signs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> multilineDisplay = sgGeneral.add(new BoolSetting.Builder()
        .name("multiline-display")
        .description("Display sign text as multiple lines as they appear on the sign.")
        .defaultValue(true)
        .build()
    );


    // Render settings

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Text color.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );


    private final Setting<SettingColor> backgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color.")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .build()
    );

    private final Setting<Boolean> showBackground = sgRender.add(new BoolSetting.Builder()
        .name("show-background")
        .description("Show background behind text.")
        .defaultValue(true)
        .build()
    );


    // Clustering settings
    private final Setting<Boolean> enableClustering = sgClustering.add(new BoolSetting.Builder()
        .name("enable-clustering")
        .description("Group nearby signs to prevent overlap.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> clusterRadius = sgClustering.add(new DoubleSetting.Builder()
        .name("cluster-radius")
        .description("Screen distance in pixels to group signs.")
        .defaultValue(100.0)
        .min(20.0)
        .max(500.0)
        .sliderRange(20.0, 200.0)
        .visible(enableClustering::get)
        .build()
    );

    private final Setting<ClusterMode> clusterMode = sgClustering.add(new EnumSetting.Builder<ClusterMode>()
        .name("cluster-mode")
        .description("How to display clustered signs.")
        .defaultValue(ClusterMode.Count)
        .visible(enableClustering::get)
        .build()
    );

    private final Setting<Integer> cycleTime = sgClustering.add(new IntSetting.Builder()
        .name("cycle-time")
        .description("Time in milliseconds between cycling signs.")
        .defaultValue(2000)
        .min(500)
        .max(10000)
        .sliderRange(500, 5000)
        .visible(() -> enableClustering.get() && clusterMode.get() == ClusterMode.Cycle)
        .build()
    );

    private final Setting<Integer> maxClusterDisplay = sgClustering.add(new IntSetting.Builder()
        .name("max-cluster-display")
        .description("Maximum signs to show in a cluster.")
        .defaultValue(5)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .visible(() -> enableClustering.get() && clusterMode.get() != ClusterMode.Count)
        .build()
    );

    private final Setting<Boolean> showClusterCount = sgClustering.add(new BoolSetting.Builder()
        .name("show-cluster-count")
        .description("Show number of signs in cluster.")
        .defaultValue(true)
        .visible(enableClustering::get)
        .build()
    );

    private final Setting<SettingColor> clusterCountColor = sgClustering.add(new ColorSetting.Builder()
        .name("cluster-count-color")
        .description("Color for cluster count indicator.")
        .defaultValue(new SettingColor(255, 200, 100, 255))
        .visible(() -> enableClustering.get() && showClusterCount.get())
        .build()
    );

    private final Setting<Double> stackSpacing = sgClustering.add(new DoubleSetting.Builder()
        .name("stack-spacing")
        .description("Vertical spacing between stacked signs.")
        .defaultValue(5.0)
        .min(0.0)
        .max(20.0)
        .sliderRange(0.0, 20.0)
        .visible(() -> enableClustering.get() && clusterMode.get() == ClusterMode.Stack)
        .build()
    );

    // Optimization settings
    private final Setting<Boolean> cullOffScreen = sgOptimization.add(new BoolSetting.Builder()
        .name("cull-off-screen")
        .description("Don't process signs that are off-screen.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> prioritizeClosest = sgOptimization.add(new BoolSetting.Builder()
        .name("prioritize-closest")
        .description("Always show closest signs first.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cacheSignText = sgOptimization.add(new BoolSetting.Builder()
        .name("cache-sign-text")
        .description("Cache sign text for better performance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> updateInterval = sgOptimization.add(new IntSetting.Builder()
        .name("update-interval")
        .description("Ticks between full sign updates.")
        .defaultValue(20)
        .min(1)
        .max(100)
        .sliderRange(1, 100)
        .visible(cacheSignText::get)
        .build()
    );

    public enum ClusterMode {
        Stack("Stack vertically"),
        Cycle("Cycle through signs"),
        Count("Show count only"),
        Smart("Smart layout");

        private final String description;

        ClusterMode(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    // Unified sign rendering data
    private static class SignRenderData {
        final BlockPos pos;
        final List<String> lines;
        final String fullText;
        final Vec3d worldPos;
        double distance;
        double screenX, screenY;
        boolean onScreen = false;

        // Render dimensions (calculated once per frame)
        double renderWidth;
        double renderHeight;
        double scale;
        Color color;

        SignRenderData(BlockPos pos, List<String> lines, Vec3d worldPos) {
            this.pos = pos;
            this.lines = new ArrayList<>(lines);
            this.fullText = String.join(" ", lines).trim();
            this.worldPos = worldPos;
        }

        void updateScreenPosition(Vector3d tempVec) {
            tempVec.set(worldPos.x, worldPos.y + 0.5, worldPos.z);
            if (NametagUtils.to2D(tempVec, 1.0)) {
                screenX = tempVec.x;
                screenY = tempVec.y;
                onScreen = true;
            } else {
                onScreen = false;
            }
        }
    }

    // Cluster that maintains sign grouping
    private static class SignCluster {
        final List<SignRenderData> signs = new ArrayList<>();
        double centerX, centerY;
        SignRenderData primarySign;
        int cycleIndex = 0;
        long lastCycleTime = 0;

        void addSign(SignRenderData sign) {
            signs.add(sign);
            sign.onScreen = true; // Ensure clustered signs are marked as on-screen
        }

        void calculateCenter() {
            if (signs.isEmpty()) return;

            // Sort by distance to get the closest as primary
            signs.sort(Comparator.comparingDouble(s -> s.distance));
            primarySign = signs.get(0);

            // Use primary sign's position as cluster center
            centerX = primarySign.screenX;
            centerY = primarySign.screenY;
        }

        SignRenderData getCurrentSign(long currentTime, int cycleTimeMs) {
            if (signs.isEmpty()) return null;
            if (signs.size() == 1) return signs.get(0);

            // Initialize lastCycleTime if it's the first time
            if (lastCycleTime == 0) {
                lastCycleTime = currentTime;
            }

            // Cycle based on configurable time
            if (currentTime - lastCycleTime >= cycleTimeMs) {
                cycleIndex = (cycleIndex + 1) % signs.size();
                lastCycleTime = currentTime;
            }

            return signs.get(cycleIndex);
        }
    }

    private final Vector3d tempVec = new Vector3d();
    private final List<SignRenderData> allSigns = new ArrayList<>();
    private final List<SignCluster> clusters = new ArrayList<>();
    private final Map<BlockPos, SignRenderData> signCache = new HashMap<>();
    private int updateTicker = 0;
    private int globalCycleIndex = 0;
    private long lastGlobalCycleTime = 0;

    public SignRender() {
        super(Bep.CATEGORY, "sign-render", "Renders sign text through walls with advanced clustering.");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        updateTicker++;
        boolean fullUpdate = !cacheSignText.get() || updateTicker >= updateInterval.get();

        if (fullUpdate) {
            updateTicker = 0;
            collectSigns();
        } else {
            updateSignPositions();
        }

        if (enableClustering.get() && !allSigns.isEmpty()) {
            createClusters();
        }

        renderSigns();
    }

    private void collectSigns() {
        allSigns.clear();
        signCache.clear();
        Vec3d playerPos = mc.player.getPos();
        double maxDist = maxDistance.get();

        List<SignRenderData> tempSignList = new ArrayList<>();

        for (BlockEntity blockEntity : Utils.blockEntities()) {
            try {
                if (!(blockEntity instanceof SignBlockEntity) &&
                    !(blockEntity instanceof HangingSignBlockEntity)) {
                    continue;
                }

                BlockPos signPos = blockEntity.getPos();
                Vec3d signVec = Vec3d.ofCenter(signPos);
                double distance = playerPos.distanceTo(signVec);

                if (distance > maxDist) continue;

                List<String> lines = extractSignLines(blockEntity);
                if (lines.isEmpty() && filterEmpty.get()) continue;

                SignRenderData signData = new SignRenderData(signPos, lines, signVec);
                signData.distance = distance;

                // Check if on screen
                signData.updateScreenPosition(tempVec);

                if (!signData.onScreen && cullOffScreen.get()) continue;

                // Pre-calculate render properties
                signData.scale = 1.0;
                signData.color = new Color(textColor.get());

                tempSignList.add(signData);
                signCache.put(signPos, signData);
            } catch (Exception ignored) {}
        }

        // Sort by distance if prioritizing closest
        if (prioritizeClosest.get()) {
            tempSignList.sort(Comparator.comparingDouble(s -> s.distance));
        }

        // Limit to max signs
        int limit = Math.min(tempSignList.size(), maxSigns.get());
        for (int i = 0; i < limit; i++) {
            allSigns.add(tempSignList.get(i));
        }
        
        // Reset global cycle index if it's out of bounds
        if (globalCycleIndex >= allSigns.size() && !allSigns.isEmpty()) {
            globalCycleIndex = 0;
        }
    }

    private void updateSignPositions() {
        if (mc.player == null) return;
        Vec3d playerPos = mc.player.getPos();

        Iterator<SignRenderData> iterator = allSigns.iterator();
        while (iterator.hasNext()) {
            SignRenderData sign = iterator.next();
            sign.distance = playerPos.distanceTo(sign.worldPos);

            // Update screen position
            sign.updateScreenPosition(tempVec);

            // Update render properties
            sign.scale = 1.0;
            sign.color = new Color(textColor.get());

            // Remove if now off-screen and culling is enabled
            if (!sign.onScreen && cullOffScreen.get()) {
                iterator.remove();
            }
        }
    }

    private void createClusters() {
        clusters.clear();

        // Reset cluster state for all signs
        for (SignRenderData sign : allSigns) {
            // Signs keep their individual screen positions
        }

        // Create list of signs to cluster
        List<SignRenderData> toCluster = new ArrayList<>(allSigns);
        Set<SignRenderData> clustered = new HashSet<>();

        double radiusSq = clusterRadius.get() * clusterRadius.get();

        while (!toCluster.isEmpty()) {
            SignRenderData seed = toCluster.remove(0);
            if (clustered.contains(seed) || !seed.onScreen) continue;

            SignCluster cluster = new SignCluster();
            cluster.addSign(seed);
            clustered.add(seed);

            // Find all signs within radius
            Iterator<SignRenderData> iter = toCluster.iterator();
            while (iter.hasNext()) {
                SignRenderData other = iter.next();
                if (!other.onScreen || clustered.contains(other)) continue;

                double dx = seed.screenX - other.screenX;
                double dy = seed.screenY - other.screenY;
                double distSq = dx * dx + dy * dy;

                if (distSq <= radiusSq) {
                    cluster.addSign(other);
                    clustered.add(other);
                }
            }

            cluster.calculateCenter();

            // Only add cluster if it has more than one sign
            if (cluster.signs.size() > 1) {
                clusters.add(cluster);
            }
        }
    }

    private void renderSigns() {
        if (allSigns.isEmpty()) return;

        TextRenderer textRenderer = TextRenderer.get();

        if (enableClustering.get() && !clusters.isEmpty()) {
            renderWithClusters(textRenderer);
        } else {
            renderAllSigns(textRenderer);
        }
    }

    private void renderWithClusters(TextRenderer textRenderer) {
        long currentTime = System.currentTimeMillis();
        Set<SignRenderData> rendered = new HashSet<>();

        // Render clusters
        for (SignCluster cluster : clusters) {
            switch (clusterMode.get()) {
                case Stack -> renderStackedCluster(cluster, textRenderer, rendered);
                case Cycle -> renderCyclingCluster(cluster, textRenderer, currentTime, rendered);
                case Count -> renderCountCluster(cluster, textRenderer, rendered);
                case Smart -> renderSmartCluster(cluster, textRenderer, rendered);
            }
        }

        // Render non-clustered signs
        for (SignRenderData sign : allSigns) {
            if (!rendered.contains(sign) && sign.onScreen) {
                renderSignAtPosition(sign, textRenderer, sign.screenX, sign.screenY);
            }
        }
    }

    private void renderAllSigns(TextRenderer textRenderer) {
        // Check if we should use cycle mode even without clustering
        if (enableClustering.get() && clusterMode.get() == ClusterMode.Cycle && !allSigns.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            
            // Initialize cycle time if first run
            if (lastGlobalCycleTime == 0) {
                lastGlobalCycleTime = currentTime;
            }
            
            // Update global cycle index
            if (currentTime - lastGlobalCycleTime >= cycleTime.get()) {
                globalCycleIndex = (globalCycleIndex + 1) % allSigns.size();
                lastGlobalCycleTime = currentTime;
            }
            
            // Ensure index is valid
            if (globalCycleIndex >= allSigns.size()) {
                globalCycleIndex = 0;
            }
            
            // Only render the current sign in the cycle
            SignRenderData currentSign = allSigns.get(globalCycleIndex);
            if (currentSign.onScreen) {
                renderSignAtPosition(currentSign, textRenderer, currentSign.screenX, currentSign.screenY);
                
                // Show cycle indicator
                if (showClusterCount.get() && allSigns.size() > 1) {
                    double signHeight = calculateSignHeight(currentSign, textRenderer);
                    String indicator = String.format("[%d/%d]",
                        globalCycleIndex + 1, allSigns.size());
                    renderTextWithBackground(
                        indicator,
                        currentSign.screenX,
                        currentSign.screenY + signHeight / 2 + 15,
                        0.7,
                        clusterCountColor.get(),
                        textRenderer
                    );
                }
            }
        } else {
            // Normal rendering - show all signs
            for (SignRenderData sign : allSigns) {
                if (sign.onScreen) {
                    renderSignAtPosition(sign, textRenderer, sign.screenX, sign.screenY);
                }
            }
        }
    }

    private void renderStackedCluster(SignCluster cluster, TextRenderer textRenderer, Set<SignRenderData> rendered) {
        double baseX = cluster.centerX;
        double baseY = cluster.centerY;
        double offsetY = 0;
        int count = 0;

        for (SignRenderData sign : cluster.signs) {
            if (count >= maxClusterDisplay.get()) break;

            renderSignAtPosition(sign, textRenderer, baseX, baseY + offsetY);
            rendered.add(sign);

            // Calculate height of this sign for next offset
            double signHeight = calculateSignHeight(sign, textRenderer);
            offsetY += signHeight + stackSpacing.get();
            count++;
        }

        // Show cluster count if there are hidden signs
        if (showClusterCount.get() && cluster.signs.size() > maxClusterDisplay.get()) {
            String countText = "+" + (cluster.signs.size() - maxClusterDisplay.get()) + " more";
            renderTextWithBackground(
                countText,
                baseX,
                baseY + offsetY,
                0.8,
                clusterCountColor.get(),
                textRenderer
            );
        }
    }

    private void renderCyclingCluster(SignCluster cluster, TextRenderer textRenderer, long currentTime, Set<SignRenderData> rendered) {
        SignRenderData currentSign = cluster.getCurrentSign(currentTime, cycleTime.get());
        if (currentSign != null) {
            renderSignAtPosition(currentSign, textRenderer, cluster.centerX, cluster.centerY);
            rendered.addAll(cluster.signs); // Mark all as rendered since we're cycling

            // Show cluster indicator
            if (showClusterCount.get() && cluster.signs.size() > 1) {
                double signHeight = calculateSignHeight(currentSign, textRenderer);
                String indicator = String.format("[%d/%d]",
                    cluster.cycleIndex + 1, cluster.signs.size());
                renderTextWithBackground(
                    indicator,
                    cluster.centerX,
                    cluster.centerY + signHeight / 2 + 15,
                    0.7,
                    clusterCountColor.get(),
                    textRenderer
                );
            }
        }
    }

    private void renderCountCluster(SignCluster cluster, TextRenderer textRenderer, Set<SignRenderData> rendered) {
        // Show primary sign
        SignRenderData primary = cluster.primarySign;
        renderSignAtPosition(primary, textRenderer, cluster.centerX, cluster.centerY);
        rendered.addAll(cluster.signs);

        // Show count
        if (cluster.signs.size() > 1) {
            double signHeight = calculateSignHeight(primary, textRenderer);
            String countText = "(" + cluster.signs.size() + " signs)";
            renderTextWithBackground(
                countText,
                cluster.centerX,
                cluster.centerY + signHeight / 2 + 10,
                0.8,
                clusterCountColor.get(),
                textRenderer
            );
        }
    }

    private void renderSmartCluster(SignCluster cluster, TextRenderer textRenderer, Set<SignRenderData> rendered) {
        int displayCount = Math.min(cluster.signs.size(), maxClusterDisplay.get());

        if (displayCount == 1) {
            renderSignAtPosition(cluster.signs.get(0), textRenderer, cluster.centerX, cluster.centerY);
            rendered.add(cluster.signs.get(0));
        } else {
            // Arrange in a circle around center
            double radius = 30.0 + (displayCount * 5.0);
            double angleStep = 2 * Math.PI / displayCount;

            for (int i = 0; i < displayCount; i++) {
                SignRenderData sign = cluster.signs.get(i);
                double angle = i * angleStep - Math.PI / 2; // Start from top
                double offsetX = Math.cos(angle) * radius;
                double offsetY = Math.sin(angle) * radius;

                renderSignAtPosition(sign, textRenderer,
                    cluster.centerX + offsetX,
                    cluster.centerY + offsetY);
                rendered.add(sign);
            }

            // Show remaining count in center if needed
            if (showClusterCount.get() && cluster.signs.size() > displayCount) {
                String countText = "+" + (cluster.signs.size() - displayCount);
                renderTextWithBackground(
                    countText,
                    cluster.centerX,
                    cluster.centerY,
                    0.9,
                    clusterCountColor.get(),
                    textRenderer
                );
            }
        }
    }

    private void renderSignAtPosition(SignRenderData sign, TextRenderer textRenderer, double centerX, double centerY) {
        // Render the main sign content at the exact center position
        if (multilineDisplay.get() && !sign.lines.isEmpty()) {
            renderMultilineSign(sign, textRenderer, centerX, centerY);
        } else if (!sign.fullText.isEmpty()) {
            renderSingleLineSign(sign, textRenderer, centerX, centerY);
        }

    }

    private void renderMultilineSign(SignRenderData sign, TextRenderer textRenderer, double centerX, double centerY) {
        // Pre-calculate all dimensions first
        textRenderer.begin(sign.scale);
        double lineHeight = textRenderer.getHeight(true);
        List<Double> lineWidths = new ArrayList<>();
        double maxWidth = 0;

        for (String line : sign.lines) {
            double width = line.isEmpty() ? 0 : textRenderer.getWidth(line, true);
            lineWidths.add(width);
            maxWidth = Math.max(maxWidth, width);
        }
        textRenderer.end();

        // Calculate ALL positions before ANY rendering
        double scaledWidth = maxWidth * sign.scale;
        double scaledLineHeight = lineHeight * sign.scale;
        double totalHeight = sign.lines.size() * scaledLineHeight;
        double bgPadding = 4;
        double bgWidth = scaledWidth + bgPadding * 2;
        double bgHeight = totalHeight + bgPadding * 2;

        // Calculate final positions
        double bgLeft = centerX - bgWidth / 2;
        double bgTop = centerY - bgHeight / 2;
        double bgCenterX = bgLeft + bgWidth / 2;

        // Pre-calculate all text positions
        List<Double> textXPositions = new ArrayList<>();
        List<Double> textYPositions = new ArrayList<>();
        for (int i = 0; i < sign.lines.size(); i++) {
            double lineWidth = lineWidths.get(i);
            double textX = (bgCenterX - lineWidth * sign.scale / 2) / sign.scale;
            double textY = (bgTop + bgPadding + i * scaledLineHeight) / sign.scale;
            textXPositions.add(textX);
            textYPositions.add(textY);
        }

        // Render background if enabled
        if (showBackground.get()) {
            Color bgColor = new Color(
                backgroundColor.get().r,
                backgroundColor.get().g,
                backgroundColor.get().b,
                (int)(backgroundColor.get().a * (sign.color.a / 255.0))
            );

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(bgLeft, bgTop, bgWidth, bgHeight, bgColor);
            Renderer2D.COLOR.render(null);
        }

        // Render all text at pre-calculated positions
        textRenderer.begin(sign.scale);
        for (int i = 0; i < sign.lines.size(); i++) {
            String line = sign.lines.get(i);
            if (!line.isEmpty()) {
                textRenderer.render(line, textXPositions.get(i), textYPositions.get(i), sign.color, true);
            }
        }
        textRenderer.end();
    }

    private void renderSingleLineSign(SignRenderData sign, TextRenderer textRenderer, double centerX, double centerY) {
        renderTextWithBackground(sign.fullText, centerX, centerY, sign.scale, sign.color, textRenderer);
    }

    private void renderTextWithBackground(String text, double centerX, double centerY, double scale, Color color, TextRenderer textRenderer) {
        // Pre-calculate text dimensions ONCE
        textRenderer.begin(scale);
        double textWidth = textRenderer.getWidth(text, true);
        double textHeight = textRenderer.getHeight(true);
        textRenderer.end();

        // Calculate final dimensions for the whole element
        double bgPadding = 4;
        double elementWidth = textWidth * scale + bgPadding * 2;
        double elementHeight = textHeight * scale + bgPadding * 2;

        // Calculate the top-left corner from center
        double elementLeft = centerX - elementWidth / 2;
        double elementTop = centerY - elementHeight / 2;

        // Render background at calculated position
        if (showBackground.get()) {
            Color bgColor = new Color(
                backgroundColor.get().r,
                backgroundColor.get().g,
                backgroundColor.get().b,
                (int)(backgroundColor.get().a * (color.a / 255.0))
            );

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(elementLeft, elementTop, elementWidth, elementHeight, bgColor);
            Renderer2D.COLOR.render(null);
        }

        // Render text at the EXACT same element position plus padding
        textRenderer.begin(scale);
        // Text position in scaled coordinates
        double textX = elementLeft + bgPadding;
        double textY = elementTop + bgPadding;
        textRenderer.render(text, textX, textY, color, true);
        textRenderer.end();
    }

    private double calculateSignHeight(SignRenderData sign, TextRenderer textRenderer) {
        textRenderer.begin(sign.scale);
        double lineHeight = textRenderer.getHeight(true);
        textRenderer.end();

        if (multilineDisplay.get() && !sign.lines.isEmpty()) {
            return sign.lines.size() * lineHeight * sign.scale + 8;
        } else {
            return lineHeight * sign.scale + 8;
        }
    }

    private List<String> extractSignLines(BlockEntity blockEntity) {
        List<String> lines = new ArrayList<>();

        try {
            SignText frontText = null;
            SignText backText = null;

            if (blockEntity instanceof SignBlockEntity sign) {
                frontText = sign.getFrontText();
                backText = sign.getBackText();
            } else if (blockEntity instanceof HangingSignBlockEntity sign) {
                frontText = sign.getFrontText();
                backText = sign.getBackText();
            }

            // Prioritize front text
            if (frontText != null) {
                List<String> frontLines = extractTextLines(frontText);
                if (!frontLines.isEmpty()) {
                    lines.addAll(frontLines);
                }
            }

            // Use back text if front is empty
            if (backText != null && lines.isEmpty()) {
                List<String> backLines = extractTextLines(backText);
                if (!backLines.isEmpty()) {
                    lines.addAll(backLines);
                }
            }
        } catch (Exception ignored) {}

        return lines;
    }

    private List<String> extractTextLines(SignText signText) {
        List<String> lines = new ArrayList<>();

        try {
            Text[] messages = signText.getMessages(false);
            if (messages != null) {
                for (Text message : messages) {
                    if (message == null) continue;

                    String line = safeExtractString(message);
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }
            }
        } catch (Exception ignored) {}

        return lines;
    }

    private String safeExtractString(Text text) {
        if (text == null) return "";

        try {
            String result = text.getString();
            if (result == null) return "";

            return cleanSignText(result);
        } catch (Exception e) {
            try {
                String literal = text.getLiteralString();
                if (literal != null) {
                    return cleanSignText(literal);
                }
            } catch (Exception ignored) {}

            return "";
        }
    }

    private String cleanSignText(String text) {
        if (text == null || text.isEmpty()) return "";

        // Remove formatting codes
        text = text.replaceAll("§.", "");
        text = text.replaceAll("&[0-9a-fklmnor]", "");

        // Remove JSON formatting
        if (text.contains("{\"") || text.contains("[\"")) {
            text = text.replaceAll("\\{\".*?\":\"(.*?)\".*?\\}", "$1");
            text = text.replaceAll("\\[\"(.*?)\"\\]", "$1");
        }

        // Remove other brackets
        text = text.replaceAll("\\{[^\\s].*?\\}", "");

        // Clean up control characters
        text = text.replaceAll("[\\p{C}&&[^\\s]]", "");
        text = text.replaceAll("[\\u0000-\\u001F\\u007F-\\u009F]", "");

        // Remove extra brackets and quotes
        text = text.replaceAll("[\\[\\]{}\"']", "");

        // Normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();

        // Limit length
        if (text.length() > 100) {
            text = text.substring(0, 97) + "...";
        }

        return text;
    }

}

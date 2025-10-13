package bep.hax.hud;
import bep.hax.Bep;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
public class MobRateHud extends HudElement {
    public static final HudElementInfo<MobRateHud> INFO = new HudElementInfo<>(
        Bep.HUD_GROUP,
        "MobRate",
        "Mob farm performance tracker with graphs.",
        MobRateHud::new
    );
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMetrics = settings.createGroup("Metrics");
    private final SettingGroup sgDisplay = settings.createGroup("Display");
    private final SettingGroup sgGraphs = settings.createGroup("Graphs");
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final Setting<Boolean> resetOnDimension = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-on-dimension")
        .description("Reset stats when changing dimensions.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> trackHostile = sgGeneral.add(new BoolSetting.Builder()
        .name("track-hostile")
        .description("Track hostile mobs.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> trackPassive = sgGeneral.add(new BoolSetting.Builder()
        .name("track-passive")
        .description("Track passive mobs.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> trackNeutral = sgGeneral.add(new BoolSetting.Builder()
        .name("track-neutral")
        .description("Track neutral mobs.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showTitle = sgMetrics.add(new BoolSetting.Builder()
        .name("show-title")
        .description("Show the title 'Mob Farm Stats'.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showSpawnRate = sgMetrics.add(new BoolSetting.Builder()
        .name("show-spawn-rate")
        .description("Show spawn rate per hour.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showMobCap = sgMetrics.add(new BoolSetting.Builder()
        .name("show-mob-cap")
        .description("Show mob cap usage.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showMobCapBar = sgMetrics.add(new BoolSetting.Builder()
        .name("show-mob-cap-bar")
        .description("Show mob cap visual bar.")
        .defaultValue(true)
        .visible(showMobCap::get)
        .build()
    );
    private final Setting<Boolean> showMobCount = sgMetrics.add(new BoolSetting.Builder()
        .name("show-mob-count")
        .description("Show current mob count in cap display.")
        .defaultValue(true)
        .visible(showMobCap::get)
        .build()
    );
    private final Setting<Boolean> showEfficiency = sgMetrics.add(new BoolSetting.Builder()
        .name("show-efficiency")
        .description("Show farm efficiency percentage.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showCategories = sgMetrics.add(new BoolSetting.Builder()
        .name("show-categories")
        .description("Show hostile/passive/neutral breakdown.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showCategoryPercentages = sgMetrics.add(new BoolSetting.Builder()
        .name("show-category-percentages")
        .description("Show percentages in category breakdown.")
        .defaultValue(true)
        .visible(showCategories::get)
        .build()
    );
    private final Setting<Boolean> showPeakRate = sgMetrics.add(new BoolSetting.Builder()
        .name("show-peak-rate")
        .description("Show peak spawn rate.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showTotalSpawned = sgMetrics.add(new BoolSetting.Builder()
        .name("show-total-spawned")
        .description("Show total mobs spawned.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showSessionTime = sgMetrics.add(new BoolSetting.Builder()
        .name("show-session-time")
        .description("Show session duration.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> showTopMobs = sgMetrics.add(new BoolSetting.Builder()
        .name("show-top-mobs")
        .description("Show top spawned mob types.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> topMobsCount = sgMetrics.add(new IntSetting.Builder()
        .name("top-mobs-count")
        .description("Number of top mobs to show.")
        .defaultValue(3)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .visible(showTopMobs::get)
        .build()
    );
    private final Setting<DisplayMode> displayMode = sgDisplay.add(new EnumSetting.Builder<DisplayMode>()
        .name("display-mode")
        .description("Information detail level.")
        .defaultValue(DisplayMode.GRAPHS)
        .build()
    );
    private final Setting<Double> textScale = sgDisplay.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Text size multiplier.")
        .defaultValue(1.0)
        .min(0.5)
        .max(2.0)
        .sliderRange(0.5, 2.0)
        .build()
    );
    private final Setting<Boolean> textShadow = sgDisplay.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Add text shadow for readability.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> lineSpacing = sgDisplay.add(new DoubleSetting.Builder()
        .name("line-spacing")
        .description("Spacing between text lines.")
        .defaultValue(2.0)
        .min(0.0)
        .max(10.0)
        .sliderRange(0.0, 10.0)
        .build()
    );
    private final Setting<Boolean> compactNumbers = sgDisplay.add(new BoolSetting.Builder()
        .name("compact-numbers")
        .description("Use compact number format (1.2k instead of 1200).")
        .defaultValue(false)
        .build()
    );
    private final Setting<GraphType> primaryGraph = sgGraphs.add(new EnumSetting.Builder<GraphType>()
        .name("primary-graph")
        .description("Primary graph to display.")
        .defaultValue(GraphType.Y_LEVEL_BAR)
        .build()
    );
    private final Setting<GraphType> secondaryGraph = sgGraphs.add(new EnumSetting.Builder<GraphType>()
        .name("secondary-graph")
        .description("Secondary graph to display.")
        .defaultValue(GraphType.RATE_TREND)
        .build()
    );
    private final Setting<Boolean> showBothGraphs = sgGraphs.add(new BoolSetting.Builder()
        .name("show-both-graphs")
        .description("Show both primary and secondary graphs.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> graphHeight = sgGraphs.add(new IntSetting.Builder()
        .name("graph-height")
        .description("Height of graphs in pixels.")
        .defaultValue(80)
        .min(40)
        .max(200)
        .sliderRange(40, 200)
        .build()
    );
    private final Setting<Integer> graphWidth = sgGraphs.add(new IntSetting.Builder()
        .name("graph-width")
        .description("Width of graphs in pixels.")
        .defaultValue(200)
        .min(100)
        .max(400)
        .sliderRange(100, 400)
        .build()
    );
    private final Setting<Boolean> showGrid = sgGraphs.add(new BoolSetting.Builder()
        .name("show-grid")
        .description("Show grid lines on graphs.")
        .defaultValue(true)
        .build()
    );
    private final Setting<SettingColor> titleColor = sgColors.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Title text color.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );
    private final Setting<SettingColor> rateColor = sgColors.add(new ColorSetting.Builder()
        .name("rate-color")
        .description("Spawn rate text color.")
        .defaultValue(new SettingColor(100, 255, 100))
        .build()
    );
    private final Setting<SettingColor> hostileColor = sgColors.add(new ColorSetting.Builder()
        .name("hostile-color")
        .description("Hostile mob color.")
        .defaultValue(new SettingColor(255, 100, 100))
        .build()
    );
    private final Setting<SettingColor> passiveColor = sgColors.add(new ColorSetting.Builder()
        .name("passive-color")
        .description("Passive mob color.")
        .defaultValue(new SettingColor(100, 200, 255))
        .build()
    );
    private final Setting<SettingColor> neutralColor = sgColors.add(new ColorSetting.Builder()
        .name("neutral-color")
        .description("Neutral mob color.")
        .defaultValue(new SettingColor(255, 255, 100))
        .build()
    );
    private final Setting<SettingColor> graphBgColor = sgColors.add(new ColorSetting.Builder()
        .name("graph-background")
        .description("Graph background color.")
        .defaultValue(new SettingColor(30, 30, 30, 150))
        .build()
    );
    private final Setting<SettingColor> graphGridColor = sgColors.add(new ColorSetting.Builder()
        .name("graph-grid")
        .description("Graph grid color.")
        .defaultValue(new SettingColor(60, 60, 60, 100))
        .build()
    );
    public enum DisplayMode {
        COMPACT, NORMAL, DETAILED, GRAPHS
    }
    public enum GraphType {
        Y_LEVEL_BAR("Y-Level Distribution"),
        RATE_TREND("Rate Trend"),
        MOB_PIE("Mob Types"),
        MOB_DONUT("Categories"),
        SPAWN_HEATMAP("Spawn Heatmap"),
        SCATTER_3D("3D Scatter"),
        EFFICIENCY_GAUGE("Efficiency"),
        CAP_RADIAL("Mob Cap"),
        COMPARISON_BARS("Category Bars");
        private final String name;
        GraphType(String name) { this.name = name; }
        @Override
        public String toString() { return name; }
    }
    private final Set<UUID> trackedMobs = Collections.synchronizedSet(new HashSet<>());
    private final Map<EntityType<?>, Integer> mobTypeCounts = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> yLevelDistribution = new ConcurrentHashMap<>();
    private final LinkedList<Double> rateHistory = new LinkedList<>();
    private final Map<String, Integer> chunkSpawnCounts = new ConcurrentHashMap<>();
    private final Set<UUID> aliveMobs = Collections.synchronizedSet(new HashSet<>());
    private long sessionStartTime = System.currentTimeMillis();
    private int totalSpawned = 0;
    private int hostileCount = 0;
    private int passiveCount = 0;
    private int neutralCount = 0;
    private double currentSpawnRate = 0;
    private double peakSpawnRate = 0;
    private double rotationAngle = 0;
    private long lastRateUpdate = 0;
    private static final long RATE_UPDATE_INTERVAL = 5000;
    private String lastDimension = "";
    public MobRateHud() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @Override
    public void render(HudRenderer renderer) {
        if (MeteorClient.mc.world == null || MeteorClient.mc.player == null) {
            renderPlaceholder(renderer);
            return;
        }
        updateMetrics();
        double y = this.y;
        double maxWidth = graphWidth.get();
        double lineHeight = renderer.textHeight(textShadow.get(), textScale.get());
        double spacing = lineSpacing.get();
        if (showTitle.get()) {
            String title = "Mob Farm Stats";
            renderer.text(title, x, y, titleColor.get(), textShadow.get(), textScale.get());
            y += lineHeight + spacing * 2;
        }
        if (displayMode.get() != DisplayMode.COMPACT || showSpawnRate.get() || showMobCap.get() || showEfficiency.get()) {
            y = renderMainMetrics(renderer, y, lineHeight, spacing);
        }
        if (showCategories.get() && displayMode.get() != DisplayMode.COMPACT) {
            y = renderCategoryBreakdown(renderer, y, lineHeight, spacing);
        }
        if (displayMode.get() == DisplayMode.DETAILED || displayMode.get() == DisplayMode.GRAPHS) {
            y = renderDetailedStats(renderer, y, lineHeight, spacing);
        }
        if (displayMode.get() == DisplayMode.GRAPHS) {
            y += spacing * 3;
            y = renderGraphs(renderer, y, lineHeight, spacing);
        }
        setSize(maxWidth, y - this.y);
    }
    private double renderMainMetrics(HudRenderer renderer, double y, double lineHeight, double spacing) {
        if (showSpawnRate.get()) {
            String rateText;
            if (compactNumbers.get() && currentSpawnRate >= 1000) {
                rateText = String.format("Rate: %.1fk/hr", currentSpawnRate / 1000);
            } else {
                rateText = String.format("Rate: %.0f/hr", currentSpawnRate);
            }
            renderer.text(rateText, x, y, getColorForRate(currentSpawnRate), textShadow.get(), textScale.get());
            y += lineHeight + spacing;
        }
        if (showMobCap.get()) {
            int mobCapUsage = calculateMobCapUsage();
            String capText;
            if (showMobCount.get()) {
                capText = String.format("Cap: %d%% (%d mobs)", mobCapUsage, aliveMobs.size());
            } else {
                capText = String.format("Cap: %d%%", mobCapUsage);
            }
            SettingColor capColor = mobCapUsage > 80 ? hostileColor.get() : 
                                    mobCapUsage > 50 ? neutralColor.get() : passiveColor.get();
            renderer.text(capText, x, y, capColor, textShadow.get(), textScale.get());
            if (showMobCapBar.get()) {
                double barX = x + renderer.textWidth(capText, textShadow.get(), textScale.get()) + spacing * 2;
                double barWidth = 60;
                double barHeight = lineHeight * 0.6;
                renderer.quad(barX, y + (lineHeight - barHeight) / 2, barWidth, barHeight, graphBgColor.get());
                renderer.quad(barX, y + (lineHeight - barHeight) / 2, barWidth * (mobCapUsage / 100.0), barHeight, capColor);
            }
            y += lineHeight + spacing;
        }
        if (showEfficiency.get()) {
            double efficiency = calculateEfficiency();
            String effText = String.format("Efficiency: %.0f%%", efficiency);
            SettingColor effColor = efficiency > 75 ? passiveColor.get() :
                                   efficiency > 50 ? neutralColor.get() : hostileColor.get();
            renderer.text(effText, x, y, effColor, textShadow.get(), textScale.get());
            y += lineHeight + spacing;
        }
        return y;
    }
    private double renderCategoryBreakdown(HudRenderer renderer, double y, double lineHeight, double spacing) {
        if (totalSpawned == 0) return y;
        y += spacing;
        if (hostileCount > 0 && trackHostile.get()) {
            String text;
            if (showCategoryPercentages.get()) {
                double percent = (hostileCount * 100.0 / totalSpawned);
                text = compactNumbers.get() && hostileCount >= 1000 ? 
                    String.format("Hostile: %.1fk (%.0f%%)", hostileCount / 1000.0, percent) :
                    String.format("Hostile: %d (%.0f%%)", hostileCount, percent);
            } else {
                text = compactNumbers.get() && hostileCount >= 1000 ?
                    String.format("Hostile: %.1fk", hostileCount / 1000.0) :
                    String.format("Hostile: %d", hostileCount);
            }
            renderer.text(text, x, y, hostileColor.get(), textShadow.get(), textScale.get() * 0.9);
            y += lineHeight * 0.9 + spacing;
        }
        if (passiveCount > 0 && trackPassive.get()) {
            String text;
            if (showCategoryPercentages.get()) {
                double percent = (passiveCount * 100.0 / totalSpawned);
                text = compactNumbers.get() && passiveCount >= 1000 ?
                    String.format("Passive: %.1fk (%.0f%%)", passiveCount / 1000.0, percent) :
                    String.format("Passive: %d (%.0f%%)", passiveCount, percent);
            } else {
                text = compactNumbers.get() && passiveCount >= 1000 ?
                    String.format("Passive: %.1fk", passiveCount / 1000.0) :
                    String.format("Passive: %d", passiveCount);
            }
            renderer.text(text, x, y, passiveColor.get(), textShadow.get(), textScale.get() * 0.9);
            y += lineHeight * 0.9 + spacing;
        }
        if (neutralCount > 0 && trackNeutral.get()) {
            String text;
            if (showCategoryPercentages.get()) {
                double percent = (neutralCount * 100.0 / totalSpawned);
                text = compactNumbers.get() && neutralCount >= 1000 ?
                    String.format("Neutral: %.1fk (%.0f%%)", neutralCount / 1000.0, percent) :
                    String.format("Neutral: %d (%.0f%%)", neutralCount, percent);
            } else {
                text = compactNumbers.get() && neutralCount >= 1000 ?
                    String.format("Neutral: %.1fk", neutralCount / 1000.0) :
                    String.format("Neutral: %d", neutralCount);
            }
            renderer.text(text, x, y, neutralColor.get(), textShadow.get(), textScale.get() * 0.9);
            y += lineHeight * 0.9 + spacing;
        }
        return y;
    }
    private double renderDetailedStats(HudRenderer renderer, double y, double lineHeight, double spacing) {
        y += spacing;
        if (showPeakRate.get()) {
            String peakText;
            if (compactNumbers.get() && peakSpawnRate >= 1000) {
                peakText = String.format("Peak: %.1fk/hr", peakSpawnRate / 1000);
            } else {
                peakText = String.format("Peak: %.0f/hr", peakSpawnRate);
            }
            renderer.text(peakText, x, y, rateColor.get(), textShadow.get(), textScale.get() * 0.85);
            y += lineHeight * 0.85 + spacing;
        }
        if (showTotalSpawned.get()) {
            String totalText;
            if (compactNumbers.get() && totalSpawned >= 1000) {
                totalText = String.format("Total: %.1fk", totalSpawned / 1000.0);
            } else {
                totalText = String.format("Total: %d", totalSpawned);
            }
            renderer.text(totalText, x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.85);
            y += lineHeight * 0.85 + spacing;
        }
        if (showSessionTime.get()) {
            long sessionTime = System.currentTimeMillis() - sessionStartTime;
            long minutes = sessionTime / 60000;
            String timeText;
            if (minutes >= 60) {
                timeText = String.format("Session: %dh %dm", minutes / 60, minutes % 60);
            } else {
                timeText = String.format("Session: %d min", minutes);
            }
            renderer.text(timeText, x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.85);
            y += lineHeight * 0.85 + spacing;
        }
        if (showTopMobs.get() && !mobTypeCounts.isEmpty()) {
            y += spacing;
            renderer.text("Top Mobs:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.85);
            y += lineHeight * 0.85 + spacing;
            List<Map.Entry<EntityType<?>, Integer>> topMobs = mobTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<EntityType<?>, Integer>comparingByValue().reversed())
                .limit(topMobsCount.get())
                .collect(Collectors.toList());
            for (Map.Entry<EntityType<?>, Integer> entry : topMobs) {
                String mobName = entry.getKey().getName().getString();
                if (mobName.length() > 15) mobName = mobName.substring(0, 15);
                String countText;
                if (compactNumbers.get() && entry.getValue() >= 1000) {
                    countText = String.format("  %s: %.1fk", mobName, entry.getValue() / 1000.0);
                } else {
                    countText = String.format("  %s: %d", mobName, entry.getValue());
                }
                renderer.text(countText, x, y, rateColor.get(), textShadow.get(), textScale.get() * 0.8);
                y += lineHeight * 0.8 + spacing;
            }
        }
        return y;
    }
    private double renderGraphs(HudRenderer renderer, double y, double lineHeight, double spacing) {
        y = renderSingleGraph(renderer, primaryGraph.get(), y, lineHeight, spacing);
        if (showBothGraphs.get() && secondaryGraph.get() != primaryGraph.get()) {
            y += spacing * 3;
            y = renderSingleGraph(renderer, secondaryGraph.get(), y, lineHeight, spacing);
        }
        return y;
    }
    private double renderSingleGraph(HudRenderer renderer, GraphType graphType, double y, double lineHeight, double spacing) {
        switch (graphType) {
            case Y_LEVEL_BAR:
                return renderYLevelGraph(renderer, y, lineHeight, spacing);
            case RATE_TREND:
                return renderRateTrendGraph(renderer, y, lineHeight, spacing);
            case MOB_PIE:
                return renderMobPieChart(renderer, y, lineHeight, spacing);
            case MOB_DONUT:
                return renderMobDonutChart(renderer, y, lineHeight, spacing);
            case SPAWN_HEATMAP:
                return renderSpawnHeatmap(renderer, y, lineHeight, spacing);
            case SCATTER_3D:
                return render3DScatterPlot(renderer, y, lineHeight, spacing);
            case EFFICIENCY_GAUGE:
                return renderEfficiencyGauge(renderer, y, lineHeight, spacing);
            case CAP_RADIAL:
                return renderCapRadialChart(renderer, y, lineHeight, spacing);
            case COMPARISON_BARS:
                return renderComparisonBars(renderer, y, lineHeight, spacing);
            default:
                return y;
        }
    }
    private double renderYLevelGraph(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Y-Level Distribution:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double graphX = x;
        double graphY = y;
        double width = graphWidth.get();
        double height = graphHeight.get();
        renderer.quad(graphX, graphY, width, height, graphBgColor.get());
        if (yLevelDistribution.isEmpty()) {
            renderer.text("No data", graphX + width/2 - 20, graphY + height/2 - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return graphY + height + spacing * 2;
        }
        int minY = yLevelDistribution.keySet().stream().min(Integer::compare).orElse(-64);
        int maxY = yLevelDistribution.keySet().stream().max(Integer::compare).orElse(320);
        int maxCount = yLevelDistribution.values().stream().max(Integer::compare).orElse(1);
        for (Map.Entry<Integer, Integer> entry : yLevelDistribution.entrySet()) {
            int yLevel = entry.getKey();
            int count = entry.getValue();
            double barX = graphX + ((yLevel - minY) / (double)(maxY - minY + 1)) * width;
            double barHeight = (count / (double) maxCount) * height * 0.9;
            double barWidth = Math.max(1, width / (maxY - minY + 1));
            SettingColor barColor = yLevel < 0 ? hostileColor.get() :
                                  yLevel < 64 ? neutralColor.get() : passiveColor.get();
            renderer.quad(barX, graphY + height - barHeight, barWidth, barHeight, barColor);
        }
        renderer.text("Y" + minY, graphX, graphY + height + 2, titleColor.get(), textShadow.get(), textScale.get() * 0.7);
        renderer.text("Y" + maxY, graphX + width - 30, graphY + height + 2, titleColor.get(), textShadow.get(), textScale.get() * 0.7);
        return graphY + height + lineHeight + spacing * 2;
    }
    private double renderRateTrendGraph(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Spawn Rate Trend:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double graphX = x;
        double graphY = y;
        double width = graphWidth.get();
        double height = graphHeight.get();
        renderer.quad(graphX, graphY, width, height, graphBgColor.get());
        if (rateHistory.size() < 2) {
            renderer.text("Collecting data...", graphX + width/2 - 40, graphY + height/2 - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return graphY + height + spacing * 2;
        }
        double maxRate = Math.max(1, rateHistory.stream().max(Double::compare).orElse(1.0));
        if (showGrid.get()) {
            for (int i = 1; i <= 4; i++) {
                double gridY = graphY + height - (height * i / 4);
                renderer.quad(graphX, gridY, width, 0.5, graphGridColor.get());
                String label = String.format("%.0f", maxRate * i / 4);
                renderer.text(label, graphX - 25, gridY - lineHeight * 0.3, 
                             titleColor.get(), textShadow.get(), textScale.get() * 0.6);
            }
        }
        for (int i = 0; i < rateHistory.size() - 1; i++) {
            double rate1 = rateHistory.get(i);
            double rate2 = rateHistory.get(i + 1);
            double x1 = graphX + (i / (double)(rateHistory.size() - 1)) * width;
            double x2 = graphX + ((i + 1) / (double)(rateHistory.size() - 1)) * width;
            double y1 = graphY + height - (rate1 / maxRate) * height;
            double y2 = graphY + height - (rate2 / maxRate) * height;
            drawLine(renderer, x1, y1, x2, y2, rateColor.get(), 2);
            renderer.quad(x1 - 2, y1 - 2, 4, 4, rateColor.get());
        }
        String currentText = String.format("%.0f/hr", currentSpawnRate);
        renderer.text(currentText, graphX + width - 40, graphY - lineHeight * 0.7, 
                     rateColor.get(), textShadow.get(), textScale.get() * 0.7);
        return graphY + height + spacing * 2;
    }
    private double renderMobPieChart(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Mob Distribution:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double centerX = x + graphWidth.get() / 2.0;
        double centerY = y + graphHeight.get() / 2.0;
        double radius = Math.min(graphWidth.get(), graphHeight.get()) / 2.0 - 10;
        if (mobTypeCounts.isEmpty()) {
            renderer.text("No data", centerX - 20, centerY - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return y + graphHeight.get() + spacing * 2;
        }
        List<Map.Entry<EntityType<?>, Integer>> sortedMobs = mobTypeCounts.entrySet().stream()
            .sorted(Map.Entry.<EntityType<?>, Integer>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());
        int total = sortedMobs.stream().mapToInt(Map.Entry::getValue).sum();
        double currentAngle = -90;
        for (int i = 0; i < sortedMobs.size(); i++) {
            Map.Entry<EntityType<?>, Integer> entry = sortedMobs.get(i);
            double percentage = entry.getValue() / (double) total;
            double sweepAngle = percentage * 360;
            SettingColor color = getColorForIndex(i);
            drawPieSlice(renderer, centerX, centerY, radius, currentAngle, sweepAngle, color);
            double labelAngle = Math.toRadians(currentAngle + sweepAngle / 2);
            double labelX = centerX + Math.cos(labelAngle) * radius * 0.7;
            double labelY = centerY + Math.sin(labelAngle) * radius * 0.7;
            String label = String.format("%.0f%%", percentage * 100);
            renderer.text(label, labelX - 10, labelY - lineHeight * 0.35, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.7);
            currentAngle += sweepAngle;
        }
        return y + graphHeight.get() + spacing * 2;
    }
    private double renderMobDonutChart(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Category Distribution:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double centerX = x + graphWidth.get() / 2.0;
        double centerY = y + graphHeight.get() / 2.0;
        double outerRadius = Math.min(graphWidth.get(), graphHeight.get()) / 2.0 - 10;
        double innerRadius = outerRadius * 0.5;
        if (totalSpawned == 0) {
            renderer.text("No data", centerX - 20, centerY - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return y + graphHeight.get() + spacing * 2;
        }
        double[] values = {hostileCount, passiveCount, neutralCount};
        SettingColor[] colors = {hostileColor.get(), passiveColor.get(), neutralColor.get()};
        double currentAngle = -90;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;
            double percentage = values[i] / totalSpawned;
            double sweepAngle = percentage * 360;
            drawDonutSlice(renderer, centerX, centerY, innerRadius, outerRadius, currentAngle, sweepAngle, colors[i]);
            currentAngle += sweepAngle;
        }
        String centerText = String.valueOf(totalSpawned);
        renderer.text(centerText, centerX - renderer.textWidth(centerText, textShadow.get(), textScale.get()) / 2,
                     centerY - lineHeight / 2, titleColor.get(), textShadow.get(), textScale.get());
        return y + graphHeight.get() + spacing * 2;
    }
    private double renderSpawnHeatmap(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Spawn Heatmap:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double graphX = x;
        double graphY = y;
        double width = graphWidth.get();
        double height = graphHeight.get();
        renderer.quad(graphX, graphY, width, height, graphBgColor.get());
        if (chunkSpawnCounts.isEmpty()) {
            renderer.text("No data", graphX + width/2 - 20, graphY + height/2 - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return graphY + height + spacing * 2;
        }
        int minChunkX = Integer.MAX_VALUE, maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE, maxChunkZ = Integer.MIN_VALUE;
        for (String key : chunkSpawnCounts.keySet()) {
            String[] parts = key.split(",");
            if (parts.length == 2) {
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);
                minChunkX = Math.min(minChunkX, cx);
                maxChunkX = Math.max(maxChunkX, cx);
                minChunkZ = Math.min(minChunkZ, cz);
                maxChunkZ = Math.max(maxChunkZ, cz);
            }
        }
        int rangeX = Math.max(1, maxChunkX - minChunkX + 1);
        int rangeZ = Math.max(1, maxChunkZ - minChunkZ + 1);
        int maxCount = chunkSpawnCounts.values().stream().max(Integer::compare).orElse(1);
        for (Map.Entry<String, Integer> entry : chunkSpawnCounts.entrySet()) {
            String[] parts = entry.getKey().split(",");
            if (parts.length == 2) {
                int chunkX = Integer.parseInt(parts[0]);
                int chunkZ = Integer.parseInt(parts[1]);
                double cellX = graphX + ((chunkX - minChunkX) / (double)rangeX) * width;
                double cellY = graphY + ((chunkZ - minChunkZ) / (double)rangeZ) * height;
                double cellSize = Math.min(width / rangeX, height / rangeZ);
                double intensity = entry.getValue() / (double) maxCount;
                SettingColor heatColor = new SettingColor(
                    (int)(255 * intensity),
                    (int)(255 * (1 - intensity)),
                    0,
                    (int)(100 + 155 * intensity)
                );
                renderer.quad(cellX, cellY, cellSize, cellSize, heatColor);
            }
        }
        return graphY + height + spacing * 2;
    }
    private double render3DScatterPlot(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("3D Spawn Points:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double graphX = x;
        double graphY = y;
        double width = graphWidth.get();
        double height = graphHeight.get();
        renderer.quad(graphX, graphY, width, height, graphBgColor.get());
        if (yLevelDistribution.isEmpty()) {
            renderer.text("No data", graphX + width/2 - 20, graphY + height/2 - lineHeight/2, 
                         titleColor.get(), textShadow.get(), textScale.get() * 0.8);
            return graphY + height + spacing * 2;
        }
        rotationAngle = (rotationAngle + 2) % 360;
        for (Map.Entry<Integer, Integer> entry : yLevelDistribution.entrySet()) {
            int yLevel = entry.getKey();
            int count = entry.getValue();
            double angle = Math.toRadians(rotationAngle);
            double projX = 0.5 + 0.3 * Math.cos(angle);
            double projY = (yLevel + 64) / 384.0;
            double pointX = graphX + projX * width;
            double pointY = graphY + (1 - projY) * height;
            double size = Math.min(6, 2 + Math.log(count + 1));
            renderer.quad(pointX - size/2, pointY - size/2, size, size, rateColor.get());
        }
        return graphY + height + spacing * 2;
    }
    private double renderEfficiencyGauge(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Farm Efficiency:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double centerX = x + graphWidth.get() / 2.0;
        double centerY = y + graphHeight.get() / 2.0;
        double radius = Math.min(graphWidth.get(), graphHeight.get()) / 2.0 - 10;
        double efficiency = calculateEfficiency();
        drawArc(renderer, centerX, centerY, radius, -225, 270, graphBgColor.get(), 8);
        double effAngle = (efficiency / 100.0) * 270;
        SettingColor effColor = efficiency > 75 ? passiveColor.get() :
                               efficiency > 50 ? neutralColor.get() : hostileColor.get();
        drawArc(renderer, centerX, centerY, radius, -225, effAngle, effColor, 6);
        String effText = String.format("%.0f%%", efficiency);
        renderer.text(effText, centerX - renderer.textWidth(effText, textShadow.get(), textScale.get()) / 2,
                     centerY - lineHeight / 2, titleColor.get(), textShadow.get(), textScale.get());
        return y + graphHeight.get() + spacing * 2;
    }
    private double renderCapRadialChart(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Mob Cap Usage:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double centerX = x + graphWidth.get() / 2.0;
        double centerY = y + graphHeight.get() / 2.0;
        double maxRadius = Math.min(graphWidth.get(), graphHeight.get()) / 2.0 - 10;
        int mobCapUsage = calculateMobCapUsage();
        for (int i = 25; i <= 100; i += 25) {
            double r = maxRadius * (i / 100.0);
            drawCircle(renderer, centerX, centerY, r, graphGridColor.get(), 1);
            if (i < 100) {
                String label = i + "%";
                renderer.text(label, centerX + r + 2, centerY - lineHeight/2, 
                             titleColor.get(), textShadow.get(), textScale.get() * 0.6);
            }
        }
        double fillRadius = maxRadius * (mobCapUsage / 100.0);
        for (double r = 0; r < fillRadius; r += 3) {
            SettingColor fillColor = new SettingColor(
                hostileColor.get().r,
                hostileColor.get().g,
                hostileColor.get().b,
                (int)(150 * (1 - r / fillRadius))
            );
            drawCircle(renderer, centerX, centerY, r, fillColor, 3);
        }
        String capText = String.format("%d%%", mobCapUsage);
        renderer.text(capText, centerX - renderer.textWidth(capText, textShadow.get(), textScale.get()) / 2,
                     centerY - lineHeight / 2, titleColor.get(), textShadow.get(), textScale.get());
        return y + graphHeight.get() + spacing * 2;
    }
    private double renderComparisonBars(HudRenderer renderer, double y, double lineHeight, double spacing) {
        renderer.text("Category Comparison:", x, y, titleColor.get(), textShadow.get(), textScale.get() * 0.9);
        y += lineHeight * 0.9 + spacing;
        double graphX = x;
        double graphY = y;
        double width = graphWidth.get();
        double height = graphHeight.get();
        renderer.quad(graphX, graphY, width, height, graphBgColor.get());
        String[] categories = {"Hostile", "Passive", "Neutral"};
        int[] counts = {hostileCount, passiveCount, neutralCount};
        SettingColor[] colors = {hostileColor.get(), passiveColor.get(), neutralColor.get()};
        int maxCount = Math.max(1, Arrays.stream(counts).max().orElse(1));
        double barWidth = width / 4;
        for (int i = 0; i < 3; i++) {
            if (counts[i] == 0) continue;
            double barX = graphX + barWidth * (i + 0.5);
            double barHeight = (counts[i] / (double) maxCount) * height * 0.8;
            renderer.quad(barX, graphY + height - barHeight, barWidth * 0.8, barHeight, colors[i]);
            String value = String.valueOf(counts[i]);
            renderer.text(value, barX + barWidth * 0.4 - renderer.textWidth(value, textShadow.get(), textScale.get() * 0.7) / 2,
                         graphY + height - barHeight - lineHeight * 0.7, colors[i], textShadow.get(), textScale.get() * 0.7);
            renderer.text(categories[i], barX, graphY + height + 2, colors[i], textShadow.get(), textScale.get() * 0.7);
        }
        return graphY + height + lineHeight + spacing * 2;
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (MeteorClient.mc.world == null || MeteorClient.mc.player == null) return;
        String currentDim = MeteorClient.mc.world.getRegistryKey().getValue().toString();
        if (resetOnDimension.get() && !currentDim.equals(lastDimension)) {
            resetStats();
            lastDimension = currentDim;
        }
        aliveMobs.clear();
        for (Entity entity : MeteorClient.mc.world.getEntities()) {
            if (!isTrackableMob(entity)) continue;
            UUID id = entity.getUuid();
            if (entity.isAlive()) {
                aliveMobs.add(id);
            }
            if (!trackedMobs.contains(id)) {
                if (!shouldTrackEntity(entity)) continue;
                trackedMobs.add(id);
                EntityType<?> type = entity.getType();
                mobTypeCounts.merge(type, 1, Integer::sum);
                int yLevel = entity.getBlockPos().getY();
                yLevelDistribution.merge(yLevel, 1, Integer::sum);
                int chunkX = entity.getBlockPos().getX() >> 4;
                int chunkZ = entity.getBlockPos().getZ() >> 4;
                String chunkKey = chunkX + "," + chunkZ;
                chunkSpawnCounts.merge(chunkKey, 1, Integer::sum);
                if (entity instanceof HostileEntity) {
                    hostileCount++;
                } else if (entity instanceof PassiveEntity) {
                    passiveCount++;
                } else {
                    neutralCount++;
                }
                totalSpawned++;
            }
        }
        long now = System.currentTimeMillis();
        if (now - lastRateUpdate > RATE_UPDATE_INTERVAL) {
            updateRateHistory();
            lastRateUpdate = now;
        }
    }
    private void updateMetrics() {
        long now = System.currentTimeMillis();
        long elapsed = now - sessionStartTime;
        if (elapsed > 1000) {
            double hours = elapsed / 3600000.0;
            currentSpawnRate = totalSpawned / hours;
            if (currentSpawnRate > peakSpawnRate) {
                peakSpawnRate = currentSpawnRate;
            }
        }
    }
    private void updateRateHistory() {
        updateMetrics();
        rateHistory.add(currentSpawnRate);
        while (rateHistory.size() > 30) {
            rateHistory.removeFirst();
        }
    }
    private int calculateMobCapUsage() {
        int hostileCap = 70;
        int passiveCap = 10;
        int totalCap = hostileCap + passiveCap + 5;
        return Math.min(100, (aliveMobs.size() * 100) / totalCap);
    }
    private double calculateEfficiency() {
        double targetRate = 2000;
        return Math.min(100, (currentSpawnRate / targetRate) * 100);
    }
    private boolean shouldTrackEntity(Entity entity) {
        if (entity instanceof HostileEntity && !trackHostile.get()) return false;
        if (entity instanceof PassiveEntity && !trackPassive.get()) return false;
        if (!(entity instanceof HostileEntity) && !(entity instanceof PassiveEntity) && !trackNeutral.get()) return false;
        return true;
    }
    private void resetStats() {
        trackedMobs.clear();
        mobTypeCounts.clear();
        yLevelDistribution.clear();
        rateHistory.clear();
        chunkSpawnCounts.clear();
        aliveMobs.clear();
        totalSpawned = 0;
        hostileCount = 0;
        passiveCount = 0;
        neutralCount = 0;
        currentSpawnRate = 0;
        peakSpawnRate = 0;
        sessionStartTime = System.currentTimeMillis();
    }
    private void renderPlaceholder(HudRenderer renderer) {
        String text = "Mob Farm Stats";
        renderer.text(text, x, y, titleColor.get(), textShadow.get(), textScale.get());
        setSize(renderer.textWidth(text, textShadow.get(), textScale.get()), 
                renderer.textHeight(textShadow.get(), textScale.get()));
    }
    private SettingColor getColorForRate(double rate) {
        if (rate > 5000) return passiveColor.get();
        if (rate > 2000) return rateColor.get();
        if (rate > 500) return neutralColor.get();
        return hostileColor.get();
    }
    private SettingColor getColorForIndex(int index) {
        SettingColor[] colors = {
            hostileColor.get(),
            passiveColor.get(),
            neutralColor.get(),
            new SettingColor(255, 150, 50),
            new SettingColor(150, 255, 150)
        };
        return colors[index % colors.length];
    }
    private static boolean isTrackableMob(Entity entity) {
        if (!(entity instanceof LivingEntity)) return false;
        return entity instanceof HostileEntity ||
               entity instanceof PassiveEntity ||
               entity instanceof IronGolemEntity ||
               entity instanceof SnowGolemEntity ||
               entity instanceof WitherEntity ||
               entity instanceof EnderDragonEntity ||
               entity instanceof EndermanEntity ||
               entity instanceof PiglinEntity ||
               entity instanceof ZombifiedPiglinEntity;
    }
    private void drawLine(HudRenderer renderer, double x1, double y1, double x2, double y2, SettingColor color, double thickness) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return;
        int steps = (int) Math.ceil(length);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = x1 + dx * t;
            double py = y1 + dy * t;
            renderer.quad(px - thickness/2, py - thickness/2, thickness, thickness, color);
        }
    }
    private void drawCircle(HudRenderer renderer, double cx, double cy, double radius, SettingColor color, double thickness) {
        int segments = 32;
        double angleStep = (Math.PI * 2) / segments;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + Math.cos(angle1) * radius;
            double y1 = cy + Math.sin(angle1) * radius;
            double x2 = cx + Math.cos(angle2) * radius;
            double y2 = cy + Math.sin(angle2) * radius;
            drawLine(renderer, x1, y1, x2, y2, color, thickness);
        }
    }
    private void drawArc(HudRenderer renderer, double cx, double cy, double radius, double startAngle, double sweepAngle, SettingColor color, double thickness) {
        int segments = Math.max(10, (int)(Math.abs(sweepAngle) / 10));
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(startAngle + (i * sweepAngle / segments));
            double angle2 = Math.toRadians(startAngle + ((i + 1) * sweepAngle / segments));
            double x1 = cx + Math.cos(angle1) * radius;
            double y1 = cy + Math.sin(angle1) * radius;
            double x2 = cx + Math.cos(angle2) * radius;
            double y2 = cy + Math.sin(angle2) * radius;
            drawLine(renderer, x1, y1, x2, y2, color, thickness);
        }
    }
    private void drawPieSlice(HudRenderer renderer, double cx, double cy, double radius, double startAngle, double sweepAngle, SettingColor color) {
        int segments = Math.max(3, (int)(Math.abs(sweepAngle) / 5));
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(startAngle + (i * sweepAngle / segments));
            double angle2 = Math.toRadians(startAngle + ((i + 1) * sweepAngle / segments));
            for (double r = 0; r <= radius; r += 2) {
                double x1 = cx + Math.cos(angle1) * r;
                double y1 = cy + Math.sin(angle1) * r;
                double x2 = cx + Math.cos(angle2) * r;
                double y2 = cy + Math.sin(angle2) * r;
                drawLine(renderer, x1, y1, x2, y2, color, 2);
            }
        }
    }
    private void drawDonutSlice(HudRenderer renderer, double cx, double cy, double innerRadius, double outerRadius, double startAngle, double sweepAngle, SettingColor color) {
        int segments = Math.max(3, (int)(Math.abs(sweepAngle) / 5));
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.toRadians(startAngle + (i * sweepAngle / segments));
            double angle2 = Math.toRadians(startAngle + ((i + 1) * sweepAngle / segments));
            for (double r = innerRadius; r <= outerRadius; r += 2) {
                double x1 = cx + Math.cos(angle1) * r;
                double y1 = cy + Math.sin(angle1) * r;
                double x2 = cx + Math.cos(angle2) * r;
                double y2 = cy + Math.sin(angle2) * r;
                drawLine(renderer, x1, y1, x2, y2, color, 2);
            }
        }
    }
}
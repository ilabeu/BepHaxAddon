package bep.hax.hud;

import bep.hax.Bep;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityList extends HudElement {
    public static final HudElementInfo<EntityList> INFO = new HudElementInfo<>(Bep.HUD_GROUP, "EntityList", "Displays nearby entities in a list.", EntityList::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showTitle = sgGeneral.add(new BoolSetting.Builder()
        .name("show-title")
        .description("Display the HUD title.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showItems = sgGeneral.add(new BoolSetting.Builder()
        .name("show-items")
        .description("Show dropped items.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showMobs = sgGeneral.add(new BoolSetting.Builder()
        .name("show-mobs")
        .description("Show mobs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("show-players")
        .description("Show players.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> maxDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-distance")
        .description("Maximum distance to show entities.")
        .defaultValue(100.0)
        .min(0.0)
        .sliderRange(0.0, 500.0)
        .build()
    );

    private final Setting<Boolean> sortByDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-by-distance")
        .description("Sort entities by distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Show distance to entities.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> textScale = sgGeneral.add(new DoubleSetting.Builder()
        .name("text-scale")
        .description("Scale of the text.")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 3.0)
        .build()
    );

    private final Setting<Boolean> textShadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Render shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> playerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("player-color")
        .description("Color for player entities.")
        .defaultValue(new SettingColor(0, 255, 0, 255))
        .build()
    );

    private final Setting<SettingColor> mobColor = sgGeneral.add(new ColorSetting.Builder()
        .name("mob-color")
        .description("Color for mob entities.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    private final Setting<SettingColor> itemColor = sgGeneral.add(new ColorSetting.Builder()
        .name("item-color")
        .description("Color for item entities.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    public EntityList() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (MeteorClient.mc.world == null || MeteorClient.mc.player == null) {
            if (isInEditor()) {
                renderer.text("Entity List", x, y, playerColor.get(), textShadow.get(), textScale.get());
                setSize(renderer.textWidth("Entity List", textShadow.get(), textScale.get()), renderer.textHeight(textShadow.get(), textScale.get()));
            }
            return;
        }

        Map<String, Aggregated> map = new HashMap<>();
        for (Entity entity : MeteorClient.mc.world.getEntities()) {
            if (entity == MeteorClient.mc.player) continue;

            // Calculate horizontal distance only (ignore Y level differences)
            double dx = entity.getX() - MeteorClient.mc.player.getX();
            double dz = entity.getZ() - MeteorClient.mc.player.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > maxDistance.get()) continue;

            boolean isItem = entity instanceof ItemEntity && showItems.get();
            boolean isMob = entity instanceof MobEntity && showMobs.get();
            boolean isPlayer = entity instanceof PlayerEntity && showPlayers.get();

            if (!isItem && !isMob && !isPlayer) continue;

            String name = getEntityName(entity);
            SettingColor color = getEntityColor(entity);

            Aggregated agg = map.get(name);
            if (agg == null) {
                agg = new Aggregated();
                agg.name = name;
                agg.color = color;
                agg.minDist = horizontalDist;
                if (isItem) {
                    agg.count = ((ItemEntity) entity).getStack().getCount();
                } else {
                    agg.count = 1;
                }
                map.put(name, agg);
            } else {
                agg.minDist = Math.min(agg.minDist, horizontalDist);
                if (isItem) {
                    agg.count += ((ItemEntity) entity).getStack().getCount();
                } else {
                    agg.count++;
                }
            }
        }

        List<Aggregated> aggregatedList = new ArrayList<>(map.values());
        if (sortByDistance.get()) {
            aggregatedList.sort(Comparator.comparingDouble(a -> a.minDist));
        }

        double curX = x;
        double curY = y;
        double maxWidth = 0;
        double height = 0;
        double textHeight = renderer.textHeight(textShadow.get(), textScale.get());
        double spacing = 2;

        if (showTitle.get()) {
            String title = "Entity List";
            double titleWidth = renderer.textWidth(title, textShadow.get(), textScale.get());
            renderer.text(title, curX, curY, playerColor.get(), textShadow.get(), textScale.get());
            curY += textHeight + spacing;
            height += textHeight + spacing;
            maxWidth = Math.max(maxWidth, titleWidth);
        }

        for (Aggregated agg : aggregatedList) {
            String text = agg.name;
            if (agg.count > 1) {
                text += " x" + agg.count;
            }
            if (showDistance.get()) {
                text += " (" + (int) agg.minDist + "m)";
            }
            double textWidth = renderer.textWidth(text, textShadow.get(), textScale.get());

            renderer.text(text, curX, curY, agg.color, textShadow.get(), textScale.get());

            curY += textHeight + spacing;
            height += textHeight + spacing;
            maxWidth = Math.max(maxWidth, textWidth);
        }

        setSize(maxWidth, height - spacing);  // Subtract last spacing
    }

    private String getEntityName(Entity entity) {
        if (entity instanceof ItemEntity item) {
            return item.getStack().getName().getString();
        } else if (entity instanceof PlayerEntity player) {
            return player.getName().getString();
        } else {
            return entity.getDisplayName().getString();
        }
    }

    private SettingColor getEntityColor(Entity entity) {
        if (entity instanceof ItemEntity) {
            return itemColor.get();
        } else if (entity instanceof MobEntity) {
            return mobColor.get();
        } else if (entity instanceof PlayerEntity) {
            return playerColor.get();
        }
        return new SettingColor(255, 255, 255, 255);  // Fallback
    }

    private static class Aggregated {
        String name;
        int count;
        double minDist;
        SettingColor color;
    }
}

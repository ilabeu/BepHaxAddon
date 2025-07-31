package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.text.Text;
import bep.hax.util.ShulkerDataParser;

import java.util.Map;

public class ShulkerOverviewModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> iconSize = sgGeneral.add(new IntSetting.Builder()
        .name("icon-size")
        .description("Size of the item icon overlay.")
        .defaultValue(12)
        .min(4)
        .max(16)
        .sliderMin(4)
        .sliderMax(16)
        .build()
    );

    public final Setting<IconPosition> iconPosition = sgGeneral.add(new EnumSetting.Builder<IconPosition>()
        .name("icon-position")
        .description("Position of the item icon overlay.")
        .defaultValue(IconPosition.Center)
        .build()
    );

    public final Setting<String> multipleText = sgGeneral.add(new StringSetting.Builder()
        .name("multiple-indicator")
        .description("Text to show when shulker contains multiple item types.")
        .defaultValue("+")
        .build()
    );

    public final Setting<Integer> multipleSize = sgGeneral.add(new IntSetting.Builder()
        .name("multiple-size")
        .description("Size of the multiple indicator text.")
        .defaultValue(8)
        .min(4)
        .max(16)
        .sliderMin(4)
        .sliderMax(16)
        .build()
    );

    public final Setting<Boolean> debugMode = sgGeneral.add(new BoolSetting.Builder()
        .name("debug-mode")
        .description("Show debug information.")
        .defaultValue(false)
        .build()
    );

    public ShulkerOverviewModule() {
        super(Bep.CATEGORY, "shulker-overview", "Overlays most common item icon on shulker boxes in inventory.");
    }

    public void renderShulkerOverlay(DrawContext context, int x, int y, ItemStack stack) {
        // Check if it's a shulker box
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        if (!(blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

        // Parse shulker contents using our utility
        Map<Item, Integer> itemCounts = ShulkerDataParser.parseShulkerContents(stack);
        if (itemCounts.isEmpty()) return;

        // Find most common item
        Map.Entry<Item, Integer> mostCommon = itemCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        if (mostCommon == null) return;

        Item item = mostCommon.getKey();
        int count = mostCommon.getValue();
        boolean hasMultiple = itemCounts.size() > 1;

        // Debug mode
        if (debugMode.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            String debug = String.format("Items: %d, Most: %s x%d",
                itemCounts.size(),
                item.getName().getString(),
                count);
            context.drawText(mc.textRenderer, debug, x, y - 10, 0xFFFFFF, true);
        }

        // Calculate render position based on setting
        int iconSize = this.iconSize.get();
        int iconX, iconY;
        switch (iconPosition.get()) {
            case BottomLeft -> {
                iconX = x;
                iconY = y + 16 - iconSize;
            }
            case TopRight -> {
                iconX = x + 16 - iconSize;
                iconY = y;
            }
            case TopLeft -> {
                iconX = x;
                iconY = y;
            }
            case Center -> {
                iconX = x + (16 - iconSize) / 2;
                iconY = y + (16 - iconSize) / 2;
            }
            default -> {
                iconX = x + 16 - iconSize;
                iconY = y + 16 - iconSize;
            }
        }

        // Render the item icon with proper depth
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 200); // Lowered z to ensure under tooltip layer

        if (iconSize == 16) {
            context.drawItem(new ItemStack(item), iconX, iconY);
        } else {
            float scale = iconSize / 16.0f;
            context.getMatrices().translate(iconX, iconY, 0);
            context.getMatrices().scale(scale, scale, 1.0f);
            context.drawItem(new ItemStack(item), 0, 0);
        }

        context.getMatrices().pop();

        // Draw multiple indicator
        if (hasMultiple && !multipleText.get().isEmpty()) {
            renderMultipleIndicator(context, x, y, multipleText.get(), multipleSize.get());
        }
    }

    private void renderMultipleIndicator(DrawContext context, int slotX, int slotY, String text, int size) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Text textComponent = Text.literal(text);
        float defaultFontHeight = mc.textRenderer.fontHeight; // Typically 9
        float scale = (float) size / defaultFontHeight;

        int unscaledWidth = mc.textRenderer.getWidth(textComponent);
        int textX = slotX + 16 - Math.round(unscaledWidth * scale) - 1;
        int textY = slotY + 1;

        // Push matrices for proper depth and scaling
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 300); // Lowered z to ensure under tooltip layer
        context.getMatrices().translate(textX, textY, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        context.drawText(mc.textRenderer, textComponent, 0, 0, 0xFFFF00, false);

        context.getMatrices().pop();
    }

    public enum IconPosition {
        BottomRight,
        BottomLeft,
        TopRight,
        TopLeft,
        Center
    }
}

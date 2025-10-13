package bep.hax.modules;
import bep.hax.Bep;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
public class ItemSearchBar extends Module {
    public ItemSearchBar() { 
        super(Bep.CATEGORY, "ItemSearchBar", "Search and highlight items in inventory and containers."); 
    }
    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final Setting<String> searchQuery = sgGeneral.add(
        new StringSetting.Builder()
            .name("search-query")
            .description("Search query to match item names. Use commas to separate multiple search terms.")
            .defaultValue("")
            .build()
    );
    private String currentSearchQuery = "";
    private final Setting<Boolean> caseSensitive = sgGeneral.add(
        new BoolSetting.Builder()
            .name("case-sensitive")
            .description("Whether the search should be case sensitive.")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> splitQueries = sgGeneral.add(
        new BoolSetting.Builder()
            .name("split-queries")
            .description("Split search queries by commas. Disable to treat commas literally.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchItemName = sgGeneral.add(
        new BoolSetting.Builder()
            .name("search-item-name")
            .description("Search in item display names.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchItemType = sgGeneral.add(
        new BoolSetting.Builder()
            .name("search-item-type")
            .description("Search in item type names.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> searchLore = sgGeneral.add(
        new BoolSetting.Builder()
            .name("search-lore")
            .description("Search in item lore/tooltip text.")
            .defaultValue(false)
            .build()
    );
    public final Setting<SettingColor> highlightColor = sgGeneral.add(
        new ColorSetting.Builder()
            .name("highlight-color")
            .description("Color to highlight matching items.")
            .defaultValue(new SettingColor(255, 255, 0, 100))
            .build()
    );
    private final Setting<Boolean> ownInventory = sgGeneral.add(
        new BoolSetting.Builder()
            .name("inventory-highlight")
            .description("Highlight items in player inventory.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> showSearchField = sgGeneral.add(
        new BoolSetting.Builder()
            .name("show-search-field")
            .description("Show search input field on top of container windows.")
            .defaultValue(true)
            .build()
    );
    private final SettingGroup sgGUI = settings.createGroup("GUI Settings");
    private final Setting<Integer> fieldWidth = sgGUI.add(
        new IntSetting.Builder()
            .name("field-width")
            .description("Width of the search field.")
            .defaultValue(160)
            .min(80)
            .max(300)
            .sliderMin(80)
            .sliderMax(300)
            .build()
    );
    private final Setting<Integer> fieldHeight = sgGUI.add(
        new IntSetting.Builder()
            .name("field-height")
            .description("Height of the search field.")
            .defaultValue(12)
            .min(8)
            .max(20)
            .sliderMin(8)
            .sliderMax(20)
            .build()
    );
    private final Setting<Integer> offsetX = sgGUI.add(
        new IntSetting.Builder()
            .name("offset-x")
            .description("Horizontal offset from container edge.")
            .defaultValue(8)
            .min(-100)
            .max(100)
            .sliderMin(-100)
            .sliderMax(100)
            .build()
    );
    private final Setting<Integer> offsetY = sgGUI.add(
        new IntSetting.Builder()
            .name("offset-y")
            .description("Vertical offset from container top (negative = above container).")
            .defaultValue(-18)
            .min(-50)
            .max(50)
            .sliderMin(-50)
            .sliderMax(50)
            .build()
    );
    private boolean shouldIgnoreCurrentScreenHandler(ClientPlayerEntity player) {
        if (mc.currentScreen == null) return true;
        if (player.currentScreenHandler == null) return true;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler instanceof PlayerScreenHandler) return !ownInventory.get();
        return !(handler instanceof AbstractFurnaceScreenHandler || handler instanceof GenericContainerScreenHandler
            || handler instanceof Generic3x3ContainerScreenHandler || handler instanceof ShulkerBoxScreenHandler
            || handler instanceof HopperScreenHandler || handler instanceof HorseScreenHandler);
    }
    private boolean matchesSearchQuery(String text, String query) {
        if (caseSensitive.get()) {
            return text.contains(query);
        } else {
            return text.toLowerCase().contains(query.toLowerCase());
        }
    }
    public void updateSearchQuery(String query) {
        this.currentSearchQuery = query;
        searchQuery.set(query);
    }
    public boolean shouldShowSearchField() {
        return showSearchField.get();
    }
    public int getFieldWidth() { return fieldWidth.get(); }
    public int getFieldHeight() { return fieldHeight.get(); }
    public int getOffsetX() { return offsetX.get(); }
    public int getOffsetY() { return offsetY.get(); }
    public boolean shouldHighlightSlot(ItemStack stack) {
        if (mc.player == null) return false;
        if (stack.isEmpty() || shouldIgnoreCurrentScreenHandler(mc.player)) return false;
        String query = !currentSearchQuery.isEmpty() ? currentSearchQuery.trim() : searchQuery.get().trim();
        if (query.isEmpty()) return false;
        if (Utils.hasItems(stack)) {
            ItemStack[] stacks = new ItemStack[27];
            Utils.getItemsInContainerItem(stack, stacks);
            for (ItemStack s : stacks) {
                if (shouldHighlightSlot(s)) return true;
            }
        }
        if (splitQueries.get() && query.contains(",")) {
            String[] queries = query.split(",");
            for (String q : queries) {
                q = q.trim();
                if (q.isEmpty()) continue;
                if (matchesItem(stack, q)) return true;
            }
        } else {
            return matchesItem(stack, query);
        }
        return false;
    }
    private boolean matchesItem(ItemStack stack, String query) {
        if (searchItemName.get()) {
            String displayName = stack.getName().getString();
            if (matchesSearchQuery(displayName, query)) return true;
        }
        if (searchItemType.get()) {
            String typeName = stack.getItem().getDefaultStack().getName().getString();
            if (matchesSearchQuery(typeName, query)) return true;
        }
        if (searchLore.get()) {
            String tooltip = stack.getComponents().toString();
            if (matchesSearchQuery(tooltip, query)) return true;
        }
        return false;
    }
}
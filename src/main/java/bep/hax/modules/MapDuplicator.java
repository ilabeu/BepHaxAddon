package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MapDuplicator extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLimits = settings.getDefaultGroup();

    // General settings
    private final Setting<Boolean> showStatus = sgGeneral.add(new BoolSetting.Builder()
        .name("show-status")
        .description("Show status messages in chat.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> silentCrafting = sgGeneral.add(new BoolSetting.Builder()
        .name("silent-crafting")
        .description("Allow crafting without opening inventory (may not work on all servers).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> craftingLoops = sgGeneral.add(new IntSetting.Builder()
        .name("crafting-loops")
        .description("Number of times to duplicate each map stack.")
        .defaultValue(1)
        .min(1)
        .max(64)
        .sliderMin(1)
        .sliderMax(64)
        .build()
    );

    // Rate limiting settings (based on MapCopier mod)
    private final Setting<Integer> maxClicksPerSecond = sgLimits.add(new IntSetting.Builder()
        .name("max-clicks-per-second")
        .description("Maximum clicks per second to avoid server kicks.")
        .defaultValue(50)
        .min(1)
        .max(100)
        .build()
    );

    private final Setting<Integer> clickDelay = sgLimits.add(new IntSetting.Builder()
        .name("click-delay")
        .description("Delay between clicks in ticks.")
        .defaultValue(2)
        .min(1)
        .max(10)
        .build()
    );

    private int tickCounter = 0;
    private boolean isCrafting = false;
    private int mapsToDuplicate = 0;
    private int emptyMapsAvailable = 0;

    // Crafting queue for all maps
    private List<CraftingTask> craftingQueue = new ArrayList<>();
    private int currentTaskIndex = 0;
    private int craftingStep = 0;

    public MapDuplicator() {
        super(Bep.CATEGORY, "Map Copier", "Automatically duplicates all maps using inventory crafting.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
        isCrafting = false;
        currentTaskIndex = 0;
        craftingStep = 0;
        craftingQueue.clear();

        // If silent crafting is disabled, automatically open inventory
        if (!silentCrafting.get()) {
            mc.execute(() -> {
                if (mc.player != null) {
                    // Open the inventory screen directly
                    mc.setScreen(new net.minecraft.client.gui.screen.ingame.InventoryScreen(mc.player));
                }
            });
        }
    }

    @Override
    public void onDeactivate() {
        isCrafting = false;
        craftingQueue.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;

        // Check if we need to analyze inventory
        if (!isCrafting) {
            // If silent crafting is disabled, check if inventory is open
            if (!silentCrafting.get()) {
                if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler) &&
                    !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                    return; // Wait for inventory to be opened
                }
            }

            analyzeInventory();
            int totalEmptyMapsNeeded = mapsToDuplicate * craftingLoops.get();
            if (mapsToDuplicate == 0 || emptyMapsAvailable < totalEmptyMapsNeeded) {
                if (showStatus.get()) {
                    if (mapsToDuplicate == 0) {
                        info("No maps to duplicate found.");
                    } else {
                        error("Insufficient empty maps! Need " + totalEmptyMapsNeeded + " (" + mapsToDuplicate + " stacks × " + craftingLoops.get() + ") but only have " + emptyMapsAvailable + ".");
                    }
                }
                toggle(); // Automatically disable the module
                return;
            }
            startCrafting();
        }

        // Handle crafting process
        if (isCrafting && tickCounter >= clickDelay.get()) {
            tickCounter = 0;
            processCrafting();
        }
    }

    private void analyzeInventory() {
        mapsToDuplicate = 0;
        emptyMapsAvailable = 0;
        craftingQueue.clear();

        // Count filled maps and create crafting tasks (including hotbar slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FILLED_MAP && stack.getCount() > 0) {
                mapsToDuplicate += 1; // Count each stack as 1, not each individual map
                // Create crafting tasks for each loop
                for (int loop = 0; loop < craftingLoops.get(); loop++) {
                    craftingQueue.add(new CraftingTask(SlotUtils.indexToId(i), stack, loop + 1));
                }
            }
        }

        // Count filled maps in main inventory (slots 9-35)
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.FILLED_MAP && stack.getCount() > 0) {
                mapsToDuplicate += 1; // Count each stack as 1, not each individual map
                // Create crafting tasks for each loop
                for (int loop = 0; loop < craftingLoops.get(); loop++) {
                    craftingQueue.add(new CraftingTask(SlotUtils.indexToId(i), stack, loop + 1));
                }
            }
        }

        // Count empty maps in hotbar (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MAP) {
                emptyMapsAvailable += stack.getCount();
            }
        }

        // Count empty maps in main inventory (slots 9-35)
        for (int i = 9; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MAP) {
                emptyMapsAvailable += stack.getCount();
            }
        }

        // Also count empty maps in crafting grid if inventory is open
        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler ||
            mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            ScreenHandler handler = mc.player.currentScreenHandler;

            // For player inventory (2x2 crafting grid), check slots 1-4
            if (handler instanceof PlayerScreenHandler) {
                for (int i = 1; i <= 4; i++) {
                    try {
                        if (i < handler.slots.size()) {
                            ItemStack stack = handler.getSlot(i).getStack();
                            if (stack.getItem() == Items.MAP) {
                                emptyMapsAvailable += stack.getCount();
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            // For crafting table (3x3 crafting grid), check slots 1-9
            else if (handler instanceof CraftingScreenHandler) {
                for (int i = 1; i <= 9; i++) {
                    try {
                        if (i < handler.slots.size()) {
                            ItemStack stack = handler.getSlot(i).getStack();
                            if (stack.getItem() == Items.MAP) {
                                emptyMapsAvailable += stack.getCount();
                            }
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }

        // Check if we have enough empty maps (considering loops)
        int totalEmptyMapsNeeded = mapsToDuplicate * craftingLoops.get();
        if (emptyMapsAvailable < totalEmptyMapsNeeded) {
            return;
        }

        if (showStatus.get()) {
            info("Found " + mapsToDuplicate + " filled map stacks and " + emptyMapsAvailable + " empty maps. Will duplicate " + mapsToDuplicate + " stacks " + craftingLoops.get() + " times each.");
        }
    }

    private void startCrafting() {
        if (craftingQueue.isEmpty()) {
            if (showStatus.get()) {
                error("Cannot start crafting: no maps to duplicate.");
            }
            return;
        }

        // Check if we have enough empty maps (considering loops)
        int totalEmptyMapsNeeded = mapsToDuplicate * craftingLoops.get();
        if (emptyMapsAvailable < totalEmptyMapsNeeded) {
            if (showStatus.get()) {
                error("Cannot start crafting: insufficient empty maps. Need " + totalEmptyMapsNeeded + " but only have " + emptyMapsAvailable + ".");
            }
            return;
        }

        currentTaskIndex = 0;
        craftingStep = 0;
        isCrafting = true;

        if (showStatus.get()) {
            info("Starting map duplication process for " + craftingQueue.size() + " crafting tasks (" + mapsToDuplicate + " stacks × " + craftingLoops.get() + " loops)...");
        }
    }

    private void processCrafting() {
        if (!isCrafting || currentTaskIndex >= craftingQueue.size()) {
            finishCrafting();
            return;
        }

        // Check if we're in player inventory or crafting table (unless silent crafting is enabled)
        if (!silentCrafting.get()) {
            if (!(mc.player.currentScreenHandler instanceof PlayerScreenHandler) &&
                !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
                if (showStatus.get()) {
                    error("Please open your inventory or a crafting table to continue duplication.");
                }
                finishCrafting(); // Stop crafting if inventory is closed
                return;
            }
        }

        // Perform crafting steps
        performCraftingStep();
    }

    private void performCraftingStep() {
        if (currentTaskIndex >= craftingQueue.size()) {
            finishCrafting();
            return;
        }

        CraftingTask currentTask = craftingQueue.get(currentTaskIndex);

        switch (craftingStep) {
            case 0: // Place filled map in crafting grid slot 1
                if (isValidSlot(currentTask.mapSlot)) {
                    InvUtils.click().slotId(currentTask.mapSlot);
                    if (isValidSlot(1)) {
                        InvUtils.click().slotId(1);
                    }
                    craftingStep = 1;
                } else {
                    info("Invalid source slot: " + currentTask.mapSlot);
                    finishCrafting();
                }
                break;

            case 1: // Find and place empty map in crafting grid slot 2
                int emptyMapSlot = findNextEmptyMap();
                if (emptyMapSlot != -1) {
                    if (isValidSlot(emptyMapSlot)) {
                        InvUtils.click().slotId(emptyMapSlot);
                        if (isValidSlot(2)) {
                            InvUtils.click().slotId(2);
                        }
                        craftingStep = 2;
                    } else {
                        info("Invalid empty map slot: " + emptyMapSlot);
                        finishCrafting();
                    }
                } else {
                    info("No empty maps available");
                    finishCrafting();
                }
                break;

            case 2: // Take crafted result from output slot
                if (isValidSlot(0)) {
                    InvUtils.click().slotId(0);
                    craftingStep = 3;
                } else {
                    info("Invalid output slot");
                    finishCrafting();
                }
                break;

            case 3: // Place duplicated maps back to original slot
                if (isValidSlot(currentTask.mapSlot)) {
                    InvUtils.click().slotId(currentTask.mapSlot);
                    craftingStep = 4;
                } else {
                    info("Invalid target slot: " + currentTask.mapSlot);
                    finishCrafting();
                }
                break;

            case 4: // Shift-click remaining filled maps from crafting grid back to inventory
                // Shift-click from slot 1 (filled map) back to original slot
                if (isValidSlot(1) && isValidSlot(currentTask.mapSlot)) {
                    // Use shift-click to automatically stack with existing items
                    InvUtils.shiftClick().slotId(1);
                }
                craftingStep = 5;
                break;

            case 5: // Move to next map
                // Check if we completed all loops for the current stack
                CraftingTask nextTask = (currentTaskIndex + 1 < craftingQueue.size()) ?
                    craftingQueue.get(currentTaskIndex + 1) : null;

                // If this was the last loop for this stack, show completion message
                if (nextTask == null || nextTask.mapSlot != currentTask.mapSlot) {
                    if (showStatus.get()) {
                        info("Map stack completed all " + craftingLoops.get() + " loop(s)!");
                    }
                }

                currentTaskIndex++;
                craftingStep = 0;
                break;
        }
    }

    private boolean isValidSlot(int slotId) {
        try {
            if (mc.player == null || mc.player.currentScreenHandler == null) return false;

            ScreenHandler handler = mc.player.currentScreenHandler;
            if (slotId < 0 || slotId >= handler.slots.size()) return false;

            // Check if the slot exists and is accessible
            return handler.getSlot(slotId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void clearCraftingGrid() {
        try {
            ScreenHandler handler = mc.player.currentScreenHandler;
            if (handler instanceof PlayerScreenHandler || handler instanceof CraftingScreenHandler) {
                // Determine crafting grid range
                int startSlot = (handler instanceof PlayerScreenHandler) ? 1 : 1;
                int endSlot = (handler instanceof PlayerScreenHandler) ? 4 : 9;

                // Clear all crafting grid slots
                for (int slot = startSlot; slot <= endSlot; slot++) {
                    if (isValidSlot(slot) && handler.getSlot(slot).hasStack()) {
                        InvUtils.click().slotId(slot);
                        // Find empty slot in inventory to put it back
                        int emptySlot = findEmptyInventorySlot();
                        if (emptySlot >= 0) {
                            int emptySlotId = SlotUtils.indexToId(emptySlot);
                            if (isValidSlot(emptySlotId)) {
                                InvUtils.click().slotId(emptySlotId);
                            }
                        }
                    }
                }

                if (showStatus.get()) {
                    info("Crafting grid cleared - all items moved to inventory");
                }
            }
        } catch (Exception e) {
            if (showStatus.get()) {
                error("Error clearing crafting grid: " + e.getMessage());
            }
        }
    }

    private int findEmptyInventorySlot() {
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findNextEmptyMap() {
        // First check hotbar slots (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MAP) {
                return SlotUtils.indexToId(i);
            }
        }

        // Then check main inventory slots (9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.MAP) {
                return SlotUtils.indexToId(i);
            }
        }

        // Then check crafting grid slots
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (handler instanceof PlayerScreenHandler) {
            // Player inventory crafting (2x2 grid, slots 1-4)
            for (int i = 1; i <= 4; i++) {
                ItemStack stack = handler.getSlot(i).getStack();
                if (stack.getItem() == Items.MAP) {
                    return i;
                }
            }
        } else if (handler instanceof CraftingScreenHandler) {
            // Crafting table (3x3 grid, slots 1-9)
            for (int i = 1; i <= 9; i++) {
                ItemStack stack = handler.getSlot(i).getStack();
                if (stack.getItem() == Items.MAP) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void finishCrafting() {
        // Clear crafting grid before finishing
        clearCraftingGrid();

        isCrafting = false;
        if (showStatus.get()) {
            info("Map duplication completed! Duplicated " + currentTaskIndex + " maps.");
        }
        toggle(); // Automatically disable the module
    }

    @Override
    public String getInfoString() {
        return mapsToDuplicate + "/" + emptyMapsAvailable;
    }

    // Helper class to track crafting tasks
    private static class CraftingTask {
        public final int mapSlot;
        public final ItemStack mapStack;
        public final int loop; // Added loop number

        public CraftingTask(int mapSlot, ItemStack mapStack, int loop) {
            this.mapSlot = mapSlot;
            this.mapStack = mapStack;
            this.loop = loop;
        }
    }
}

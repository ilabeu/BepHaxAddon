package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ElytraSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> durabilityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("Durability Threshold")
        .description("Swap elytra when durability drops below this value.")
        .defaultValue(10)
        .min(1)
        .max(100)
        .sliderRange(1, 50)
        .build()
    );

    private final Setting<Boolean> onlyWhileFlying = sgGeneral.add(new BoolSetting.Builder()
        .name("Only While Flying")
        .description("Only swap elytras while actively flying.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pauseInInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("Pause In Inventory")
        .description("Don't swap while inventory is open to prevent desync.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> swapCooldown = sgGeneral.add(new IntSetting.Builder()
        .name("Swap Cooldown")
        .description("Ticks to wait after swapping before checking again.")
        .defaultValue(100)
        .min(20)
        .max(200)
        .sliderRange(20, 200)
        .build()
    );

    private final Setting<Boolean> notifySwap = sgGeneral.add(new BoolSetting.Builder()
        .name("Notify Swap")
        .description("Send a chat message when swapping elytras.")
        .defaultValue(true)
        .build()
    );

    private int cooldownTimer = 0;
    private boolean needsSwap = false;
    private int swapStage = 0;
    private int stageTimer = 0;
    private int targetSlot = -1;
    
    private int originalElytraSlot = -1;
    private int originalHotbarSlot = -1;
    private ItemStack originalHotbarItem = ItemStack.EMPTY;
    private boolean hasSwappedOnce = false;

    public ElytraSwap() {
        super(
            Bep.CATEGORY,
            "ElytraSwap",
            "Automatically swaps elytras when they reach low durability."
        );
    }

    @Override
    public void onActivate() {
        resetSwapState();
    }

    @Override
    public void onDeactivate() {
        resetSwapState();
    }

    private void resetSwapState() {
        cooldownTimer = 0;
        needsSwap = false;
        swapStage = 0;
        stageTimer = 0;
        targetSlot = -1;
        originalElytraSlot = -1;
        originalHotbarSlot = -1;
        originalHotbarItem = ItemStack.EMPTY;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Handle cooldown
        if (cooldownTimer > 0) {
            cooldownTimer--;
            return;
        }

        // Safety check - don't swap with inventory open
        if (pauseInInventory.get() && mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
            resetSwapState();
            return;
        }

        // Check if wearing elytra
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chestItem.getItem().equals(Items.ELYTRA)) {
            return;
        }

        // Check if we should only swap while flying
        if (onlyWhileFlying.get() && !mc.player.isGliding()) {
            return;
        }

        // Process swap stages
        if (needsSwap) {
            processSwapStages();
            return;
        }

        // Check durability
        int currentDurability = chestItem.getMaxDamage() - chestItem.getDamage();
        if (currentDurability <= durabilityThreshold.get()) {
            initiateSwap();
        }
    }

    private void initiateSwap() {
        // Find best elytra in inventory
        int bestSlot = -1;
        int bestDurability = durabilityThreshold.get();

        // Check entire inventory (0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem().equals(Items.ELYTRA)) {
                int durability = stack.getMaxDamage() - stack.getDamage();
                if (durability > bestDurability) {
                    bestDurability = durability;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot == -1) {
            return;
        }

        targetSlot = bestSlot;
        needsSwap = true;
        swapStage = 1;
        stageTimer = 0;
    }

    private void processSwapStages() {
        stageTimer++;

        // Add delay between stages to prevent desync
        if (stageTimer < 5) return;

        switch (swapStage) {
            case 1 -> {
                // Stage 1: Track original positions and prepare for swap
                if (!hasSwappedOnce) {
                    // First swap - remember where the new elytra came from
                    originalElytraSlot = targetSlot;
                }
                
                // Move elytra to hotbar if it's in main inventory
                if (targetSlot >= 9) {
                    int hotbarSlot = -1;
                    
                    // Always try to use the same hotbar slot for consistency
                    if (originalHotbarSlot != -1 && originalHotbarSlot < 9) {
                        // Use the previously used hotbar slot
                        hotbarSlot = originalHotbarSlot;
                    } else {
                        // Find empty slot or use slot 0
                        for (int i = 0; i < 9; i++) {
                            if (mc.player.getInventory().getStack(i).isEmpty()) {
                                hotbarSlot = i;
                                break;
                            }
                        }
                        
                        if (hotbarSlot == -1) {
                            hotbarSlot = 0; // Use first slot if no empty slot
                        }
                        
                        // Remember what was in this hotbar slot before (only on first swap)
                        if (!hasSwappedOnce) {
                            originalHotbarSlot = hotbarSlot;
                            originalHotbarItem = mc.player.getInventory().getStack(hotbarSlot).copy();
                        }
                    }

                    // Use InvUtils to move the item
                    InvUtils.move().from(targetSlot).toHotbar(hotbarSlot);

                    targetSlot = hotbarSlot;
                    swapStage = 2;
                    stageTimer = 0;
                } else {
                    // Already in hotbar
                    if (!hasSwappedOnce) {
                        originalHotbarSlot = targetSlot;
                        originalHotbarItem = ItemStack.EMPTY; // It was already an elytra in hotbar
                    }
                    swapStage = 2;
                    stageTimer = 0;
                }
            }
            case 2 -> {
                // Stage 2: Swap with chest slot
                // Swap to the elytra slot temporarily
                InvUtils.swap(targetSlot, false);

                // Use right-click to equip it
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);

                // Swap back to previous slot
                InvUtils.swapBack();

                swapStage = 3;
                stageTimer = 0;
            }
            case 3 -> {
                // Stage 3: Clean up and restore positions
                
                // After swap, the damaged elytra is now in the hotbar at targetSlot
                // We need to move it back to where the good elytra originally came from
                if (hasSwappedOnce && originalElytraSlot >= 9 && targetSlot < 9) {
                    // Schedule moving the damaged elytra back to its original location
                    stageTimer = 0;
                    swapStage = 4;
                    return;
                }
                
                // Mark that we've completed a swap
                if (!hasSwappedOnce) {
                    hasSwappedOnce = true;
                }
                
                // Notify
                if (notifySwap.get()) {
                    ItemStack newChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                    if (newChest.getItem().equals(Items.ELYTRA)) {
                        int newDurability = newChest.getMaxDamage() - newChest.getDamage();
                        info("Swapped to elytra with " + newDurability + " durability");
                    }
                }

                // Clear swap flags but keep position tracking
                needsSwap = false;
                swapStage = 0;
                stageTimer = 0;
                targetSlot = -1;
                cooldownTimer = swapCooldown.get();
            }
            case 4 -> {
                // Stage 4: Move damaged elytra back to original position
                if (stageTimer < 5) {
                    stageTimer++;
                    return;
                }
                
                // Move the damaged elytra from hotbar back to the original slot
                InvUtils.move().fromHotbar(targetSlot).to(originalElytraSlot);
                
                // If there was an original item in the hotbar, restore it
                if (!originalHotbarItem.isEmpty() && originalHotbarSlot != -1) {
                    // Look for the original item in inventory
                    for (int i = 9; i < 36; i++) {
                        ItemStack stack = mc.player.getInventory().getStack(i);
                        if (ItemStack.areItemsEqual(stack, originalHotbarItem)) {
                            InvUtils.move().from(i).toHotbar(originalHotbarSlot);
                            break;
                        }
                    }
                }
                
                if (notifySwap.get()) {
                    info("Items restored to original positions");
                }
                
                // Clear all flags
                needsSwap = false;
                swapStage = 0;
                stageTimer = 0;
                targetSlot = -1;
                cooldownTimer = swapCooldown.get();
            }
        }
    }
}

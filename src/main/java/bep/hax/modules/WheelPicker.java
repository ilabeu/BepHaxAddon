package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.util.InputUtil;

import java.util.concurrent.ThreadLocalRandom;

public class WheelPicker extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSlots = settings.createGroup("Slot Actions");
    private final SettingGroup sgSpam = settings.createGroup("Spam Protection");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General settings
    private final Setting<Keybind> activationKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("activation-key")
        .description("Key to activate the wheel picker.")
        .defaultValue(Keybind.fromKey(GLFW.GLFW_KEY_V))
        .build());

    private final Setting<Integer> wheelRadius = sgGeneral.add(new IntSetting.Builder()
        .name("wheel-radius")
        .description("Radius of the wheel in pixels.")
        .defaultValue(100)
        .min(60)
        .max(10000)
        .sliderRange(60, 2000)
        .build());

    private final Setting<Integer> wheelX = sgGeneral.add(new IntSetting.Builder()
        .name("wheel-x-offset")
        .description("X offset from center of screen (negative = left, positive = right).")
        .defaultValue(0)
        .min(-100000)
        .max(10000)
        .sliderRange(-10000, 10000)
        .build());

    private final Setting<Integer> wheelY = sgGeneral.add(new IntSetting.Builder()
        .name("wheel-y-offset")
        .description("Y offset from center of screen (negative = up, positive = down).")
        .defaultValue(0)
        .min(-10000)
        .max(100000)
        .sliderRange(-10000, 10000)
        .build());

    // Slot configurations (8 slots total)
    private final SlotConfig[] slots = new SlotConfig[8];

    // Spam protection
    private final Setting<Boolean> spamProtection = sgSpam.add(new BoolSetting.Builder()
        .name("spam-protection")
        .description("Enable spam protection for messages.")
        .defaultValue(true)
        .build());

    private final Setting<Integer> messageDelay = sgSpam.add(new IntSetting.Builder()
        .name("message-delay")
        .description("Minimum delay between messages in milliseconds.")
        .defaultValue(1000)
        .min(100)
        .max(5000)
        .sliderRange(100, 5000)
        .visible(spamProtection::get)
        .build());

    private final Setting<Boolean> insertRandomBrackets = sgSpam.add(new BoolSetting.Builder()
        .name("insert-random-brackets")
        .description("Insert random text within [] brackets to bypass spam filters.")
        .defaultValue(true)
        .visible(spamProtection::get)
        .build());

    // Render settings
    private final Setting<SettingColor> backgroundColor = sgRender.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Background color of wheel sections.")
        .defaultValue(new SettingColor(40, 40, 40, 120))
        .build());

    private final Setting<SettingColor> selectedColor = sgRender.add(new ColorSetting.Builder()
        .name("selected-color")
        .description("Color of the selected section.")
        .defaultValue(new SettingColor(100, 200, 255, 160))
        .build());

    private final Setting<SettingColor> borderColor = sgRender.add(new ColorSetting.Builder()
        .name("border-color")
        .description("Border color between sections.")
        .defaultValue(new SettingColor(255, 255, 255, 100))
        .build());

    private final Setting<SettingColor> textColor = sgRender.add(new ColorSetting.Builder()
        .name("text-color")
        .description("Text color for labels.")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build());

    private final Setting<SettingColor> moduleActiveColor = sgRender.add(new ColorSetting.Builder()
        .name("module-active-color")
        .description("Text color for active modules.")
        .defaultValue(new SettingColor(100, 255, 100, 255))
        .build());

    // State variables
    private boolean wheelActive = false;
    private int selectedSlot = -1;
    private long lastMessageTime = 0;

    // Store initial mouse position when wheel opens
    private double initialMouseX = 0;
    private double initialMouseY = 0;

    // Store original mouse state
    private boolean wasGrabbed = false;

    private static final String RANDOM_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final String[] SLOT_NAMES = {
        "Top", "Top-Right", "Right", "Bottom-Right",
        "Bottom", "Bottom-Left", "Left", "Top-Left"
    };

    public WheelPicker() {
        super(Bep.CATEGORY, "wheel-picker", "GTA-style wheel menu for quick macros and actions.");

        // Initialize slot configurations
        for (int i = 0; i < 8; i++) {
            slots[i] = new SlotConfig(i);
        }
    }

    @Override
    public void onActivate() {
        wheelActive = false;
        selectedSlot = -1;
    }

    @Override
    public void onDeactivate() {
        wheelActive = false;
        selectedSlot = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Don't activate if any GUI is open
        if (mc.currentScreen != null) {
            wheelActive = false;
            return;
        }

        boolean keyPressed = activationKey.get().isPressed();

        if (keyPressed && !wheelActive) {
            wheelActive = true;
            selectedSlot = -1;
            // Store original mouse state
            wasGrabbed = mc.mouse.isCursorLocked();
            // Unlock cursor to show mouse
            if (wasGrabbed) {
                mc.mouse.unlockCursor();
            }

            // Set cursor position to center of screen (no offset)
            GLFW.glfwSetCursorPos(mc.getWindow().getHandle(),
                mc.getWindow().getWidth() / 2.0,
                mc.getWindow().getHeight() / 2.0);
            // Store center position as initial
            initialMouseX = mc.getWindow().getWidth() / 2.0;
            initialMouseY = mc.getWindow().getHeight() / 2.0;
            // Pause game input
            KeyBinding.unpressAll();
        } else if (!keyPressed && wheelActive) {
            // Execute action when key is released
            if (selectedSlot >= 0 && selectedSlot < 8) {
                executeSlotAction(selectedSlot);
            }
            // Restore mouse state
            if (wasGrabbed) {
                mc.mouse.lockCursor();
            }
            wheelActive = false;
        }

        if (wheelActive) {
            // Update selected slot based on mouse position
            updateSelectedSlot();
            // Keep input paused while wheel is active
            KeyBinding.unpressAll();
        }
    }

    private void updateSelectedSlot() {
        // Get scaled dimensions (same as rendering)
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        // Convert mouse position to scaled coordinates
        double mouseX = mc.mouse.getX() * scaledWidth / (double)mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * scaledHeight / (double)mc.getWindow().getHeight();

        // Calculate center WITHOUT offsets (mouse always uses screen center)
        double centerX = scaledWidth / 2.0;
        double centerY = scaledHeight / 2.0;

        // Calculate offset from center
        double deltaX = mouseX - centerX;
        double deltaY = mouseY - centerY;

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // If cursor is too close to center, no selection
        if (distance < 20) {
            selectedSlot = -1;
            return;
        }

        // Calculate angle and determine slot
        double angle = Math.atan2(deltaY, deltaX);
        double degrees = Math.toDegrees(angle);

        // Normalize to 0-360
        if (degrees < 0) degrees += 360;

        // Rotate so slot 0 (Top) starts at top
        degrees = (degrees + 90) % 360;

        // Calculate which slot (8 total slots, 45 degrees each)
        selectedSlot = (int)((degrees + 22.5) / 45) % 8;
    }

    private void executeSlotAction(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 8) return;

        SlotConfig slot = slots[slotIndex];
        MacroAction action = slot.action.get();

        if (action == MacroAction.NONE) return;

        switch (action) {
            case TOGGLE_MODULE:
                String moduleName = slot.moduleName.get();
                if (!moduleName.isEmpty()) {
                    Module module = Modules.get().get(moduleName);
                    if (module != null) {
                        module.toggle();
                        info(String.format("%s: %s", moduleName,
                            module.isActive() ? "§aENABLED" : "§cDISABLED"));
                    } else {
                        warning("Module not found: " + moduleName);
                    }
                }
                break;

            case SEND_MESSAGE:
                String message = slot.message.get();
                if (!message.isEmpty()) {
                    sendMessageWithProtection(message);
                }
                break;

            case RUN_COMMAND:
                String command = slot.command.get();
                if (!command.isEmpty()) {
                    // Apply random substitution to commands if spam protection is enabled
                    if (spamProtection.get()) {
                        command = applyRandomSubstitution(command);
                    }
                    mc.player.networkHandler.sendChatCommand(command);
                }
                break;
        }
    }

    private void sendMessageWithProtection(String message) {
        if (spamProtection.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMessageTime < messageDelay.get()) {
                warning("Message blocked by spam protection.");
                return;
            }
            lastMessageTime = currentTime;

            message = applyRandomSubstitution(message);

            // Add invisible character for anti-spam
            String[] invisibleChars = {"\u200B", "\u200C", "\u200D"};
            message += invisibleChars[ThreadLocalRandom.current().nextInt(invisibleChars.length)];
        }

        mc.player.networkHandler.sendChatMessage(message);
    }

    private String applyRandomSubstitution(String text) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        // Replace [RANDOM] with random alphanumeric string
        while (text.contains("[RANDOM]")) {
            String randomString = generateRandomString(5 + random.nextInt(4)); // 5-8 chars
            text = text.replaceFirst("\\[RANDOM\\]", randomString);
        }

        // Insert random text in brackets if enabled
        if (insertRandomBrackets.get()) {
            // Find all bracket pairs and replace their contents
            StringBuilder result = new StringBuilder();
            int lastEnd = 0;
            int bracketStart = text.indexOf('[');

            while (bracketStart != -1) {
                int bracketEnd = text.indexOf(']', bracketStart);
                if (bracketEnd != -1) {
                    // Add text before bracket
                    result.append(text.substring(lastEnd, bracketStart));
                    // Add bracket with random alphanumeric content
                    String randomString = generateRandomString(3 + random.nextInt(3)); // 3-5 chars
                    result.append("[").append(randomString).append("]");
                    lastEnd = bracketEnd + 1;
                    bracketStart = text.indexOf('[', lastEnd);
                } else {
                    break;
                }
            }
            // Add remaining text
            result.append(text.substring(lastEnd));
            text = result.toString();
        }

        return text;
    }

    private String generateRandomString(int length) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(random.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!wheelActive) return;

        DrawContext context = event.drawContext;

        // Get scaled dimensions
        int scaledWidth = mc.getWindow().getScaledWidth();
        int scaledHeight = mc.getWindow().getScaledHeight();

        // Calculate center in scaled coordinates with user-defined offsets
        int centerX = scaledWidth / 2 + wheelX.get();
        int centerY = scaledHeight / 2 + wheelY.get();

        // Enable blending for transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Use radius directly
        int radius = wheelRadius.get();

        // Draw wheel
        renderWheel(context, centerX, centerY, radius);

        RenderSystem.disableBlend();
    }

    private void renderWheel(DrawContext context, int centerX, int centerY, int radius) {
        // Draw background circle first
        drawFilledCircle(context, centerX, centerY, radius, backgroundColor.get());

        // Draw selected section on top of background
        if (selectedSlot >= 0 && selectedSlot < 8) {
            drawWheelSection(context, centerX, centerY, radius, selectedSlot, true);
        }

        // Draw labels on top
        for (int i = 0; i < 8; i++) {
            drawSectionLabel(context, centerX, centerY, radius, i);
        }
    }

    private void drawFilledCircle(DrawContext context, int centerX, int centerY, int radius, Color color) {
        // Draw a filled circle using horizontal lines
        for (int y = -radius; y <= radius; y++) {
            int width = (int)Math.sqrt(radius * radius - y * y);
            context.fill(centerX - width, centerY + y, centerX + width + 1, centerY + y + 1, color.getPacked());
        }
    }

    private void drawCircleOutline(DrawContext context, int centerX, int centerY, int radius, Color color, int thickness) {
        // Draw circle outline with specified thickness
        for (int t = 0; t < thickness; t++) {
            int r = radius - t;
            if (r <= 0) continue;

            int x = 0;
            int y = r;
            int d = 3 - 2 * r;

            while (y >= x) {
                // Draw 8 octants
                drawPixel(context, centerX + x, centerY + y, color.getPacked());
                drawPixel(context, centerX - x, centerY + y, color.getPacked());
                drawPixel(context, centerX + x, centerY - y, color.getPacked());
                drawPixel(context, centerX - x, centerY - y, color.getPacked());
                drawPixel(context, centerX + y, centerY + x, color.getPacked());
                drawPixel(context, centerX - y, centerY + x, color.getPacked());
                drawPixel(context, centerX + y, centerY - x, color.getPacked());
                drawPixel(context, centerX - y, centerY - x, color.getPacked());

                x++;
                if (d > 0) {
                    y--;
                    d = d + 4 * (x - y) + 10;
                } else {
                    d = d + 4 * x + 6;
                }
            }
        }
    }

    private void drawPixel(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 1, y + 1, color);
    }

    private void drawWheelSection(DrawContext context, int centerX, int centerY, int radius, int sectionIndex, boolean selected) {
        if (!selected) return; // Only draw if selected

        // Each section is 45 degrees
        // Section 0 (Top) starts at -112.5 degrees (which is -90 - 22.5)
        // This centers the top section vertically
        double startAngle = Math.toRadians(sectionIndex * 45 - 90 - 22.5);
        double endAngle = startAngle + Math.toRadians(45);

        Color color = selectedColor.get();

        // Draw the pie slice as triangles with more segments for smoother rendering
        int segments = 45;
        for (int i = 0; i < segments; i++) {
            double angle1 = startAngle + (endAngle - startAngle) * i / segments;
            double angle2 = startAngle + (endAngle - startAngle) * (i + 1) / segments;

            int x1 = centerX + (int)(Math.cos(angle1) * radius);
            int y1 = centerY + (int)(Math.sin(angle1) * radius);
            int x2 = centerX + (int)(Math.cos(angle2) * radius);
            int y2 = centerY + (int)(Math.sin(angle2) * radius);

            fillTriangle(context, centerX, centerY, x1, y1, x2, y2, color.getPacked());
        }
    }

    private void fillTriangle(DrawContext context, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
        // Simple triangle filling - find bounds
        int minY = Math.min(Math.min(y0, y1), y2);
        int maxY = Math.max(Math.max(y0, y1), y2);

        for (int y = minY; y <= maxY; y++) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;

            // Check each edge for intersection at this y
            // Edge 0-1
            if ((y0 <= y && y <= y1) || (y1 <= y && y <= y0)) {
                if (y1 != y0) {
                    int x = x0 + (x1 - x0) * (y - y0) / (y1 - y0);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }

            // Edge 1-2
            if ((y1 <= y && y <= y2) || (y2 <= y && y <= y1)) {
                if (y2 != y1) {
                    int x = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }

            // Edge 2-0
            if ((y2 <= y && y <= y0) || (y0 <= y && y <= y2)) {
                if (y0 != y2) {
                    int x = x2 + (x0 - x2) * (y - y2) / (y0 - y2);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }

            if (minX <= maxX) {
                context.fill(minX, y, maxX + 1, y + 1, color);
            }
        }
    }

    private void drawThickLine(DrawContext context, int x0, int y0, int x1, int y1, int color, int thickness) {
        // Draw a thick line by drawing multiple parallel lines
        double angle = Math.atan2(y1 - y0, x1 - x0);
        double perpAngle = angle + Math.PI / 2;

        for (int t = -thickness/2; t <= thickness/2; t++) {
            int offsetX = (int)(Math.cos(perpAngle) * t);
            int offsetY = (int)(Math.sin(perpAngle) * t);
            drawLine(context, x0 + offsetX, y0 + offsetY, x1 + offsetX, y1 + offsetY, color);
        }
    }

    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        // Simple Bresenham's line algorithm
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            context.fill(x, y, x + 1, y + 1, color);

            if (x == x1 && y == y1) break;

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private void drawSectionLabel(DrawContext context, int centerX, int centerY, int radius, int sectionIndex) {
        SlotConfig slot = slots[sectionIndex];
        String label = getSlotLabel(slot);
        if (label.isEmpty()) return;

        // Calculate the middle angle of this section
        double midAngle = Math.toRadians(sectionIndex * 45 - 90); // -90 to start from top

        // Position label at 2/3 of the radius
        int labelRadius = radius * 2 / 3;
        int labelX = centerX + (int)(Math.cos(midAngle) * labelRadius);
        int labelY = centerY + (int)(Math.sin(midAngle) * labelRadius);

        // Check if module is active for coloring
        boolean isModuleActive = false;
        if (slot.action.get() == MacroAction.TOGGLE_MODULE && !slot.moduleName.get().isEmpty()) {
            Module module = Modules.get().get(slot.moduleName.get());
            if (module != null) {
                isModuleActive = module.isActive();
            }
        }

        // Draw the label centered at the calculated position
        Color textColor = isModuleActive ? moduleActiveColor.get() : this.textColor.get();
        int textWidth = mc.textRenderer.getWidth(label);
        int textHeight = mc.textRenderer.fontHeight;

        context.drawText(mc.textRenderer, label,
            labelX - textWidth / 2,
            labelY - textHeight / 2,
            textColor.getPacked(), false);
    }


    private String getSlotLabel(SlotConfig slot) {
        MacroAction action = slot.action.get();
        if (action == MacroAction.NONE) return "";

        switch (action) {
            case TOGGLE_MODULE:
                String module = slot.moduleName.get();
                if (module.isEmpty()) return "";
                Module m = Modules.get().get(module);
                if (m != null) {
                    String state = m.isActive() ? " ✓" : "";
                    return (module.length() > 8 ? module.substring(0, 8) : module) + state;
                }
                return module.length() > 10 ? module.substring(0, 8) + ".." : module;

            case SEND_MESSAGE:
                String msg = slot.message.get();
                return msg.isEmpty() ? "" :
                    (msg.length() > 10 ? msg.substring(0, 8) + ".." : msg);

            case RUN_COMMAND:
                String cmd = slot.command.get();
                return cmd.isEmpty() ? "" :
                    "/" + (cmd.length() > 9 ? cmd.substring(0, 7) + ".." : cmd);

            default:
                return "";
        }
    }


    // Inner class for slot configuration
    private class SlotConfig {
        public final Setting<MacroAction> action;
        public final Setting<String> moduleName;
        public final Setting<String> message;
        public final Setting<String> command;

        public SlotConfig(int index) {
            String slotName = SLOT_NAMES[index];

            action = sgSlots.add(new EnumSetting.Builder<MacroAction>()
                .name(slotName.toLowerCase().replace("-", "") + "-action")
                .description("Action for " + slotName + " slot")
                .defaultValue(MacroAction.NONE)
                .build());

            moduleName = sgSlots.add(new StringSetting.Builder()
                .name(slotName.toLowerCase().replace("-", "") + "-module")
                .description("Module name for " + slotName)
                .defaultValue("")
                .visible(() -> action.get() == MacroAction.TOGGLE_MODULE)
                .build());

            message = sgSlots.add(new StringSetting.Builder()
                .name(slotName.toLowerCase().replace("-", "") + "-message")
                .description("Message for " + slotName + " (use [] or [RANDOM] for random text)")
                .defaultValue("")
                .visible(() -> action.get() == MacroAction.SEND_MESSAGE)
                .build());

            command = sgSlots.add(new StringSetting.Builder()
                .name(slotName.toLowerCase().replace("-", "") + "-command")
                .description("Command for " + slotName + " (without /)")
                .defaultValue("")
                .visible(() -> action.get() == MacroAction.RUN_COMMAND)
                .build());
        }
    }

    public enum MacroAction {
        NONE("None"),
        TOGGLE_MODULE("Toggle Module"),
        SEND_MESSAGE("Send Message"),
        RUN_COMMAND("Run Command");

        private final String name;

        MacroAction(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

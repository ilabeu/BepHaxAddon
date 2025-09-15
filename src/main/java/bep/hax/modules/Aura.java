package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import bep.hax.util.EntityUtil;
import bep.hax.util.Rotation;
import bep.hax.util.PVPRotationManager;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import bep.hax.modules.PVPModule;
import bep.hax.util.*;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * Attacks nearby entities automatically with advanced targeting and weapon management.
 */
public class Aura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General settings
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings hand when attacking.")
        .defaultValue(true)
        .build());

    private final Setting<TargetUtils.TargetMode> mode = sgGeneral.add(new EnumSetting.Builder<TargetUtils.TargetMode>()
        .name("mode")
        .description("Target selection mode.")
        .defaultValue(TargetUtils.TargetMode.SWITCH)
        .build());

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Attack range.")
        .defaultValue(4.5)
        .min(0)
        .max(6)
        .sliderMax(6)
        .build());

    private final Setting<Double> wallRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("wall-range")
        .description("Attack range through walls.")
        .defaultValue(3.0)
        .min(0)
        .max(6)
        .sliderMax(6)
        .build());

    private final Setting<Boolean> vanillaRange = sgGeneral.add(new BoolSetting.Builder()
        .name("vanilla-range")
        .description("Only attack within vanilla range.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> attackDelay = sgGeneral.add(new BoolSetting.Builder()
        .name("attack-delay")
        .description("Uses vanilla attack cooldown.")
        .defaultValue(true)
        .build());

    private final Setting<Double> attackSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("attack-speed")
        .description("Attack speed delay (higher = faster attacks like PVP).")
        .defaultValue(20.0)
        .min(1.0)
        .max(20)
        .sliderMax(20)
        .visible(() -> !attackDelay.get())
        .build());

    private final Setting<AutoSwap> autoSwap = sgGeneral.add(new EnumSetting.Builder<AutoSwap>()
        .name("auto-swap")
        .description("Automatically swaps to best weapon.")
        .defaultValue(AutoSwap.Normal)
        .build());

    private final Setting<Boolean> swordOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("sword-only")
        .description("Only attacks when holding a weapon.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> pauseOnEat = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-eat")
        .description("Pauses when eating or drinking.")
        .defaultValue(true)
        .build());

    // Rotation settings
    private final Setting<Boolean> rotate = sgRotation.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates towards targets.")
        .defaultValue(true)
        .build());

    private final Setting<RotationUtils.HitVector> hitVector = sgRotation.add(new EnumSetting.Builder<RotationUtils.HitVector>()
        .name("hit-vector")
        .description("Which part of the entity to aim for.")
        .defaultValue(RotationUtils.HitVector.TORSO)
        .visible(rotate::get)
        .build());

    private final Setting<Boolean> silentRotate = sgRotation.add(new BoolSetting.Builder()
        .name("silent-rotate")
        .description("Rotates silently server-side.")
        .defaultValue(false)
        .visible(rotate::get)
        .build());

    private final Setting<Boolean> yawStep = sgRotation.add(new BoolSetting.Builder()
        .name("yaw-step")
        .description("Limits rotation speed to avoid flags.")
        .defaultValue(false)
        .visible(rotate::get)
        .build());

    private final Setting<Integer> yawStepLimit = sgRotation.add(new IntSetting.Builder()
        .name("yaw-step-limit")
        .description("Maximum yaw rotation per tick.")
        .defaultValue(180)
        .min(1)
        .max(180)
        .sliderMax(180)
        .visible(() -> rotate.get() && yawStep.get())
        .build());

    // Targeting settings
    private final Setting<TargetUtils.Priority> priority = sgTargeting.add(new EnumSetting.Builder<TargetUtils.Priority>()
        .name("priority")
        .description("Target selection priority.")
        .defaultValue(TargetUtils.Priority.HEALTH)
        .build());

    private final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("Range to search for targets.")
        .defaultValue(6.0)
        .min(0)
        .max(10)
        .sliderMax(10)
        .build());

    private final Setting<Double> fov = sgTargeting.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Field of view for target selection.")
        .defaultValue(180.0)
        .min(0)
        .max(180)
        .sliderMax(180)
        .build());

    private final Setting<Boolean> players = sgTargeting.add(new BoolSetting.Builder()
        .name("players")
        .description("Target players.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> monsters = sgTargeting.add(new BoolSetting.Builder()
        .name("monsters")
        .description("Target monsters.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> neutrals = sgTargeting.add(new BoolSetting.Builder()
        .name("neutrals")
        .description("Target neutral mobs.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> animals = sgTargeting.add(new BoolSetting.Builder()
        .name("animals")
        .description("Target animals.")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> invisibles = sgTargeting.add(new BoolSetting.Builder()
        .name("invisibles")
        .description("Target invisible entities.")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> armorCheck = sgTargeting.add(new BoolSetting.Builder()
        .name("armor-check")
        .description("Only target entities with armor.")
        .defaultValue(false)
        .build());

    private final Setting<Integer> ticksExisted = sgTargeting.add(new IntSetting.Builder()
        .name("ticks-existed")
        .description("Minimum entity age in ticks.")
        .defaultValue(0)
        .min(0)
        .max(200)
        .sliderMax(200)
        .build());

    private final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
        .name("ignore-passive")
        .description("Ignores passive/neutral mobs when they are not aggressive.")
        .defaultValue(true)
        .build());

    // Render settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Renders target ESP.")
        .defaultValue(true)
        .build());

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build());

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Side color.")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .visible(render::get)
        .build());

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Line color.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(render::get)
        .build());

    // Internal state - matching PVP exactly
    private Entity entityTarget;
    private long lastAttackTime;
    private int oldSlot = -1;
    private boolean rotated = false;
    private float[] silentRotations;
    private PVPRotationManager rotationManager;

    // Timers matching PVP
    private long switchTimer = 0;
    private long autoSwapTimer = 0;

    public Aura() {
        super(Bep.CATEGORY, "aura", "Automatically attacks nearby entities.");
        rotationManager = PVPRotationManager.getInstance();
    }

    @Override
    public void onActivate() {
        entityTarget = null;
        lastAttackTime = 0;
        oldSlot = -1;
        rotated = false;
        silentRotations = null;
        switchTimer = System.currentTimeMillis();
        autoSwapTimer = System.currentTimeMillis();
    }

    @Override
    public void onDeactivate() {
        entityTarget = null;
        silentRotations = null;
        rotated = false;

        if (oldSlot != -1 && autoSwap.get() == AutoSwap.Silent) {
            mc.player.getInventory().selectedSlot = oldSlot;
            oldSlot = -1;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Skip if paused or using items
        if (pauseOnEat.get() && shouldPauseForEating()) {
            return;
        }

        // Skip if spectator
        if (mc.player.isSpectator()) {
            return;
        }

        final Vec3d eyepos = mc.player.getEyePos();

        // Update target using PVP's exact logic
        entityTarget = switch (mode.get()) {
            case SWITCH -> getAttackTarget(eyepos);
            case SINGLE -> {
                if (entityTarget == null || !entityTarget.isAlive() || !isInAttackRange(entityTarget, eyepos)) {
                    yield getAttackTarget(eyepos);
                }
                yield entityTarget;
            }
        };

        // Check target and swap delay - exactly like PVP
        if (entityTarget == null || !switchTimerPassed()) {
            silentRotations = null;
            return;
        }

        // Handle hotbar keys and using items - reset swap timer
        if ((mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) ||
            mc.options.attackKey.isPressed() || isHotbarKeysPressed()) {
            autoSwapTimer = System.currentTimeMillis();
        }

        int slot = getBestWeaponSlot();
        boolean silentSwapped = false;

        // Handle weapon swapping exactly like PVP
        if (!isHoldingWeapon() && slot != -1) {
            switch (autoSwap.get()) {
                case Normal -> {
                    if (autoSwapTimerPassed()) {
                        mc.player.getInventory().selectedSlot = slot;
                    }
                }
                case Silent -> {
                    if (oldSlot == -1) {
                        oldSlot = mc.player.getInventory().selectedSlot;
                    }
                    mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                    silentSwapped = true;
                }
            }
        }

        // Check if holding weapon (unless silent swap)
        if (!isHoldingWeapon() && autoSwap.get() != AutoSwap.Silent) {
            return;
        }

        // Handle rotation exactly like PVP
        if (rotate.get()) {
            handlePVPRotation();
        } else {
            rotated = true;
        }

        // Check rotation blocked, rotated, and in range - exactly like PVP
        if (isRotationBlocked() || (!rotated && rotate.get()) || !isInAttackRange(entityTarget, eyepos)) {
            // Sync inventory if silent swapped
            if (autoSwap.get() == AutoSwap.Silent && silentSwapped) {
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot != -1 ? oldSlot : mc.player.getInventory().selectedSlot));
            }
            return;
        }

        // Attack using PVP timing logic
        if (attackDelayConfig()) {
            handleAttackDelay();
        } else {
            handleAttackSpeed();
        }

        // Sync silent inventory
        if (autoSwap.get() == AutoSwap.Silent && silentSwapped) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot != -1 ? oldSlot : mc.player.getInventory().selectedSlot));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer = System.currentTimeMillis(); // Reset switch timer exactly like PVP
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get() || entityTarget == null) return;

        // Only render if we have weapon or silent swap
        if (!(isHoldingWeapon() || autoSwap.get() == AutoSwap.Silent)) return;

        // Calculate fade based on time since last attack
        long timeSinceAttack = System.currentTimeMillis() - lastAttackTime;
        float fade = 1.0f - MathHelper.clamp(timeSinceAttack / 1000.0f, 0.0f, 1.0f);

        SettingColor sideColorFaded = new SettingColor(sideColor.get());
        SettingColor lineColorFaded = new SettingColor(lineColor.get());
        sideColorFaded.a = (int) (sideColor.get().a * fade);
        lineColorFaded.a = (int) (lineColor.get().a * fade);

        event.renderer.box(entityTarget.getBoundingBox(), sideColorFaded, lineColorFaded, shapeMode.get(), 0);
    }

    // Helper methods matching PVP exactly
    private boolean switchTimerPassed() {
        // Default swap delay of 0ms (instant) like PVP default
        return System.currentTimeMillis() - switchTimer >= 0;
    }

    private boolean autoSwapTimerPassed() {
        return System.currentTimeMillis() - autoSwapTimer >= 500; // 500ms like PVP
    }

    private boolean isHotbarKeysPressed() {
        // Check if any hotbar keys are pressed
        for (int i = 0; i < 9; i++) {
            if (mc.options.hotbarKeys[i].isPressed()) {
                return true;
            }
        }
        return false;
    }

    // Target finding - exactly like PVP
    private Entity getAttackTarget(Vec3d eyepos) {
        double min = Double.MAX_VALUE;
        Entity attackTarget = null;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null || entity == mc.player || !entity.isAlive() ||
                !isValidTarget(entity) || entity.age < ticksExisted.get()) {
                continue;
            }

            if (entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
                continue;
            }

            // Armor check exactly like PVP
            if (armorCheck.get() && entity instanceof LivingEntity livingEntity &&
                !livingEntity.getArmorItems().iterator().hasNext()) {
                continue;
            }

            double dist = eyepos.distanceTo(entity.getPos());
            if (dist <= targetRange.get()) {
                switch (priority.get()) {
                    case DISTANCE -> {
                        if (dist < min) {
                            min = dist;
                            attackTarget = entity;
                        }
                    }
                    case HEALTH -> {
                        if (entity instanceof LivingEntity living) {
                            float health = living.getHealth() + living.getAbsorptionAmount();
                            if (health < min) {
                                min = health;
                                attackTarget = entity;
                            }
                        }
                    }
                    case ARMOR -> {
                        if (entity instanceof LivingEntity living) {
                            float armor = TargetUtils.getArmorDurability(living);
                            if (armor < min) {
                                min = armor;
                                attackTarget = entity;
                            }
                        }
                    }
                }
            }
        }
        return attackTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive() || (entity.isInvisible() && !invisibles.get())) {
            return false;
        }

        // Check for non-aggressive passive/neutral mobs
        if (ignorePassive.get() && EntityUtil.isNeutral(entity)) {
            // Skip neutral mobs that are not currently aggressive
            if (!EntityUtil.isAggressive(entity)) {
                return false;
            }
        }

        if (entity instanceof PlayerEntity && players.get()) return true;

        // Use our EntityUtil for entity classification
        if (monsters.get() && EntityUtil.isMonster(entity)) return true;
        if (neutrals.get() && EntityUtil.isNeutral(entity)) return true;
        if (animals.get() && EntityUtil.isPassive(entity)) return true;

        return false;
    }

    // Range checking - exactly like PVP
    private boolean isInAttackRange(Entity entity, Vec3d eyepos) {
        Vec3d entityPos = getAttackRotateVec(entity);
        double dist = eyepos.distanceTo(entityPos);
        return isInAttackRange(dist, eyepos, entityPos);
    }

    private boolean isInAttackRange(double dist, Vec3d pos, Vec3d entityPos) {
        if (vanillaRange.get() && dist > 3.0f) {
            return false;
        }
        if (dist > range.get()) {
            return false;
        }

        BlockHitResult result = mc.world.raycast(new RaycastContext(
            pos, entityPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE, mc.player));

        if (result != null && !result.getBlockPos().equals(BlockPos.ofFloored(entityPos)) && dist > wallRange.get()) {
            return false;
        }

        if (fov.get() != 180.0f) {
            float[] rots = RotationUtils.getRotationsTo(pos, entityPos);
            float diff = MathHelper.wrapDegrees(mc.player.getYaw()) - rots[0];
            float magnitude = Math.abs(diff);
            return magnitude <= fov.get();
        }

        return true;
    }

    // Rotation handling - exactly like PVP
    private void handlePVPRotation() {
        if (entityTarget == null) {
            rotated = false;
            return;
        }

        float[] rotation = RotationUtils.getRotationsTo(mc.player.getEyePos(),
            getAttackRotateVec(entityTarget));

        if (!silentRotate.get() && yawStep.get()) {
            float serverYaw = rotationManager.getWrappedYaw();
            float diff = serverYaw - rotation[0];
            float diff1 = Math.abs(diff);
            if (diff1 > 180.0f) {
                diff += diff > 0.0f ? -360.0f : 360.0f;
            }
            int dir = diff > 0.0f ? -1 : 1;
            float deltaYaw = dir * yawStepLimit.get();
            float yaw;
            if (diff1 > yawStepLimit.get()) {
                yaw = serverYaw + deltaYaw;
                rotated = false;
            } else {
                yaw = rotation[0];
                rotated = true;
            }
            rotation[0] = yaw;
        } else {
            rotated = true;
        }

        if (silentRotate.get()) {
            silentRotations = rotation;
        } else {
            // Don't use rotation manager for client rotations, just set directly like PVP
            mc.player.setYaw(rotation[0]);
            mc.player.setPitch(MathHelper.clamp(rotation[1], -90.0f, 90.0f));
        }
    }

    private Vec3d getAttackRotateVec(Entity entity) {
        Vec3d feetPos = entity.getPos();
        return switch (hitVector.get()) {
            case FEET -> feetPos;
            case TORSO -> feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
            case EYES -> entity.getEyePos();
            case CLOSEST -> {
                Vec3d eyePos = mc.player.getEyePos();
                Vec3d torsoPos = feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
                Vec3d eyesPos = entity.getEyePos();

                double feetDist = eyePos.squaredDistanceTo(feetPos);
                double torsoDist = eyePos.squaredDistanceTo(torsoPos);
                double eyesDist = eyePos.squaredDistanceTo(eyesPos);

                if (feetDist <= torsoDist && feetDist <= eyesDist) {
                    yield feetPos;
                } else if (torsoDist <= eyesDist) {
                    yield torsoPos;
                } else {
                    yield eyesPos;
                }
            }
        };
    }

    // Weapon and attack detection
    private boolean isHoldingWeapon() {
        if (!swordOnly.get()) return true;

        ItemStack stack = mc.player.getMainHandStack();
        return stack.getItem() instanceof SwordItem ||
            stack.getItem() instanceof AxeItem ||
            stack.getItem() instanceof TridentItem ||
            stack.getItem() instanceof MaceItem;
    }

    private int getBestWeaponSlot() {
        float bestDamage = 0.0f;
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            MutableDouble damageMutable = new MutableDouble(0.0);
            AttributeModifiersComponent attributeModifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attributeModifiers != null) {
                attributeModifiers.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                    if (entry == EntityAttributes.ATTACK_DAMAGE) {
                        damageMutable.add(modifier.value());
                    }
                });
            }
            float damage = (float) damageMutable.doubleValue();
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean isRotationBlocked() {
        // Check if other modules are blocking rotations
        // For now, always return false since we don't have other modules that might block
        return false;
    }

    private boolean attackDelayConfig() {
        return attackDelay.get();
    }

    // Attack delay handling - exactly like PVP
    private void handleAttackDelay() {
        // Use the weapon slot calculated earlier, or current slot if weapon checking is disabled
        int weaponSlot = getBestWeaponSlot();
        int slotToUse = (weaponSlot == -1 || !swordOnly.get()) ? mc.player.getInventory().selectedSlot : weaponSlot;
        ItemStack weapon = mc.player.getInventory().getStack(slotToUse);

        MutableDouble attackSpeedAttr = new MutableDouble(
            mc.player.getAttributeBaseValue(EntityAttributes.ATTACK_SPEED));

        AttributeModifiersComponent attributeModifiers = weapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null) {
            attributeModifiers.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                if (entry.equals(EntityAttributes.ATTACK_SPEED)) {
                    attackSpeedAttr.add(modifier.value());
                }
            });
        }

        double attackCooldownTicks = 1.0 / attackSpeedAttr.getValue() * 20.0;

        // PVP's exact timing calculation with TPS compensation
        // Since we don't have TPS sync system, use simplified version but keep the same structure
        float ticks = 0.0f; // Would be: 20.0f - Managers.TICK.getTickSync(tpsSyncConfig.getValue());
        float currentTime = (System.currentTimeMillis() - lastAttackTime) + (ticks * 50.0f);

        if ((currentTime / 50.0f) >= attackCooldownTicks && attackTarget(entityTarget)) {
            lastAttackTime = System.currentTimeMillis();
        }
    }
    private void handleAttackSpeed() {
        // Match PVP's exact formula: attackSpeed * 50ms delay from 1000ms base
        float delay = (attackSpeed.get().floatValue() * 50.0f);
        long currentTime = System.currentTimeMillis() - lastAttackTime;

        // PVP uses 1000.0f - delay, meaning higher attackSpeed = faster attacks
        if (currentTime >= (1000.0f - delay) && attackTarget(entityTarget)) {
            lastAttackTime = System.currentTimeMillis();
        }
    }

    // Attack method - exactly like PVP
    private boolean attackTarget(Entity entity) {
        if (silentRotate.get() && silentRotations != null) {
            rotationManager.setRotationSilent(silentRotations[0], silentRotations[1]);
        }

        PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking());
        mc.getNetworkHandler().sendPacket(packet);

        if (swing.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }

        if (silentRotate.get()) {
            rotationManager.setRotationSilentSync();
        }

        return true;
    }

    public Entity getTarget() {
        return entityTarget;
    }

    private boolean shouldPauseForEating() {
        // Check if player is using food items
        if (mc.player.isUsingItem()) {
            ItemStack activeItem = mc.player.getActiveItem();
            if (activeItem.get(DataComponentTypes.FOOD) != null) {
                return true;
            }
        }
        
        // Use standard PlayerUtils pause conditions for other items
        return PlayerUtils.shouldPause(true, true, true);
    }

    public enum AutoSwap {
        Off,
        Normal,
        Silent
    }
}
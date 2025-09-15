package bep.hax.modules;

import bep.hax.Bep;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;
import java.util.List;

public class KillEffects extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();




    // General settings
    public final Setting<EffectType> effectType = sgGeneral.add(new EnumSetting.Builder<EffectType>()
        .name("effect-type")
        .description("The type of kill effect to display.")
        .defaultValue(EffectType.ENTITY)
        .build()
    );

    // Entity settings
    public final Setting<EntityEffectType> entityEffect = sgGeneral.add(new EnumSetting.Builder<EntityEffectType>()
        .name("entity-effect")
        .description("The entity effect to spawn.")
        .defaultValue(EntityEffectType.LIGHTNING_BOLT)
        .visible(() -> effectType.get() == EffectType.ENTITY)
        .build()
    );

    public final Setting<Integer> entityAmount = sgGeneral.add(new IntSetting.Builder()
        .name("entity-amount")
        .description("Number of entities to spawn.")
        .defaultValue(1)
        .range(1, 5)
        .sliderRange(1, 5)
        .visible(() -> effectType.get() == EffectType.ENTITY)
        .build()
    );

    // Particle settings


    public final Setting<List<ParticleType<?>>> particleTypes = sgGeneral.add(new ParticleTypeListSetting.Builder()
        .name("particle-types")
        .description("Types of particles to spawn.")
        .defaultValue(ParticleTypes.EXPLOSION)
        .visible(() -> effectType.get() == EffectType.PARTICLE)
        .build()
    );

    public final Setting<Integer> particleAmount = sgGeneral.add(new IntSetting.Builder()
        .name("particle-amount")
        .description("Number of particles to spawn.")
        .defaultValue(50)
        .range(10, 100)
        .sliderRange(10, 100)
        .visible(() -> effectType.get() == EffectType.PARTICLE)
        .build()
    );

    public final Setting<List<SoundEvent>> soundEvents = sgGeneral.add(new SoundEventListSetting.Builder()
        .name("sound-events")
        .description("Types of sounds to play. Only the first sound in the list will be played.")
        .defaultValue(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER)
        .visible(() -> effectType.get() == EffectType.PARTICLE)
        .build()
    );

    public final Setting<Integer> soundVolume = sgGeneral.add(new IntSetting.Builder()
        .name("sound-volume")
        .description("Volume of the sound.")
        .defaultValue(100)
        .range(0, 200)
        .sliderRange(0, 200)
        .visible(() -> effectType.get() == EffectType.PARTICLE && !soundEvents.get().isEmpty())
        .build()
    );

    // Entity filter settings
    private final SettingGroup sgEntityFilter = settings.createGroup("Entity Filter");

    public final Setting<Boolean> players = sgEntityFilter.add(new BoolSetting.Builder()
        .name("players")
        .description("Trigger effects on player deaths.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hostileMobs = sgEntityFilter.add(new BoolSetting.Builder()
        .name("hostile-mobs")
        .description("Trigger effects on hostile mob deaths.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> passiveMobs = sgEntityFilter.add(new BoolSetting.Builder()
        .name("passive-mobs")
        .description("Trigger effects on passive mob deaths.")
        .defaultValue(false)
        .build()
    );

    public KillEffects() {
        super(Bep.CATEGORY, "kill-effects", "Displays effects when entities die.");
    }

    @EventHandler
    private void onPacketReceive(meteordevelopment.meteorclient.events.packets.PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;

        // Check for entity death
        if (packet.getStatus() == 3) { // Death status
            try {
                Entity entity = packet.getEntity(mc.world);
                if (entity != null && entity != mc.player && isValidEntity(entity)) {
                    triggerKillEffect(entity);
                }
            } catch (Exception e) {
                // Ignore any entity access errors
            }
        }
    }

    private void triggerKillEffect(Entity entity) {
        Vec3d pos = entity.getPos();

        switch (effectType.get()) {
            case ENTITY -> spawnEntityEffect(pos);
            case PARTICLE -> spawnParticleEffect(pos);
        }
    }

    private void spawnEntityEffect(Vec3d pos) {
        if (mc.world == null) return;

        switch (entityEffect.get()) {
            case LIGHTNING_BOLT -> {
                // Just use visual and sound effects instead of spawning actual entities
                // This avoids the entity tracking issues that cause crashes
                for (int i = 0; i < entityAmount.get(); i++) {
                    spawnLightningEffects(pos);
                }
            }
        }
    }
    
    private void spawnLightningEffects(Vec3d pos) {
        if (mc.world == null) return;
        
        // Visual lightning effect using particles
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        
        // Create vertical lightning bolt effect with particles
        for (int y = 0; y < 10; y++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (random.nextDouble() - 0.5) * 0.5;
            
            mc.world.addParticle(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                pos.x + offsetX, pos.y + y * 2, pos.z + offsetZ,
                0, 0, 0
            );
            
            mc.world.addParticle(
                net.minecraft.particle.ParticleTypes.END_ROD,
                pos.x + offsetX, pos.y + y * 2, pos.z + offsetZ,
                0, 0, 0
            );
        }
        
        // Additional spark particles around the strike point
        for (int j = 0; j < 20; j++) {
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 1.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;
            
            mc.world.addParticle(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                offsetX * 0.1, 0.1, offsetZ * 0.1
            );
        }
        
        // Play thunder sound
        mc.world.playSound(pos.x, pos.y, pos.z,
            net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
            net.minecraft.sound.SoundCategory.AMBIENT, 1.0f, 1.0f, false);
    }

    private void spawnParticleEffect(Vec3d pos) {
        if (mc.world == null) return;

        // Use ThreadLocalRandom instead of Math.random() to avoid threading issues
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        // Spawn particles if selected
        List<ParticleType<?>> selectedParticles = particleTypes.get();
        if (!selectedParticles.isEmpty()) {
            for (int i = 0; i < particleAmount.get(); i++) {
                ParticleType<?> particleType = selectedParticles.get(i % selectedParticles.size());
                double offsetX = (random.nextDouble() - 0.5) * 4.0;
                double offsetY = random.nextDouble() * 2.0;
                double offsetZ = (random.nextDouble() - 0.5) * 4.0;

                // Cast to ParticleEffect for addParticle
                if (particleType instanceof net.minecraft.particle.ParticleEffect particleEffect) {
                    mc.world.addParticle(
                        particleEffect,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0, 0
                    );
                }
            }
        }

        // Play sound if selected
        List<SoundEvent> selectedSounds = soundEvents.get();
        if (!selectedSounds.isEmpty()) {
            float volume = soundVolume.get() / 100.0f;
            SoundEvent sound = selectedSounds.get(0); // Play first selected sound
            mc.world.playSound(pos.x, pos.y, pos.z,
                sound,
                net.minecraft.sound.SoundCategory.AMBIENT, volume, 1.0f, false);
        }
    }

    private void spawnFallbackParticles(Vec3d pos) {
        // Use ThreadLocalRandom instead of Math.random() to avoid threading issues
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();

        for (int j = 0; j < 20; j++) {
            double offsetX = (random.nextDouble() - 0.5) * 4.0;
            double offsetY = random.nextDouble() * 8.0;
            double offsetZ = (random.nextDouble() - 0.5) * 4.0;

            mc.world.addParticle(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                0, 0, 0
            );
        }
    }

    private boolean isValidEntity(Entity entity) {
        if (entity == null) return false;

        try {
            // Check if entity type is enabled
            if (entity instanceof PlayerEntity) {
                return players.get();
            } else if (entity instanceof net.minecraft.entity.mob.HostileEntity) {
                return hostileMobs.get();
            } else if (entity instanceof net.minecraft.entity.passive.PassiveEntity &&
                       !(entity instanceof net.minecraft.entity.mob.HostileEntity)) {
                return passiveMobs.get();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public enum EffectType {
        ENTITY("Entity"),
        PARTICLE("Particle");

        private final String title;
        EffectType(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }

    public enum EntityEffectType {
        LIGHTNING_BOLT("Lightning Bolt");

        private final String title;
        EntityEffectType(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }




}

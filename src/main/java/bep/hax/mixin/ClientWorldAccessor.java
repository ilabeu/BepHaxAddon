package bep.hax.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for ClientWorld to play sounds manually
 */
@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {

    @Invoker("playSound")
    void hookPlaySound(double x, double y, double z, SoundEvent sound, SoundCategory category,
                      float volume, float pitch, boolean useDistance, long seed);
}

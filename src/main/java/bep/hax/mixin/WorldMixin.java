package bep.hax.mixin;
import net.minecraft.world.World;
import bep.hax.modules.AutoSmith;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sound.SoundCategory;
import bep.hax.modules.StashBrander;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess, AutoCloseable {
    @Inject(method = "playSoundAtBlockCenter", at = @At("HEAD"), cancellable = true)
    private void mixinPlaySoundAtBlockCenter(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        AutoSmith smith = modules.get(AutoSmith.class);
        StashBrander brander = modules.get(StashBrander.class);
        if (brander.isActive() && brander.shouldMute()) {
            if (sound == SoundEvents.BLOCK_ANVIL_USE || sound == SoundEvents.BLOCK_ANVIL_BREAK) ci.cancel();
        }
        if (smith.isActive() && smith.muteSmithy.get()) {
            if (sound == SoundEvents.BLOCK_SMITHING_TABLE_USE) ci.cancel();
        }
    }
}

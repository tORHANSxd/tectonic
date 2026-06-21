package dev.worldgen.tectonic.mixin;

import dev.worldgen.tectonic.Tectonic;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.world.level.biome.Biome$TemperatureModifier$2")
public abstract class TemperatureModifierMixin {
    @ModifyVariable(
        method = "modifyTemperature",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    public BlockPos tectonic$adjustSnowStart(BlockPos pos) {
        return pos == null ? null : pos.above(Tectonic.CONFIG.getState().general.snowStartOffset);
    }
}

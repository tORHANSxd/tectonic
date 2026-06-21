package dev.worldgen.tectonic.mixin;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin {
    @Inject(
        method = "getOrCreateOffsetList",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void tectonic$stopHeightDecreasingLogSpam(ShortList[] positions, int index, CallbackInfoReturnable<ShortList> cir) {
        if (index >= positions.length) {
            cir.setReturnValue(new ShortArrayList());
        }
    }
}

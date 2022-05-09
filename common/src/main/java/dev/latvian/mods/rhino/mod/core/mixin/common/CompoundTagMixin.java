package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CompoundTag.class, priority = 1001)
public abstract class CompoundTagMixin {
	@Shadow
	@RemapForJS("merge")
	public abstract CompoundTag merge(CompoundTag tag);
}

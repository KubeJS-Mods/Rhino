package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.CompoundTagWrapper;
import dev.latvian.mods.rhino.util.CustomJavaObjectWrapper;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(value = CompoundTag.class, priority = 1001)
public abstract class CompoundTagMixin implements CustomJavaObjectWrapper.AsMap {
	@Shadow
	@Final
	private Map<String, Tag> tags;

	@Override
	public Map<?, ?> wrapAsJavaMap() {
		return new CompoundTagWrapper((CompoundTag) (Object) this, tags);
	}

	@Shadow
	@RemapForJS("merge")
	public abstract CompoundTag merge(CompoundTag tag);
}

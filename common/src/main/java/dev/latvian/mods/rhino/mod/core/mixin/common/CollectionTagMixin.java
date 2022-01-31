package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.util.CollectionTagWrapper;
import dev.latvian.mods.rhino.util.CustomJavaObjectWrapper;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(CollectionTag.class)
public abstract class CollectionTagMixin<T extends Tag> implements CustomJavaObjectWrapper.AsList {
	@Override
	public List<?> wrapAsJavaList() {
		return new CollectionTagWrapper((CollectionTag<Tag>) (Object) this);
	}
}

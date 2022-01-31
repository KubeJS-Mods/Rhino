package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import dev.latvian.mods.rhino.util.ListLike;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CollectionTag.class)
public abstract class CollectionTagMixin<T extends Tag> implements ListLike<T> {
	public CollectionTag<T> getCollectionLL() {
		return (CollectionTag<T>) (Object) this;
	}

	@Nullable
	@Override
	public T getLL(int index) {
		return (T) NBTWrapper.fromTag(getCollectionLL().get(index));
	}

	@Override
	public int sizeLL() {
		return getCollectionLL().size();
	}

	@Override
	public boolean addLL(T value) {
		return getCollectionLL().add((T) NBTWrapper.toTag(value));
	}

	@Override
	public void addLL(int index, T value) {
		getCollectionLL().add(index, (T) NBTWrapper.toTag(value));
	}

	@Override
	public T setLL(int index, T value) {
		return getCollectionLL().set(index, (T) NBTWrapper.toTag(value));
	}

	@Override
	public T removeLL(int index) {
		return getCollectionLL().remove(index);
	}

	@Override
	public void clearLL() {
		getCollectionLL().clear();
	}
}

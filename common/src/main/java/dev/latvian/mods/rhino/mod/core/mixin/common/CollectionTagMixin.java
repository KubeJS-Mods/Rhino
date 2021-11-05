package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import dev.latvian.mods.rhino.util.ListLike;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CollectionTag.class)
public abstract class CollectionTagMixin implements ListLike<Object> {
	@Nullable
	@Override
	public Object getLL(int index) {
		return NBTWrapper.fromTag((Tag) ((CollectionTag) (Object) this).get(index));
	}

	@Override
	public void setLL(int index, Object value) {
		((CollectionTag) (Object) this).set(index, NBTWrapper.toTag(value));
	}

	@Override
	public int sizeLL() {
		return ((CollectionTag) (Object) this).size();
	}

	@Override
	public void removeLL(int index) {
		((CollectionTag) (Object) this).remove(index);
	}

	@Override
	public void clearLL() {
		((CollectionTag) (Object) this).clear();
	}
}

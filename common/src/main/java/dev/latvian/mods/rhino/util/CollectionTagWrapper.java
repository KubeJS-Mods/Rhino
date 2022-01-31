package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;

public class CollectionTagWrapper extends AbstractList<Object> implements Wrapper {
	private final CollectionTag<Tag> tags;

	public CollectionTagWrapper(CollectionTag<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public Object unwrap() {
		return tags;
	}

	@Nullable
	@Override
	public Object get(int index) {
		return NBTWrapper.fromTag(tags.get(index));
	}

	@Override
	public int size() {
		return tags.size();
	}

	@Override
	public boolean add(Object value) {
		return tags.add(NBTWrapper.toTag(value));
	}

	@Override
	public void add(int index, Object value) {
		tags.add(index, NBTWrapper.toTag(value));
	}

	@Override
	public Object set(int index, Object value) {
		return NBTWrapper.fromTag(tags.set(index, NBTWrapper.toTag(value)));
	}

	@Override
	public Object remove(int index) {
		return NBTWrapper.fromTag(tags.remove(index));
	}

	@Override
	public boolean remove(Object value) {
		return tags.remove(NBTWrapper.toTag(value));
	}

	@Override
	public void clear() {
		tags.clear();
	}
}

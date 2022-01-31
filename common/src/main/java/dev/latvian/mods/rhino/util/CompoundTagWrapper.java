package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Wrapper;
import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class CompoundTagWrapper extends AbstractMap<String, Object> implements Wrapper {
	private final CompoundTag compoundTag;
	private final Map<String, Tag> tags;

	public CompoundTagWrapper(CompoundTag compoundTag, Map<String, Tag> tags) {
		this.compoundTag = compoundTag;
		this.tags = tags;
	}

	@Override
	public Object unwrap() {
		return compoundTag;
	}

	@Override
	public Object get(Object key) {
		return NBTWrapper.fromTag(tags.get(key));
	}

	@Override
	public boolean containsKey(Object key) {
		return tags.containsKey(key);
	}

	@Override
	public Object put(String key, Object v) {
		Tag t = NBTWrapper.toTag(v);

		if (t == null) {
			return NBTWrapper.fromTag(tags.remove(key));
		} else {
			return NBTWrapper.fromTag(tags.put(key, t));
		}
	}

	@Override
	public Set<String> keySet() {
		return tags.keySet();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
		Set<Map.Entry<String, Object>> set = new LinkedHashSet<>();

		for (Map.Entry<String, Tag> entry : tags.entrySet()) {
			set.add(new AbstractMap.SimpleEntry<>(entry.getKey(), NBTWrapper.fromTag(entry.getValue())));
		}

		return set;
	}

	@Override
	public Object remove(Object key) {
		return NBTWrapper.fromTag(tags.remove(key));
	}

	@Override
	public void clear() {
		tags.clear();
	}
}

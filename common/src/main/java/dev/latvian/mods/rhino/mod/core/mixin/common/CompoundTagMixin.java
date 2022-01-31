package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import dev.latvian.mods.rhino.util.MapLike;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mixin(value = CompoundTag.class, priority = 1001)
public abstract class CompoundTagMixin implements MapLike<String, Object> {
	@Shadow
	@Final
	private Map<String, Tag> tags;

	@Override
	public Object getML(Object key) {
		return NBTWrapper.fromTag(tags.get(key));
	}

	@Override
	public boolean containsKeyML(Object key) {
		return tags.containsKey(key);
	}

	@Override
	public Object putML(String key, Object v) {
		Tag t = NBTWrapper.toTag(v);

		if (t == null) {
			return tags.remove(key);
		} else {
			return tags.put(key, t);
		}
	}

	@Override
	public Set<String> keysML() {
		return tags.keySet();
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySetML() {
		Set<Map.Entry<String, Object>> set = new LinkedHashSet<>();

		for (Map.Entry<String, Tag> entry : tags.entrySet()) {
			set.add(new AbstractMap.SimpleEntry<>(entry.getKey(), NBTWrapper.fromTag(entry.getValue())));
		}

		return set;
	}

	@Override
	public Object removeML(Object key) {
		return tags.remove(key);
	}

	@Override
	public void clearML() {
		tags.clear();
	}

	@Shadow
	@RemapForJS("merge")
	public abstract CompoundTag merge(CompoundTag tag);
}

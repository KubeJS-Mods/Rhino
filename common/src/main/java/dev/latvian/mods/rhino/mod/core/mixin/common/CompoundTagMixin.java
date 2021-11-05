package dev.latvian.mods.rhino.mod.core.mixin.common;

import dev.latvian.mods.rhino.mod.util.NBTWrapper;
import dev.latvian.mods.rhino.util.MapLike;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Map;

@Mixin(CompoundTag.class)
public abstract class CompoundTagMixin implements MapLike<String, Object> {
	@Shadow
	@Final
	private Map<String, Tag> tags;

	@Override
	public Object getML(String key) {
		return NBTWrapper.fromTag(tags.get(key));
	}

	@Override
	public boolean containsKeyML(String key) {
		return tags.containsKey(key);
	}

	@Override
	public void putML(String key, Object v) {
		Tag t = NBTWrapper.toTag(v);

		if (t == null) {
			tags.remove(key);
		} else {
			tags.put(key, t);
		}
	}

	@Override
	public Collection<String> keysML() {
		return tags.keySet();
	}

	@Override
	public void removeML(String key) {
		tags.remove(key);
	}

	@Override
	public void clearML() {
		tags.clear();
	}

	@Shadow
	@RemapForJS("merge")
	public abstract CompoundTag merge(CompoundTag tag);
}

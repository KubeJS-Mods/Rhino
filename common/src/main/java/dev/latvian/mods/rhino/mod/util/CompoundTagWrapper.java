package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.SharedContextData;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record CompoundTagWrapper(CompoundTag tag) implements CustomJavaToJsWrapper {
	@Override
	public Scriptable convertJavaToJs(SharedContextData data, Scriptable scope, Class<?> staticType) {
		return new NativeJavaMap(data, scope, tag, NBTUtils.accessTagMap(tag), Tag.class, NBTUtils.VALUE_UNWRAPPER);
	}
}

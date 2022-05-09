package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public record CompoundTagWrapper(CompoundTag tag) implements CustomJavaToJsWrapper {
	@Override
	public Scriptable convertJavaToJs(Context cx, Scriptable scope, Class<?> staticType) {
		return new NativeJavaMap(scope, tag, tag.tags, Tag.class, NBTUtils.VALUE_UNWRAPPER);
	}
}

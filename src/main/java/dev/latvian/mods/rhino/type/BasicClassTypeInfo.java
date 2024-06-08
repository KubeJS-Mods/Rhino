package dev.latvian.mods.rhino.type;

import java.util.IdentityHashMap;
import java.util.Map;

public class BasicClassTypeInfo extends ClassTypeInfo {
	static final Map<Class<?>, BasicClassTypeInfo> CACHE = new IdentityHashMap<>();

	BasicClassTypeInfo(Class<?> type) {
		super(type);
	}
}

package dev.latvian.mods.rhino;

import java.util.IdentityHashMap;
import java.util.Map;

public class CachedClassStorage {
	public static final CachedClassStorage GLOBAL_PUBLIC = new CachedClassStorage(false);
	public static final CachedClassStorage GLOBAL_PROTECTED = new CachedClassStorage(true);

	private final Map<Class<?>, CachedClassInfo> map;
	public final CachedClassInfo objectClass;
	public final boolean includeProtected;

	public CachedClassStorage(boolean includeProtected) {
		this.map = new IdentityHashMap<>();
		this.objectClass = new CachedClassInfo(this, Object.class);
		this.includeProtected = includeProtected;
	}

	public synchronized CachedClassInfo get(Class<?> type) {
		if (type == null || type == Object.class) {
			return objectClass;
		}

		return map.computeIfAbsent(type, c -> new CachedClassInfo(this, c));
	}

	public String getDebugClassName(Class<?> type) {
		var name = type.getName();

		if (name.startsWith("java.lang.") || name.startsWith("java.util.")) {
			return name.substring(10);
		} else if (name.startsWith("java.function.")) {
			return name.substring(14);
		} else {
			return name;
		}
	}
}

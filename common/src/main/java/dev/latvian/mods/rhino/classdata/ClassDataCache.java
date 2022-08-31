package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.SharedContextData;

import java.util.HashMap;
import java.util.Map;

public class ClassDataCache {
	public final SharedContextData data;
	final ClassData objectClassData;
	private final Object lock;
	private final Map<Class<?>, ClassData> cache;
	private final ClassData arrayClassData;
	private final ClassData classClassData;

	public ClassDataCache(SharedContextData d) {
		data = d;
		lock = new Object();
		cache = new HashMap<>();
		objectClassData = new ClassData(this, Object.class);
		arrayClassData = new ClassData(this, Object[].class);
		classClassData = new ClassData(this, Class.class);
	}

	public ClassData of(Class<?> c) {
		if (c == null || c == Object.class) {
			return objectClassData;
		} else if (c == Class.class) {
			return classClassData;
		} else if (c.isArray()) {
			return arrayClassData;
		}

		synchronized (lock) {
			ClassData d = cache.get(c);

			if (d == null) {
				d = new ClassData(this, c);
				cache.put(c, d);
			}

			return d;
		}
	}
}

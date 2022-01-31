package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface MapLike<K, V> extends CustomJavaObjectWrapper {
	@Override
	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
		return new NativeJavaMap(scope, new MapLikeWrapper<>(this));
	}

	@Nullable
	V getML(Object key);

	default boolean containsKeyML(Object key) {
		return getML(key) != null;
	}

	default V putML(K key, V value) {
		throw new UnsupportedOperationException("Can't put values in this map!");
	}

	default Set<K> keysML() {
		return Collections.emptySet();
	}

	default Set<Map.Entry<K, V>> entrySetML() {
		Set<Map.Entry<K, V>> set = new HashSet<>();

		for (K key : keysML()) {
			set.add(new AbstractMap.SimpleEntry<>(key, getML(key)));
		}

		return set;
	}

	default V removeML(Object key) {
		throw new UnsupportedOperationException("Can't remove values from this map!");
	}

	default void clearML() {
		for (K key : new ArrayList<>(keysML())) {
			removeML(key);
		}
	}
}

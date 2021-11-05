package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMapLike;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface MapLike<K, T> extends CustomJavaObjectWrapper {
	@Override
	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
		return new NativeJavaMapLike(scope, this);
	}

	@Nullable
	T getML(K key);

	default boolean containsKeyML(K key) {
		return getML(key) != null;
	}

	default void putML(K key, T value) {
		throw new UnsupportedOperationException("Can't insert values in this map!");
	}

	default Collection<K> keysML() {
		return Collections.emptySet();
	}

	default void removeML(K key) {
		throw new UnsupportedOperationException("Can't delete values from this map!");
	}

	default void clearML() {
		throw new UnsupportedOperationException("Can't clear this map!");
	}
}

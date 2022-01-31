package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaList;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

public interface ListLike<T> extends CustomJavaObjectWrapper {
	@Override
	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
		return new NativeJavaList(scope, new ListLikeWrapper<>(this));
	}

	@Nullable
	T getLL(int index);

	int sizeLL();

	default boolean addLL(T value) {
		throw new UnsupportedOperationException("Can't add values in this list!");
	}

	default void addLL(int index, T value) {
		throw new UnsupportedOperationException("Can't add values in this list!");
	}

	default T setLL(int index, T value) {
		throw new UnsupportedOperationException("Can't set values in this list!");
	}

	default T removeLL(int index) {
		throw new UnsupportedOperationException("Can't remove values from this list!");
	}

	default void clearLL() {
		throw new UnsupportedOperationException("Can't clear this list!");
	}
}

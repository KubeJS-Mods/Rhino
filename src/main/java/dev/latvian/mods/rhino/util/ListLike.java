package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaListLike;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.Nullable;

public interface ListLike<T> extends CustomJavaObjectWrapper {
	@Override
	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
		return new NativeJavaListLike(scope, this);
	}

	@Nullable
	T getLL(int index);

	default void setLL(int index, T value) {
		throw new UnsupportedOperationException("Can't insert values in this map!");
	}

	int sizeLL();
}
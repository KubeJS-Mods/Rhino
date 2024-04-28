package dev.latvian.mods.rhino;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Executable;

public interface WrappedExecutable {
	Object invoke(Context cx, Scriptable scope, Object self, Object[] args) throws Exception;

	default Object construct(Context cx, Scriptable scope, Object[] args) throws Exception {
		throw new UnsupportedOperationException();
	}

	default boolean isStatic() {
		return false;
	}

	default Class<?> getReturnType() {
		return Void.TYPE;
	}

	@Nullable
	default Executable unwrap() {
		return null;
	}
}

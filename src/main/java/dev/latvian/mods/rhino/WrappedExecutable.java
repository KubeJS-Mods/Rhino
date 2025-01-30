package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

public interface WrappedExecutable {
	Object invoke(Context cx, Scriptable scope, Object self, Object[] args) throws Throwable;

	default Object construct(Context cx, Scriptable scope, Object[] args) throws Throwable {
		throw new UnsupportedOperationException();
	}

	default boolean isStatic() {
		return false;
	}

	default TypeInfo getReturnType() {
		return TypeInfo.PRIMITIVE_VOID;
	}

	@Nullable
	default CachedExecutableInfo unwrap() {
		return null;
	}
}

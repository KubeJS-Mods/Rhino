package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.CachedExecutableInfo;
import dev.latvian.mods.rhino.CachedMethodInfo;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.WrappedExecutable;
import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

public record WrappedReflectionMethod(CachedMethodInfo method) implements WrappedExecutable {
	public static WrappedExecutable of(@Nullable CachedMethodInfo method) {
		return method == null ? null : new WrappedReflectionMethod(method);
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, Object self, Object[] args) throws Throwable {
		return method.invoke(cx, scope, self, args);
	}

	@Override
	public boolean isStatic() {
		return method.isStatic;
	}

	@Override
	public TypeInfo getReturnType() {
		return method.getReturnType();
	}

	@Override
	@Nullable
	public CachedExecutableInfo unwrap() {
		return method;
	}
}

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class CachedMethodInfo extends CachedExecutableInfo {
	public static class Accessible {
		CachedMethodInfo info;
		MethodSignature signature;
		String name = "";
		boolean hidden = false;

		public CachedMethodInfo getInfo() {
			return info;
		}

		public MethodSignature getSignature() {
			return signature;
		}

		public String getName() {
			return name;
		}

		boolean isHidden() {
			return hidden;
		}
	}

	final Method method;
	private TypeInfo returnType;
	protected MethodHandle methodHandle;

	public CachedMethodInfo(CachedClassInfo parent, Method m) {
		super(parent, m);
		this.method = m;
	}

	@Override
	public TypeInfo getReturnType() {
		if (returnType == null) {
			returnType = TypeInfo.safeOf(method::getGenericReturnType);
		}

		return returnType;
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object instance, Object... args) throws Throwable {
		var parameters = getParameters();

		// FIXME: Fix vararg method invocation
		if (parameters.isVarArg()) {
			if (parent.storage.includeProtected && Modifier.isProtected(modifiers) && !method.isAccessible()) {
				method.setAccessible(true);
			}

			return method.invoke(isStatic ? null : instance, transformArgs(cx, null, parameters, args));
		} else {
			var mh = methodHandle;

			if (mh == null) {
				if (parent.storage.includeProtected && Modifier.isProtected(modifiers) && !method.isAccessible()) {
					method.setAccessible(true);
				}

				mh = methodHandle = cx.factory.getMethodHandlesLookup().unreflect(method);
			}

			return mh.invokeWithArguments(transformArgs(cx, isStatic ? null : instance, parameters, args));
		}
	}
}

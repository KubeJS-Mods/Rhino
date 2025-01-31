package dev.latvian.mods.rhino;

import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;

public class CachedConstructorInfo extends CachedExecutableInfo {
	public static class Accessible {
		CachedConstructorInfo info;
		MethodSignature signature;
		String name = "";

		public CachedConstructorInfo getInfo() {
			return info;
		}

		public MethodSignature getSignature() {
			return signature;
		}

		public String getName() {
			return name;
		}
	}

	final Constructor<?> constructor;
	protected MethodHandle methodHandle;

	public CachedConstructorInfo(CachedClassInfo parent, Constructor<?> constructor) {
		super(parent, constructor);
		this.constructor = constructor;
	}

	@Override
	public Object invoke(Context cx, Scriptable scope, @Nullable Object instance, Object... args) throws Throwable {
		var parameters = getParameters();

		if (parameters.isVarArg()) {
			if (!constructor.isAccessible()) {
				constructor.setAccessible(true);
			}

			// FIXME: Fix vararg method invocation
			return constructor.newInstance(transformArgs(cx, null, parameters, args));
		} else {
			var mh = methodHandle;

			if (mh == null) {
				mh = methodHandle = cx.factory.getMethodHandlesLookup().unreflectConstructor(constructor);
			}

			return mh.invokeWithArguments(transformArgs(cx, null, parameters, args));
		}
	}
}

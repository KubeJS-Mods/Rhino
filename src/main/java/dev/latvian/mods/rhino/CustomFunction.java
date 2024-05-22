package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;

public class CustomFunction extends BaseFunction {
	private final String functionName;
	private final Func func;
	private final TypeInfo[] argTypes;

	public CustomFunction(String functionName, Func func, TypeInfo[] argTypes) {
		this.functionName = functionName;
		this.func = func;
		this.argTypes = argTypes.length == 0 ? TypeInfo.EMPTY_ARRAY : argTypes;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// First, we marshall the args.
		Object[] origArgs = args;
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			Object coerced = cx.jsToJava(arg, argTypes[i]);

			if (coerced != arg) {
				if (origArgs == args) {
					args = args.clone();
				}
				args[i] = coerced;
			}
		}

		return func.call(cx, args);
	}

	@FunctionalInterface
	public interface Func {
		Object call(Context cx, Object[] args);
	}

	@FunctionalInterface
	public interface NoArgFunc extends Func {
		Object call(Context cx);

		@Override
		default Object call(Context cx, Object[] args) {
			return call(cx);
		}
	}
}

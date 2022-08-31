package dev.latvian.mods.rhino;

public class CustomFunction extends BaseFunction {
	public static final Class<?>[] NO_ARGS = new Class<?>[0];
	private final String functionName;
	private final Func func;
	private final Class<?>[] argTypes;
	public CustomFunction(String functionName, Func func, Class<?>[] argTypes) {
		this.functionName = functionName;
		this.func = func;
		this.argTypes = argTypes;
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		SharedContextData data = SharedContextData.get(cx, scope);
		// First, we marshall the args.
		Object[] origArgs = args;
		for (int i = 0; i < args.length; i++) {
			Object arg = args[i];
			Object coerced = Context.jsToJava(data, arg, argTypes[i]);

			if (coerced != arg) {
				if (origArgs == args) {
					args = args.clone();
				}
				args[i] = coerced;
			}
		}

		Object retval = func.call(args);

		if (retval == null) {
			return Undefined.instance;
		}

		SharedContextData contextData = SharedContextData.get(cx, scope);
		Object wrapped = contextData.getWrapFactory().wrap(contextData, scope, retval, retval.getClass());

		if (wrapped == null) {
			wrapped = Undefined.instance;
		}
		return wrapped;
	}

	@FunctionalInterface
	public interface Func {
		Object call(Object[] args);
	}

	@FunctionalInterface
	public interface NoArgFunc extends Func {
		Object call();

		@Override
		default Object call(Object[] args) {
			return call();
		}
	}
}

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.util.List;

public class CachedExecutableInfo extends CachedMemberInfo {
	final Executable executable;
	private MethodSignature signature;
	private final int parameterCount;
	private CachedParameters parameters;

	public CachedExecutableInfo(CachedClassInfo parent, Executable e) {
		super(parent, e, e.getName(), (e instanceof Constructor<?> ? Modifier.STATIC : 0) | e.getModifiers());
		this.executable = e;
		this.parameterCount = e.getParameterCount();
	}

	@Override
	public Executable getCached() {
		return executable;
	}

	public MethodSignature getSignature() {
		if (signature == null) {
			signature = new MethodSignature(executable);
		}

		return signature;
	}

	public TypeInfo getReturnType() {
		return getDeclaringClass().getTypeInfo();
	}

	public CachedParameters getParameters() {
		var v = parameters;

		if (v == null) {
			var tc = executable.getParameterTypes();
			var ti = TypeInfo.safeOfArray(executable::getGenericParameterTypes);
			boolean fcx = tc.length > 0 && Context.class.isAssignableFrom(tc[0]);

			if (fcx) {
				var ntc = new Class<?>[tc.length - 1];
				System.arraycopy(tc, 1, ntc, 0, ntc.length);
				tc = ntc;

				var nti = new TypeInfo[ti.length - 1];
				System.arraycopy(ti, 1, nti, 0, nti.length);
				ti = nti;
			}

			boolean varArgs = executable.isVarArgs();

			if (fcx && ti.length == 0 && !varArgs) {
				v = parameters = CachedParameters.EMPTY_FIRST_CX;
			} else if (ti.length == 0 && !varArgs) {
				v = parameters = CachedParameters.EMPTY;
			} else {
				v = parameters = new CachedParameters(tc.length, List.of(tc), List.of(ti), fcx, varArgs ? ti[ti.length - 1].componentType() : null);
			}
		}

		return v;
	}

	public Object[] transformArgs(Context cx, @Nullable Object instance, CachedParameters parameters, Object[] args) {
		Object[] iargs = args;

		var fcx = parameters.firstArgContext();
		int off = (instance != null ? 1 : 0) + (fcx ? 1 : 0);

		if (off > 0) {
			iargs = new Object[args.length + off];
			int pos = 0;

			if (instance != null) {
				iargs[pos++] = instance;
			}

			if (fcx) {
				iargs[pos] = cx;
			}

			System.arraycopy(args, 0, iargs, off, args.length);
		}

		return iargs;
	}

	public Object invoke(Context cx, Scriptable scope, @Nullable Object instance, Object[] args) throws Throwable {
		return null;
	}

	@Override
	public String toString() {
		return parent.getTypeInfo() + "#" + originalName + "(" + ".".repeat(parameterCount) + ")";
	}

	public void appendDebugParams(StringBuilder builder) {
		builder.append('(');

		var params = getParameters();

		for (int i = 0; i < params.count(); i++) {
			if (i > 0) {
				builder.append(", ");
			}

			var type = params.types().get(i);

			if (params.isVarArg() && i == params.count() - 1 && type.isArray()) {
				parent.storage.get(type.componentType()).appendDebugType(builder);
				builder.append("...");
			} else {
				parent.storage.get(type).appendDebugType(builder);
			}
		}

		builder.append(')');
	}
}

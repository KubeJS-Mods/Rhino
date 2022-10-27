package dev.latvian.mods.rhino;

import java.lang.reflect.Field;

public class FieldAndMethods extends NativeJavaMethod {
	public transient Field field;
	public transient Object javaObject;

	FieldAndMethods(Scriptable scope, MemberBox[] methods, Field field) {
		super(methods);
		this.field = field;
		setParentScope(scope);
		setPrototype(getFunctionPrototype(scope));
	}

	@Override
	public Object getDefaultValue(Class<?> hint) {
		if (hint == ScriptRuntime.FunctionClass) {
			return this;
		}
		Object rval;
		Class<?> type;
		try {
			rval = field.get(javaObject);
			type = field.getType();
		} catch (IllegalAccessException accEx) {
			throw Context.reportRuntimeError1("msg.java.internal.private", field.getName());
		}
		SharedContextData data = SharedContextData.get(getParentScope());
		rval = data.getWrapFactory().wrap(data, this, rval, type);
		if (rval instanceof Scriptable) {
			rval = ((Scriptable) rval).getDefaultValue(hint);
		}
		return rval;
	}
}

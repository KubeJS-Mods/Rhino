package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DefaultValueTypeHint;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class FieldAndMethods extends NativeJavaMethod {
	public transient Field field;
	public transient Object javaObject;

	FieldAndMethods(Scriptable scope, MemberBox[] methods, Field field, Context cx) {
		super(methods);
		this.field = field;
		setParentScope(scope);
		setPrototype(getFunctionPrototype(scope, cx));
	}

	@Override
	public Object getDefaultValue(Context cx, DefaultValueTypeHint hint) {
		if (hint == DefaultValueTypeHint.FUNCTION) {
			return this;
		}
		Object rval;
		Class<?> type;
		Type genericType;
		try {
			rval = field.get(javaObject);
			type = field.getType();
			genericType = field.getGenericType();
		} catch (IllegalAccessException accEx) {
			throw Context.reportRuntimeError1("msg.java.internal.private", field.getName(), cx);
		}
		rval = cx.wrap(this, rval, type, genericType);
		if (rval instanceof Scriptable) {
			rval = ((Scriptable) rval).getDefaultValue(cx, hint);
		}
		return rval;
	}
}

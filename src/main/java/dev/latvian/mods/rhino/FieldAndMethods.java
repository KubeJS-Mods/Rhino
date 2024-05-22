package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.DefaultValueTypeHint;

import java.lang.reflect.Field;

public class FieldAndMethods extends NativeJavaMethod {
	public transient Field field;
	public transient TypeInfo fieldType;
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
		try {
			rval = field.get(javaObject);
		} catch (IllegalAccessException accEx) {
			throw Context.reportRuntimeError1("msg.java.internal.private", field.getName(), cx);
		}

		if (fieldType == null) {
			this.fieldType = TypeInfo.of(field.getGenericType());
		}

		rval = cx.wrap(this, rval, fieldType);
		if (rval instanceof Scriptable) {
			rval = ((Scriptable) rval).getDefaultValue(cx, hint);
		}
		return rval;
	}
}

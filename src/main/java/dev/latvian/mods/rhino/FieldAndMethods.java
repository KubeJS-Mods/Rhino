package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DefaultValueTypeHint;

public class FieldAndMethods extends NativeJavaMethod {
	public transient CachedFieldInfo fieldInfo;
	public transient Object javaObject;

	FieldAndMethods(Scriptable scope, MemberBox[] methods, CachedFieldInfo fieldInfo, Context cx) {
		super(methods);
		this.fieldInfo = fieldInfo;
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
			rval = fieldInfo.get(cx, javaObject);
		} catch (Throwable accEx) {
			throw Context.reportRuntimeError1("msg.java.internal.private", fieldInfo.rename, cx);
		}

		rval = cx.wrap(this, rval, fieldInfo.getType());
		if (rval instanceof Scriptable) {
			rval = ((Scriptable) rval).getDefaultValue(cx, hint);
		}
		return rval;
	}
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.lang.reflect.Array;

/**
 * This class reflects Java arrays into the JavaScript environment.
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 * @see NativeJavaObject
 */

public class NativeJavaArray extends NativeJavaObject implements SymbolScriptable {
	public static NativeJavaArray wrap(Scriptable scope, Object array, Context cx) {
		return new NativeJavaArray(scope, array, cx);
	}

	Object array;
	int length;
	Class<?> cls;

	public NativeJavaArray(Scriptable scope, Object array, Context cx) {
		super(scope, null, ScriptRuntime.ObjectClass, cx);
		Class<?> cl = array.getClass();
		if (!cl.isArray()) {
			throw new RuntimeException("Array expected");
		}
		this.array = array;
		this.length = Array.getLength(array);
		this.cls = cl.getComponentType();
	}

	@Override
	public String getClassName() {
		return "JavaArray";
	}

	@Override
	public Object unwrap() {
		return array;
	}

	@Override
	public boolean has(String id, Scriptable start, Context cx) {
		return id.equals("length") || super.has(id, start, cx);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return 0 <= index && index < length;
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return SymbolKey.IS_CONCAT_SPREADABLE.equals(key) || super.has(cx, key, start);
	}

	@Override
	public Object get(String id, Scriptable start, Context cx) {
		if (id.equals("length")) {
			return length;
		}
		Object result = super.get(id, start, cx);
		if (result == NOT_FOUND && !ScriptableObject.hasProperty(getPrototype(cx), id, cx)) {
			throw Context.reportRuntimeError2("msg.java.member.not.found", array.getClass().getName(), id, cx);
		}
		return result;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (0 <= index && index < length) {
			Object obj = Array.get(array, index);
			return cx.sharedContextData.getWrapFactory().wrap(cx, this, obj, cls);
		}
		return Undefined.instance;
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return Boolean.TRUE;
		}
		return super.get(cx, key, start);
	}

	@Override
	public void put(String id, Scriptable start, Object value, Context cx) {
		// Ignore assignments to "length"--it's readonly.
		if (!id.equals("length")) {
			throw Context.reportRuntimeError1("msg.java.array.member.not.found", id, cx);
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (0 <= index && index < length) {
			Array.set(array, index, Context.jsToJava(cx, value, cls));
		} else {
			throw Context.reportRuntimeError2("msg.java.array.index.out.of.bounds", String.valueOf(index), String.valueOf(length - 1), cx);
		}
	}

	@Override
	public void delete(Context cx, Symbol key) {
		// All symbols are read-only
	}

	@Override
	public Object getDefaultValue(Class<?> hint, Context cx) {
		if (hint == null || hint == ScriptRuntime.StringClass) {
			return array.toString();
		}
		if (hint == ScriptRuntime.BooleanClass) {
			return Boolean.TRUE;
		}
		if (hint == ScriptRuntime.NumberClass) {
			return ScriptRuntime.NaNobj;
		}
		return this;
	}

	@Override
	public Object[] getIds(Context cx) {
		Object[] result = new Object[length];
		int i = length;
		while (--i >= 0) {
			result[i] = i;
		}
		return result;
	}

	@Override
	public boolean hasInstance(Scriptable value, Context cx) {
		if (!(value instanceof Wrapper)) {
			return false;
		}
		Object instance = ((Wrapper) value).unwrap();
		return cls.isInstance(instance);
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		if (prototype == null) {
			prototype = ScriptableObject.getArrayPrototype(this.getParentScope(), cx);
		}
		return prototype;
	}
}

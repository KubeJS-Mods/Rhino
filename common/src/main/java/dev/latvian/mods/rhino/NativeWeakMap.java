/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakMap class. As per the spec, keys must be
 * ordinary objects. Since there is no defined "equality" for objects, comparisions
 * are done strictly by object equality. Both ES6 and the java.util.WeakHashMap class
 * have the same basic structure -- entries are removed automatically when the sole
 * remaining reference to the key is a weak reference. Therefore, we can use
 * WeakHashMap as the basis of this implementation and preserve the same semantics.
 */
public class NativeWeakMap extends IdScriptableObject {
	private static final Object MAP_TAG = "WeakMap";
	private static final Object NULL_VALUE = new Object();
	private static final int Id_constructor = 1;
	private static final int Id_delete = 2;
	private static final int Id_get = 3;
	private static final int Id_has = 4;
	private static final int Id_set = 5;
	private static final int SymbolId_toStringTag = 6;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeWeakMap m = new NativeWeakMap();
		m.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	private static NativeWeakMap realThis(Scriptable thisObj, IdFunctionObject f, Context cx) {
		if (thisObj == null) {
			throw incompatibleCallError(f, cx);
		}
		try {
			final NativeWeakMap nm = (NativeWeakMap) thisObj;
			if (!nm.instanceOfWeakMap) {
				// Check for "Map internal data tag"
				throw incompatibleCallError(f, cx);
			}
			return nm;
		} catch (ClassCastException cce) {
			throw incompatibleCallError(f, cx);
		}
	}

	private final transient WeakHashMap<Scriptable, Object> map = new WeakHashMap<>();
	private boolean instanceOfWeakMap = false;

	@Override
	public String getClassName() {
		return "WeakMap";
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

		if (!f.hasTag(MAP_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor:
				if (thisObj == null) {
					NativeWeakMap nm = new NativeWeakMap();
					nm.instanceOfWeakMap = true;
					if (args.length > 0) {
						NativeMap.loadFromIterable(cx, scope, nm, args[0]);
					}
					return nm;
				}
				throw ScriptRuntime.typeError1(cx, "msg.no.new", "WeakMap");
			case Id_delete:
				return realThis(thisObj, f, cx).js_delete(args.length > 0 ? args[0] : Undefined.instance);
			case Id_get:
				return realThis(thisObj, f, cx).js_get(args.length > 0 ? args[0] : Undefined.instance);
			case Id_has:
				return realThis(thisObj, f, cx).js_has(args.length > 0 ? args[0] : Undefined.instance);
			case Id_set:
				return realThis(thisObj, f, cx).js_set(args.length > 0 ? args[0] : Undefined.instance, args.length > 1 ? args[1] : Undefined.instance, cx);
		}
		throw new IllegalArgumentException("WeakMap.prototype has no method: " + f.getFunctionName());
	}

	private Object js_delete(Object key) {
		if (!ScriptRuntime.isObject(key)) {
			return Boolean.FALSE;
		}
		return map.remove(key) != null;
	}

	private Object js_get(Object key) {
		if (!ScriptRuntime.isObject(key)) {
			return Undefined.instance;
		}
		Object result = map.get(key);
		if (result == null) {
			return Undefined.instance;
		} else if (result == NULL_VALUE) {
			return null;
		}
		return result;
	}

	private Object js_has(Object key) {
		if (!ScriptRuntime.isObject(key)) {
			return Boolean.FALSE;
		}
		return map.containsKey(key);
	}

	private Object js_set(Object key, Object v, Context cx) {
		// As the spec says, only a true "Object" can be the key to a WeakMap.
		// Use the default object equality here. ScriptableObject does not override
		// equals or hashCode, which means that in effect we are only keying on object identity.
		// This is all correct according to the ECMAscript spec.
		if (!ScriptRuntime.isObject(key)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, key));
		}
		// Map.get() does not distinguish between "not found" and a null value. So,
		// replace true null here with a marker so that we can re-convert in "get".
		final Object value = (v == null ? NULL_VALUE : v);
		map.put((Scriptable) key, value);
		return this;
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		if (id == SymbolId_toStringTag) {
			initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG, getClassName(), DONTENUM | READONLY);
			return;
		}

		String s, fnName = null;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 0;
				s = "constructor";
			}
			case Id_delete -> {
				arity = 1;
				s = "delete";
			}
			case Id_get -> {
				arity = 1;
				s = "get";
			}
			case Id_has -> {
				arity = 1;
				s = "has";
			}
			case Id_set -> {
				arity = 2;
				s = "set";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(MAP_TAG, id, s, fnName, arity, cx);
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.TO_STRING_TAG.equals(k)) {
			return SymbolId_toStringTag;
		}
		return 0;
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "delete" -> Id_delete;
			case "get" -> Id_get;
			case "has" -> Id_has;
			case "set" -> Id_set;
			default -> 0;
		};
	}
}

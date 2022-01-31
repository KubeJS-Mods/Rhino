/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.WeakHashMap;

/**
 * This is an implementation of the ES6 WeakSet class. It is very similar to
 * NativeWeakMap, with the exception being that it doesn't store any values.
 * Java will GC the key only when there is no longer any reference to it other
 * than the weak reference. That means that it is important that the "value"
 * that we put in the WeakHashMap here is not one that contains the key.
 */
public class NativeWeakSet extends IdScriptableObject {
	@Serial
	private static final long serialVersionUID = 2065753364224029534L;

	private static final Object MAP_TAG = "WeakSet";

	private boolean instanceOfWeakSet = false;

	private transient WeakHashMap<Scriptable, Boolean> map = new WeakHashMap<>();

	static void init(Scriptable scope, boolean sealed) {
		NativeWeakSet m = new NativeWeakSet();
		m.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
	}

	@Override
	public String getClassName() {
		return "WeakSet";
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
					NativeWeakSet ns = new NativeWeakSet();
					ns.instanceOfWeakSet = true;
					if (args.length > 0) {
						NativeSet.loadFromIterable(cx, scope, ns, args[0]);
					}
					return ns;
				}
				throw ScriptRuntime.typeError1("msg.no.new", "WeakSet");
			case Id_add:
				return realThis(thisObj, f).js_add(args.length > 0 ? args[0] : Undefined.instance);
			case Id_delete:
				return realThis(thisObj, f).js_delete(args.length > 0 ? args[0] : Undefined.instance);
			case Id_has:
				return realThis(thisObj, f).js_has(args.length > 0 ? args[0] : Undefined.instance);
		}
		throw new IllegalArgumentException("WeakMap.prototype has no method: " + f.getFunctionName());
	}

	private Object js_add(Object key) {
		// As the spec says, only a true "Object" can be the key to a WeakSet.
		// Use the default object equality here. ScriptableObject does not override
		// equals or hashCode, which means that in effect we are only keying on object identity.
		// This is all correct according to the ECMAscript spec.
		if (!ScriptRuntime.isObject(key)) {
			throw ScriptRuntime.typeError1("msg.arg.not.object", ScriptRuntime.typeof(key));
		}
		// Add a value to the map, but don't make it the key -- otherwise the WeakHashMap
		// will never GC anything.
		map.put((Scriptable) key, Boolean.TRUE);
		return this;
	}

	private Object js_delete(Object key) {
		if (!ScriptRuntime.isObject(key)) {
			return Boolean.FALSE;
		}
		return map.remove(key) != null;
	}

	private Object js_has(Object key) {
		if (!ScriptRuntime.isObject(key)) {
			return Boolean.FALSE;
		}
		return map.containsKey(key);
	}

	private static NativeWeakSet realThis(Scriptable thisObj, IdFunctionObject f) {
		if (thisObj == null) {
			throw incompatibleCallError(f);
		}
		try {
			final NativeWeakSet ns = (NativeWeakSet) thisObj;
			if (!ns.instanceOfWeakSet) {
				// Check for "Set internal data tag"
				throw incompatibleCallError(f);
			}
			return ns;
		} catch (ClassCastException cce) {
			throw incompatibleCallError(f);
		}
	}

	@Override
	protected void initPrototypeId(int id) {
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
			case Id_add -> {
				arity = 1;
				s = "add";
			}
			case Id_delete -> {
				arity = 1;
				s = "delete";
			}
			case Id_has -> {
				arity = 1;
				s = "has";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(MAP_TAG, id, s, fnName, arity);
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.TO_STRING_TAG.equals(k)) {
			return SymbolId_toStringTag;
		}
		return 0;
	}

	// #string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "add" -> Id_add;
			case "delete" -> Id_delete;
			case "has" -> Id_has;
			default -> super.findPrototypeId(s);
		};
	}

	private static final int Id_constructor = 1;
	private static final int Id_add = 2;
	private static final int Id_delete = 3;
	private static final int Id_has = 4;
	private static final int SymbolId_toStringTag = 5;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

	// #/string_id_map#

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		map = new WeakHashMap<>();
	}
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Iterator;

public class NativeMap extends IdScriptableObject {
	static final String ITERATOR_TAG = "Map Iterator";
	private static final Object MAP_TAG = "Map";
	private static final Object NULL_VALUE = new Object();
	// Note that "SymbolId_iterator" is not present here. That's because the spec
	// requires that it be the same value as the "entries" prototype property.
	private static final int Id_constructor = 1;
	private static final int Id_set = 2;
	private static final int Id_get = 3;
	private static final int Id_delete = 4;
	private static final int Id_has = 5;
	private static final int Id_clear = 6;
	private static final int Id_keys = 7;
	private static final int Id_values = 8;
	private static final int Id_entries = 9;
	private static final int Id_forEach = 10;
	private static final int SymbolId_getSize = 11;
	private static final int SymbolId_toStringTag = 12;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

	static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeMap obj = new NativeMap(cx);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false, cx);

		ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
		desc.put(cx, "enumerable", desc, Boolean.FALSE);
		desc.put(cx, "configurable", desc, Boolean.TRUE);
		desc.put(cx, "get", desc, obj.get(cx, NativeSet.GETSIZE, obj));
		obj.defineOwnProperty(cx, "size", desc);

		if (sealed) {
			obj.sealObject(cx);
		}
	}

	/**
	 * If an "iterable" object was passed to the constructor, there are many many things
	 * to do... Make this static because NativeWeakMap has the exact same requirement.
	 */
	static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject map, Object arg1) {
		if ((arg1 == null) || Undefined.instance.equals(arg1)) {
			return;
		}

		// Call the "[Symbol.iterator]" property as a function.
		final Object ito = ScriptRuntime.callIterator(cx, scope, arg1);
		if (Undefined.instance.equals(ito)) {
			// Per spec, ignore if the iterator is undefined
			return;
		}

		// Find the "add" function of our own prototype, since it might have
		// been replaced. Since we're not fully constructed yet, create a dummy instance
		// so that we can get our own prototype.
		ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, map.getClassName()), cx);
		final Callable set = ScriptRuntime.getPropFunctionAndThis(cx, scope, dummy.getPrototype(cx), "set");
		cx.lastStoredScriptable();

		// Finally, run through all the iterated values and add them!
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
			for (Object val : it) {
				Scriptable sVal = ensureScriptable(val, cx);
				if (sVal instanceof Symbol) {
					throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, sVal));
				}
				Object finalKey = sVal.get(cx, 0, sVal);
				if (finalKey == NOT_FOUND) {
					finalKey = Undefined.instance;
				}
				Object finalVal = sVal.get(cx, 1, sVal);
				if (finalVal == NOT_FOUND) {
					finalVal = Undefined.instance;
				}
				set.call(cx, scope, map, new Object[]{finalKey, finalVal});
			}
		}
	}

	private static NativeMap realThis(Scriptable thisObj, IdFunctionObject f, Context cx) {
		if (thisObj == null) {
			throw incompatibleCallError(f, cx);
		}
		try {
			final NativeMap nm = (NativeMap) thisObj;
			if (!nm.instanceOfMap) {
				// Check for "Map internal data tag"
				throw incompatibleCallError(f, cx);
			}
			return nm;
		} catch (ClassCastException cce) {
			throw incompatibleCallError(f, cx);
		}
	}

	private final Hashtable entries;
	private boolean instanceOfMap = false;

	public NativeMap(Context cx) {
		entries = new Hashtable(cx);
	}

	@Override
	public String getClassName() {
		return "Map";
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
					NativeMap nm = new NativeMap(cx);
					nm.instanceOfMap = true;
					if (args.length > 0) {
						loadFromIterable(cx, scope, nm, args[0]);
					}
					return nm;
				}
				throw ScriptRuntime.typeError1(cx, "msg.no.new", "Map");
			case Id_set:
				return realThis(thisObj, f, cx).js_set(cx, args.length > 0 ? args[0] : Undefined.instance, args.length > 1 ? args[1] : Undefined.instance);
			case Id_delete:
				return realThis(thisObj, f, cx).js_delete(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_get:
				return realThis(thisObj, f, cx).js_get(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_has:
				return realThis(thisObj, f, cx).js_has(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_clear:
				return realThis(thisObj, f, cx).js_clear(cx);
			case Id_keys:
				return realThis(thisObj, f, cx).js_iterator(scope, NativeCollectionIterator.Type.KEYS, cx);
			case Id_values:
				return realThis(thisObj, f, cx).js_iterator(scope, NativeCollectionIterator.Type.VALUES, cx);
			case Id_entries:
				return realThis(thisObj, f, cx).js_iterator(scope, NativeCollectionIterator.Type.BOTH, cx);
			case Id_forEach:
				return realThis(thisObj, f, cx).js_forEach(cx, scope, args.length > 0 ? args[0] : Undefined.instance, args.length > 1 ? args[1] : Undefined.instance);
			case SymbolId_getSize:
				return realThis(thisObj, f, cx).js_getSize();
		}
		throw new IllegalArgumentException("Map.prototype has no method: " + f.getFunctionName());
	}

	private Object js_set(Context cx, Object k, Object v) {
		// Map.get() does not distinguish between "not found" and a null value. So,
		// replace true null here with a marker so that we can re-convert in "get".
		final Object value = (v == null ? NULL_VALUE : v);
		// Special handling of "negative zero" from the spec.
		Object key = k;
		if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
			key = ScriptRuntime.zeroObj;
		}
		entries.put(cx, key, value);
		return this;
	}

	private Object js_delete(Context cx, Object arg) {
		final Object e = entries.delete(cx, arg);
		return e != null;
	}

	private Object js_get(Context cx, Object arg) {
		final Object val = entries.get(cx, arg);
		if (val == null) {
			return Undefined.instance;
		}
		if (val == NULL_VALUE) {
			return null;
		}
		return val;
	}

	private Object js_has(Context cx, Object arg) {
		return entries.has(cx, arg);
	}

	private Object js_getSize() {
		return entries.size();
	}

	private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type, Context cx) {
		return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator(), cx);
	}

	private Object js_clear(Context cx) {
		entries.clear(cx);
		return Undefined.instance;
	}

	private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
		if (!(arg1 instanceof final Callable f)) {
			throw ScriptRuntime.typeError2(cx, "msg.isnt.function", arg1, ScriptRuntime.typeof(cx, arg1));
		}

		boolean isStrict = cx.isStrictMode();
		Iterator<Hashtable.Entry> i = entries.iterator();
		while (i.hasNext()) {
			// Per spec must convert every time so that primitives are always regenerated...
			Scriptable thisObj = ScriptRuntime.toObjectOrNull(cx, arg2, scope);

			if (thisObj == null && !isStrict) {
				thisObj = scope;
			}
			if (thisObj == null) {
				thisObj = Undefined.SCRIPTABLE_UNDEFINED;
			}

			final Hashtable.Entry e = i.next();
			Object val = e.value;
			if (val == NULL_VALUE) {
				val = null;
			}

			f.call(cx, scope, thisObj, new Object[]{val, e.key, this});
		}
		return Undefined.instance;
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		switch (id) {
			case SymbolId_getSize -> {
				initPrototypeMethod(MAP_TAG, id, NativeSet.GETSIZE, "get size", 0, cx);
				return;
			}
			case SymbolId_toStringTag -> {
				initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG, getClassName(), DONTENUM | READONLY);
				return;
			}
			// fallthrough
		}

		String s, fnName = null;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 0;
				s = "constructor";
			}
			case Id_set -> {
				arity = 2;
				s = "set";
			}
			case Id_get -> {
				arity = 1;
				s = "get";
			}
			case Id_delete -> {
				arity = 1;
				s = "delete";
			}
			case Id_has -> {
				arity = 1;
				s = "has";
			}
			case Id_clear -> {
				arity = 0;
				s = "clear";
			}
			case Id_keys -> {
				arity = 0;
				s = "keys";
			}
			case Id_values -> {
				arity = 0;
				s = "values";
			}
			case Id_entries -> {
				arity = 0;
				s = "entries";
			}
			case Id_forEach -> {
				arity = 1;
				s = "forEach";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(MAP_TAG, id, s, fnName, arity, cx);
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (NativeSet.GETSIZE.equals(k)) {
			return SymbolId_getSize;
		}
		if (SymbolKey.ITERATOR.equals(k)) {
			// ECMA spec says that the "Symbol.iterator" property of the prototype has the
			// "same value" as the "entries" property. We implement this by returning the
			// ID of "entries" when the iterator symbol is accessed.
			return Id_entries;
		}
		if (SymbolKey.TO_STRING_TAG.equals(k)) {
			return SymbolId_toStringTag;
		}
		return 0;
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "set" -> Id_set;
			case "get" -> Id_get;
			case "delete" -> Id_delete;
			case "has" -> Id_has;
			case "clear" -> Id_clear;
			case "keys" -> Id_keys;
			case "values" -> Id_values;
			case "entries" -> Id_entries;
			case "forEach" -> Id_forEach;
			default -> 0;
		};
	}
}

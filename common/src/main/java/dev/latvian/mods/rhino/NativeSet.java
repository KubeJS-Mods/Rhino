/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Iterator;

public class NativeSet extends IdScriptableObject {
	static final String ITERATOR_TAG = "Set Iterator";
	static final SymbolKey GETSIZE = new SymbolKey("[Symbol.getSize]");
	private static final Object SET_TAG = "Set";
	// Note that SymbolId_iterator is not present because it is required to have the
	// same value as the "values" entry.
	// Similarly, "keys" is supposed to have the same value as "values," which is why
	// both have the same ID.
	private static final int Id_constructor = 1;
	private static final int Id_add = 2;
	private static final int Id_delete = 3;
	private static final int Id_has = 4;
	private static final int Id_clear = 5;
	private static final int Id_keys = 6;
	private static final int Id_values = 6;  // These are deliberately the same to match the spec
	private static final int Id_entries = 7;
	private static final int Id_forEach = 8;
	private static final int SymbolId_getSize = 9;
	private static final int SymbolId_toStringTag = 10;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

	static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeSet obj = new NativeSet(cx);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false, cx);

		ScriptableObject desc = (ScriptableObject) cx.newObject(scope);
		desc.put("enumerable", desc, Boolean.FALSE, cx);
		desc.put("configurable", desc, Boolean.TRUE, cx);
		desc.put("get", desc, obj.get(cx, GETSIZE, obj), cx);
		obj.defineOwnProperty(cx, "size", desc);

		if (sealed) {
			obj.sealObject(cx);
		}
	}

	/**
	 * If an "iterable" object was passed to the constructor, there are many many things
	 * to do. This is common code with NativeWeakSet.
	 */
	static void loadFromIterable(Context cx, Scriptable scope, ScriptableObject set, Object arg1) {
		if ((arg1 == null) || Undefined.instance.equals(arg1)) {
			return;
		}

		// Call the "[Symbol.iterator]" property as a function.
		Object ito = ScriptRuntime.callIterator(cx, scope, arg1);
		if (Undefined.instance.equals(ito)) {
			// Per spec, ignore if the iterator returns undefined
			return;
		}

		// Find the "add" function of our own prototype, since it might have
		// been replaced. Since we're not fully constructed yet, create a dummy instance
		// so that we can get our own prototype.
		ScriptableObject dummy = ensureScriptableObject(cx.newObject(scope, set.getClassName()), cx);
		final Callable add = ScriptRuntime.getPropFunctionAndThis(cx, scope, dummy.getPrototype(cx), "add");
		// Clean up the value left around by the previous function
		ScriptRuntime.lastStoredScriptable(cx);

		// Finally, run through all the iterated values and add them!
		try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, ito)) {
			for (Object val : it) {
				final Object finalVal = val == NOT_FOUND ? Undefined.instance : val;
				add.call(cx, scope, set, new Object[]{finalVal});
			}
		}
	}

	private static NativeSet realThis(Scriptable thisObj, IdFunctionObject f, Context cx) {
		if (thisObj == null) {
			throw incompatibleCallError(f, cx);
		}
		try {
			final NativeSet ns = (NativeSet) thisObj;
			if (!ns.instanceOfSet) {
				// If we get here, then this object doesn't have the "Set internal data slot."
				throw incompatibleCallError(f, cx);
			}
			return ns;
		} catch (ClassCastException cce) {
			throw incompatibleCallError(f, cx);
		}
	}

	private final Hashtable entries;
	private boolean instanceOfSet = false;

	public NativeSet(Context cx) {
		entries = new Hashtable(cx);
	}

	@Override
	public String getClassName() {
		return "Set";
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(SET_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		final int id = f.methodId();
		switch (id) {
			case Id_constructor:
				if (thisObj == null) {
					NativeSet ns = new NativeSet(cx);
					ns.instanceOfSet = true;
					if (args.length > 0) {
						loadFromIterable(cx, scope, ns, args[0]);
					}
					return ns;
				} else {
					throw ScriptRuntime.typeError1(cx, "msg.no.new", "Set");
				}
			case Id_add:
				return realThis(thisObj, f, cx).js_add(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_delete:
				return realThis(thisObj, f, cx).js_delete(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_has:
				return realThis(thisObj, f, cx).js_has(cx, args.length > 0 ? args[0] : Undefined.instance);
			case Id_clear:
				return realThis(thisObj, f, cx).js_clear(cx);
			case Id_values:
				return realThis(thisObj, f, cx).js_iterator(scope, NativeCollectionIterator.Type.VALUES, cx);
			case Id_entries:
				return realThis(thisObj, f, cx).js_iterator(scope, NativeCollectionIterator.Type.BOTH, cx);
			case Id_forEach:
				return realThis(thisObj, f, cx).js_forEach(cx, scope, args.length > 0 ? args[0] : Undefined.instance, args.length > 1 ? args[1] : Undefined.instance);
			case SymbolId_getSize:
				return realThis(thisObj, f, cx).js_getSize();
		}
		throw new IllegalArgumentException("Set.prototype has no method: " + f.getFunctionName());
	}

	private Object js_add(Context cx, Object k) {
		// Special handling of "negative zero" from the spec.
		Object key = k;
		if ((key instanceof Number) && ((Number) key).doubleValue() == ScriptRuntime.negativeZero) {
			key = ScriptRuntime.zeroObj;
		}
		entries.put(cx, key, key);
		return this;
	}

	private Object js_delete(Context cx, Object arg) {
		final Object ov = entries.delete(cx, arg);
		return ov != null;
	}

	private Object js_has(Context cx, Object arg) {
		return entries.has(cx, arg);
	}

	private Object js_clear(Context cx) {
		entries.clear(cx);
		return Undefined.instance;
	}

	private Object js_getSize() {
		return entries.size();
	}

	private Object js_iterator(Scriptable scope, NativeCollectionIterator.Type type, Context cx) {
		return new NativeCollectionIterator(scope, ITERATOR_TAG, type, entries.iterator(), cx);
	}

	private Object js_forEach(Context cx, Scriptable scope, Object arg1, Object arg2) {
		if (!(arg1 instanceof final Callable f)) {
			throw ScriptRuntime.notFunctionError(cx, arg1);
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
			f.call(cx, scope, thisObj, new Object[]{e.value, e.value, this});
		}
		return Undefined.instance;
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		switch (id) {
			case SymbolId_getSize -> {
				initPrototypeMethod(SET_TAG, id, GETSIZE, "get size", 0, cx);
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
			case Id_clear -> {
				arity = 0;
				s = "clear";
			}
			case Id_entries -> {
				arity = 0;
				s = "entries";
			}
			case Id_values -> {
				arity = 0;
				s = "values";
			}
			case Id_forEach -> {
				arity = 1;
				s = "forEach";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(SET_TAG, id, s, fnName, arity, cx);
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (GETSIZE.equals(k)) {
			return SymbolId_getSize;
		}
		if (SymbolKey.ITERATOR.equals(k)) {
			return Id_values;
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
			case "add" -> Id_add;
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


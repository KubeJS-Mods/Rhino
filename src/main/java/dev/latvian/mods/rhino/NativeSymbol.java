/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.HashMap;
import java.util.Map;

/**
 * This is an implementation of the standard "Symbol" type that implements
 * all of its weird properties. One of them is that some objects can have
 * an "internal data slot" that makes them a Symbol and others cannot.
 */

public class NativeSymbol extends IdScriptableObject implements Symbol {
	public static final String CLASS_NAME = "Symbol";

	private static final Object GLOBAL_TABLE_KEY = new Object();
	private static final Object CONSTRUCTOR_SLOT = new Object();
	private static final int ConstructorId_keyFor = -2;
	private static final int ConstructorId_for = -1;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_valueOf = 4;
	private static final int SymbolId_toStringTag = 3;
	private static final int SymbolId_toPrimitive = 5;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toPrimitive;

	public static void init(Context cx, Scriptable scope, boolean sealed) {
		NativeSymbol obj = new NativeSymbol("");
		ScriptableObject ctor = obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, false, cx);

		cx.putThreadLocal(CONSTRUCTOR_SLOT, Boolean.TRUE);
		try {
			createStandardSymbol(cx, scope, ctor, "iterator", SymbolKey.ITERATOR);
			createStandardSymbol(cx, scope, ctor, "species", SymbolKey.SPECIES);
			createStandardSymbol(cx, scope, ctor, "toStringTag", SymbolKey.TO_STRING_TAG);
			createStandardSymbol(cx, scope, ctor, "hasInstance", SymbolKey.HAS_INSTANCE);
			createStandardSymbol(cx, scope, ctor, "isConcatSpreadable", SymbolKey.IS_CONCAT_SPREADABLE);
			createStandardSymbol(cx, scope, ctor, "isRegExp", SymbolKey.IS_REGEXP);
			createStandardSymbol(cx, scope, ctor, "toPrimitive", SymbolKey.TO_PRIMITIVE);
			createStandardSymbol(cx, scope, ctor, "match", SymbolKey.MATCH);
			createStandardSymbol(cx, scope, ctor, "replace", SymbolKey.REPLACE);
			createStandardSymbol(cx, scope, ctor, "search", SymbolKey.SEARCH);
			createStandardSymbol(cx, scope, ctor, "split", SymbolKey.SPLIT);
			createStandardSymbol(cx, scope, ctor, "unscopables", SymbolKey.UNSCOPABLES);

		} finally {
			cx.removeThreadLocal(CONSTRUCTOR_SLOT);
		}

		if (sealed) {
			// Can't seal until we have created all the stuff above!
			ctor.sealObject(cx);
		}
	}

	/**
	 * Use this when we need to create symbols internally because of the convoluted way we have to
	 * construct them.
	 */
	public static NativeSymbol construct(Context cx, Scriptable scope, Object[] args) {
		cx.putThreadLocal(CONSTRUCTOR_SLOT, Boolean.TRUE);
		try {
			return (NativeSymbol) cx.newObject(scope, CLASS_NAME, args);
		} finally {
			cx.removeThreadLocal(CONSTRUCTOR_SLOT);
		}
	}

	// #string_id_map#

	private static void createStandardSymbol(Context cx, Scriptable scope, ScriptableObject ctor, String name, SymbolKey key) {
		Scriptable sym = cx.newObject(scope, CLASS_NAME, new Object[]{name, key});
		ctor.defineProperty(cx, name, sym, DONTENUM | READONLY | PERMANENT);
	}

	private static NativeSymbol getSelf(Context cx, Object thisObj) {
		try {
			return (NativeSymbol) thisObj;
		} catch (ClassCastException cce) {
			throw ScriptRuntime.typeError1(cx, "msg.invalid.type", thisObj.getClass().getName());
		}
	}

	private static NativeSymbol js_constructor(Context cx, Object[] args) {
		String desc;
		if (args.length > 0) {
			if (Undefined.INSTANCE.equals(args[0])) {
				desc = "";
			} else {
				desc = ScriptRuntime.toString(cx, args[0]);
			}
		} else {
			desc = "";
		}

		if (args.length > 1) {
			return new NativeSymbol((SymbolKey) args[1]);
		}

		return new NativeSymbol(new SymbolKey(desc));
	}

	private static boolean isStrictMode(Context cx) {
		return (cx != null) && cx.isStrictMode();
	}

	private final SymbolKey key;
	private final NativeSymbol symbolData;

	/**
	 * This has to be used only for constructing the prototype instance.
	 * This sets symbolData to null (see isSymbol() for more).
	 *
	 * @param desc the description
	 */
	private NativeSymbol(String desc) {
		this.key = new SymbolKey(desc);
		this.symbolData = null;
	}

	private NativeSymbol(SymbolKey key) {
		this.key = key;
		this.symbolData = this;
	}

	public NativeSymbol(NativeSymbol s) {
		this.key = s.key;
		this.symbolData = s.symbolData;
	}

	@Override
	public String getClassName() {
		return CLASS_NAME;
	}

	// #/string_id_map#

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor, Context cx) {
		super.fillConstructorProperties(ctor, cx);
		addIdFunctionProperty(ctor, CLASS_NAME, ConstructorId_for, "for", 1, cx);
		addIdFunctionProperty(ctor, CLASS_NAME, ConstructorId_keyFor, "keyFor", 1, cx);
	}

	@Override
	protected int findPrototypeId(String s) {
		int id = 0;
		//  #generated# Last update: 2016-01-26 16:39:41 PST
		L0:
		{
			id = 0;
			String X = null;
			int s_length = s.length();
			if (s_length == 7) {
				X = "valueOf";
				id = Id_valueOf;
			} else if (s_length == 8) {
				X = "toString";
				id = Id_toString;
			} else if (s_length == 11) {
				X = "constructor";
				id = Id_constructor;
			}
			if (X != null && X != s && !X.equals(s)) {
				id = 0;
			}
			break L0;
		}
		//  #/generated#
		return id;
	}

	@Override
	protected int findPrototypeId(Symbol key) {
		if (SymbolKey.TO_STRING_TAG.equals(key)) {
			return SymbolId_toStringTag;
		} else if (SymbolKey.TO_PRIMITIVE.equals(key)) {
			return SymbolId_toPrimitive;
		}
		return 0;
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		switch (id) {
			case Id_constructor -> initPrototypeMethod(CLASS_NAME, id, "constructor", 0, cx);
			case Id_toString -> initPrototypeMethod(CLASS_NAME, id, "toString", 0, cx);
			case Id_valueOf -> initPrototypeMethod(CLASS_NAME, id, "valueOf", 0, cx);
			case SymbolId_toStringTag -> initPrototypeValue(id, SymbolKey.TO_STRING_TAG, CLASS_NAME, DONTENUM | READONLY);
			case SymbolId_toPrimitive -> initPrototypeMethod(CLASS_NAME, id, SymbolKey.TO_PRIMITIVE, "Symbol.toPrimitive", 1, cx);
			default -> super.initPrototypeId(id, cx);
		}
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(CLASS_NAME)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case ConstructorId_for:
				return js_for(cx, scope, args);
			case ConstructorId_keyFor:
				return js_keyFor(cx, scope, args);

			case Id_constructor:
				if (thisObj == null) {
					if (cx.getThreadLocal(CONSTRUCTOR_SLOT) == null) {
						// We should never get to this via "new".
						throw ScriptRuntime.typeError0(cx, "msg.no.symbol.new");
					}
					// Unless we are being called by our own internal "new"
					return js_constructor(cx, args);
				}
				return construct(cx, scope, args);

			case Id_toString:
				return getSelf(cx, thisObj).toString();
			case Id_valueOf:
			case SymbolId_toPrimitive:
				return getSelf(cx, thisObj).js_valueOf();
			default:
				return super.execIdCall(f, cx, scope, thisObj, args);
		}
	}

	private Object js_valueOf() {
		// In the case that "Object()" was called we actually have a different "internal slot"
		return symbolData;
	}

	private Object js_for(Context cx, Scriptable scope, Object[] args) {
		String name = (args.length > 0 ? ScriptRuntime.toString(cx, args[0]) : ScriptRuntime.toString(cx, Undefined.INSTANCE));

		Map<String, NativeSymbol> table = getGlobalMap();
		NativeSymbol ret = table.get(name);

		if (ret == null) {
			ret = construct(cx, scope, new Object[]{name});
			table.put(name, ret);
		}
		return ret;
	}

	private Object js_keyFor(Context cx, Scriptable scope, Object[] args) {
		Object s = (args.length > 0 ? args[0] : Undefined.INSTANCE);
		if (!(s instanceof NativeSymbol sym)) {
			throw ScriptRuntime.throwCustomError(cx, scope, "TypeError", "Not a Symbol");
		}

		Map<String, NativeSymbol> table = getGlobalMap();
		for (Map.Entry<String, NativeSymbol> e : table.entrySet()) {
			if (e.getValue().key == sym.key) {
				return e.getKey();
			}
		}
		return Undefined.INSTANCE;
	}

	// Symbol objects have a special property that one cannot add properties.

	@Override
	public String toString() {
		return key.toString();
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		if (!isSymbol()) {
			super.put(cx, name, start, value);
		} else if (isStrictMode(cx)) {
			throw ScriptRuntime.typeError0(cx, "msg.no.assign.symbol.strict");
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (!isSymbol()) {
			super.put(cx, index, start, value);
		} else if (isStrictMode(cx)) {
			throw ScriptRuntime.typeError0(cx, "msg.no.assign.symbol.strict");
		}
	}

	@Override
	public void put(Context cx, Symbol key, Scriptable start, Object value) {
		if (!isSymbol()) {
			super.put(cx, key, start, value);
		} else if (isStrictMode(cx)) {
			throw ScriptRuntime.typeError0(cx, "msg.no.assign.symbol.strict");
		}
	}

	/**
	 * Object() on a Symbol constructs an object which is NOT a symbol, but which has an "internal data slot"
	 * that is. Furthermore, such an object has the Symbol prototype so this particular object is still used.
	 * Account for that here: an "Object" that was created from a Symbol has a different value of the slot.
	 */
	public boolean isSymbol() {
		return (symbolData == this);
	}

	@Override
	public MemberType getTypeOf() {
		return (isSymbol() ? MemberType.SYMBOL : super.getTypeOf());
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object x) {
		return key.equals(x);
	}

	SymbolKey getKey() {
		return key;
	}

	@SuppressWarnings("unchecked")
	private Map<String, NativeSymbol> getGlobalMap() {
		ScriptableObject top = (ScriptableObject) getTopLevelScope(this);
		Map<String, NativeSymbol> map = (Map<String, NativeSymbol>) top.getAssociatedValue(GLOBAL_TABLE_KEY);
		if (map == null) {
			map = new HashMap<>();
			top.associateValue(GLOBAL_TABLE_KEY, map);
		}
		return map;
	}
}

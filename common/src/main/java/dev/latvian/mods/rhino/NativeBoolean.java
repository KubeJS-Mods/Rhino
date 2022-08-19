/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the Boolean native object.
 * See ECMA 15.6.
 *
 * @author Norris Boyd
 */
final class NativeBoolean extends IdScriptableObject {
	private static final Object BOOLEAN_TAG = "Boolean";

	static void init(Scriptable scope, boolean sealed) {
		NativeBoolean obj = new NativeBoolean(false);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed);
	}

	NativeBoolean(boolean b) {
		booleanValue = b;
	}

	@Override
	public String getClassName() {
		return "Boolean";
	}

	@Override
	public Object getDefaultValue(Class<?> typeHint) {
		// This is actually non-ECMA, but will be proposed
		// as a change in round 2.
		if (typeHint == ScriptRuntime.BooleanClass) {
			return ScriptRuntime.wrapBoolean(booleanValue);
		}
		return super.getDefaultValue(typeHint);
	}

	@Override
	protected void initPrototypeId(int id) {
		switch (id) {
			case Id_constructor -> initPrototypeMethod(BOOLEAN_TAG, id, "constructor", 1);
			case Id_toString -> initPrototypeMethod(BOOLEAN_TAG, id, "toString", 0);
			case Id_toSource -> initPrototypeMethod(BOOLEAN_TAG, id, "toSource", 0);
			case Id_valueOf -> initPrototypeMethod(BOOLEAN_TAG, id, "valueOf", 0);
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(BOOLEAN_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();

		if (id == Id_constructor) {
			boolean b;
			if (args.length == 0) {
				b = false;
			} else {
				// see special handling in ScriptRuntime.toBoolean(Object)
				// avoidObjectDetection() is used to implement document.all
				// see Note on page
				//   https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Boolean
				b = (!(args[0] instanceof ScriptableObject) || !((ScriptableObject) args[0]).avoidObjectDetection()) && ScriptRuntime.toBoolean(args[0]);
			}
			if (thisObj == null) {
				// new Boolean(val) creates a new boolean object.
				return new NativeBoolean(b);
			}
			// Boolean(val) converts val to a boolean.
			return ScriptRuntime.wrapBoolean(b);
		}

		// The rest of Boolean.prototype methods require thisObj to be Boolean

		if (!(thisObj instanceof NativeBoolean)) {
			throw incompatibleCallError(f);
		}
		boolean value = ((NativeBoolean) thisObj).booleanValue;

		switch (id) {

			case Id_toString:
				return value ? "true" : "false";

			case Id_toSource:
				return "not_supported";

			case Id_valueOf:
				return ScriptRuntime.wrapBoolean(value);
		}
		throw new IllegalArgumentException(String.valueOf(id));
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toSource" -> Id_toSource;
			case "valueOf" -> Id_valueOf;
			default -> 0;
		};
	}

	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int Id_valueOf = 4;
	private static final int MAX_PROTOTYPE_ID = 4;

	private final boolean booleanValue;
}

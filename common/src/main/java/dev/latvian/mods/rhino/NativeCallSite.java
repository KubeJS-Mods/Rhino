/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class is used by the V8 extension "Error.prepareStackTrace." It is
 * passed to that function, which may then use it to format the stack as it sees
 * fit.
 */

public class NativeCallSite extends IdScriptableObject {
	private static final String CALLSITE_TAG = "CallSite";
	private static final int Id_constructor = 1;
	private static final int Id_getThis = 2;
	private static final int Id_getTypeName = 3;
	private static final int Id_getFunction = 4;
	private static final int Id_getFunctionName = 5;
	private static final int Id_getMethodName = 6;
	private static final int Id_getFileName = 7;
	private static final int Id_getLineNumber = 8;
	private static final int Id_getColumnNumber = 9;
	private static final int Id_getEvalOrigin = 10;
	private static final int Id_isToplevel = 11;
	private static final int Id_isEval = 12;
	private static final int Id_isNative = 13;

	// #string_id_map#
	private static final int Id_isConstructor = 14;
	private static final int Id_toString = 15;
	private static final int MAX_PROTOTYPE_ID = 15;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeCallSite cs = new NativeCallSite();
		cs.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	static NativeCallSite make(Scriptable scope, Scriptable ctorObj, Context cx) {
		NativeCallSite cs = new NativeCallSite();
		Scriptable proto = (Scriptable) (ctorObj.get("prototype", ctorObj, cx));
		cs.setParentScope(scope);
		cs.setPrototype(proto);
		return cs;
	}

	private static Object js_toString(Scriptable obj, Context cx) {
		while (obj != null && !(obj instanceof NativeCallSite)) {
			obj = obj.getPrototype(cx);
		}
		if (obj == null) {
			return NOT_FOUND;
		}
		NativeCallSite cs = (NativeCallSite) obj;
		StringBuilder sb = new StringBuilder();
		cs.element.renderJavaStyle(sb);
		return sb.toString();
	}

	private static Object getFunctionName(Scriptable obj, Context cx) {
		while (obj != null && !(obj instanceof NativeCallSite)) {
			obj = obj.getPrototype(cx);
		}
		if (obj == null) {
			return NOT_FOUND;
		}
		NativeCallSite cs = (NativeCallSite) obj;
		return (cs.element == null ? null : cs.element.functionName);
	}

	private static Object getFileName(Scriptable obj, Context cx) {
		while (obj != null && !(obj instanceof NativeCallSite)) {
			obj = obj.getPrototype(cx);
		}
		if (obj == null) {
			return NOT_FOUND;
		}
		NativeCallSite cs = (NativeCallSite) obj;
		return (cs.element == null ? null : cs.element.fileName);
	}

	private static Object getLineNumber(Scriptable obj, Context cx) {
		while (obj != null && !(obj instanceof NativeCallSite)) {
			obj = obj.getPrototype(cx);
		}
		if (obj == null) {
			return NOT_FOUND;
		}
		NativeCallSite cs = (NativeCallSite) obj;
		if ((cs.element == null) || (cs.element.lineNumber < 0)) {
			return Undefined.instance;
		}
		return cs.element.lineNumber;
	}

	private ScriptStackElement element;

	private NativeCallSite() {
	}

	void setElement(ScriptStackElement elt) {
		this.element = elt;
	}

	@Override
	public String getClassName() {
		return "CallSite";
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		String s;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 0;
				s = "constructor";
			}
			case Id_getThis -> {
				arity = 0;
				s = "getThis";
			}
			case Id_getTypeName -> {
				arity = 0;
				s = "getTypeName";
			}
			case Id_getFunction -> {
				arity = 0;
				s = "getFunction";
			}
			case Id_getFunctionName -> {
				arity = 0;
				s = "getFunctionName";
			}
			case Id_getMethodName -> {
				arity = 0;
				s = "getMethodName";
			}
			case Id_getFileName -> {
				arity = 0;
				s = "getFileName";
			}
			case Id_getLineNumber -> {
				arity = 0;
				s = "getLineNumber";
			}
			case Id_getColumnNumber -> {
				arity = 0;
				s = "getColumnNumber";
			}
			case Id_getEvalOrigin -> {
				arity = 0;
				s = "getEvalOrigin";
			}
			case Id_isToplevel -> {
				arity = 0;
				s = "isToplevel";
			}
			case Id_isEval -> {
				arity = 0;
				s = "isEval";
			}
			case Id_isNative -> {
				arity = 0;
				s = "isNative";
			}
			case Id_isConstructor -> {
				arity = 0;
				s = "isConstructor";
			}
			case Id_toString -> {
				arity = 0;
				s = "toString";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(CALLSITE_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(CALLSITE_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		return switch (id) {
			case Id_constructor -> make(scope, f, cx);
			case Id_getFunctionName -> getFunctionName(thisObj, cx);
			case Id_getFileName -> getFileName(thisObj, cx);
			case Id_getLineNumber -> getLineNumber(thisObj, cx);
			case Id_getThis, Id_getTypeName, Id_getFunction, Id_getColumnNumber -> Undefined.instance;
			case Id_getMethodName -> null;
			case Id_getEvalOrigin, Id_isEval, Id_isConstructor, Id_isNative, Id_isToplevel -> Boolean.FALSE;
			case Id_toString -> js_toString(thisObj, cx);
			default -> throw new IllegalArgumentException(String.valueOf(id));
		};
	}

	@Override
	public String toString() {
		if (element == null) {
			return "";
		}
		return element.toString();
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "isEval" -> Id_isEval;
			case "getThis" -> Id_getThis;
			case "isNative" -> Id_isNative;
			case "toString" -> Id_toString;
			case "isToplevel" -> Id_isToplevel;
			case "getFileName" -> Id_getFileName;
			case "constructor" -> Id_constructor;
			case "getFunction" -> Id_getFunction;
			case "getTypeName" -> Id_getTypeName;
			case "getEvalOrigin" -> Id_getEvalOrigin;
			case "getLineNumber" -> Id_getLineNumber;
			case "getMethodName" -> Id_getMethodName;
			case "isConstructor" -> Id_isConstructor;
			case "getColumnNumber" -> Id_getColumnNumber;
			case "getFunctionName" -> Id_getFunctionName;
			default -> 0;
		};
	}
	// #/string_id_map#
}
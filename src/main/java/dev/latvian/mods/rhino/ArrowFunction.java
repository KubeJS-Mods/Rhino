/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * The class for  Arrow Function Definitions
 * EcmaScript 6 Rev 14, March 8, 2013 Draft spec , 13.2
 */
public class ArrowFunction extends BaseFunction {
	static boolean equalObjectGraphs(Context cx, ArrowFunction f1, ArrowFunction f2, EqualObjectGraphs eq) {
		return eq.equalGraphs(cx, f1.boundThis, f2.boundThis) && eq.equalGraphs(cx, f1.targetFunction, f2.targetFunction);
	}

	private final Callable targetFunction;
	private final Scriptable boundThis;

	public ArrowFunction(Context cx, Scriptable scope, Callable targetFunction, Scriptable boundThis) {
		this.targetFunction = targetFunction;
		this.boundThis = boundThis;

		ScriptRuntime.setFunctionProtoAndParent(cx, scope, this);

		Function thrower = ScriptRuntime.typeErrorThrower(cx);
		NativeObject throwing = new NativeObject(cx);
		throwing.put(cx, "get", throwing, thrower);
		throwing.put(cx, "set", throwing, thrower);
		throwing.put(cx, "enumerable", throwing, Boolean.FALSE);
		throwing.put(cx, "configurable", throwing, Boolean.FALSE);
		throwing.preventExtensions();

		this.defineOwnProperty(cx, "caller", throwing, false);
		this.defineOwnProperty(cx, "arguments", throwing, false);
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable callThis = boundThis != null ? boundThis : cx.getTopCallOrThrow();
		return cx.callSync(targetFunction, scope, callThis, args);
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		throw ScriptRuntime.typeError1(cx, "msg.not.ctor", toString());
	}

	@Override
	public boolean hasInstance(Context cx, Scriptable instance) {
		if (targetFunction instanceof Function) {
			return ((Function) targetFunction).hasInstance(cx, instance);
		}
		throw ScriptRuntime.typeError0(cx, "msg.not.ctor");
	}

	@Override
	public int getLength() {
		if (targetFunction instanceof BaseFunction) {
			return ((BaseFunction) targetFunction).getLength();
		}
		return 0;
	}

	@Override
	public int getArity() {
		return getLength();
	}

	@Override
	public String toString() {
		if (targetFunction instanceof BaseFunction) {
			return "ArrowFunction (" + ((BaseFunction) targetFunction).getLength() + ") => {...}";
		}

		return "ArrowFunction";
	}
}

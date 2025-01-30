/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class reflects a single Java constructor into the JavaScript
 * environment.  It satisfies a request for an overloaded constructor,
 * as introduced in LiveConnect 3.
 * All NativeJavaConstructors behave as JSRef `bound' methods, in that they
 * always construct the same NativeJavaClass regardless of any reparenting
 * that may occur.
 *
 * @author Frank Mitchell
 * @see NativeJavaMethod
 * @see NativeJavaClass
 */

public class NativeJavaConstructor extends BaseFunction {
	MemberBox ctor;

	public NativeJavaConstructor(MemberBox ctor) {
		this.ctor = ctor;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return NativeJavaClass.constructSpecific(cx, scope, args, ctor);
	}

	@Override
	public String getFunctionName() {
		String sig = JavaMembers.liveConnectSignature(ctor.parameters().types());
		return "<init>".concat(sig);
	}

	@Override
	public String toString() {
		return "[JavaConstructor " + ctor.getName() + "]";
	}
}


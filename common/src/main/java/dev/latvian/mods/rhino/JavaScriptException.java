/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import java.io.Serial;

/**
 * Java reflection of JavaScript exceptions.
 * Instances of this class are thrown by the JavaScript 'throw' keyword.
 *
 * @author Mike McCabe
 */
public class JavaScriptException extends RhinoException {
	@Serial
	private static final long serialVersionUID = -7666130513694669293L;
	private final Object value;

	/**
	 * Create a JavaScript exception wrapping the given JavaScript value
	 *
	 * @param value the JavaScript value thrown.
	 */
	public JavaScriptException(Object value, String sourceName, int lineNumber) {
		recordErrorOrigin(sourceName, lineNumber, null, 0);
		this.value = value;
	}

	@Override
	public String details() {
		if (value == null) {
			return "null";
		} else if (value instanceof NativeError) {
			return value.toString();
		}
		try {
			return ScriptRuntime.toString(value);
		} catch (RuntimeException rte) {
			// ScriptRuntime.toString may throw a RuntimeException
			if (value instanceof Scriptable) {
				return ScriptRuntime.defaultObjectToString((Scriptable) value);
			}
			return value.toString();
		}
	}

	/**
	 * @return the value wrapped by this exception
	 */
	public Object getValue() {
		return value;
	}
}

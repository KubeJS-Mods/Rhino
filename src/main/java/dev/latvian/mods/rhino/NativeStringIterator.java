/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public final class NativeStringIterator extends ES6Iterator {
	private static final String ITERATOR_TAG = "StringIterator";

	static void init(ScriptableObject scope, boolean sealed, Context cx) {
		init(scope, sealed, new NativeStringIterator(), ITERATOR_TAG, cx);
	}

	private String string;
	private int index;

	/**
	 * Only for constructing the prototype object.
	 */
	private NativeStringIterator() {
		super();
	}

	NativeStringIterator(Context cx, Scriptable scope, Object stringLike) {
		super(scope, ITERATOR_TAG, cx);
		this.index = 0;
		this.string = ScriptRuntime.toString(cx, stringLike);
	}

	@Override
	public String getClassName() {
		return "String Iterator";
	}

	@Override
	protected boolean isDone(Context cx, Scriptable scope) {
		return index >= string.length();
	}

	@Override
	protected Object nextValue(Context cx, Scriptable scope) {
		int newIndex = string.offsetByCodePoints(index, 1);
		Object value = string.substring(index, newIndex);
		index = newIndex;
		return value;
	}

	@Override
	protected String getTag() {
		return ITERATOR_TAG;
	}
}

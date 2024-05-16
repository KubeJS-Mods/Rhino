/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DefaultValueTypeHint;

/**
 * This class implements the Undefined value in JavaScript.
 */
public class Undefined {
	public static final Scriptable SCRIPTABLE_INSTANCE = new Scriptable() {
		@Override
		public String toString() {
			return "undefined";
		}

		@Override
		public boolean equals(Object obj) {
			return isUndefined(obj);
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public String getClassName() {
			throw new UnsupportedOperationException("undefined doesn't support getClassName");
		}

		@Override
		public Object get(Context cx, String name, Scriptable start) {
			throw new UnsupportedOperationException("undefined doesn't support get");
		}

		@Override
		public Object get(Context cx, int index, Scriptable start) {
			throw new UnsupportedOperationException("undefined doesn't support get");
		}

		@Override
		public boolean has(Context cx, String name, Scriptable start) {
			throw new UnsupportedOperationException("undefined doesn't support has");
		}

		@Override
		public boolean has(Context cx, int index, Scriptable start) {
			throw new UnsupportedOperationException("undefined doesn't support has");
		}

		@Override
		public void put(Context cx, String name, Scriptable start, Object value) {
			throw new UnsupportedOperationException("undefined doesn't support put");
		}

		@Override
		public void put(Context cx, int index, Scriptable start, Object value) {
			throw new UnsupportedOperationException("undefined doesn't support put");
		}

		@Override
		public void delete(Context cx, String name) {
			throw new UnsupportedOperationException("undefined doesn't support delete");
		}

		@Override
		public void delete(Context cx, int index) {
			throw new UnsupportedOperationException("undefined doesn't support delete");
		}

		@Override
		public Scriptable getPrototype(Context cx) {
			throw new UnsupportedOperationException("undefined doesn't support getPrototype");
		}

		@Override
		public void setPrototype(Scriptable prototype) {
			throw new UnsupportedOperationException("undefined doesn't support setPrototype");
		}

		@Override
		public Scriptable getParentScope() {
			throw new UnsupportedOperationException("undefined doesn't support getParentScope");
		}

		@Override
		public void setParentScope(Scriptable parent) {
			throw new UnsupportedOperationException("undefined doesn't support setParentScope");
		}

		@Override
		public Object[] getIds(Context cx) {
			throw new UnsupportedOperationException("undefined doesn't support getIds");
		}

		@Override
		public Object getDefaultValue(Context cx, DefaultValueTypeHint hint) {
			throw new UnsupportedOperationException("undefined doesn't support getDefaultValue");
		}

		@Override
		public boolean hasInstance(Context cx, Scriptable instance) {
			throw new UnsupportedOperationException("undefined doesn't support hasInstance");
		}
	};

	public static final Object INSTANCE = new Undefined();

	public static boolean isUndefined(Object obj) {
		return obj == INSTANCE || obj == SCRIPTABLE_INSTANCE;
	}

	private Undefined() {
	}

	@Override
	public String toString() {
		return "undefined";
	}

	@Override
	public boolean equals(Object obj) {
		return isUndefined(obj) || super.equals(obj);
	}

	@Override
	public int hashCode() {
		return 0;
	}
}

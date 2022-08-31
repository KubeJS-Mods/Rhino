/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public abstract class ES6Iterator extends IdScriptableObject {
	public static final String NEXT_METHOD = "next";
	public static final String DONE_PROPERTY = "done";
	public static final String RETURN_PROPERTY = "return";
	public static final String VALUE_PROPERTY = "value";
	public static final String RETURN_METHOD = "return";
	private static final int Id_next = 1;
	private static final int SymbolId_iterator = 2;
	private static final int SymbolId_toStringTag = 3;
	private static final int MAX_PROTOTYPE_ID = SymbolId_toStringTag;

	protected static void init(ScriptableObject scope, boolean sealed, IdScriptableObject prototype, String tag) {
		if (scope != null) {
			prototype.setParentScope(scope);
			prototype.setPrototype(getObjectPrototype(scope));
		}
		prototype.activatePrototypeMap(MAX_PROTOTYPE_ID);
		if (sealed) {
			prototype.sealObject();
		}

		// Need to access Iterator prototype when constructing
		// Iterator instances, but don't have a iterator constructor
		// to use to find the prototype. Use the "associateValue"
		// approach instead.
		if (scope != null) {
			scope.associateValue(tag, prototype);
		}
	}

	// 25.1.1.3 The IteratorResult Interface
	static Scriptable makeIteratorResult(Context cx, Scriptable scope, Boolean done) {
		return makeIteratorResult(cx, scope, done, Undefined.instance);
	}

	static Scriptable makeIteratorResult(Context cx, Scriptable scope, Boolean done, Object value) {
		final Scriptable iteratorResult = cx.newObject(scope);
		ScriptableObject.putProperty(iteratorResult, VALUE_PROPERTY, value);
		ScriptableObject.putProperty(iteratorResult, DONE_PROPERTY, done);
		return iteratorResult;
	}

	protected boolean exhausted = false;
	private String tag;

	protected ES6Iterator() {
	}

	protected ES6Iterator(Scriptable scope, String tag) {
		// Set parent and prototype properties. Since we don't have a
		// "Iterator" constructor in the top scope, we stash the
		// prototype in the top scope's associated value.
		this.tag = tag;
		Scriptable top = ScriptableObject.getTopLevelScope(scope);
		this.setParentScope(top);
		IdScriptableObject prototype = (IdScriptableObject) ScriptableObject.getTopScopeValue(top, tag);
		setPrototype(prototype);
	}

	@Override
	protected void initPrototypeId(int id) {
		switch (id) {
			case Id_next -> {
				initPrototypeMethod(getTag(), id, NEXT_METHOD, 0);
			}
			case SymbolId_iterator -> {
				initPrototypeMethod(getTag(), id, SymbolKey.ITERATOR, "[Symbol.iterator]", DONTENUM | READONLY);
			}
			case SymbolId_toStringTag -> {
				initPrototypeValue(SymbolId_toStringTag, SymbolKey.TO_STRING_TAG, getClassName(), DONTENUM | READONLY);
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(getTag())) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();

		if (!(thisObj instanceof ES6Iterator iterator)) {
			throw incompatibleCallError(f);
		}

		return switch (id) {
			case Id_next -> iterator.next(cx, scope);
			case SymbolId_iterator -> iterator;
			default -> throw new IllegalArgumentException(String.valueOf(id));
		};
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.ITERATOR.equals(k)) {
			return SymbolId_iterator;
		} else if (SymbolKey.TO_STRING_TAG.equals(k)) {
			return SymbolId_toStringTag;
		}
		return 0;
	}

	@Override
	protected int findPrototypeId(String s) {
		if (NEXT_METHOD.equals(s)) {
			return Id_next;
		}
		return 0;
	}

	abstract protected boolean isDone(Context cx, Scriptable scope);

	abstract protected Object nextValue(Context cx, Scriptable scope);

	protected Object next(Context cx, Scriptable scope) {
		Object value = Undefined.instance;
		boolean done = isDone(cx, scope) || this.exhausted;
		if (!done) {
			value = nextValue(cx, scope);
		} else {
			this.exhausted = true;
		}
		return makeIteratorResult(cx, scope, done, value);
	}

	protected String getTag() {
		return tag;
	}
}

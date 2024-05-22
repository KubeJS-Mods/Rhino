/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;

import java.util.Iterator;

/**
 * This class implements iterator objects. See
 * http://developer.mozilla.org/en/docs/New_in_JavaScript_1.7#Iterators
 *
 * @author Norris Boyd
 */
public final class NativeIterator extends IdScriptableObject {
	public static final String ITERATOR_PROPERTY_NAME = "__iterator__";
	private static final Object ITERATOR_TAG = "Iterator";
	private static final String STOP_ITERATION = "StopIteration";
	private static final int Id_constructor = 1;
	private static final int Id_next = 2;
	private static final int Id___iterator__ = 3;
	private static final int MAX_PROTOTYPE_ID = 3;

	public static class StopIteration extends NativeObject {
		private Object value = Undefined.INSTANCE;

		public StopIteration(Context cx) {
			super(cx.factory);
		}

		public StopIteration(Context cx, Object val) {
			this(cx);
			this.value = val;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public String getClassName() {
			return STOP_ITERATION;
		}

		/* StopIteration has custom instanceof behavior since it
		 * doesn't have a constructor.
		 */
		@Override
		public boolean hasInstance(Context cx, Scriptable instance) {
			return instance instanceof StopIteration;
		}
	}

	static public class WrappedJavaIterator {
		private final Context localContext;
		private final Iterator<?> iterator;
		private final Scriptable scope;

		WrappedJavaIterator(Context cx, Iterator<?> iterator, Scriptable scope) {
			this.localContext = cx;
			this.iterator = iterator;
			this.scope = scope;
		}

		public Object next() {
			if (!iterator.hasNext()) {
				// Out of values. Throw StopIteration.
				throw new JavaScriptException(localContext, NativeIterator.getStopIterationObject(scope, localContext), null, 0);
			}
			return iterator.next();
		}

		public Object __iterator__(boolean b) {
			return this;
		}
	}

	static void init(Context cx, ScriptableObject scope, boolean sealed) {
		// Iterator
		NativeIterator iterator = new NativeIterator();
		iterator.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);

		// Generator
		ES6Generator.init(scope, sealed, cx);

		// StopIteration
		NativeObject obj = new StopIteration(cx);
		obj.setPrototype(getObjectPrototype(scope, cx));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject(cx);
		}
		defineProperty(scope, STOP_ITERATION, obj, DONTENUM, cx);
		// Use "associateValue" so that generators can continue to
		// throw StopIteration even if the property of the global
		// scope is replaced or deleted.
		scope.associateValue(ITERATOR_TAG, obj);
	}

	/**
	 * Get the value of the "StopIteration" object. Note that this value
	 * is stored in the top-level scope using "associateValue" so the
	 * value can still be found even if a script overwrites or deletes
	 * the global "StopIteration" property.
	 *
	 * @param scope a scope whose parent chain reaches a top-level scope
	 * @return the StopIteration object
	 */
	public static Object getStopIterationObject(Scriptable scope, Context cx) {
		Scriptable top = getTopLevelScope(scope);
		return getTopScopeValue(top, ITERATOR_TAG, cx);
	}

	/* The JavaScript constructor */
	private static Object jsConstructor(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (args.length == 0 || args[0] == null || args[0] == Undefined.INSTANCE) {
			Object argument = args.length == 0 ? Undefined.INSTANCE : args[0];
			throw ScriptRuntime.typeError1(cx, "msg.no.properties", ScriptRuntime.toString(cx, argument));
		}
		Scriptable obj = ScriptRuntime.toObject(cx, scope, args[0]);
		boolean keyOnly = args.length > 1 && ScriptRuntime.toBoolean(cx, args[1]);
		if (thisObj != null) {
			// Called as a function. Convert to iterator if possible.

			// For objects that implement java.lang.Iterable or
			// java.util.Iterator, have JavaScript Iterator call the underlying
			// iteration methods
			Iterator<?> iterator = getJavaIterator(obj);
			if (iterator != null) {
				scope = getTopLevelScope(scope);
				return cx.wrap(scope, new WrappedJavaIterator(cx, iterator, scope), TypeInfo.of(WrappedJavaIterator.class));
			}

			// Otherwise, just call the runtime routine
			Scriptable jsIterator = ScriptRuntime.toIterator(cx, scope, obj, keyOnly);
			if (jsIterator != null) {
				return jsIterator;
			}
		}

		// Otherwise, just set up to iterate over the properties of the object.
		// Do not call __iterator__ method.
		IdEnumeration objectIterator = ScriptRuntime.enumInit(cx, scope, obj, keyOnly ? ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR : ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR);
		objectIterator.enumNumbers = true;
		NativeIterator result = new NativeIterator(objectIterator);
		result.setPrototype(getClassPrototype(scope, result.getClassName(), cx));
		result.setParentScope(scope);
		return result;
	}

	/**
	 * If "obj" is a java.util.Iterator or a java.lang.Iterable, return a
	 * wrapping as a JavaScript Iterator. Otherwise, return null.
	 * This method is in VMBridge since Iterable is a JDK 1.5 addition.
	 */
	static private Iterator<?> getJavaIterator(Object obj) {
		if (obj instanceof Wrapper) {
			Object unwrapped = ((Wrapper) obj).unwrap();
			Iterator<?> iterator = null;
			if (unwrapped instanceof Iterator) {
				iterator = (Iterator<?>) unwrapped;
			}
			if (unwrapped instanceof Iterable) {
				iterator = ((Iterable<?>) unwrapped).iterator();
			}
			return iterator;
		}
		return null;
	}

	private IdEnumeration objectIterator;

	// #string_id_map#

	/**
	 * Only for constructing the prototype object.
	 */
	private NativeIterator() {
	}

	private NativeIterator(IdEnumeration objectIterator) {
		this.objectIterator = objectIterator;
	}

	@Override
	public String getClassName() {
		return "Iterator";
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		String s;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 2;
				s = "constructor";
			}
			case Id_next -> {
				arity = 0;
				s = "next";
			}
			case Id___iterator__ -> {
				arity = 1;
				s = ITERATOR_PROPERTY_NAME;
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(ITERATOR_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ITERATOR_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();

		if (id == Id_constructor) {
			return jsConstructor(cx, scope, thisObj, args);
		}

		if (!(thisObj instanceof NativeIterator iterator)) {
			throw incompatibleCallError(f, cx);
		}

		return switch (id) {
			case Id_next -> iterator.objectIterator.nextExec(cx, scope);
			case Id___iterator__ ->
				/// XXX: what about argument? SpiderMonkey apparently ignores it
				thisObj;
			default -> throw new IllegalArgumentException(String.valueOf(id));
		};
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "next" -> Id_next;
			case "__iterator__" -> Id___iterator__;
			case "constructor" -> Id_constructor;
			default -> 0;
		};
	}
}


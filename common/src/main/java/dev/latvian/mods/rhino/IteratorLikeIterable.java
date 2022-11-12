/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This is a class that makes it easier to iterate over "iterator-like" objects as defined
 * in the ECMAScript spec. The caller is responsible for retrieving an object that implements
 * the "iterator" pattern. This class will follow that pattern and throw appropriate
 * JavaScript exceptions.
 * <p>
 * The pattern that the target class should follow is:
 * * It must have a function property called "next"
 * * The function must return an object with a boolean value called "done".
 * * If "done" is true, then the returned object should also contain a "value" property.
 * * If it has a function property called "return" then it will be called
 * when the caller is done iterating.
 */
public class IteratorLikeIterable implements Iterable<Object>, Closeable {
	private final Context cx;
	private final Scriptable scope;
	private final Callable next;
	private final Callable returnFunc;
	private final Scriptable iterator;
	private boolean closed;

	public IteratorLikeIterable(Context cx, Scriptable scope, Object target) {
		this.cx = cx;
		this.scope = scope;
		// This will throw if "next" is not a function or undefined
		next = ScriptRuntime.getPropFunctionAndThis(cx, scope, target, ES6Iterator.NEXT_METHOD);
		iterator = ScriptRuntime.lastStoredScriptable(cx);
		Object retObj = ScriptRuntime.getObjectPropNoWarn(cx, scope, target, ES6Iterator.RETURN_PROPERTY);
		// We only care about "return" if it is not null or undefined
		if ((retObj != null) && !Undefined.isUndefined(retObj)) {
			if (!(retObj instanceof Callable)) {
				throw ScriptRuntime.notFunctionError(cx, target, retObj, ES6Iterator.RETURN_PROPERTY);
			}
			returnFunc = (Callable) retObj;
		} else {
			returnFunc = null;
		}
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			if (returnFunc != null) {
				returnFunc.call(cx, scope, iterator, ScriptRuntime.EMPTY_OBJECTS);
			}
		}
	}

	@Override
	public Itr iterator() {
		return new Itr();
	}

	public final class Itr implements Iterator<Object> {
		private Object nextVal;
		private boolean isDone;

		@Override
		public boolean hasNext() {
			Object val = next.call(cx, scope, iterator, ScriptRuntime.EMPTY_OBJECTS);
			// This will throw if "val" is not an object. 
			// "getObjectPropNoWarn" won't, so do this as follows.
			Object doneval = ScriptableObject.getProperty(ScriptableObject.ensureScriptable(val, cx), ES6Iterator.DONE_PROPERTY, cx);
			if (doneval == Scriptable.NOT_FOUND) {
				doneval = Undefined.instance;
			}
			// It's OK if done is undefined.
			if (ScriptRuntime.toBoolean(cx, doneval)) {
				isDone = true;
				return false;
			}
			nextVal = ScriptRuntime.getObjectPropNoWarn(cx, scope, val, ES6Iterator.VALUE_PROPERTY);
			return true;
		}

		@Override
		public Object next() {
			if (isDone) {
				throw new NoSuchElementException();
			}
			return nextVal;
		}
	}
}

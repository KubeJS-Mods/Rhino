package dev.latvian.mods.rhino;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Consumer;

/**
 * This is the enumeration needed by the for..in statement.
 * <p>
 * See ECMA 12.6.3.
 * <p>
 * IdEnumeration maintains a ObjToIntMap to make sure a given
 * id is enumerated only once across multiple objects in a
 * prototype chain.
 * <p>
 * XXX - ECMA delete doesn't hide properties in the prototype,
 * but js/ref does. This means that the js/ref for..in can
 * avoid maintaining a hash table and instead perform lookups
 * to see if a given property has already been enumerated.
 */
public class IdEnumeration implements Serializable, Consumer<Object> {
	@Serial
	private static final long serialVersionUID = 1L;
	Scriptable obj;
	Object[] ids;
	ObjToIntMap used;
	Object currentId;
	int index;
	int enumType; /* one of ENUM_INIT_KEYS, ENUM_INIT_VALUES, ENUM_INIT_ARRAY, ENUMERATE_VALUES_IN_ORDER */

	// if true, integer ids will be returned as numbers rather than strings
	boolean enumNumbers;

	IdEnumerationIterator iterator;
	public Object tempResult;

	public Boolean next(Context cx) {
		if (iterator != null) {
			if (enumType == ScriptRuntime.ENUMERATE_VALUES_IN_ORDER) {
				if (iterator.enumerationIteratorHasNext(cx, this)) {
					currentId = tempResult;
					tempResult = null;
					return Boolean.TRUE;
				} else {
					tempResult = null;
					return Boolean.FALSE;
				}
			}

			try {
				if (iterator.enumerationIteratorNext(cx, this)) {
					currentId = tempResult;
				}

				return Boolean.TRUE;
			} catch (JavaScriptException e) {
				if (e.getValue() instanceof NativeIterator.StopIteration) {
					return Boolean.FALSE;
				}

				throw e;
			} finally {
				tempResult = null;
			}
		}

		for (; ; ) {
			if (obj == null) {
				return Boolean.FALSE;
			}
			if (index == ids.length) {
				obj = obj.getPrototype();
				changeObject();
				continue;
			}
			Object id = ids[index++];
			if (used != null && used.has(id)) {
				continue;
			}
			if (id instanceof Symbol) {
				continue;
			} else if (id instanceof String strId) {
				if (!obj.has(strId, obj)) {
					continue;   // must have been deleted
				}
				currentId = strId;
			} else {
				int intId = ((Number) id).intValue();
				if (!obj.has(intId, obj)) {
					continue;   // must have been deleted
				}
				currentId = enumNumbers ? Integer.valueOf(intId) : String.valueOf(intId);
			}
			return Boolean.TRUE;
		}
	}

	public void changeObject() {
		Object[] nids = null;
		while (obj != null) {
			nids = obj.getIds();
			if (nids.length != 0) {
				break;
			}
			obj = obj.getPrototype();
		}
		if (obj != null && ids != null) {
			Object[] previous = ids;
			int L = previous.length;
			if (used == null) {
				used = new ObjToIntMap(L);
			}
			for (int i = 0; i != L; ++i) {
				used.intern(previous[i]);
			}
		}
		ids = nids;
		index = 0;
	}

	public Object getId(Context cx) {
		if (iterator != null) {
			return currentId;
		}

		switch (enumType) {
			case ScriptRuntime.ENUMERATE_KEYS:
			case ScriptRuntime.ENUMERATE_KEYS_NO_ITERATOR:
				return currentId;
			case ScriptRuntime.ENUMERATE_VALUES:
			case ScriptRuntime.ENUMERATE_VALUES_NO_ITERATOR:
				return getValue(cx);
			case ScriptRuntime.ENUMERATE_ARRAY:
			case ScriptRuntime.ENUMERATE_ARRAY_NO_ITERATOR:
				Object[] elements = {currentId, getValue(cx)};
				return cx.newArray(ScriptableObject.getTopLevelScope(obj), elements);
			default:
				throw Kit.codeBug();
		}
	}

	public Object getValue(Context cx) {
		Object result;

		if (ScriptRuntime.isSymbol(currentId)) {
			SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(obj);
			result = so.get((Symbol) currentId, obj);
		} else {
			ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, currentId);
			if (s.stringId == null) {
				result = obj.get(s.index, obj);
			} else {
				result = obj.get(s.stringId, obj);
			}
		}

		return result;
	}

	public Object nextExec(Context cx, Scriptable scope) {
		Boolean b = next(cx);

		if (!b) {
			// Out of values. Throw StopIteration.
			throw new JavaScriptException(NativeIterator.getStopIterationObject(scope), null, 0);
		}

		return getId(cx);
	}

	@Override
	public void accept(Object o) {
		tempResult = o;
	}

	/*
	public static Boolean enumNext(Object enumObj) {
		IdEnumeration x = (IdEnumeration) enumObj;
		if (x.iterator != null) {
			if (x.enumType == ENUMERATE_VALUES_IN_ORDER) {
				return enumNextInOrder(x);
			}
			Object v = ScriptableObject.getProperty(x.iterator, "next");
			if (!(v instanceof Callable f)) {
				return Boolean.FALSE;
			}
			Context cx = Context.getContext();
			try {
				x.currentId = f.call(cx, x.iterator.getParentScope(), x.iterator, emptyArgs);
				return Boolean.TRUE;
			} catch (JavaScriptException e) {
				if (e.getValue() instanceof NativeIterator.StopIteration) {
					return Boolean.FALSE;
				}
				throw e;
			}
		}

		// for (; ; )
	}

	private static Boolean enumNextInOrder(IdEnumeration enumObj) {
		Object v = ScriptableObject.getProperty(enumObj.iterator, ES6Iterator.NEXT_METHOD);
		if (!(v instanceof Callable f)) {
			throw notFunctionError(enumObj.iterator, ES6Iterator.NEXT_METHOD);
		}
		Context cx = Context.getContext();
		Scriptable scope = enumObj.iterator.getParentScope();
		Object r = f.call(cx, scope, enumObj.iterator, emptyArgs);
		Scriptable iteratorResult = toObject(cx, scope, r);
		Object done = ScriptableObject.getProperty(iteratorResult, ES6Iterator.DONE_PROPERTY);
		if (done != Scriptable.NOT_FOUND && toBoolean(done)) {
			return Boolean.FALSE;
		}
		enumObj.currentId = ScriptableObject.getProperty(iteratorResult, ES6Iterator.VALUE_PROPERTY);
		return Boolean.TRUE;
	}
	 */
}

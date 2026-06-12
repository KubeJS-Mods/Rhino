package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.regexp.NativeRegExp;

import java.util.Comparator;

/**
 * Abstract Operations for Array-like objects as defined by EcmaScript,
 * shared by NativeArray and (in upstream Rhino) the TypedArray views.
 */
public class ArrayLikeAbstractOperations {
	public enum IterativeOperation {
		EVERY,
		FILTER,
		FOR_EACH,
		MAP,
		SOME,
		FIND,
		FIND_INDEX,
		FIND_LAST,
		FIND_LAST_INDEX,
	}

	public enum ReduceOperation {
		REDUCE,
		REDUCE_RIGHT,
	}

	/**
	 * Implements the methods "every", "filter", "forEach", "map", and "some".
	 */
	public static Object iterativeMethod(Context cx, IdFunctionObject fun, IterativeOperation operation, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (IterativeOperation.FIND == operation || IterativeOperation.FIND_INDEX == operation || IterativeOperation.FIND_LAST == operation || IterativeOperation.FIND_LAST_INDEX == operation) {
			ScriptRuntimeES6.requireObjectCoercible(cx, o, fun);
		}

		long length = NativeArray.getLengthProperty(cx, o, operation == IterativeOperation.MAP);
		Object callbackArg = args.length > 0 ? args[0] : Undefined.INSTANCE;

		Function f = getCallbackArg(cx, callbackArg);
		Scriptable parent = ScriptableObject.getTopLevelScope(f);
		Scriptable thisArg;
		if (args.length < 2 || args[1] == null || args[1] == Undefined.INSTANCE) {
			thisArg = parent;
		} else {
			thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
		}

		Scriptable array = null;
		if (operation == IterativeOperation.FILTER || operation == IterativeOperation.MAP) {
			int resultLength = operation == IterativeOperation.MAP ? (int) length : 0;
			array = cx.newArray(scope, resultLength);
		}
		long j = 0;
		boolean reverse = operation == IterativeOperation.FIND_LAST || operation == IterativeOperation.FIND_LAST_INDEX;
		long start = reverse ? length - 1 : 0;
		long end = reverse ? -1 : length;
		long increment = reverse ? -1 : +1;
		for (long i = start; i != end; i += increment) {
			Object[] innerArgs = new Object[3];
			Object elem = getRawElem(o, i, cx);
			if (elem == Scriptable.NOT_FOUND) {
				if (operation == IterativeOperation.FIND || operation == IterativeOperation.FIND_INDEX || operation == IterativeOperation.FIND_LAST || operation == IterativeOperation.FIND_LAST_INDEX) {
					elem = Undefined.INSTANCE;
				} else {
					continue;
				}
			}
			innerArgs[0] = elem;
			innerArgs[1] = i;
			innerArgs[2] = o;
			Object result = f.call(cx, parent, thisArg, innerArgs);
			switch (operation) {
				case EVERY:
					if (!ScriptRuntime.toBoolean(cx, result)) {
						return Boolean.FALSE;
					}
					break;
				case FILTER:
					if (ScriptRuntime.toBoolean(cx, result)) {
						defineElem(cx, array, j++, innerArgs[0]);
					}
					break;
				case FOR_EACH:
					break;
				case MAP:
					defineElem(cx, array, i, result);
					break;
				case SOME:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return Boolean.TRUE;
					}
					break;
				case FIND:
				case FIND_LAST:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return elem;
					}
					break;
				case FIND_INDEX:
				case FIND_LAST_INDEX:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return ScriptRuntime.wrapNumber(i);
					}
					break;
			}
		}
		return switch (operation) {
			case EVERY -> Boolean.TRUE;
			case FILTER, MAP -> array;
			case SOME -> Boolean.FALSE;
			case FIND_INDEX, FIND_LAST_INDEX -> ScriptRuntime.wrapNumber(-1);
			default -> Undefined.INSTANCE;
		};
	}

	static Function getCallbackArg(Context cx, Object callbackArg) {
		if (!(callbackArg instanceof Function f)) {
			throw ScriptRuntime.notFunctionError(cx, callbackArg);
		}
		if (callbackArg instanceof NativeRegExp) {
			// Previously, it was allowed to pass RegExp instance as a callback (it implements Function)
			// But according to ES2015 21.2.6 Properties of RegExp Instances:
			// > RegExp instances are ordinary objects that inherit properties from the RegExp prototype object.
			// > RegExp instances have internal slots [[RegExpMatcher]], [[OriginalSource]], and [[OriginalFlags]].
			// so, no [[Call]] for RegExp-s
			throw ScriptRuntime.notFunctionError(cx, callbackArg);
		}

		return f;
	}

	static void defineElem(Context cx, Scriptable target, long index, Object value) {
		if (index > Integer.MAX_VALUE) {
			String id = Long.toString(index);
			target.put(cx, id, target, value);
		} else {
			target.put(cx, (int) index, target, value);
		}
	}

	// same as NativeArray::getElem, but without converting NOT_FOUND to undefined
	static Object getRawElem(Scriptable target, long index, Context cx) {
		if (index > Integer.MAX_VALUE) {
			return ScriptableObject.getProperty(target, Long.toString(index), cx);
		}
		return ScriptableObject.getProperty(target, (int) index, cx);
	}

	public static long toSliceIndex(double value, long length) {
		long result;
		if (value < 0.0) {
			if (value + length < 0.0) {
				result = 0;
			} else {
				result = (long) (value + length);
			}
		} else if (value > length) {
			result = length;
		} else {
			result = (long) value;
		}
		return result;
	}

	/**
	 * Implements the methods "reduce" and "reduceRight".
	 */
	public static Object reduceMethod(Context cx, ReduceOperation operation, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		long length = NativeArray.getLengthProperty(cx, o, false);
		Object callbackArg = args.length > 0 ? args[0] : Undefined.INSTANCE;
		if (callbackArg == null || !(callbackArg instanceof Function f)) {
			throw ScriptRuntime.notFunctionError(cx, callbackArg);
		}
		Scriptable parent = ScriptableObject.getTopLevelScope(f);
		// hack to serve both reduce and reduceRight with the same loop
		boolean movingLeft = operation == ReduceOperation.REDUCE;
		Object value = args.length > 1 ? args[1] : Scriptable.NOT_FOUND;
		for (long i = 0; i < length; i++) {
			long index = movingLeft ? i : (length - 1 - i);
			Object elem = getRawElem(o, index, cx);
			if (elem == Scriptable.NOT_FOUND) {
				continue;
			}
			if (value == Scriptable.NOT_FOUND) {
				// no initial value passed, use first element found as inital value
				value = elem;
			} else {
				Object[] innerArgs = {value, elem, index, o};
				value = f.call(cx, parent, parent, innerArgs);
			}
		}
		if (value == Scriptable.NOT_FOUND) {
			// reproduce spidermonkey error message
			throw ScriptRuntime.typeError0(cx, "msg.empty.array.reduce");
		}
		return value;
	}

	public static Comparator<Object> getSortComparator(final Context cx, final Scriptable scope, final Object[] args) {
		if (args.length > 0 && Undefined.INSTANCE != args[0]) {
			return getSortComparatorFromArguments(cx, scope, args);
		}
		return new ElementComparator(new StringLikeComparator(cx));
	}

	public static ElementComparator getSortComparatorFromArguments(final Context cx, final Scriptable scope, final Object[] args) {
		final Callable jsCompareFunction = ScriptRuntime.getValueFunctionAndThis(cx, args[0]);
		final Scriptable funThis = cx.lastStoredScriptable();
		final Object[] cmpBuf = new Object[2]; // Buffer for cmp arguments
		return new ElementComparator((x, y) -> {
			// This comparator is invoked only for non-undefined objects
			cmpBuf[0] = x;
			cmpBuf[1] = y;
			Object ret = jsCompareFunction.call(cx, scope, funThis, cmpBuf);
			double d = ScriptRuntime.toNumber(cx, ret);
			int cmp = Double.compare(d, 0);
			if (cmp < 0) {
				return -1;
			} else if (cmp > 0) {
				return +1;
			}
			return 0;
		});
	}

	// Comparators for the js_sort method. Putting them here lets us unit-test them better.

	public record StringLikeComparator(Context cx) implements Comparator<Object> {
		@Override
		public int compare(final Object x, final Object y) {
			final String a = ScriptRuntime.toString(cx, x);
			final String b = ScriptRuntime.toString(cx, y);
			return a.compareTo(b);
		}
	}

	public record ElementComparator(Comparator<Object> child) implements Comparator<Object> {
		@Override
		public int compare(final Object x, final Object y) {
			// Sort NOT_FOUND to very end, Undefined before that, exclusively, as per
			// ECMA 22.1.3.25.1.
			if (x == Undefined.INSTANCE) {
				if (y == Undefined.INSTANCE) {
					return 0;
				}
				if (y == Scriptable.NOT_FOUND) {
					return -1;
				}
				return 1;
			} else if (x == Scriptable.NOT_FOUND) {
				return y == Scriptable.NOT_FOUND ? 0 : 1;
			}

			if (y == Scriptable.NOT_FOUND) {
				return -1;
			}
			if (y == Undefined.INSTANCE) {
				return -1;
			}

			return child.compare(x, y);
		}
	}
}

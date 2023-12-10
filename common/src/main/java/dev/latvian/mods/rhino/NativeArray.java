/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.regexp.NativeRegExp;
import dev.latvian.mods.rhino.util.DataObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * This class implements the Array native object.
 *
 * @author Norris Boyd
 * @author Mike McCabe
 */
public class NativeArray extends IdScriptableObject implements List, DataObject {
	/*
	 * Optimization possibilities and open issues:
	 * - Long vs. double schizophrenia.  I suspect it might be better
	 * to use double throughout.
	 *
	 * - Functions that need a new Array call "new Array" in the
	 * current scope rather than using a hardwired constructor;
	 * "Array" could be redefined.  It turns out that js calls the
	 * equivalent of "new Array" in the current scope, except that it
	 * always gets at least an object back, even when Array == null.
	 */

	private static final Object ARRAY_TAG = "Array";
	private static final Long NEGATIVE_ONE = (long) -1;
	private static final int Id_length = 1;
	private static final int MAX_INSTANCE_ID = 1;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_toSource = 4;
	private static final int Id_join = 5;
	private static final int Id_reverse = 6;
	private static final int Id_sort = 7;
	private static final int Id_push = 8;
	private static final int Id_pop = 9;
	private static final int Id_shift = 10;
	private static final int Id_unshift = 11;
	private static final int Id_splice = 12;
	private static final int Id_concat = 13;
	private static final int Id_slice = 14;
	private static final int Id_indexOf = 15;
	private static final int Id_lastIndexOf = 16;
	private static final int Id_every = 17;
	private static final int Id_filter = 18;
	private static final int Id_forEach = 19;
	private static final int Id_map = 20;
	private static final int Id_some = 21;
	private static final int Id_find = 22;
	private static final int Id_findIndex = 23;
	private static final int Id_reduce = 24;
	private static final int Id_reduceRight = 25;
	private static final int Id_fill = 26;
	private static final int Id_keys = 27;
	private static final int Id_values = 28;
	private static final int Id_entries = 29;
	private static final int Id_includes = 30;
	private static final int Id_copyWithin = 31;
	private static final int SymbolId_iterator = 32;
	private static final int MAX_PROTOTYPE_ID = SymbolId_iterator;
	private static final int ConstructorId_join = -Id_join;
	private static final int ConstructorId_reverse = -Id_reverse;
	private static final int ConstructorId_sort = -Id_sort;
	private static final int ConstructorId_push = -Id_push;
	private static final int ConstructorId_pop = -Id_pop;
	private static final int ConstructorId_shift = -Id_shift;
	private static final int ConstructorId_unshift = -Id_unshift;
	private static final int ConstructorId_splice = -Id_splice;
	private static final int ConstructorId_concat = -Id_concat;
	private static final int ConstructorId_slice = -Id_slice;
	private static final int ConstructorId_indexOf = -Id_indexOf;
	private static final int ConstructorId_lastIndexOf = -Id_lastIndexOf;
	private static final int ConstructorId_every = -Id_every;
	private static final int ConstructorId_filter = -Id_filter;
	private static final int ConstructorId_forEach = -Id_forEach;
	private static final int ConstructorId_map = -Id_map;
	private static final int ConstructorId_some = -Id_some;
	private static final int ConstructorId_find = -Id_find;
	private static final int ConstructorId_findIndex = -Id_findIndex;
	private static final int ConstructorId_reduce = -Id_reduce;
	private static final int ConstructorId_reduceRight = -Id_reduceRight;
	private static final int ConstructorId_isArray = -26;
	private static final int ConstructorId_of = -27;
	private static final int ConstructorId_from = -28;
	/**
	 * The default capacity for <code>dense</code>.
	 */
	private static final int DEFAULT_INITIAL_CAPACITY = 10;
	/**
	 * The factor to grow <code>dense</code> by.
	 */
	private static final double GROW_FACTOR = 1.5;
	private static final int MAX_PRE_GROW_SIZE = (int) (Integer.MAX_VALUE / GROW_FACTOR);
	/**
	 * The maximum size of <code>dense</code> that will be allocated initially.
	 */
	private static int maximumInitialCapacity = 10000;

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
			if (x == Undefined.instance) {
				if (y == Undefined.instance) {
					return 0;
				}
				if (y == NOT_FOUND) {
					return -1;
				}
				return 1;
			} else if (x == NOT_FOUND) {
				return y == NOT_FOUND ? 0 : 1;
			}

			if (y == NOT_FOUND) {
				return -1;
			}
			if (y == Undefined.instance) {
				return -1;
			}

			return child.compare(x, y);
		}
	}

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeArray obj = new NativeArray(cx, 0);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	static int getMaximumInitialCapacity() {
		return maximumInitialCapacity;
	}

	static void setMaximumInitialCapacity(int maximumInitialCapacity) {
		NativeArray.maximumInitialCapacity = maximumInitialCapacity;
	}

	private static long toArrayIndex(Context cx, Object id) {
		if (id instanceof String) {
			return toArrayIndex(cx, (String) id);
		} else if (id instanceof Number) {
			return toArrayIndex(((Number) id).doubleValue());
		}
		return -1;
	}

	// if id is an array index (ECMA 15.4.0), return the number,
	// otherwise return -1L
	private static long toArrayIndex(Context cx, String id) {
		long index = toArrayIndex(ScriptRuntime.toNumber(cx, id));
		// Assume that ScriptRuntime.toString(index) is the same
		// as java.lang.Long.toString(index) for long
		if (Long.toString(index).equals(id)) {
			return index;
		}
		return -1;
	}

	// methods to implement java.util.List

	private static long toArrayIndex(double d) {
		if (!Double.isNaN(d)) {
			long index = ScriptRuntime.toUint32(d);
			if (index == d && index != 4294967295L) {
				return index;
			}
		}
		return -1;
	}

	private static int toDenseIndex(Context cx, Object id) {
		long index = toArrayIndex(cx, id);
		return 0 <= index && index < Integer.MAX_VALUE ? (int) index : -1;
	}

	/**
	 * See ECMA 15.4.1,2
	 */
	private static Object jsConstructor(Context cx, Scriptable scope, Object[] args) {
		if (args.length == 0) {
			return new NativeArray(cx, 0);
		}

		Object arg0 = args[0];
		if (args.length > 1 || !(arg0 instanceof Number)) {
			return new NativeArray(cx, args);
		}
		long len = ScriptRuntime.toUint32(cx, arg0);
		if (len != ((Number) arg0).doubleValue()) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}
		return new NativeArray(cx, len);
	}

	private static Scriptable callConstructorOrCreateArray(Context cx, Scriptable scope, Scriptable arg, long length, boolean lengthAlways) {
		Scriptable result = null;

		if (arg instanceof Function) {
			try {
				final Object[] args = (lengthAlways || (length > 0)) ? new Object[]{length} : ScriptRuntime.EMPTY_OBJECTS;
				result = ((Function) arg).construct(cx, scope, args);
			} catch (EcmaError ee) {
				if (!"TypeError".equals(ee.getName())) {
					throw ee;
				}
				// If we get here then it is likely that the function we called is not really
				// a constructor. Unfortunately there's no better way to tell in Rhino right now.
			}
		}

		if (result == null) {
			// "length" below is really a hint so don't worry if it's really large
			result = cx.newArray(scope, (length > Integer.MAX_VALUE) ? 0 : (int) length);
		}

		return result;
	}

	private static Object js_from(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		final Scriptable items = ScriptRuntime.toObject(cx, scope, (args.length >= 1) ? args[0] : Undefined.instance);
		Object mapArg = (args.length >= 2) ? args[1] : Undefined.instance;
		Scriptable thisArg = Undefined.SCRIPTABLE_UNDEFINED;
		final boolean mapping = !Undefined.isUndefined(mapArg);
		Function mapFn = null;

		if (mapping) {
			if (!(mapArg instanceof Function)) {
				throw ScriptRuntime.typeError0(cx, "msg.map.function.not");
			}
			mapFn = (Function) mapArg;
			if (args.length >= 3) {
				thisArg = ensureScriptable(args[2], cx);
			}
		}

		Object iteratorProp = getProperty(items, SymbolKey.ITERATOR, cx);
		if (!(items instanceof NativeArray) && (iteratorProp != NOT_FOUND) && !Undefined.isUndefined(iteratorProp)) {
			final Object iterator = ScriptRuntime.callIterator(cx, scope, items);
			if (!Undefined.isUndefined(iterator)) {
				final Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, 0, false);
				long k = 0;
				try (IteratorLikeIterable it = new IteratorLikeIterable(cx, scope, iterator)) {
					for (Object temp : it) {
						if (mapping) {
							temp = mapFn.call(cx, scope, thisArg, new Object[]{temp, k});
						}
						defineElem(cx, result, k, temp);
						k++;
					}
				}
				setLengthProperty(cx, result, k);
				return result;
			}
		}

		final long length = getLengthProperty(cx, items, false);
		final Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, length, true);
		for (long k = 0; k < length; k++) {
			Object temp = getRawElem(items, k, cx);
			if (temp != NOT_FOUND) {
				if (mapping) {
					temp = mapFn.call(cx, scope, thisArg, new Object[]{temp, k});
				}
				defineElem(cx, result, k, temp);
			}
		}

		setLengthProperty(cx, result, length);
		return result;
	}

	private static Object js_of(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		final Scriptable result = callConstructorOrCreateArray(cx, scope, thisObj, args.length, true);

		for (int i = 0; i < args.length; i++) {
			defineElem(cx, result, i, args[i]);
		}
		setLengthProperty(cx, result, args.length);

		return result;
	}

	/* Support for generic Array-ish objects.  Most of the Array
	 * functions try to be generic; anything that has a length
	 * property is assumed to be an array.
	 * getLengthProperty returns 0 if obj does not have the length property
	 * or its value is not convertible to a number.
	 */
	static long getLengthProperty(Context cx, Scriptable obj, boolean throwIfTooLarge) {
		// These will both give numeric lengths within Uint32 range.
		if (obj instanceof NativeString) {
			return ((NativeString) obj).getLength();
		}
		if (obj instanceof NativeArray) {
			return ((NativeArray) obj).getLength();
		}

		Object len = getProperty(obj, "length", cx);
		if (len == NOT_FOUND) {
			// toUint32(undefined) == 0
			return 0;
		}

		double doubleLen = ScriptRuntime.toNumber(cx, len);
		if (doubleLen > NativeNumber.MAX_SAFE_INTEGER) {
			if (throwIfTooLarge) {
				String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
				throw ScriptRuntime.rangeError(cx, msg);
			}
			return (int) NativeNumber.MAX_SAFE_INTEGER;
		}
		if (doubleLen < 0) {
			return 0;
		}
		return ScriptRuntime.toUint32(cx, len);
	}

	private static Object setLengthProperty(Context cx, Scriptable target, long length) {
		Object len = ScriptRuntime.wrapNumber(length);
		putProperty(target, "length", len, cx);
		return len;
	}

	/* Utility functions to encapsulate index > Integer.MAX_VALUE
	 * handling.  Also avoids unnecessary object creation that would
	 * be necessary to use the general ScriptRuntime.get/setElem
	 * functions... though this is probably premature optimization.
	 */
	private static void deleteElem(Scriptable target, long index, Context cx) {
		int i = (int) index;
		if (i == index) {
			target.delete(cx, i);
		} else {
			target.delete(cx, Long.toString(index));
		}
	}

	private static Object getElem(Context cx, Scriptable target, long index) {
		Object elem = getRawElem(target, index, cx);
		return (elem != NOT_FOUND ? elem : Undefined.instance);
	}

	// same as getElem, but without converting NOT_FOUND to undefined
	private static Object getRawElem(Scriptable target, long index, Context cx) {
		if (index > Integer.MAX_VALUE) {
			return getProperty(target, Long.toString(index), cx);
		}
		return getProperty(target, (int) index, cx);
	}

	private static void defineElem(Context cx, Scriptable target, long index, Object value) {
		if (index > Integer.MAX_VALUE) {
			String id = Long.toString(index);
			target.put(cx, id, target, value);
		} else {
			target.put(cx, (int) index, target, value);
		}
	}

	private static void setElem(Context cx, Scriptable target, long index, Object value) {
		if (index > Integer.MAX_VALUE) {
			String id = Long.toString(index);
			putProperty(target, id, value, cx);
		} else {
			putProperty(target, (int) index, value, cx);
		}
	}

	// Similar as setElem(), but triggers deleteElem() if value is NOT_FOUND
	private static void setRawElem(Context cx, Scriptable target, long index, Object value) {
		if (value == NOT_FOUND) {
			deleteElem(target, index, cx);
		} else {
			setElem(cx, target, index, value);
		}
	}

	private static String toStringHelper(Context cx, Scriptable scope, Scriptable thisObj, boolean toLocale) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		/* It's probably redundant to handle long lengths in this
		 * function; StringBuilders are limited to 2^31 in java.
		 */
		long length = getLengthProperty(cx, o, false);

		StringBuilder result = new StringBuilder(256);

		// whether to return '4,unquoted,5' or '[4, "quoted", 5]'
		String separator;

		separator = ",";

		boolean haslast = false;
		long i = 0;

		boolean toplevel, iterating;
		if (cx.iterating == null) {
			toplevel = true;
			iterating = false;
			cx.iterating = new ObjToIntMap(31);
		} else {
			toplevel = false;
			iterating = cx.iterating.has(o);
		}

		// Make sure cx.iterating is set to null when done
		// so we don't leak memory
		try {
			if (!iterating) {
				// stop recursion
				cx.iterating.put(o, 0);

				// make toSource print null and undefined values in recent versions
				for (i = 0; i < length; i++) {
					if (i > 0) {
						result.append(separator);
					}
					Object elem = getRawElem(o, i, cx);
					if (elem == NOT_FOUND || elem == null || elem == Undefined.instance) {
						haslast = false;
						continue;
					}
					haslast = true;

					if (false) {
						result.append(ScriptRuntime.uneval(cx, scope, elem));

					} else if (elem instanceof String) {
						result.append((String) elem);

					} else {
						if (toLocale) {
							Callable fun;
							Scriptable funThis;
							fun = ScriptRuntime.getPropFunctionAndThis(cx, scope, elem, "toLocaleString");
							funThis = cx.lastStoredScriptable();
							elem = fun.call(cx, scope, funThis, ScriptRuntime.EMPTY_OBJECTS);
						}
						result.append(ScriptRuntime.toString(cx, elem));
					}
				}

				// processing of thisObj done, remove it from the recursion detector
				// to allow thisObj to be again in the array later on
				cx.iterating.remove(o);
			}
		} finally {
			if (toplevel) {
				cx.iterating = null;
			}
		}

		if (false) {
			//for [,,].length behavior; we want toString to be symmetric.
			if (!haslast && i > 0) {
				result.append(", ]");
			} else {
				result.append(']');
			}
		}
		return result.toString();
	}

	/**
	 * See ECMA 15.4.4.3
	 */
	private static String js_join(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		long llength = getLengthProperty(cx, o, false);
		int length = (int) llength;
		if (llength != length) {
			throw Context.reportRuntimeError1("msg.arraylength.too.big", String.valueOf(llength), cx);
		}
		// if no args, use "," as separator
		String separator = (args.length < 1 || args[0] == Undefined.instance) ? "," : ScriptRuntime.toString(cx, args[0]);
		if (o instanceof NativeArray na) {
			if (na.denseOnly) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < length; i++) {
					if (i != 0) {
						sb.append(separator);
					}
					if (i < na.dense.length) {
						Object temp = na.dense[i];
						if (temp != null && temp != Undefined.instance && temp != NOT_FOUND) {
							sb.append(ScriptRuntime.toString(cx, temp));
						}
					}
				}
				return sb.toString();
			}
		}
		if (length == 0) {
			return "";
		}
		String[] buf = new String[length];
		int total_size = 0;
		for (int i = 0; i != length; i++) {
			Object temp = getElem(cx, o, i);
			if (temp != null && temp != Undefined.instance) {
				String str = ScriptRuntime.toString(cx, temp);
				total_size += str.length();
				buf[i] = str;
			}
		}
		total_size += (length - 1) * separator.length();
		StringBuilder sb = new StringBuilder(total_size);
		for (int i = 0; i != length; i++) {
			if (i != 0) {
				sb.append(separator);
			}
			String str = buf[i];
			if (str != null) {
				// str == null for undefined or null
				sb.append(str);
			}
		}
		return sb.toString();
	}

	/**
	 * See ECMA 15.4.4.4
	 */
	private static Scriptable js_reverse(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray na) {
			if (na.denseOnly) {
				for (int i = 0, j = ((int) na.length) - 1; i < j; i++, j--) {
					Object temp = na.dense[i];
					na.dense[i] = na.dense[j];
					na.dense[j] = temp;
				}
				return o;
			}
		}
		long len = getLengthProperty(cx, o, false);

		long half = len / 2;
		for (long i = 0; i < half; i++) {
			long j = len - i - 1;
			Object temp1 = getRawElem(o, i, cx);
			Object temp2 = getRawElem(o, j, cx);
			setRawElem(cx, o, i, temp2);
			setRawElem(cx, o, j, temp1);
		}
		return o;
	}

	/**
	 * See ECMA 15.4.4.5
	 */
	private static Scriptable js_sort(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		final Comparator<Object> comparator;
		if (args.length > 0 && Undefined.instance != args[0]) {
			final Callable jsCompareFunction = ScriptRuntime.getValueFunctionAndThis(cx, args[0]);
			final Scriptable funThis = cx.lastStoredScriptable();
			final Object[] cmpBuf = new Object[2]; // Buffer for cmp arguments
			comparator = new ElementComparator((x, y) -> {
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
		} else {
			comparator = new ElementComparator(new StringLikeComparator(cx));
		}

		long llength = getLengthProperty(cx, o, false);
		final int length = (int) llength;
		if (llength != length) {
			throw Context.reportRuntimeError1("msg.arraylength.too.big", String.valueOf(llength), cx);
		}
		// copy the JS array into a working array, so it can be
		// sorted cheaply.
		final Object[] working = new Object[length];
		for (int i = 0; i != length; ++i) {
			working[i] = getRawElem(o, i, cx);
		}

		Sorting.get().hybridSort(working, comparator);

		// copy the working array back into thisObj
		for (int i = 0; i < length; ++i) {
			setRawElem(cx, o, i, working[i]);
		}

		return o;
	}

	private static Object js_push(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray na) {
			if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
				for (Object arg : args) {
					na.dense[(int) na.length++] = arg;
				}
				return ScriptRuntime.wrapNumber(na.length);
			}
		}
		long length = getLengthProperty(cx, o, false);
		for (int i = 0; i < args.length; i++) {
			setElem(cx, o, length + i, args[i]);
		}

		length += args.length;

		return setLengthProperty(cx, o, length);
	}

	private static Object js_pop(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		Object result;
		if (o instanceof NativeArray na) {
			if (na.denseOnly && na.length > 0) {
				na.length--;
				result = na.dense[(int) na.length];
				na.dense[(int) na.length] = NOT_FOUND;
				return result;
			}
		}
		long length = getLengthProperty(cx, o, false);
		if (length > 0) {
			length--;

			// Get the to-be-deleted property's value.
			result = getElem(cx, o, length);

			// We need to delete the last property, because 'thisObj' may not
			// have setLength which does that for us.
			deleteElem(o, length, cx);
		} else {
			result = Undefined.instance;
		}
		// necessary to match js even when length < 0; js pop will give a
		// length property to any target it is called on.
		setLengthProperty(cx, o, length);

		return result;
	}

	private static Object js_shift(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray na) {
			if (na.denseOnly && na.length > 0) {
				na.length--;
				Object result = na.dense[0];
				System.arraycopy(na.dense, 1, na.dense, 0, (int) na.length);
				na.dense[(int) na.length] = NOT_FOUND;
				return result == NOT_FOUND ? Undefined.instance : result;
			}
		}
		Object result;
		long length = getLengthProperty(cx, o, false);
		if (length > 0) {
			long i = 0;
			length--;

			// Get the to-be-deleted property's value.
			result = getElem(cx, o, i);

			/*
			 * Slide down the array above the first element.  Leave i
			 * set to point to the last element.
			 */
			if (length > 0) {
				for (i = 1; i <= length; i++) {
					Object temp = getRawElem(o, i, cx);
					setRawElem(cx, o, i - 1, temp);
				}
			}
			// We need to delete the last property, because 'thisObj' may not
			// have setLength which does that for us.
			deleteElem(o, length, cx);
		} else {
			result = Undefined.instance;
		}
		setLengthProperty(cx, o, length);
		return result;
	}

	private static Object js_unshift(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		if (o instanceof NativeArray na) {
			if (na.denseOnly && na.ensureCapacity((int) na.length + args.length)) {
				System.arraycopy(na.dense, 0, na.dense, args.length, (int) na.length);
				System.arraycopy(args, 0, na.dense, 0, args.length);
				na.length += args.length;
				return ScriptRuntime.wrapNumber(na.length);
			}
		}
		long length = getLengthProperty(cx, o, false);
		int argc = args.length;

		if (args.length > 0) {
			/*  Slide up the array to make room for args at the bottom */
			if (length > 0) {
				for (long last = length - 1; last >= 0; last--) {
					Object temp = getRawElem(o, last, cx);
					setRawElem(cx, o, last + argc, temp);
				}
			}

			/* Copy from argv to the bottom of the array. */
			for (int i = 0; i < args.length; i++) {
				setElem(cx, o, i, args[i]);
			}
		}
		/* Follow Perl by returning the new array length. */
		length += args.length;
		return setLengthProperty(cx, o, length);
	}

	private static Object js_splice(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		NativeArray na = null;
		boolean denseMode = false;
		if (o instanceof NativeArray) {
			na = (NativeArray) o;
			denseMode = na.denseOnly;
		}

		/* create an empty Array to return. */
		scope = getTopLevelScope(scope);
		int argc = args.length;
		if (argc == 0) {
			return cx.newArray(scope, 0);
		}
		long length = getLengthProperty(cx, o, false);

		/* Convert the first argument into a starting index. */
		long begin = toSliceIndex(ScriptRuntime.toInteger(cx, args[0]), length);
		argc--;

		/* Convert the second argument into count */
		long count;
		if (args.length == 1) {
			count = length - begin;
		} else {
			double dcount = ScriptRuntime.toInteger(cx, args[1]);
			if (dcount < 0) {
				count = 0;
			} else if (dcount > (length - begin)) {
				count = length - begin;
			} else {
				count = (long) dcount;
			}
			argc--;
		}

		long end = begin + count;

		/* If there are elements to remove, put them into the return value. */
		Object result;
		if (count != 0) {
			if (denseMode) {
				int intLen = (int) (end - begin);
				Object[] copy = new Object[intLen];
				System.arraycopy(na.dense, (int) begin, copy, 0, intLen);
				result = cx.newArray(scope, copy);
			} else {
				Scriptable resultArray = cx.newArray(scope, 0);
				for (long last = begin; last != end; last++) {
					Object temp = getRawElem(o, last, cx);
					if (temp != NOT_FOUND) {
						setElem(cx, resultArray, last - begin, temp);
					}
				}
				// Need to set length for sparse result array
				setLengthProperty(cx, resultArray, end - begin);
				result = resultArray;
			}
		} else {
			result = cx.newArray(scope, 0);
		}

		/* Find the direction (up or down) to copy and make way for argv. */
		long delta = argc - count;
		if (denseMode && length + delta < Integer.MAX_VALUE && na.ensureCapacity((int) (length + delta))) {
			System.arraycopy(na.dense, (int) end, na.dense, (int) (begin + argc), (int) (length - end));
			if (argc > 0) {
				System.arraycopy(args, 2, na.dense, (int) begin, argc);
			}
			if (delta < 0) {
				Arrays.fill(na.dense, (int) (length + delta), (int) length, NOT_FOUND);
			}
			na.length = length + delta;
			return result;
		}

		if (delta > 0) {
			for (long last = length - 1; last >= end; last--) {
				Object temp = getRawElem(o, last, cx);
				setRawElem(cx, o, last + delta, temp);
			}
		} else if (delta < 0) {
			for (long last = end; last < length; last++) {
				Object temp = getRawElem(o, last, cx);
				setRawElem(cx, o, last + delta, temp);
			}
			// Do this backwards because some implementations might use a
			// non-sparse array and therefore might not be able to handle
			// deleting elements "in the middle". This makes us compatible
			// with older Rhino releases.
			for (long k = length - 1; k >= length + delta; --k) {
				deleteElem(o, k, cx);
			}
		}

		/* Copy from argv into the hole to complete the splice. */
		int argoffset = args.length - argc;
		for (int i = 0; i < argc; i++) {
			setElem(cx, o, begin + i, args[i + argoffset]);
		}

		/* Update length in case we deleted elements from the end. */
		setLengthProperty(cx, o, length + delta);
		return result;
	}

	private static boolean isConcatSpreadable(Context cx, Scriptable scope, Object val) {
		// First, look for the new @@isConcatSpreadable test as per ECMAScript 6 and up
		if (val instanceof Scriptable) {
			final Object spreadable = getProperty((Scriptable) val, SymbolKey.IS_CONCAT_SPREADABLE, cx);
			if ((spreadable != NOT_FOUND) && !Undefined.isUndefined(spreadable)) {
				// If @@isConcatSpreadable was undefined, we have to fall back to testing for an array.
				// Otherwise, we found some value
				return ScriptRuntime.toBoolean(cx, spreadable);
			}
		}

		return js_isArray(val);
	}

	// Concat elements of "arg" into the destination, with optimizations for native,
	// dense arrays.
	private static long concatSpreadArg(Context cx, Scriptable result, Scriptable arg, long offset) {
		long srclen = getLengthProperty(cx, arg, false);
		long newlen = srclen + offset;

		// First, optimize for a pair of native, dense arrays
		if ((newlen <= Integer.MAX_VALUE) && (result instanceof final NativeArray denseResult)) {
			if (denseResult.denseOnly && (arg instanceof final NativeArray denseArg)) {
				if (denseArg.denseOnly) {
					// Now we can optimize
					denseResult.ensureCapacity((int) newlen);
					System.arraycopy(denseArg.dense, 0, denseResult.dense, (int) offset, (int) srclen);
					return newlen;
				}
				// We could also optimize here if we are copying to a dense target from a non-dense
				// native array. However, if the source array is very sparse then the result will be
				// very bad -- so don't.
			}
		}

		// If we get here then we have to do things the generic way
		long dstpos = offset;
		for (long srcpos = 0; srcpos < srclen; srcpos++, dstpos++) {
			final Object temp = getRawElem(arg, srcpos, cx);
			if (temp != NOT_FOUND) {
				defineElem(cx, result, dstpos, temp);
			}
		}
		return newlen;
	}

	// Comparators for the js_sort method. Putting them here lets us unit-test them better.

	private static long doConcat(Context cx, Scriptable scope, Scriptable result, Object arg, long offset) {
		if (isConcatSpreadable(cx, scope, arg)) {
			return concatSpreadArg(cx, result, (Scriptable) arg, offset);
		}
		defineElem(cx, result, offset, arg);
		return offset + 1;
	}

	/*
	 * See Ecma 262v3 15.4.4.4
	 */
	private static Scriptable js_concat(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		// create an empty Array to return.
		scope = getTopLevelScope(scope);
		final Scriptable result = cx.newArray(scope, 0);

		long length = doConcat(cx, scope, result, o, 0);
		for (Object arg : args) {
			length = doConcat(cx, scope, result, arg, length);
		}

		setLengthProperty(cx, result, length);
		return result;
	}

	private static Scriptable js_slice(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		Scriptable result = cx.newArray(scope, 0);
		long len = getLengthProperty(cx, o, false);

		long begin, end;
		if (args.length == 0) {
			begin = 0;
			end = len;
		} else {
			begin = toSliceIndex(ScriptRuntime.toInteger(cx, args[0]), len);
			if (args.length == 1 || args[1] == Undefined.instance) {
				end = len;
			} else {
				end = toSliceIndex(ScriptRuntime.toInteger(cx, args[1]), len);
			}
		}

		for (long slot = begin; slot < end; slot++) {
			Object temp = getRawElem(o, slot, cx);
			if (temp != NOT_FOUND) {
				defineElem(cx, result, slot - begin, temp);
			}
		}
		setLengthProperty(cx, result, Math.max(0, end - begin));

		return result;
	}

	private static long toSliceIndex(double value, long length) {
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

	private static Object js_indexOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long length = getLengthProperty(cx, o, false);
		/*
		 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:indexOf
		 * The index at which to begin the search. Defaults to 0, i.e. the
		 * whole array will be searched. If the index is greater than or
		 * equal to the length of the array, -1 is returned, i.e. the array
		 * will not be searched. If negative, it is taken as the offset from
		 * the end of the array. Note that even when the index is negative,
		 * the array is still searched from front to back. If the calculated
		 * index is less than 0, the whole array will be searched.
		 */
		long start;
		if (args.length < 2) {
			// default
			start = 0;
		} else {
			start = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (start < 0) {
				start += length;
				if (start < 0) {
					start = 0;
				}
			}
			if (start > length - 1) {
				return NEGATIVE_ONE;
			}
		}
		if (o instanceof NativeArray na) {
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) start; i < length; i++) {
					Object val = na.dense[i];
					if (val == NOT_FOUND && proto != null) {
						val = getProperty(proto, i, cx);
					}
					if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
						return (long) i;
					}
				}
				return NEGATIVE_ONE;
			}
		}
		for (long i = start; i < length; i++) {
			Object val = getRawElem(o, i, cx);
			if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
				return i;
			}
		}
		return NEGATIVE_ONE;
	}

	private static Object js_lastIndexOf(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long length = getLengthProperty(cx, o, false);
		/*
		 * From http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference:Objects:Array:lastIndexOf
		 * The index at which to start searching backwards. Defaults to the
		 * array's length, i.e. the whole array will be searched. If the
		 * index is greater than or equal to the length of the array, the
		 * whole array will be searched. If negative, it is taken as the
		 * offset from the end of the array. Note that even when the index
		 * is negative, the array is still searched from back to front. If
		 * the calculated index is less than 0, -1 is returned, i.e. the
		 * array will not be searched.
		 */
		long start;
		if (args.length < 2) {
			// default
			start = length - 1;
		} else {
			start = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (start >= length) {
				start = length - 1;
			} else if (start < 0) {
				start += length;
			}
			if (start < 0) {
				return NEGATIVE_ONE;
			}
		}
		if (o instanceof NativeArray na) {
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) start; i >= 0; i--) {
					Object val = na.dense[i];
					if (val == NOT_FOUND && proto != null) {
						val = getProperty(proto, i, cx);
					}
					if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
						return (long) i;
					}
				}
				return NEGATIVE_ONE;
			}
		}
		for (long i = start; i >= 0; i--) {
			Object val = getRawElem(o, i, cx);
			if (val != NOT_FOUND && ScriptRuntime.shallowEq(cx, val, compareTo)) {
				return i;
			}
		}
		return NEGATIVE_ONE;
	}

	/*
       See ECMA-262 22.1.3.13
    */
	private static Boolean js_includes(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Object compareTo = args.length > 0 ? args[0] : Undefined.instance;

		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = ScriptRuntime.toLength(cx, new Object[]{getProperty(thisObj, "length", cx)}, 0);
		if (len == 0) {
			return Boolean.FALSE;
		}

		long k;
		if (args.length < 2) {
			k = 0;
		} else {
			k = (long) ScriptRuntime.toInteger(cx, args[1]);
			if (k < 0) {
				k += len;
				if (k < 0) {
					k = 0;
				}
			}
			if (k > len - 1) {
				return Boolean.FALSE;
			}
		}
		if (o instanceof NativeArray na) {
			if (na.denseOnly) {
				Scriptable proto = na.getPrototype(cx);
				for (int i = (int) k; i < len; i++) {
					Object elementK = na.dense[i];
					if (elementK == NOT_FOUND && proto != null) {
						elementK = getProperty(proto, i, cx);
					}
					if (elementK == NOT_FOUND) {
						elementK = Undefined.instance;
					}
					if (ScriptRuntime.sameZero(cx, elementK, compareTo)) {
						return Boolean.TRUE;
					}
				}
				return Boolean.FALSE;
			}
		}
		for (; k < len; k++) {
			Object elementK = getRawElem(o, k, cx);
			if (elementK == NOT_FOUND) {
				elementK = Undefined.instance;
			}
			if (ScriptRuntime.sameZero(cx, elementK, compareTo)) {
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	private static Object js_fill(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o, false);

		long relativeStart = 0;
		if (args.length >= 2) {
			relativeStart = (long) ScriptRuntime.toInteger(cx, args[1]);
		}
		final long k;
		if (relativeStart < 0) {
			k = Math.max((len + relativeStart), 0);
		} else {
			k = Math.min(relativeStart, len);
		}

		long relativeEnd = len;
		if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
			relativeEnd = (long) ScriptRuntime.toInteger(cx, args[2]);
		}
		final long fin;
		if (relativeEnd < 0) {
			fin = Math.max((len + relativeEnd), 0);
		} else {
			fin = Math.min(relativeEnd, len);
		}

		Object value = args.length > 0 ? args[0] : Undefined.instance;
		for (long i = k; i < fin; i++) {
			setRawElem(cx, thisObj, i, value);
		}

		return thisObj;
	}

	private static Object js_copyWithin(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);
		long len = getLengthProperty(cx, o, false);

		Object targetArg = (args.length >= 1) ? args[0] : Undefined.instance;
		long relativeTarget = (long) ScriptRuntime.toInteger(cx, targetArg);
		long to;
		if (relativeTarget < 0) {
			to = Math.max((len + relativeTarget), 0);
		} else {
			to = Math.min(relativeTarget, len);
		}

		Object startArg = (args.length >= 2) ? args[1] : Undefined.instance;
		long relativeStart = (long) ScriptRuntime.toInteger(cx, startArg);
		long from;
		if (relativeStart < 0) {
			from = Math.max((len + relativeStart), 0);
		} else {
			from = Math.min(relativeStart, len);
		}

		long relativeEnd = len;
		if (args.length >= 3 && !Undefined.isUndefined(args[2])) {
			relativeEnd = (long) ScriptRuntime.toInteger(cx, args[2]);
		}
		final long fin;
		if (relativeEnd < 0) {
			fin = Math.max((len + relativeEnd), 0);
		} else {
			fin = Math.min(relativeEnd, len);
		}

		long count = Math.min(fin - from, len - to);
		int direction = 1;
		if (from < to && to < from + count) {
			direction = -1;
			from = from + count - 1;
			to = to + count - 1;
		}

		// Optimize for a native array. If properties were overridden with setters
		// and other non-default options then we won't get here.
		if ((o instanceof NativeArray na) && (count <= Integer.MAX_VALUE)) {
			if (na.denseOnly) {
				for (; count > 0; count--) {
					na.dense[(int) to] = na.dense[(int) from];
					from += direction;
					to += direction;
				}

				return thisObj;
			}
		}

		for (; count > 0; count--) {
			final Object temp = getRawElem(o, from, cx);
			if ((temp == NOT_FOUND) || Undefined.isUndefined(temp)) {
				deleteElem(o, to, cx);
			} else {
				setElem(cx, o, to, temp);
			}

			from += direction;
			to += direction;
		}

		return thisObj;
	}

	/**
	 * Implements the methods "every", "filter", "forEach", "map", and "some".
	 */
	private static Object iterativeMethod(Context cx, IdFunctionObject idFunctionObject, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		// execIdCall(..) uses a trick for all the ConstructorId_xxx calls
		// they are handled like object calls by adjusting the args list
		// as a result we have to handle ConstructorId_xxx calls (negative id)
		// the same way and always us the abs value of the id for method selection
		int id = Math.abs(idFunctionObject.methodId());
		if (Id_find == id || Id_findIndex == id) {
			ScriptRuntimeES6.requireObjectCoercible(cx, o, idFunctionObject);
		}

		long length = getLengthProperty(cx, o, id == Id_map);
		Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
		if (callbackArg == null || !(callbackArg instanceof Function f)) {
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

		Scriptable parent = getTopLevelScope(f);
		Scriptable thisArg;
		if (args.length < 2 || args[1] == null || args[1] == Undefined.instance) {
			thisArg = parent;
		} else {
			thisArg = ScriptRuntime.toObject(cx, scope, args[1]);
		}

		Scriptable array = null;
		if (id == Id_filter || id == Id_map) {
			int resultLength = id == Id_map ? (int) length : 0;
			array = cx.newArray(scope, resultLength);
		}
		long j = 0;
		for (long i = 0; i < length; i++) {
			Object[] innerArgs = new Object[3];
			Object elem = getRawElem(o, i, cx);
			if (elem == NOT_FOUND) {
				if (id == Id_find || id == Id_findIndex) {
					elem = Undefined.instance;
				} else {
					continue;
				}
			}
			innerArgs[0] = elem;
			innerArgs[1] = i;
			innerArgs[2] = o;
			Object result = f.call(cx, parent, thisArg, innerArgs);
			switch (id) {
				case Id_every:
					if (!ScriptRuntime.toBoolean(cx, result)) {
						return Boolean.FALSE;
					}
					break;
				case Id_filter:
					if (ScriptRuntime.toBoolean(cx, result)) {
						defineElem(cx, array, j++, innerArgs[0]);
					}
					break;
				case Id_forEach:
					break;
				case Id_map:
					defineElem(cx, array, i, result);
					break;
				case Id_some:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return Boolean.TRUE;
					}
					break;
				case Id_find:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return elem;
					}
					break;
				case Id_findIndex:
					if (ScriptRuntime.toBoolean(cx, result)) {
						return ScriptRuntime.wrapNumber(i);
					}
					break;
			}
		}
		return switch (id) {
			case Id_every -> Boolean.TRUE;
			case Id_filter, Id_map -> array;
			case Id_some -> Boolean.FALSE;
			case Id_findIndex -> ScriptRuntime.wrapNumber(-1);
			default -> Undefined.instance;
		};
	}

	/**
	 * Implements the methods "reduce" and "reduceRight".
	 */
	private static Object reduceMethod(Context cx, int id, Scriptable scope, Scriptable thisObj, Object[] args) {
		Scriptable o = ScriptRuntime.toObject(cx, scope, thisObj);

		long length = getLengthProperty(cx, o, false);
		Object callbackArg = args.length > 0 ? args[0] : Undefined.instance;
		if (callbackArg == null || !(callbackArg instanceof Function f)) {
			throw ScriptRuntime.notFunctionError(cx, callbackArg);
		}
		Scriptable parent = getTopLevelScope(f);
		// hack to serve both reduce and reduceRight with the same loop
		boolean movingLeft = id == Id_reduce;
		Object value = args.length > 1 ? args[1] : NOT_FOUND;
		for (long i = 0; i < length; i++) {
			long index = movingLeft ? i : (length - 1 - i);
			Object elem = getRawElem(o, index, cx);
			if (elem == NOT_FOUND) {
				continue;
			}
			if (value == NOT_FOUND) {
				// no initial value passed, use first element found as inital value
				value = elem;
			} else {
				Object[] innerArgs = {value, elem, index, o};
				value = f.call(cx, parent, parent, innerArgs);
			}
		}
		if (value == NOT_FOUND) {
			// reproduce spidermonkey error message
			throw ScriptRuntime.typeError0(cx, "msg.empty.array.reduce");
		}
		return value;
	}

	private static boolean js_isArray(Object o) {
		return o instanceof NativeJavaList || o instanceof List || o instanceof Scriptable s && "Array".equals(s.getClassName());
	}

	private final Context localContext;

	/**
	 * Internal representation of the JavaScript array's length property.
	 */
	private long length;
	/**
	 * Attributes of the array's length property
	 */
	private int lengthAttr = DONTENUM | PERMANENT;
	/**
	 * Fast storage for dense arrays. Sparse arrays will use the superclass's
	 * hashtable storage scheme.
	 */
	private Object[] dense;
	/**
	 * True if all numeric properties are stored in <code>dense</code>.
	 */
	private boolean denseOnly;

	public NativeArray(Context cx, long lengthArg) {
		localContext = cx;
		denseOnly = lengthArg <= maximumInitialCapacity;
		if (denseOnly) {
			int intLength = (int) lengthArg;
			if (intLength < DEFAULT_INITIAL_CAPACITY) {
				intLength = DEFAULT_INITIAL_CAPACITY;
			}
			dense = new Object[intLength];
			Arrays.fill(dense, NOT_FOUND);
		}
		length = lengthArg;
	}

	public NativeArray(Context cx, Object[] array) {
		localContext = cx;
		denseOnly = true;
		dense = array;
		length = array.length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		for (int i = 0; i < size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}

			sb.append(get(i));
		}

		return sb.append(']').toString();
	}

	@Override
	public String getClassName() {
		return "Array";
	}

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected void setInstanceIdAttributes(int id, int attr, Context cx) {
		if (id == Id_length) {
			lengthAttr = attr;
		}
	}

	@Override
	protected int findInstanceIdInfo(String s, Context cx) {
		if (s.equals("length")) {
			return instanceIdInfo(lengthAttr, Id_length);
		}
		return super.findInstanceIdInfo(s, cx);
	}

	@Override
	protected String getInstanceIdName(int id) {
		if (id == Id_length) {
			return "length";
		}
		return super.getInstanceIdName(id);
	}

	@Override
	protected Object getInstanceIdValue(int id, Context cx) {
		if (id == Id_length) {
			return ScriptRuntime.wrapNumber(length);
		}
		return super.getInstanceIdValue(id, cx);
	}

	@Override
	protected void setInstanceIdValue(int id, Object value, Context cx) {
		if (id == Id_length) {
			setLength(cx, value);
			return;
		}
		super.setInstanceIdValue(id, value, cx);
	}

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor, Context cx) {
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_join, "join", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reverse, "reverse", 0, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_sort, "sort", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_push, "push", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_pop, "pop", 0, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_shift, "shift", 0, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_unshift, "unshift", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_splice, "splice", 2, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_concat, "concat", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_slice, "slice", 2, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_indexOf, "indexOf", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_lastIndexOf, "lastIndexOf", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_every, "every", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_filter, "filter", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_forEach, "forEach", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_map, "map", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_some, "some", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_find, "find", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_findIndex, "findIndex", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reduce, "reduce", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_reduceRight, "reduceRight", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_isArray, "isArray", 1, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_of, "of", 0, cx);
		addIdFunctionProperty(ctor, ARRAY_TAG, ConstructorId_from, "from", 1, cx);
		super.fillConstructorProperties(ctor, cx);
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		if (id == SymbolId_iterator) {
			initPrototypeMethod(ARRAY_TAG, id, SymbolKey.ITERATOR, "[Symbol.iterator]", 0, cx);
			return;
		}

		String s, fnName = null;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 1;
				s = "constructor";
			}
			case Id_toString -> {
				arity = 0;
				s = "toString";
			}
			case Id_toLocaleString -> {
				arity = 0;
				s = "toLocaleString";
			}
			case Id_toSource -> {
				arity = 0;
				s = "toSource";
			}
			case Id_join -> {
				arity = 1;
				s = "join";
			}
			case Id_reverse -> {
				arity = 0;
				s = "reverse";
			}
			case Id_sort -> {
				arity = 1;
				s = "sort";
			}
			case Id_push -> {
				arity = 1;
				s = "push";
			}
			case Id_pop -> {
				arity = 0;
				s = "pop";
			}
			case Id_shift -> {
				arity = 0;
				s = "shift";
			}
			case Id_unshift -> {
				arity = 1;
				s = "unshift";
			}
			case Id_splice -> {
				arity = 2;
				s = "splice";
			}
			case Id_concat -> {
				arity = 1;
				s = "concat";
			}
			case Id_slice -> {
				arity = 2;
				s = "slice";
			}
			case Id_indexOf -> {
				arity = 1;
				s = "indexOf";
			}
			case Id_lastIndexOf -> {
				arity = 1;
				s = "lastIndexOf";
			}
			case Id_every -> {
				arity = 1;
				s = "every";
			}
			case Id_filter -> {
				arity = 1;
				s = "filter";
			}
			case Id_forEach -> {
				arity = 1;
				s = "forEach";
			}
			case Id_map -> {
				arity = 1;
				s = "map";
			}
			case Id_some -> {
				arity = 1;
				s = "some";
			}
			case Id_find -> {
				arity = 1;
				s = "find";
			}
			case Id_findIndex -> {
				arity = 1;
				s = "findIndex";
			}
			case Id_reduce -> {
				arity = 1;
				s = "reduce";
			}
			case Id_reduceRight -> {
				arity = 1;
				s = "reduceRight";
			}
			case Id_fill -> {
				arity = 1;
				s = "fill";
			}
			case Id_keys -> {
				arity = 0;
				s = "keys";
			}
			case Id_values -> {
				arity = 0;
				s = "values";
			}
			case Id_entries -> {
				arity = 0;
				s = "entries";
			}
			case Id_includes -> {
				arity = 1;
				s = "includes";
			}
			case Id_copyWithin -> {
				arity = 2;
				s = "copyWithin";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}

		initPrototypeMethod(ARRAY_TAG, id, s, fnName, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ARRAY_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		again:
		for (; ; ) {
			switch (id) {
				case ConstructorId_join:
				case ConstructorId_reverse:
				case ConstructorId_sort:
				case ConstructorId_push:
				case ConstructorId_pop:
				case ConstructorId_shift:
				case ConstructorId_unshift:
				case ConstructorId_splice:
				case ConstructorId_concat:
				case ConstructorId_slice:
				case ConstructorId_indexOf:
				case ConstructorId_lastIndexOf:
				case ConstructorId_every:
				case ConstructorId_filter:
				case ConstructorId_forEach:
				case ConstructorId_map:
				case ConstructorId_some:
				case ConstructorId_find:
				case ConstructorId_findIndex:
				case ConstructorId_reduce:
				case ConstructorId_reduceRight: {
					// this is a small trick; we will handle all the ConstructorId_xxx calls
					// the same way the object calls are processed
					// so we adjust the args, inverting the id and
					// restarting the method selection
					// Attention: the implementations have to be aware of this
					if (args.length > 0) {
						thisObj = ScriptRuntime.toObject(cx, scope, args[0]);
						Object[] newArgs = new Object[args.length - 1];
						System.arraycopy(args, 1, newArgs, 0, newArgs.length);
						args = newArgs;
					}
					id = -id;
					continue again;
				}

				case ConstructorId_isArray:
					return args.length > 0 && js_isArray(args[0]);

				case ConstructorId_of: {
					return js_of(cx, scope, thisObj, args);
				}

				case ConstructorId_from: {
					return js_from(cx, scope, thisObj, args);
				}

				case Id_constructor: {
					boolean inNewExpr = (thisObj == null);
					if (!inNewExpr) {
						// IdFunctionObject.construct will set up parent, proto
						return f.construct(cx, scope, args);
					}
					return jsConstructor(cx, scope, args);
				}

				case Id_toString:
					return toStringHelper(cx, scope, thisObj, false);

				case Id_toLocaleString:
					return toStringHelper(cx, scope, thisObj, true);

				case Id_toSource:
					return "not_supported";

				case Id_join:
					return js_join(cx, scope, thisObj, args);

				case Id_reverse:
					return js_reverse(cx, scope, thisObj, args);

				case Id_sort:
					return js_sort(cx, scope, thisObj, args);

				case Id_push:
					return js_push(cx, scope, thisObj, args);

				case Id_pop:
					return js_pop(cx, scope, thisObj, args);

				case Id_shift:
					return js_shift(cx, scope, thisObj, args);

				case Id_unshift:
					return js_unshift(cx, scope, thisObj, args);

				case Id_splice:
					return js_splice(cx, scope, thisObj, args);

				case Id_concat:
					return js_concat(cx, scope, thisObj, args);

				case Id_slice:
					return js_slice(cx, scope, thisObj, args);

				case Id_indexOf:
					return js_indexOf(cx, scope, thisObj, args);

				case Id_lastIndexOf:
					return js_lastIndexOf(cx, scope, thisObj, args);

				case Id_includes:
					return js_includes(cx, scope, thisObj, args);

				case Id_fill:
					return js_fill(cx, scope, thisObj, args);

				case Id_copyWithin:
					return js_copyWithin(cx, scope, thisObj, args);

				case Id_every:
				case Id_filter:
				case Id_forEach:
				case Id_map:
				case Id_some:
				case Id_find:
				case Id_findIndex:
					return iterativeMethod(cx, f, scope, thisObj, args);
				case Id_reduce:
				case Id_reduceRight:
					return reduceMethod(cx, id, scope, thisObj, args);

				case Id_keys:
					thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
					return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.KEYS);

				case Id_entries:
					thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
					return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.ENTRIES);

				case Id_values:
				case SymbolId_iterator:
					thisObj = ScriptRuntime.toObject(cx, scope, thisObj);
					return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.VALUES);
			}
			throw new IllegalArgumentException("Array.prototype has no method: " + f.getFunctionName());
		}
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (!denseOnly && isGetterOrSetter(null, index, false)) {
			return super.get(cx, index, start);
		}
		if (dense != null && 0 <= index && index < dense.length) {
			return dense[index];
		}
		return super.get(cx, index, start);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (!denseOnly && isGetterOrSetter(null, index, false)) {
			return super.has(cx, index, start);
		}
		if (dense != null && 0 <= index && index < dense.length) {
			return dense[index] != NOT_FOUND;
		}
		return super.has(cx, index, start);
	}

	@Override
	public void put(Context cx, String id, Scriptable start, Object value) {
		super.put(cx, id, start, value);
		if (start == this) {
			// If the object is sealed, super will throw exception
			long index = toArrayIndex(cx, id);
			if (index >= length) {
				length = index + 1;
				denseOnly = false;
			}
		}
	}

	private boolean ensureCapacity(int capacity) {
		if (capacity > dense.length) {
			if (capacity > MAX_PRE_GROW_SIZE) {
				denseOnly = false;
				return false;
			}
			capacity = Math.max(capacity, (int) (dense.length * GROW_FACTOR));
			Object[] newDense = new Object[capacity];
			System.arraycopy(dense, 0, newDense, 0, dense.length);
			Arrays.fill(newDense, dense.length, newDense.length, NOT_FOUND);
			dense = newDense;
		}
		return true;
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (start == this && !isSealed(cx) && dense != null && 0 <= index && (denseOnly || !isGetterOrSetter(null, index, true))) {
			if (!isExtensible() && this.length <= index) {
				return;
			} else if (index < dense.length) {
				dense[index] = value;
				if (this.length <= index) {
					this.length = (long) index + 1;
				}
				return;
			} else if (denseOnly && index < dense.length * GROW_FACTOR && ensureCapacity(index + 1)) {
				dense[index] = value;
				this.length = (long) index + 1;
				return;
			} else {
				denseOnly = false;
			}
		}
		super.put(cx, index, start, value);
		if (start == this && (lengthAttr & READONLY) == 0) {
			// only set the array length if given an array index (ECMA 15.4.0)
			if (this.length <= index) {
				// avoid overflowing index!
				this.length = (long) index + 1;
			}
		}
	}

	@Override
	public void delete(Context cx, int index) {
		if (dense != null && 0 <= index && index < dense.length && !isSealed(cx) && (denseOnly || !isGetterOrSetter(null, index, true))) {
			dense[index] = NOT_FOUND;
		} else {
			super.delete(cx, index);
		}
	}

	@Override
	public Object[] getIds(Context cx, boolean nonEnumerable, boolean getSymbols) {
		Object[] superIds = super.getIds(cx, nonEnumerable, getSymbols);
		if (dense == null) {
			return superIds;
		}
		int N = dense.length;
		long currentLength = length;
		if (N > currentLength) {
			N = (int) currentLength;
		}
		if (N == 0) {
			return superIds;
		}
		int superLength = superIds.length;
		Object[] ids = new Object[N + superLength];

		int presentCount = 0;
		for (int i = 0; i != N; ++i) {
			// Replace existing elements by their indexes
			if (dense[i] != NOT_FOUND) {
				ids[presentCount] = i;
				++presentCount;
			}
		}
		if (presentCount != N) {
			// dense contains deleted elems, need to shrink the result
			Object[] tmp = new Object[presentCount + superLength];
			System.arraycopy(ids, 0, tmp, 0, presentCount);
			ids = tmp;
		}
		System.arraycopy(superIds, 0, ids, presentCount, superLength);
		return ids;
	}

	public List<Integer> getIndexIds(Context cx) {
		Object[] ids = getIds(cx);
		List<Integer> indices = new ArrayList<>(ids.length);
		for (Object id : ids) {
			int int32Id = ScriptRuntime.toInt32(cx, id);
			if (int32Id >= 0 && ScriptRuntime.toString(cx, int32Id).equals(ScriptRuntime.toString(cx, id))) {
				indices.add(int32Id);
			}
		}
		return indices;
	}

	private ScriptableObject defaultIndexPropertyDescriptor(Object value, Context cx) {
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		ScriptableObject desc = new NativeObject(cx);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "value", value, EMPTY);
		desc.defineProperty(cx, "writable", Boolean.TRUE, EMPTY);
		desc.defineProperty(cx, "enumerable", Boolean.TRUE, EMPTY);
		desc.defineProperty(cx, "configurable", Boolean.TRUE, EMPTY);
		return desc;
	}

	// #/string_id_map#

	@Override
	public int getAttributes(Context cx, int index) {
		if (dense != null && index >= 0 && index < dense.length && dense[index] != NOT_FOUND) {
			return EMPTY;
		}
		return super.getAttributes(cx, index);
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (dense != null) {
			int index = toDenseIndex(cx, id);
			if (0 <= index && index < dense.length && dense[index] != NOT_FOUND) {
				Object value = dense[index];
				return defaultIndexPropertyDescriptor(value, cx);
			}
		}
		return super.getOwnPropertyDescriptor(cx, id);
	}

	@Override
	protected void defineOwnProperty(Context cx, Object id, ScriptableObject desc, boolean checkValid) {
		if (dense != null) {
			Object[] values = dense;
			dense = null;
			denseOnly = false;
			for (int i = 0; i < values.length; i++) {
				if (values[i] != NOT_FOUND) {
					put(cx, i, this, values[i]);
				}
			}
		}
		long index = toArrayIndex(cx, id);
		if (index >= length) {
			length = index + 1;
		}
		super.defineOwnProperty(cx, id, desc, checkValid);
	}

	public long getLength() {
		return length;
	}

	private void setLength(Context cx, Object val) {
		/* XXX do we satisfy this?
		 * 15.4.5.1 [[Put]](P, V):
		 * 1. Call the [[CanPut]] method of A with name P.
		 * 2. If Result(1) is false, return.
		 * ?
		 */
		if ((lengthAttr & READONLY) != 0) {
			return;
		}

		double d = ScriptRuntime.toNumber(cx, val);
		long longVal = ScriptRuntime.toUint32(d);
		if (longVal != d) {
			String msg = ScriptRuntime.getMessage0("msg.arraylength.bad");
			throw ScriptRuntime.rangeError(cx, msg);
		}

		if (denseOnly) {
			if (longVal < length) {
				// downcast okay because denseOnly
				Arrays.fill(dense, (int) longVal, dense.length, NOT_FOUND);
				length = longVal;
				return;
			} else if (longVal < MAX_PRE_GROW_SIZE && longVal < (length * GROW_FACTOR) && ensureCapacity((int) longVal)) {
				length = longVal;
				return;
			} else {
				denseOnly = false;
			}
		}
		if (longVal < length) {
			// remove all properties between longVal and length
			if (length - longVal > 0x1000) {
				// assume that the representation is sparse
				Object[] e = getIds(cx); // will only find in object itself
				for (Object id : e) {
					if (id instanceof String strId) {
						// > MAXINT will appear as string
						long index = toArrayIndex(cx, strId);
						if (index >= longVal) {
							delete(cx, strId);
						}
					} else {
						int index = (Integer) id;
						if (index >= longVal) {
							delete(cx, index);
						}
					}
				}
			} else {
				// assume a dense representation
				for (long i = longVal; i < length; i++) {
					deleteElem(this, i, cx);
				}
			}
		}
		length = longVal;
	}

	/**
	 * Change the value of the internal flag that determines whether all
	 * storage is handed by a dense backing array rather than an associative
	 * store.
	 *
	 * @param denseOnly new value for denseOnly flag
	 * @throws IllegalArgumentException if an attempt is made to enable
	 *                                  denseOnly after it was disabled; NativeArray code is not written
	 *                                  to handle switching back to a dense representation
	 */
	void setDenseOnly(boolean denseOnly) {
		if (denseOnly && !this.denseOnly) {
			throw new IllegalArgumentException();
		}
		this.denseOnly = denseOnly;
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) > -1;
	}

	@Override
	public Object[] toArray() {
		return toArray(ScriptRuntime.EMPTY_OBJECTS);
	}

	@Override
	public Object[] toArray(Object[] a) {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		int len = (int) longLen;
		Object[] array = a.length >= len ? a : (Object[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), len);
		for (int i = 0; i < len; i++) {
			array[i] = get(i);
		}
		return array;
	}

	@Override
	public boolean containsAll(Collection c) {
		for (Object aC : c) {
			if (!contains(aC)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int size() {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		return (int) longLen;
	}

	@Override
	public boolean isEmpty() {
		return length == 0;
	}

	public Object get(long index, Context cx) {
		if (index < 0 || index >= length) {
			throw new IndexOutOfBoundsException();
		}
		Object value = getRawElem(this, index, cx);
		if (value == NOT_FOUND || value == Undefined.instance) {
			return null;
		} else if (value instanceof Wrapper) {
			return ((Wrapper) value).unwrap();
		} else {
			return value;
		}
	}

	@Override
	public Object get(int index) {
		return get(index, localContext);
	}

	@Override
	public int indexOf(Object o) {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		int len = (int) longLen;
		if (o == null) {
			for (int i = 0; i < len; i++) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < len; i++) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		int len = (int) longLen;
		if (o == null) {
			for (int i = len - 1; i >= 0; i--) {
				if (get(i) == null) {
					return i;
				}
			}
		} else {
			for (int i = len - 1; i >= 0; i--) {
				if (o.equals(get(i))) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public Iterator iterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator listIterator() {
		return listIterator(0);
	}

	@Override
	public ListIterator listIterator(final int start) {
		long longLen = length;
		if (longLen > Integer.MAX_VALUE) {
			throw new IllegalStateException();
		}
		final int len = (int) longLen;

		if (start < 0 || start > len) {
			throw new IndexOutOfBoundsException("Index: " + start);
		}

		return new ListIterator() {

			int cursor = start;

			@Override
			public boolean hasNext() {
				return cursor < len;
			}

			@Override
			public Object next() {
				if (cursor == len) {
					throw new NoSuchElementException();
				}
				return get(cursor++);
			}

			@Override
			public boolean hasPrevious() {
				return cursor > 0;
			}

			@Override
			public Object previous() {
				if (cursor == 0) {
					throw new NoSuchElementException();
				}
				return get(--cursor);
			}

			@Override
			public int nextIndex() {
				return cursor;
			}

			@Override
			public int previousIndex() {
				return cursor - 1;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void add(Object o) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(Object o) {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public boolean add(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object set(int index, Object element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected int findPrototypeId(Symbol k) {
		if (SymbolKey.ITERATOR.equals(k)) {
			return SymbolId_iterator;
		}
		return 0;
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toLocaleString" -> Id_toLocaleString;
			case "toSource" -> Id_toSource;
			case "join" -> Id_join;
			case "reverse" -> Id_reverse;
			case "sort" -> Id_sort;
			case "push" -> Id_push;
			case "pop" -> Id_pop;
			case "shift" -> Id_shift;
			case "unshift" -> Id_unshift;
			case "splice" -> Id_splice;
			case "concat" -> Id_concat;
			case "slice" -> Id_slice;
			case "indexOf" -> Id_indexOf;
			case "lastIndexOf" -> Id_lastIndexOf;
			case "every" -> Id_every;
			case "filter" -> Id_filter;
			case "forEach" -> Id_forEach;
			case "map" -> Id_map;
			case "some" -> Id_some;
			case "find" -> Id_find;
			case "findIndex" -> Id_findIndex;
			case "reduce" -> Id_reduce;
			case "reduceRight" -> Id_reduceRight;
			case "fill" -> Id_fill;
			case "keys" -> Id_keys;
			case "values" -> Id_values;
			case "entries" -> Id_entries;
			case "includes" -> Id_includes;
			case "copyWithin" -> Id_copyWithin;
			default -> 0;
		};
	}

	@Override
	public <T> T createDataObject(Supplier<T> instanceFactory, Context cx) {
		List<T> list = createDataObjectList(instanceFactory, cx);

		if (list.isEmpty()) {
			throw new ArrayIndexOutOfBoundsException("Array doesn't contain any objects");
		}

		return list.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> createDataObjectList(Supplier<T> instanceFactory, Context cx) {
		List<T> list = new ArrayList<>();

		for (Object o : this) {
			if (o instanceof DataObject) {
				list.add(((DataObject) o).createDataObject(instanceFactory, cx));
			} else {
				list.add((T) o);
			}
		}

		return list;
	}

	@Override
	public boolean isDataObjectList() {
		return true;
	}
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.mod.util.ToStringJS;
import dev.latvian.mods.rhino.regexp.NativeRegExp;
import dev.latvian.mods.rhino.regexp.RegExp;
import dev.latvian.mods.rhino.util.ClassVisibilityContext;
import dev.latvian.mods.rhino.util.DefaultValueTypeHint;
import dev.latvian.mods.rhino.util.SpecialEquality;
import dev.latvian.mods.rhino.v8dtoa.DoubleConversion;
import dev.latvian.mods.rhino.v8dtoa.FastDtoa;

import java.lang.reflect.Array;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This is the class that implements the runtime.
 *
 * @author Norris Boyd
 */

public class ScriptRuntime {
	public static final Object[] EMPTY_OBJECTS = new Object[0];
	public static final String[] EMPTY_STRINGS = new String[0];
	public final static Class<Boolean> BooleanClass = Boolean.class;
	public final static Class<Byte> ByteClass = Byte.class;
	public final static Class<Character> CharacterClass = Character.class;
	public final static Class<Class> ClassClass = Class.class;
	public final static Class<Double> DoubleClass = Double.class;
	public final static Class<Float> FloatClass = Float.class;
	public final static Class<Integer> IntegerClass = Integer.class;
	public final static Class<Long> LongClass = Long.class;
	public final static Class<Number> NumberClass = Number.class;
	public final static Class<Object> ObjectClass = Object.class;
	public final static Class<Short> ShortClass = Short.class;
	public final static Class<String> StringClass = String.class;
	public final static Class<Date> DateClass = Date.class;
	public final static Class<?> ContextClass = Context.class;
	public final static Class<Function> FunctionClass = Function.class;
	public final static Class<ScriptableObject> ScriptableObjectClass = ScriptableObject.class;
	public static final Class<Scriptable> ScriptableClass = Scriptable.class;
	public static final double NaN = Double.NaN;
	public static final Double NaNobj = NaN;
	// Preserve backward-compatibility with historical value of this.
	public static final double negativeZero = Double.longBitsToDouble(0x8000000000000000L);
	public static final Double zeroObj = 0.0;
	public static final Double negativeZeroObj = -0.0;
	public static final int ENUMERATE_KEYS = 0;
	public static final int ENUMERATE_VALUES = 1;
	public static final int ENUMERATE_ARRAY = 2;
	public static final int ENUMERATE_KEYS_NO_ITERATOR = 3;
	public static final int ENUMERATE_VALUES_NO_ITERATOR = 4;
	public static final int ENUMERATE_ARRAY_NO_ITERATOR = 5;
	public static final int ENUMERATE_VALUES_IN_ORDER = 6;
	public static final MessageProvider messageProvider = new DefaultMessageProvider();
	private static final Object LIBRARY_SCOPE_KEY = "LIBRARY_SCOPE";

	static class NoSuchMethodShim implements Callable {
		String methodName;
		Callable noSuchMethodMethod;

		NoSuchMethodShim(Callable noSuchMethodMethod, String methodName) {
			this.noSuchMethodMethod = noSuchMethodMethod;
			this.methodName = methodName;
		}

		/**
		 * Perform the call.
		 *
		 * @param cx      the current Context for this thread
		 * @param scope   the scope to use to resolve properties.
		 * @param thisObj the JavaScript <code>this</code> object
		 * @param args    the array of arguments
		 * @return the result of the call
		 */
		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			Object[] nestedArgs = new Object[2];

			nestedArgs[0] = methodName;
			nestedArgs[1] = newArrayLiteral(cx, scope, args, null);
			return noSuchMethodMethod.call(cx, scope, thisObj, nestedArgs);
		}

	}

	/**
	 * Helper to return a string or an integer.
	 * Always use a null check on s.stringId to determine
	 * if the result is string or integer.
	 *
	 * @see ScriptRuntime#toStringIdOrIndex(Context, Object)
	 */
	static final class StringIdOrIndex {
		final String stringId;
		final int index;

		StringIdOrIndex(String stringId) {
			this.stringId = stringId;
			this.index = -1;
		}

		StringIdOrIndex(int index) {
			this.stringId = null;
			this.index = index;
		}
	}

	/* OPT there's a noticable delay for the first error!  Maybe it'd
	 * make sense to use a ListResourceBundle instead of a properties
	 * file to avoid (synchronized) text parsing.
	 */
	private static class DefaultMessageProvider implements MessageProvider {
		@Override
		public String getMessage(String messageId, Object[] arguments) {
			final String defaultResource = "dev.latvian.mods.rhino.resources.Messages";

			Locale locale = Locale.getDefault();

			// ResourceBundle does caching.
			ResourceBundle rb = ResourceBundle.getBundle(defaultResource, locale);

			String formatString;
			try {
				formatString = rb.getString(messageId);
			} catch (java.util.MissingResourceException mre) {
				throw new RuntimeException("no message resource found for message property " + messageId);
			}

			/*
			 * It's OK to format the string, even if 'arguments' is null;
			 * we need to format it anyway, to make double ''s collapse to
			 * single 's.
			 */
			MessageFormat formatter = new MessageFormat(formatString);
			return formatter.format(arguments);
		}
	}

	/**
	 * Returns representation of the [[ThrowTypeError]] object.
	 * See ECMA 5 spec, 13.2.3
	 */
	public static BaseFunction typeErrorThrower(Context cx) {
		if (cx.typeErrorThrower == null) {
			BaseFunction thrower = new BaseFunction() {
				@Override
				public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
					throw typeError0(cx, "msg.op.not.allowed");
				}

				@Override
				public int getLength() {
					return 0;
				}
			};
			ScriptRuntime.setFunctionProtoAndParent(cx, cx.getTopCallScope(), thrower);
			thrower.preventExtensions();
			cx.typeErrorThrower = thrower;
		}
		return cx.typeErrorThrower;
	}

	public static boolean isRhinoRuntimeType(Class<?> cl) {
		if (cl.isPrimitive()) {
			return (cl != Character.TYPE);
		}
		return (cl == StringClass || cl == BooleanClass || NumberClass.isAssignableFrom(cl) || ScriptableClass.isAssignableFrom(cl));
	}

	public static ScriptableObject initSafeStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
		if (scope == null) {
			scope = new NativeObject(cx.factory);
		}
		scope.associateValue(LIBRARY_SCOPE_KEY, scope);

		BaseFunction.init(scope, sealed, cx);
		NativeObject.init(cx, scope, sealed);

		Scriptable objectProto = ScriptableObject.getObjectPrototype(scope, cx);

		// Function.prototype.__proto__ should be Object.prototype
		Scriptable functionProto = ScriptableObject.getClassPrototype(scope, "Function", cx);
		functionProto.setPrototype(objectProto);

		// Set the prototype of the object passed in if need be
		if (scope.getPrototype(cx) == null) {
			scope.setPrototype(objectProto);
		}

		// must precede NativeGlobal since it's needed therein
		NativeError.init(scope, sealed, cx);
		NativeGlobal.init(cx, scope, sealed);

		NativeArray.init(scope, sealed, cx);
		NativeString.init(scope, sealed, cx);
		NativeBoolean.init(scope, sealed, cx);
		NativeNumber.init(scope, sealed, cx);
		NativeDate.init(scope, sealed, cx);
		NativeMath.init(scope, sealed, cx);
		NativeJSON.init(scope, sealed, cx);

		NativeWith.init(scope, sealed, cx);
		NativeCall.init(scope, sealed, cx);

		NativeIterator.init(cx, scope, sealed); // Also initializes NativeGenerator & ES6Generator

		NativeArrayIterator.init(scope, sealed, cx);
		NativeStringIterator.init(scope, sealed, cx);

		NativeRegExp.init(cx, scope, sealed);

		NativeSymbol.init(cx, scope, sealed);
		NativeCollectionIterator.init(scope, NativeSet.ITERATOR_TAG, sealed, cx);
		NativeCollectionIterator.init(scope, NativeMap.ITERATOR_TAG, sealed, cx);
		NativeMap.init(cx, scope, sealed);
		NativeSet.init(cx, scope, sealed);
		NativeWeakMap.init(scope, sealed, cx);
		NativeWeakSet.init(scope, sealed, cx);

		if (scope instanceof TopLevel) {
			((TopLevel) scope).cacheBuiltins(scope, sealed, cx);
		}

		return scope;
	}

	public static ScriptableObject initStandardObjects(Context cx, ScriptableObject scope, boolean sealed) {
		ScriptableObject s = initSafeStandardObjects(cx, scope, sealed);
		JavaAdapter.init(cx, s, sealed);
		return s;
	}

	public static ScriptableObject getLibraryScopeOrNull(Scriptable scope, Context cx) {
		ScriptableObject libScope;
		libScope = (ScriptableObject) ScriptableObject.getTopScopeValue(scope, LIBRARY_SCOPE_KEY, cx);
		return libScope;
	}

	// It is public so NativeRegExp can access it.
	public static boolean isJSLineTerminator(int c) {
		// Optimization for faster check for eol character:
		// they do not have 0xDFD0 bits set
		if ((c & 0xDFD0) != 0) {
			return false;
		}
		return c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029;
	}

	public static boolean isJSWhitespaceOrLineTerminator(int c) {
		return (isStrWhiteSpaceChar(c) || isJSLineTerminator(c));
	}

	/**
	 * Indicates if the character is a Str whitespace char according to ECMA spec:
	 * StrWhiteSpaceChar :::
	 * <TAB>
	 * <SP>
	 * <NBSP>
	 * <FF>
	 * <VT>
	 * <CR>
	 * <LF>
	 * <LS>
	 * <PS>
	 * <USP>
	 * <BOM>
	 */
	static boolean isStrWhiteSpaceChar(int c) {
		return switch (c) { // <SP>
			// <LF>
			// <CR>
			// <TAB>
			// <NBSP>
			// <FF>
			// <VT>
			// <LS>
			// <PS>
			case ' ', '\n', '\r', '\t', '\u00A0', '\u000C', '\u000B', '\u2028', '\u2029', '\uFEFF' -> // <BOM>
				true;
			default -> Character.getType(c) == Character.SPACE_SEPARATOR;
		};
	}

	public static Number wrapNumber(double x) {
		if (Double.isNaN(x)) {
			return ScriptRuntime.NaNobj;
		}
		return x;
	}

	/**
	 * Convert the value to a boolean.
	 * <p>
	 * See ECMA 9.2.
	 */
	public static boolean toBoolean(Context cx, Object val) {
		if (val instanceof Boolean) {
			return (Boolean) val;
		}
		if (val == null || val == Undefined.INSTANCE) {
			return false;
		}
		if (val instanceof CharSequence) {
			return ((CharSequence) val).length() != 0;
		}
		if (val instanceof Number) {
			double d = ((Number) val).doubleValue();
			return (!Double.isNaN(d) && d != 0.0);
		}
		if (val instanceof Scriptable) {
			return !(val instanceof ScriptableObject) || !((ScriptableObject) val).avoidObjectDetection();
		}
		warnAboutNonJSObject(cx, val);
		return true;
	}

	/**
	 * Convert the value to a number.
	 * <p>
	 * See ECMA 9.3.
	 */
	public static double toNumber(Context cx, Object val) {
		if (val instanceof Number) {
			return ((Number) val).doubleValue();
		}
		if (val == null) {
			return +0.0;
		}
		if (val == Undefined.INSTANCE) {
			return NaN;
		}
		if (val instanceof String) {
			return toNumber(cx, (String) val);
		}
		if (val instanceof CharSequence) {
			return toNumber(cx, val.toString());
		}
		if (val instanceof Boolean) {
			return (Boolean) val ? 1 : +0.0;
		}
		if (val instanceof Symbol) {
			throw typeError0(cx, "msg.not.a.number");
		}
		if (val instanceof Scriptable) {
			val = ((Scriptable) val).getDefaultValue(cx, DefaultValueTypeHint.NUMBER);
			if ((val instanceof Scriptable) && !isSymbol(val)) {
				throw errorWithClassName("msg.primitive.expected", val, cx);
			}
			return toNumber(cx, val);
		}
		warnAboutNonJSObject(cx, val);
		return NaN;
	}

	public static double toNumber(Context cx, Object[] args, int index) {
		return (index < args.length) ? toNumber(cx, args[index]) : NaN;
	}

	static double stringPrefixToNumber(String s, int start, int radix) {
		return stringToNumber(s, start, s.length() - 1, radix, true);
	}

	static double stringToNumber(String s, int start, int end, int radix) {
		return stringToNumber(s, start, end, radix, false);
	}

	/*
	 * Helper function for toNumber, parseInt, and TokenStream.getToken.
	 */
	private static double stringToNumber(String source, int sourceStart, int sourceEnd, int radix, boolean isPrefix) {
		char digitMax = '9';
		char lowerCaseBound = 'a';
		char upperCaseBound = 'A';
		if (radix < 10) {
			digitMax = (char) ('0' + radix - 1);
		}
		if (radix > 10) {
			lowerCaseBound = (char) ('a' + radix - 10);
			upperCaseBound = (char) ('A' + radix - 10);
		}
		int end;
		double sum = 0.0;
		for (end = sourceStart; end <= sourceEnd; end++) {
			char c = source.charAt(end);
			int newDigit;
			if ('0' <= c && c <= digitMax) {
				newDigit = c - '0';
			} else if ('a' <= c && c < lowerCaseBound) {
				newDigit = c - 'a' + 10;
			} else if ('A' <= c && c < upperCaseBound) {
				newDigit = c - 'A' + 10;
			} else if (!isPrefix) {
				return NaN; // isn't a prefix but found unexpected char
			} else {
				break; // unexpected char
			}
			sum = sum * radix + newDigit;
		}
		if (sourceStart == end) { // stopped right at the beginning
			return NaN;
		}
		if (sum > NativeNumber.MAX_SAFE_INTEGER) {
			if (radix == 10) {
				/* If we're accumulating a decimal number and the number
				 * is >= 2^53, then the result from the repeated multiply-add
				 * above may be inaccurate.  Call Java to get the correct
				 * answer.
				 */
				try {
					return Double.parseDouble(source.substring(sourceStart, end));
				} catch (NumberFormatException nfe) {
					return NaN;
				}
			} else if (radix == 2 || radix == 4 || radix == 8 || radix == 16 || radix == 32) {
				/* The number may also be inaccurate for one of these bases.
				 * This happens if the addition in value*radix + digit causes
				 * a round-down to an even least significant mantissa bit
				 * when the first dropped bit is a one.  If any of the
				 * following digits in the number (which haven't been added
				 * in yet) are nonzero then the correct action would have
				 * been to round up instead of down.  An example of this
				 * occurs when reading the number 0x1000000000000081, which
				 * rounds to 0x1000000000000000 instead of 0x1000000000000100.
				 */
				int bitShiftInChar = 1;
				int digit = 0;

				final int SKIP_LEADING_ZEROS = 0;
				final int FIRST_EXACT_53_BITS = 1;
				final int AFTER_BIT_53 = 2;
				final int ZEROS_AFTER_54 = 3;
				final int MIXED_AFTER_54 = 4;

				int state = SKIP_LEADING_ZEROS;
				int exactBitsLimit = 53;
				double factor = 0.0;
				boolean bit53 = false;
				// bit54 is the 54th bit (the first dropped from the mantissa)
				boolean bit54 = false;
				int pos = sourceStart;

				for (; ; ) {
					if (bitShiftInChar == 1) {
						if (pos == end) {
							break;
						}
						digit = source.charAt(pos++);
						if ('0' <= digit && digit <= '9') {
							digit -= '0';
						} else if ('a' <= digit && digit <= 'z') {
							digit -= 'a' - 10;
						} else {
							digit -= 'A' - 10;
						}
						bitShiftInChar = radix;
					}
					bitShiftInChar >>= 1;
					boolean bit = (digit & bitShiftInChar) != 0;

					switch (state) {
						case SKIP_LEADING_ZEROS:
							if (bit) {
								--exactBitsLimit;
								sum = 1.0;
								state = FIRST_EXACT_53_BITS;
							}
							break;
						case FIRST_EXACT_53_BITS:
							sum *= 2.0;
							if (bit) {
								sum += 1.0;
							}
							--exactBitsLimit;
							if (exactBitsLimit == 0) {
								bit53 = bit;
								state = AFTER_BIT_53;
							}
							break;
						case AFTER_BIT_53:
							bit54 = bit;
							factor = 2.0;
							state = ZEROS_AFTER_54;
							break;
						case ZEROS_AFTER_54:
							if (bit) {
								state = MIXED_AFTER_54;
							}
							// fallthrough
						case MIXED_AFTER_54:
							factor *= 2;
							break;
					}
				}
				switch (state) {
					case SKIP_LEADING_ZEROS:
						sum = 0.0;
						break;
					case FIRST_EXACT_53_BITS:
					case AFTER_BIT_53:
						// do nothing
						break;
					case ZEROS_AFTER_54:
						// x1.1 -> x1 + 1 (round up)
						// x0.1 -> x0 (round down)
						if (bit54 & bit53) {
							sum += 1.0;
						}
						sum *= factor;
						break;
					case MIXED_AFTER_54:
						// x.100...1.. -> x + 1 (round up)
						// x.0anything -> x (round down)
						if (bit54) {
							sum += 1.0;
						}
						sum *= factor;
						break;
				}
			}
			/* We don't worry about inaccurate numbers for any other base. */
		}
		return sum;
	}

	/**
	 * ToNumber applied to the String type
	 * <p>
	 * See the #sec-tonumber-applied-to-the-string-type section of ECMA
	 */
	public static double toNumber(Context cx, String s) {
		final int len = s.length();

		// Skip whitespace at the start
		int start = 0;
		char startChar;
		for (; ; ) {
			if (start == len) {
				// empty or contains only whitespace
				return +0.0;
			}
			startChar = s.charAt(start);
			if (!ScriptRuntime.isStrWhiteSpaceChar(startChar)) {
				// found first non-whitespace character
				break;
			}
			start++;
		}

		// Skip whitespace at the end
		int end = len - 1;
		char endChar;
		while (ScriptRuntime.isStrWhiteSpaceChar(endChar = s.charAt(end))) {
			end--;
		}

		// Do not break scripts relying on old non-compliant conversion
		// (see bug #368)
		// 1. makes ToNumber parse only a valid prefix in hex literals (similar to 'parseInt()')
		//    ToNumber('0x10 something') => 16
		// 2. allows plus and minus signs for hexadecimal numbers
		//    ToNumber('-0x10') => -16
		// 3. disables support for binary ('0b10') and octal ('0o13') literals
		//    ToNumber('0b1') => NaN
		//    ToNumber('0o5') => NaN
		final boolean oldParsingMode = cx == null;

		// Handle non-base10 numbers
		if (startChar == '0') {
			if (start + 2 <= end) {
				final char radixC = s.charAt(start + 1);
				int radix = -1;
				if (radixC == 'x' || radixC == 'X') {
					radix = 16;
				} else if (!oldParsingMode && (radixC == 'o' || radixC == 'O')) {
					radix = 8;
				} else if (!oldParsingMode && (radixC == 'b' || radixC == 'B')) {
					radix = 2;
				}
				if (radix != -1) {
					if (oldParsingMode) {
						return stringPrefixToNumber(s, start + 2, radix);
					}
					return stringToNumber(s, start + 2, end, radix);
				}
			}
		} else if (oldParsingMode && (startChar == '+' || startChar == '-')) {
			// If in old parsing mode, check for a signed hexadecimal number
			if (start + 3 <= end && s.charAt(start + 1) == '0') {
				final char radixC = s.charAt(start + 2);
				if (radixC == 'x' || radixC == 'X') {
					double val = stringPrefixToNumber(s, start + 3, 16);
					return startChar == '-' ? -val : val;
				}
			}
		}

		if (endChar == 'y') {
			// check for "Infinity"
			if (startChar == '+' || startChar == '-') {
				start++;
			}
			if (start + 7 == end && s.regionMatches(start, "Infinity", 0, 8)) {
				return startChar == '-' ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
			}
			return NaN;
		}
		// A base10, non-infinity number:
		// just try a normal floating point conversion
		String sub = s.substring(start, end + 1);
		// Quick test to check string contains only valid characters because
		// Double.parseDouble() can be slow and accept input we want to reject
		for (int i = sub.length() - 1; i >= 0; i--) {
			char c = sub.charAt(i);
			if (('0' <= c && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
				continue;
			}
			return NaN;
		}
		try {
			return Double.parseDouble(sub);
		} catch (NumberFormatException ex) {
			return NaN;
		}
	}

	/**
	 * Helper function for builtin objects that use the varargs form.
	 * ECMA function formal arguments are undefined if not supplied;
	 * this function pads the argument array out to the expected
	 * length, if necessary.
	 */
	public static Object[] padArguments(Object[] args, int count) {
		if (count < args.length) {
			return args;
		}

		Object[] result = new Object[count];
		System.arraycopy(args, 0, result, 0, args.length);
		if (args.length < count) {
			Arrays.fill(result, args.length, count, Undefined.INSTANCE);
		}
		return result;
	}

	public static String escapeAndWrapString(String s) {
		var c = s.indexOf('\'') == -1 ? '\'' : '"';
		var escaped = escapeString(s, c);
		return c + escaped + c;
	}

	/**
	 * For escaping strings printed by object and array literals; not quite
	 * the same as 'escape.'
	 */
	public static String escapeString(String s, char escapeQuote) {
		if (!(escapeQuote == '"' || escapeQuote == '\'')) {
			Kit.codeBug();
		}
		StringBuilder sb = null;

		for (int i = 0, L = s.length(); i != L; ++i) {
			int c = s.charAt(i);

			if (' ' <= c && c <= '~' && c != escapeQuote && c != '\\') {
				// an ordinary print character (like C isprint()) and not "
				// or \ .
				if (sb != null) {
					sb.append((char) c);
				}
				continue;
			}
			if (sb == null) {
				sb = new StringBuilder(L + 3);
				sb.append(s);
				sb.setLength(i);
			}

			int escape = switch (c) {
				case '\b' -> 'b';
				case '\f' -> 'f';
				case '\n' -> 'n';
				case '\r' -> 'r';
				case '\t' -> 't';
				case 0xb -> 'v'; // Java lacks \v.
				case ' ' -> ' ';
				case '\\' -> '\\';
				default -> -1;
			};
			if (escape >= 0) {
				// an \escaped sort of character
				sb.append('\\');
				sb.append((char) escape);
			} else if (c == escapeQuote) {
				sb.append('\\');
				sb.append(escapeQuote);
			} else {
				int hexSize;
				if (c < 256) {
					// 2-digit hex
					sb.append("\\x");
					hexSize = 2;
				} else {
					// Unicode.
					sb.append("\\u");
					hexSize = 4;
				}
				// append hexadecimal form of c left-padded with 0
				for (int shift = (hexSize - 1) * 4; shift >= 0; shift -= 4) {
					int digit = 0xf & (c >> shift);
					int hc = (digit < 10) ? '0' + digit : 'a' - 10 + digit;
					sb.append((char) hc);
				}
			}
		}
		return (sb == null) ? s : sb.toString();
	}

	static boolean isValidIdentifierName(Context cx, String s, boolean isStrict) {
		int L = s.length();
		if (L == 0) {
			return false;
		}
		if (!Character.isJavaIdentifierStart(s.charAt(0))) {
			return false;
		}
		for (int i = 1; i != L; ++i) {
			if (!Character.isJavaIdentifierPart(s.charAt(i))) {
				return false;
			}
		}
		return !TokenStream.isKeyword(s, isStrict);
	}

	public static CharSequence toCharSequence(Context cx, Object val) {
		if (val instanceof NativeString) {
			return ((NativeString) val).toCharSequence();
		}
		return val instanceof CharSequence ? (CharSequence) val : toString(cx, val);
	}

	/**
	 * Convert the value to a string.
	 * <p>
	 * See ECMA 9.8.
	 */
	public static String toString(Context cx, Object val) {
		if (val == null) {
			return "null";
		}
		if (val == Undefined.INSTANCE || val == Undefined.SCRIPTABLE_INSTANCE) {
			return "undefined";
		}
		if (val instanceof String) {
			return (String) val;
		}
		if (val instanceof CharSequence) {
			return val.toString();
		}
		if (val instanceof Number) {
			// XXX should we just teach NativeNumber.stringValue()
			// about Numbers?
			return numberToString(cx, ((Number) val).doubleValue(), 10);
		}
		if (val instanceof Symbol) {
			throw typeError0(cx, "msg.not.a.string");
		}
		if (val instanceof Scriptable) {
			val = ((Scriptable) val).getDefaultValue(cx, DefaultValueTypeHint.STRING);
			if ((val instanceof Scriptable) && !isSymbol(val)) {
				throw errorWithClassName("msg.primitive.expected", val, cx);
			}
			return toString(cx, val);
		}
		if (val.getClass().isArray()) {
			var builder = new StringBuilder();
			int length = Array.getLength(val);

			if (length == 0) {
				builder.append("[]");
			} else {
				builder.append('[');

				for (int i = 0; i < length; i++) {
					if (i > 0) {
						builder.append(", ");
					}

					builder.append(toString(cx, Array.get(val, i)));
				}

				builder.append(']');
			}

			return builder.toString();
		}
		return ToStringJS.toStringJS(cx, val);
	}

	static String defaultObjectToString(Scriptable obj) {
		if (obj == null) {
			return "[object Null]";
		}
		if (Undefined.isUndefined(obj)) {
			return "[object Undefined]";
		}
		return "[object " + obj.getClassName() + ']';
	}

	public static String toString(Context cx, Object[] args, int index) {
		return (index < args.length) ? toString(cx, args[index]) : "undefined";
	}

	/**
	 * Optimized version of toString(Object) for numbers.
	 */
	public static String toString(Context cx, double val) {
		return numberToString(cx, val, 10);
	}

	public static String numberToString(Context cx, double d, int base) {
		if ((base < 2) || (base > 36)) {
			throw Context.reportRuntimeError1("msg.bad.radix", Integer.toString(base), cx);
		}

		if (Double.isNaN(d)) {
			return "NaN";
		}
		if (d == Double.POSITIVE_INFINITY) {
			return "Infinity";
		}
		if (d == Double.NEGATIVE_INFINITY) {
			return "-Infinity";
		}
		if (d == 0.0) {
			return "0";
		}

		if (base != 10) {
			return DToA.JS_dtobasestr(base, d);
		}
		// V8 FastDtoa can't convert all numbers, so try it first but
		// fall back to old DToA in case it fails
		String result = FastDtoa.numberToString(d);
		if (result != null) {
			return result;
		}
		StringBuilder buffer = new StringBuilder();
		DToA.JS_dtostr(buffer, DToA.DTOSTR_STANDARD, 0, d);
		return buffer.toString();
	}

	static String uneval(Context cx, Scriptable scope, Object value) {
		if (value == null) {
			return "null";
		}
		if (value == Undefined.INSTANCE) {
			return "undefined";
		}
		if (value instanceof CharSequence) {
			return escapeAndWrapString(value.toString());
		}
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			if (d == 0 && 1 / d < 0) {
				return "-0";
			}
			return toString(cx, d);
		}
		if (value instanceof Boolean) {
			return toString(cx, value);
		}
		if (value instanceof Scriptable obj) {
			// Wrapped Java objects won't have "toSource" and will report
			// errors for get()s of nonexistent name, so use has() first
			if (ScriptableObject.hasProperty(obj, "toSource", cx)) {
				Object v = ScriptableObject.getProperty(obj, "toSource", cx);
				if (v instanceof Function f) {
					return toString(cx, f.call(cx, scope, obj, EMPTY_OBJECTS));
				}
			}
			return toString(cx, value);
		}
		warnAboutNonJSObject(cx, value);
		return value.toString();
	}

	/**
	 * <strong>Warning</strong>: This doesn't allow to resolve primitive
	 * prototype properly when many top scopes are involved
	 *
	 * @deprecated Use {@link #toObjectOrNull(Context, Object, Scriptable)} instead
	 */
	@Deprecated
	public static Scriptable toObjectOrNull(Context cx, Object obj) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else if (obj != null && obj != Undefined.INSTANCE) {
			return toObject(cx, cx.getTopCallOrThrow(), obj);
		}
		return null;
	}

	/**
	 * @param scope the scope that should be used to resolve primitive prototype
	 */
	public static Scriptable toObjectOrNull(Context cx, Object obj, Scriptable scope) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		} else if (obj != null && obj != Undefined.INSTANCE) {
			return toObject(cx, scope, obj);
		}
		return null;
	}

	/**
	 * Convert the value to an object.
	 * <p>
	 * See ECMA 9.9.
	 */
	public static Scriptable toObject(Context cx, Scriptable scope, Object val) {
		if (val == null) {
			throw typeError0(cx, "msg.null.to.object");
		} else if (Undefined.isUndefined(val)) {
			throw typeError0(cx, "msg.undef.to.object");
		} else if (isSymbol(val)) {
			NativeSymbol result = new NativeSymbol((NativeSymbol) val);
			setBuiltinProtoAndParent(cx, scope, result, TopLevel.Builtins.Symbol);
			return result;
		} else if (val instanceof Scriptable) {
			return (Scriptable) val;
		} else if (val instanceof CharSequence) {
			// FIXME we want to avoid toString() here, especially for concat()
			NativeString result = new NativeString((CharSequence) val);
			setBuiltinProtoAndParent(cx, scope, result, TopLevel.Builtins.String);
			return result;
		} else if (val instanceof Number) {
			NativeNumber result = new NativeNumber(cx, ((Number) val).doubleValue());
			setBuiltinProtoAndParent(cx, scope, result, TopLevel.Builtins.Number);
			return result;
		} else if (val instanceof Boolean) {
			NativeBoolean result = new NativeBoolean((Boolean) val);
			setBuiltinProtoAndParent(cx, scope, result, TopLevel.Builtins.Boolean);
			return result;
		} else {
			// Extension: Wrap as a LiveConnect object.
			Object wrapped = cx.wrap(scope, val);
			if (wrapped instanceof Scriptable) {
				return (Scriptable) wrapped;
			}
			throw errorWithClassName("msg.invalid.type", val, cx);
		}
	}

	public static Scriptable newObject(Context cx, Scriptable scope, String constructorName, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = getExistingCtor(cx, scope, constructorName);
		if (args == null) {
			args = ScriptRuntime.EMPTY_OBJECTS;
		}
		return ctor.construct(cx, scope, args);
	}

	public static Scriptable newBuiltinObject(Context cx, Scriptable scope, TopLevel.Builtins type, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = TopLevel.getBuiltinCtor(cx, scope, type);
		if (args == null) {
			args = ScriptRuntime.EMPTY_OBJECTS;
		}
		return ctor.construct(cx, scope, args);
	}

	static Scriptable newNativeError(Context cx, Scriptable scope, TopLevel.NativeErrors type, Object[] args) {
		scope = ScriptableObject.getTopLevelScope(scope);
		Function ctor = TopLevel.getNativeErrorCtor(cx, scope, type);
		if (args == null) {
			args = ScriptRuntime.EMPTY_OBJECTS;
		}
		return ctor.construct(cx, scope, args);
	}

	/**
	 * See ECMA 9.4.
	 */
	public static double toInteger(Context cx, Object val) {
		return toInteger(toNumber(cx, val));
	}

	// convenience method
	public static double toInteger(double d) {
		// if it's NaN
		if (Double.isNaN(d)) {
			return +0.0;
		}

		if ((d == 0.0) || Double.isInfinite(d)) {
			return d;
		}

		if (d > 0.0) {
			return Math.floor(d);
		}

		return Math.ceil(d);
	}

	public static double toInteger(Context cx, Object[] args, int index) {
		return (index < args.length) ? toInteger(cx, args[index]) : +0.0;
	}

	public static long toLength(Context cx, Object[] args, int index) {
		double len = toInteger(cx, args, index);
		if (len <= +0.0) {
			return 0;
		}
		return (long) Math.min(len, NativeNumber.MAX_SAFE_INTEGER);
	}

	/**
	 * See ECMA 9.5.
	 */
	public static int toInt32(Context cx, Object val) {
		// short circuit for common integer values
		if (val instanceof Integer) {
			return (Integer) val;
		}

		return toInt32(toNumber(cx, val));
	}

	public static int toInt32(Context cx, Object[] args, int index) {
		return (index < args.length) ? toInt32(cx, args[index]) : 0;
	}

	public static int toInt32(double d) {
		return DoubleConversion.doubleToInt32(d);
	}

	/**
	 * See ECMA 9.6.
	 *
	 * @return long value representing 32 bits unsigned integer
	 */
	public static long toUint32(double d) {
		return DoubleConversion.doubleToInt32(d) & 0xffffffffL;
	}

	public static long toUint32(Context cx, Object val) {
		return toUint32(toNumber(cx, val));
	}

	/**
	 * See ECMA 9.7.
	 */
	public static char toUint16(Context cx, Object val) {
		double d = toNumber(cx, val);
		return (char) DoubleConversion.doubleToInt32(d);
	}

	public static Object getTopLevelProp(Context cx, Scriptable scope, String id) {
		scope = ScriptableObject.getTopLevelScope(scope);
		return ScriptableObject.getProperty(scope, id, cx);
	}

	static Function getExistingCtor(Context cx, Scriptable scope, String constructorName) {
		Object ctorVal = ScriptableObject.getProperty(scope, constructorName, cx);
		if (ctorVal instanceof Function) {
			return (Function) ctorVal;
		}
		if (ctorVal == Scriptable.NOT_FOUND) {
			throw Context.reportRuntimeError1("msg.ctor.not.found", constructorName, cx);
		}
		throw Context.reportRuntimeError1("msg.not.ctor", constructorName, cx);
	}

	/**
	 * Return -1L if str is not an index, or the index value as lower 32
	 * bits of the result. Note that the result needs to be cast to an int
	 * in order to produce the actual index, which may be negative.
	 */
	public static long indexFromString(String str) {
		// The length of the decimal string representation of
		//  Integer.MAX_VALUE, 2147483647
		final int MAX_VALUE_LENGTH = 10;

		int len = str.length();
		if (len > 0) {
			int i = 0;
			boolean negate = false;
			int c = str.charAt(0);
			if (c == '-') {
				if (len > 1) {
					c = str.charAt(1);
					if (c == '0') {
						return -1L; // "-0" is not an index
					}
					i = 1;
					negate = true;
				}
			}
			c -= '0';
			if (0 <= c && c <= 9 && len <= (negate ? MAX_VALUE_LENGTH + 1 : MAX_VALUE_LENGTH)) {
				// Use negative numbers to accumulate index to handle
				// Integer.MIN_VALUE that is greater by 1 in absolute value
				// then Integer.MAX_VALUE
				int index = -c;
				int oldIndex = 0;
				i++;
				if (index != 0) {
					// Note that 00, 01, 000 etc. are not indexes
					while (i != len && 0 <= (c = str.charAt(i) - '0') && c <= 9) {
						oldIndex = index;
						index = 10 * index - c;
						i++;
					}
				}
				// Make sure all characters were consumed and that it couldn't
				// have overflowed.
				if (i == len && (oldIndex > (Integer.MIN_VALUE / 10) || (oldIndex == (Integer.MIN_VALUE / 10) && c <= (negate ? -(Integer.MIN_VALUE % 10) : (Integer.MAX_VALUE % 10))))) {
					return 0xFFFFFFFFL & (negate ? index : -index);
				}
			}
		}
		return -1L;
	}

	/**
	 * If str is a decimal presentation of Uint32 value, return it as long.
	 * Othewise return -1L;
	 */
	public static long testUint32String(String str) {
		// The length of the decimal string representation of
		//  UINT32_MAX_VALUE, 4294967296
		final int MAX_VALUE_LENGTH = 10;

		int len = str.length();
		if (1 <= len && len <= MAX_VALUE_LENGTH) {
			int c = str.charAt(0);
			c -= '0';
			if (c == 0) {
				// Note that 00,01 etc. are not valid Uint32 presentations
				return (len == 1) ? 0L : -1L;
			}
			if (1 <= c && c <= 9) {
				long v = c;
				for (int i = 1; i != len; ++i) {
					c = str.charAt(i) - '0';
					if (!(0 <= c && c <= 9)) {
						return -1;
					}
					v = 10 * v + c;
				}
				// Check for overflow
				if ((v >>> 32) == 0) {
					return v;
				}
			}
		}
		return -1;
	}

	/**
	 * If s represents index, then return index value wrapped as Integer
	 * and othewise return s.
	 */
	static Object getIndexObject(String s) {
		long indexTest = indexFromString(s);
		if (indexTest >= 0) {
			return (int) indexTest;
		}
		return s;
	}

	/**
	 * If d is exact int value, return its value wrapped as Integer
	 * and othewise return d converted to String.
	 */
	static Object getIndexObject(Context cx, double d) {
		int i = (int) d;
		if (i == d) {
			return i;
		}
		return toString(cx, d);
	}

	/**
	 * If toString(id) is a decimal presentation of int32 value, then id
	 * is index. In this case return null and make the index available
	 * as ScriptRuntime.lastIndexResult(cx). Otherwise return toString(id).
	 */
	static StringIdOrIndex toStringIdOrIndex(Context cx, Object id) {
		if (id instanceof Number) {
			double d = ((Number) id).doubleValue();
			int index = (int) d;
			if (index == d) {
				return new StringIdOrIndex(index);
			}
			return new StringIdOrIndex(toString(cx, id));
		}
		String s;
		if (id instanceof String) {
			s = (String) id;
		} else {
			s = toString(cx, id);
		}
		long indexTest = indexFromString(s);
		if (indexTest >= 0) {
			return new StringIdOrIndex((int) indexTest);
		}
		return new StringIdOrIndex(s);
	}

	/**
	 * Call obj.[[Get]](id)
	 */
	public static Object getObjectElem(Context cx, Scriptable scope, Object obj, Object elem) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(cx, obj, elem);
		}
		return getObjectElem(cx, sobj, elem);
	}

	public static Object getObjectElem(Context cx, Scriptable obj, Object elem) {
		Object result;

		if (isSymbol(elem)) {
			result = ScriptableObject.getProperty(obj, (Symbol) elem, cx);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				int index = s.index;
				result = ScriptableObject.getProperty(obj, index, cx);
			} else {
				result = ScriptableObject.getProperty(obj, s.stringId, cx);
			}
		}

		if (result == Scriptable.NOT_FOUND) {
			result = Undefined.INSTANCE;
		}

		return result;
	}

	/**
	 * Version of getObjectElem when elem is a valid JS identifier name.
	 *
	 * @param scope the scope that should be used to resolve primitive prototype
	 */
	public static Object getObjectProp(Context cx, Scriptable scope, Object obj, String property) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(cx, obj, property);
		}
		return getObjectProp(cx, sobj, property);
	}

	public static Object getObjectProp(Context cx, Scriptable obj, String property) {

		Object result = ScriptableObject.getProperty(obj, property, cx);
		if (result == Scriptable.NOT_FOUND) {
			return Undefined.INSTANCE;
		}

		return result;
	}

	public static Object getObjectPropNoWarn(Context cx, Scriptable scope, Object obj, String property) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(cx, obj, property);
		}
		Object result = ScriptableObject.getProperty(sobj, property, cx);
		if (result == Scriptable.NOT_FOUND) {
			return Undefined.INSTANCE;
		}
		return result;
	}

	public static Object getObjectPropOptional(Context cx, Scriptable scope, Object obj, String property) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			return Undefined.INSTANCE;
		}
		Object result = ScriptableObject.getProperty(sobj, property, cx);
		if (result == Scriptable.NOT_FOUND) {
			return Undefined.INSTANCE;
		}
		return result;
	}

	/**
	 * A cheaper and less general version of the above for well-known argument
	 * types.
	 */
	public static Object getObjectIndex(Context cx, Scriptable scope, Object obj, double dblIndex) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefReadError(cx, obj, toString(cx, dblIndex));
		}

		int index = (int) dblIndex;
		if (index == dblIndex) {
			return getObjectIndex(cx, sobj, index);
		}
		String s = toString(cx, dblIndex);
		return getObjectProp(cx, sobj, s);
	}

	public static Object getObjectIndex(Context cx, Scriptable obj, int index) {
		Object result = ScriptableObject.getProperty(obj, index, cx);
		if (result == Scriptable.NOT_FOUND) {
			result = Undefined.INSTANCE;
		}

		return result;
	}

	/**
	 * Call obj.[[Put]](id, value)
	 */
	public static Object setObjectElem(Context cx, Scriptable scope, Object obj, Object elem, Object value) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(cx, obj, elem, value);
		}
		return setObjectElem(cx, sobj, elem, value);
	}

	public static Object setObjectElem(Context cx, Scriptable obj, Object elem, Object value) {
		if (isSymbol(elem)) {
			ScriptableObject.putProperty(obj, (Symbol) elem, value, cx);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				ScriptableObject.putProperty(obj, s.index, value, cx);
			} else {
				ScriptableObject.putProperty(obj, s.stringId, value, cx);
			}
		}

		return value;
	}

	/**
	 * Version of setObjectElem when elem is a valid JS identifier name.
	 */
	public static Object setObjectProp(Context cx, Scriptable scope, Object obj, String property, Object value) {
		if (!(obj instanceof Scriptable) && cx.isStrictMode()) {
			throw undefWriteError(cx, obj, property, value);
		}

		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(cx, obj, property, value);
		}

		return setObjectProp(cx, sobj, property, value);
	}

	public static Object setObjectProp(Context cx, Scriptable obj, String property, Object value) {
		ScriptableObject.putProperty(obj, property, value, cx);
		return value;
	}

	/**
	 * A cheaper and less general version of the above for well-known argument
	 * types.
	 */
	public static Object setObjectIndex(Context cx, Scriptable scope, Object obj, double dblIndex, Object value) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw undefWriteError(cx, obj, String.valueOf(dblIndex), value);
		}

		int index = (int) dblIndex;
		if (index == dblIndex) {
			return setObjectIndex(cx, sobj, index, value);
		}
		String s = toString(cx, dblIndex);
		return setObjectProp(cx, sobj, s, value);
	}

	public static Object setObjectIndex(Context cx, Scriptable obj, int index, Object value) {
		ScriptableObject.putProperty(obj, index, value, cx);
		return value;
	}

	public static boolean deleteObjectElem(Context cx, Scriptable target, Object elem) {
		if (isSymbol(elem)) {
			SymbolScriptable so = ScriptableObject.ensureSymbolScriptable(target, cx);
			Symbol s = (Symbol) elem;
			so.delete(cx, s);
			return !so.has(cx, s, target);
		}
		StringIdOrIndex s = toStringIdOrIndex(cx, elem);
		if (s.stringId == null) {
			target.delete(cx, s.index);
			return !target.has(cx, s.index, target);
		}
		target.delete(cx, s.stringId);
		return !target.has(cx, s.stringId, target);
	}

	public static boolean hasObjectElem(Context cx, Scriptable target, Object elem) {
		boolean result;

		if (isSymbol(elem)) {
			result = ScriptableObject.hasProperty(target, (Symbol) elem, cx);
		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId == null) {
				result = ScriptableObject.hasProperty(target, s.index, cx);
			} else {
				result = ScriptableObject.hasProperty(target, s.stringId, cx);
			}
		}

		return result;
	}

	public static Object refGet(Context cx, Ref ref) {
		return ref.get(cx);
	}

	public static Object refSet(Context cx, Scriptable scope, Ref ref, Object value) {
		return ref.set(cx, scope, value);
	}

	public static Object refDel(Context cx, Ref ref) {
		return ref.delete(cx);
	}

	static boolean isSpecialProperty(String s) {
		return s.equals("__proto__") || s.equals("__parent__");
	}

	public static Ref specialRef(Context cx, Scriptable scope, Object obj, String specialProperty) {
		return SpecialRef.createSpecial(cx, scope, obj, specialProperty);
	}

	/**
	 * The delete operator
	 * <p>
	 * See ECMA 11.4.1
	 * <p>
	 * In ECMA 0.19, the description of the delete operator (11.4.1)
	 * assumes that the [[Delete]] method returns a value. However,
	 * the definition of the [[Delete]] operator (8.6.2.5) does not
	 * define a return value. Here we assume that the [[Delete]]
	 * method doesn't return a value.
	 */
	public static Object delete(Context cx, Scriptable scope, Object obj, Object id, boolean isName) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			if (isName) {
				return Boolean.TRUE;
			}
			throw undefDeleteError(cx, obj, id);
		}
		return deleteObjectElem(cx, sobj, id);
	}

	/**
	 * Looks up a name in the scope chain and returns its value.
	 */
	public static Object name(Context cx, Scriptable scope, String name) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (result == Scriptable.NOT_FOUND) {
				throw notFoundError(cx, scope, name);
			}
			return result;
		}

		return nameOrFunction(cx, scope, parent, name, false);
	}

	private static Object nameOrFunction(Context cx, Scriptable scope, Scriptable parentScope, String name, boolean asFunctionCall) {
		Object result;
		Scriptable thisObj = scope; // It is used only if asFunctionCall==true.

		while (true) {
			if (scope instanceof NativeWith) {
				Scriptable withObj = scope.getPrototype(cx);
				result = ScriptableObject.getProperty(withObj, name, cx);
				if (result != Scriptable.NOT_FOUND) {
					// function this should be the target object of with
					thisObj = withObj;
					break;
				}
			} else if (scope instanceof NativeCall) {
				// NativeCall does not prototype chain and Scriptable.get
				// can be called directly.
				result = scope.get(cx, name, scope);
				if (result != Scriptable.NOT_FOUND) {
					if (asFunctionCall) {
						// ECMA 262 requires that this for nested funtions
						// should be top scope
						thisObj = ScriptableObject.getTopLevelScope(parentScope);
					}
					break;
				}
			} else {
				// Can happen if Rhino embedding decided that nested
				// scopes are useful for what ever reasons.
				result = ScriptableObject.getProperty(scope, name, cx);
				if (result != Scriptable.NOT_FOUND) {
					thisObj = scope;
					break;
				}
			}
			scope = parentScope;
			parentScope = parentScope.getParentScope();
			if (parentScope == null) {
				result = topScopeName(cx, scope, name);
				if (result == Scriptable.NOT_FOUND) {
					throw notFoundError(cx, scope, name);
				}
				// For top scope thisObj for functions is always scope itself.
				thisObj = scope;
				break;
			}
		}

		if (asFunctionCall) {
			if (!(result instanceof Callable)) {
				throw notFunctionError(cx, result, name);
			}
			cx.storeScriptable(thisObj);
		}

		return result;
	}

	private static Object topScopeName(Context cx, Scriptable scope, String name) {
		return ScriptableObject.getProperty(scope, name, cx);
	}

	/**
	 * Returns the object in the scope chain that has a given property.
	 * <p>
	 * The order of evaluation of an assignment expression involves
	 * evaluating the lhs to a reference, evaluating the rhs, and then
	 * modifying the reference with the rhs value. This method is used
	 * to 'bind' the given name to an object containing that property
	 * so that the side effects of evaluating the rhs do not affect
	 * which property is modified.
	 * Typically used in conjunction with setName.
	 * <p>
	 * See ECMA 10.1.4
	 */
	public static Scriptable bind(Context cx, Scriptable scope, String id) {
		Scriptable firstXMLObject = null;
		Scriptable parent = scope.getParentScope();
		childScopesChecks:
		if (parent != null) {
			// Check for possibly nested "with" scopes first
			while (scope instanceof NativeWith) {
				Scriptable withObj = scope.getPrototype(cx);
				if (ScriptableObject.hasProperty(withObj, id, cx)) {
					return withObj;
				}

				scope = parent;
				parent = parent.getParentScope();
				if (parent == null) {
					break childScopesChecks;
				}
			}
			for (; ; ) {
				if (ScriptableObject.hasProperty(scope, id, cx)) {
					return scope;
				}
				scope = parent;
				parent = parent.getParentScope();
				if (parent == null) {
					break childScopesChecks;
				}
			}
		}
		// scope here is top scope
		if (ScriptableObject.hasProperty(scope, id, cx)) {
			return scope;
		}
		// Nothing was found, but since XML objects always bind
		// return one if found
		return firstXMLObject;
	}

	public static Object setName(Context cx, Scriptable scope, Scriptable bound, Object value, String id) {
		if (bound != null) {
			// TODO: we used to special-case XMLObject here, but putProperty
			// seems to work for E4X and it's better to optimize  the common case
			ScriptableObject.putProperty(bound, id, value, cx);
		} else {
			// "newname = 7;", where 'newname' has not yet
			// been defined, creates a new property in the
			// top scope unless strict mode is specified.
			Context.reportError(cx, ScriptRuntime.getMessage1("msg.assn.create.strict", id));
			// Find the top scope by walking up the scope chain.
			bound = ScriptableObject.getTopLevelScope(scope);
			bound.put(cx, id, bound, value);
		}
		return value;
	}

	public static Object strictSetName(Context cx, Scriptable scope, Scriptable bound, Object value, String id) {
		if (bound != null) {
			// TODO: The LeftHandSide also may not be a reference to a
			// data property with the attribute value {[[Writable]]:false},
			// to an accessor property with the attribute value
			// {[[Put]]:undefined}, nor to a non-existent property of an
			// object whose [[Extensible]] internal property has the value
			// false. In these cases a TypeError exception is thrown (11.13.1).
			// TODO: we used to special-case XMLObject here, but putProperty
			// seems to work for E4X and we should optimize  the common case
			ScriptableObject.putProperty(bound, id, value, cx);
			return value;
		}
		// See ES5 8.7.2
		String msg = "Assignment to undefined \"" + id + "\" in strict mode";
		throw constructError(cx, "ReferenceError", msg);
	}

	public static Object setConst(Context cx, Scriptable bound, Object value, String id) {
		ScriptableObject.putConstProperty(bound, id, value, cx);
		return value;
	}

	public static Scriptable toIterator(Context cx, Scriptable scope, Scriptable obj, boolean keyOnly) {
		if (ScriptableObject.hasProperty(obj, NativeIterator.ITERATOR_PROPERTY_NAME, cx)) {
			Object v = ScriptableObject.getProperty(obj, NativeIterator.ITERATOR_PROPERTY_NAME, cx);
			if (!(v instanceof Callable f)) {
				throw typeError0(cx, "msg.invalid.iterator");
			}
			Object[] args = new Object[]{keyOnly ? Boolean.TRUE : Boolean.FALSE};
			v = f.call(cx, scope, obj, args);
			if (!(v instanceof Scriptable)) {
				throw typeError0(cx, "msg.iterator.primitive");
			}
			return (Scriptable) v;
		}
		return null;
	}

	public static IdEnumeration enumInit(Context cx, Scriptable scope, Object value, int enumType) {
		IdEnumeration x = new IdEnumeration();
		x.obj = toObjectOrNull(cx, value, scope);
		// "for of" loop
		if (enumType == ENUMERATE_VALUES_IN_ORDER) {
			x.enumType = enumType;
			x.iterator = null;
			return enumInitInOrder(cx, x);
		}
		if (x.obj == null) {
			// null or undefined do not cause errors but rather lead to empty
			// "for in" loop
			return x;
		}
		x.enumType = enumType;
		x.iterator = null;
		if (enumType != ENUMERATE_KEYS_NO_ITERATOR && enumType != ENUMERATE_VALUES_NO_ITERATOR && enumType != ENUMERATE_ARRAY_NO_ITERATOR) {
			x.iterator = toIterator(cx, x.obj.getParentScope(), x.obj, enumType == ScriptRuntime.ENUMERATE_KEYS);
		}
		if (x.iterator == null) {
			// enumInit should read all initial ids before returning
			// or "for (a.i in a)" would wrongly enumerate i in a as well
			x.changeObject(cx);
		}

		return x;
	}

	private static IdEnumeration enumInitInOrder(Context cx, IdEnumeration x) {
		Object iterator = x.obj instanceof SymbolScriptable ? ScriptableObject.getProperty(x.obj, SymbolKey.ITERATOR, cx) : null;

		if (!(iterator instanceof Callable f)) {
			if (iterator instanceof IdEnumerationIterator) {
				x.iterator = (IdEnumerationIterator) iterator;
				return x;
			}

			throw typeError1(cx, "msg.not.iterable", toString(cx, x.obj));
		}

		Scriptable scope = x.obj.getParentScope();
		Object v = f.call(cx, scope, x.obj, EMPTY_OBJECTS);

		if (!(v instanceof Scriptable)) {
			if (v instanceof IdEnumerationIterator) {
				x.iterator = (IdEnumerationIterator) v;
				return x;
			}

			throw typeError1(cx, "msg.not.iterable", toString(cx, x.obj));
		}

		x.iterator = (Scriptable) v;
		return x;
	}

	/**
	 * Prepare for calling name(...): return function corresponding to
	 * name and make current top scope available
	 * as ScriptRuntime.lastStoredScriptable() for consumption as thisObj.
	 * The caller must call ScriptRuntime.lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getNameFunctionAndThis(Context cx, Scriptable scope, String name) {
		Scriptable parent = scope.getParentScope();
		if (parent == null) {
			Object result = topScopeName(cx, scope, name);
			if (!(result instanceof Callable)) {
				if (result == Scriptable.NOT_FOUND) {
					throw notFoundError(cx, scope, name);
				}
				throw notFunctionError(cx, result, name);
			}
			// Top scope is not NativeWith or NativeCall => thisObj == scope
			Scriptable thisObj = scope;
			cx.storeScriptable(thisObj);
			return (Callable) result;
		}

		// name will call storeScriptable(cx, thisObj);
		return (Callable) nameOrFunction(cx, scope, parent, name, true);
	}

	/**
	 * Prepare for calling obj[id](...): return function corresponding to
	 * obj[id] and make obj properly converted to Scriptable available
	 * as ScriptRuntime.lastStoredScriptable() for consumption as thisObj.
	 * The caller must call ScriptRuntime.lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getElemFunctionAndThis(Context cx, Scriptable scope, Object obj, Object elem) {
		Scriptable thisObj;
		Object value;

		if (isSymbol(elem)) {
			thisObj = toObjectOrNull(cx, obj, scope);
			if (thisObj == null) {
				throw undefCallError(cx, obj, String.valueOf(elem));
			}
			value = ScriptableObject.getProperty(thisObj, (Symbol) elem, cx);

		} else {
			StringIdOrIndex s = toStringIdOrIndex(cx, elem);
			if (s.stringId != null) {
				return getPropFunctionAndThis(cx, scope, obj, s.stringId);
			}

			thisObj = toObjectOrNull(cx, obj, scope);
			if (thisObj == null) {
				throw undefCallError(cx, obj, String.valueOf(elem));
			}

			value = ScriptableObject.getProperty(thisObj, s.index, cx);
		}

		if (!(value instanceof Callable)) {
			throw notFunctionError(cx, value, elem);
		}

		cx.storeScriptable(thisObj);
		return (Callable) value;
	}

	/**
	 * Prepare for calling obj.property(...): return function corresponding to
	 * obj.property and make obj properly converted to Scriptable available
	 * as ScriptRuntime.lastStoredScriptable() for consumption as thisObj.
	 * The caller must call ScriptRuntime.lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getPropFunctionAndThis(Context cx, Scriptable scope, Object obj, String property) {
		Scriptable thisObj = toObjectOrNull(cx, obj, scope);
		return getPropFunctionAndThisHelper(cx, thisObj, obj, property);
	}

	private static Callable getPropFunctionAndThisHelper(Context cx, Scriptable thisObj, Object obj, String property) {
		if (thisObj == null) {
			throw undefCallError(cx, obj, property);
		}

		Object value = ScriptableObject.getProperty(thisObj, property, cx);
		if (!(value instanceof Callable)) {
			Object noSuchMethod = ScriptableObject.getProperty(thisObj, "__noSuchMethod__", cx);
			if (noSuchMethod instanceof Callable) {
				value = new NoSuchMethodShim((Callable) noSuchMethod, property);
			}
		}

		if (!(value instanceof Callable)) {
			throw notFunctionError(cx, thisObj, value, property);
		}

		cx.storeScriptable(thisObj);
		return (Callable) value;
	}

	/**
	 * Prepare for calling &lt;expression&gt;(...): return function corresponding to
	 * &lt;expression&gt; and make parent scope of the function available
	 * as ScriptRuntime.lastStoredScriptable() for consumption as thisObj.
	 * The caller must call ScriptRuntime.lastStoredScriptable() immediately
	 * after calling this method.
	 */
	public static Callable getValueFunctionAndThis(Context cx, Object value) {
		if (!(value instanceof Callable f)) {
			throw notFunctionError(cx, value);
		}

		Scriptable thisObj = null;
		if (f instanceof Scriptable) {
			thisObj = ((Scriptable) f).getParentScope();
		}

		if (thisObj == null) {
			thisObj = cx.getTopCallOrThrow();
		}

		if (thisObj.getParentScope() != null) {
			if (thisObj instanceof NativeWith) {
				// functions defined inside with should have with target
				// as their thisObj
			} else if (thisObj instanceof NativeCall) {
				// nested functions should have top scope as their thisObj
				thisObj = ScriptableObject.getTopLevelScope(thisObj);
			}
		}
		cx.storeScriptable(thisObj);
		return f;
	}

	/**
	 * Given an object, get the "Symbol.iterator" element, throw a TypeError if it
	 * is not present, then call the result, (throwing a TypeError if the result is
	 * not a function), and return that result, whatever it is.
	 */
	public static Object callIterator(Context cx, Scriptable scope, Object obj) {
		final Callable getIterator = ScriptRuntime.getElemFunctionAndThis(cx, scope, obj, SymbolKey.ITERATOR);
		final Scriptable iterable = cx.lastStoredScriptable();
		return getIterator.call(cx, scope, iterable, ScriptRuntime.EMPTY_OBJECTS);
	}

	/**
	 * Given an iterator result, return true if and only if there is a "done"
	 * property that's true.
	 */
	public static boolean isIteratorDone(Context cx, Object result) {
		if (!(result instanceof Scriptable)) {
			return false;
		}
		final Object prop = getObjectProp(cx, (Scriptable) result, ES6Iterator.DONE_PROPERTY);
		return toBoolean(cx, prop);
	}

	/**
	 * Perform function call in reference context. Should always
	 * return value that can be passed to
	 * {@link #refGet(Context, Ref)} or {@link #refSet(Context, Scriptable, Ref, Object)}
	 * arbitrary number of times.
	 * The args array reference should not be stored in any object that is
	 * can be GC-reachable after this method returns. If this is necessary,
	 * store args.clone(), not args array itself.
	 */
	public static Ref callRef(Context cx, Scriptable thisObj, Callable function, Object[] args) {
		if (function instanceof RefCallable rfunction) {
			Ref ref = rfunction.refCall(cx, thisObj, args);
			if (ref == null) {
				throw new IllegalStateException(rfunction.getClass().getName() + ".refCall() returned null");
			}
			return ref;
		}
		// No runtime support for now
		String msg = getMessage1("msg.no.ref.from.function", toString(cx, function));
		throw constructError(cx, "ReferenceError", msg);
	}

	/**
	 * Operator new.
	 * <p>
	 * See ECMA 11.2.2
	 */
	public static Scriptable newObject(Object fun, Context cx, Scriptable scope, Object[] args) {
		if (!(fun instanceof Function function)) {
			throw notFunctionError(cx, fun);
		}
		return function.construct(cx, scope, args);
	}

	public static Object callSpecial(Context cx, Scriptable scope, Callable fun, Scriptable thisObj, Object[] args, Scriptable callerThis, int callType, String filename, int lineNumber) {
		if (callType == Node.SPECIALCALL_EVAL) {
			if (thisObj.getParentScope() == null && NativeGlobal.isEvalFunction(fun)) {
				return evalSpecial(cx, scope, callerThis, args, filename, lineNumber);
			}
		} else if (callType == Node.SPECIALCALL_WITH) {
			if (NativeWith.isWithFunction(fun)) {
				throw Context.reportRuntimeError1("msg.only.from.new", "With", cx);
			}
		} else {
			throw Kit.codeBug();
		}

		return fun.call(cx, scope, thisObj, args);
	}

	public static Object newSpecial(Context cx, Scriptable scope, Object fun, Object[] args, int callType) {
		if (callType == Node.SPECIALCALL_EVAL) {
			if (NativeGlobal.isEvalFunction(fun)) {
				throw typeError1(cx, "msg.not.ctor", "eval");
			}
		} else if (callType == Node.SPECIALCALL_WITH) {
			if (NativeWith.isWithFunction(fun)) {
				return NativeWith.newWithSpecial(cx, scope, args);
			}
		} else {
			throw Kit.codeBug();
		}

		return newObject(fun, cx, scope, args);
	}

	/**
	 * Function.prototype.apply and Function.prototype.call
	 * <p>
	 * See Ecma 15.3.4.[34]
	 */
	public static Object applyOrCall(Context cx, Scriptable scope, boolean isApply, Scriptable thisObj, Object[] args) {
		int L = args.length;
		Callable function = getCallable(cx, thisObj);

		Scriptable callThis = null;

		if (L != 0) {
			callThis = args[0] == Undefined.INSTANCE ? Undefined.SCRIPTABLE_INSTANCE : toObjectOrNull(cx, args[0], scope);
		}

		Object[] callArgs;
		if (isApply) {
			// Follow Ecma 15.3.4.3
			callArgs = L <= 1 ? ScriptRuntime.EMPTY_OBJECTS : getApplyArguments(cx, args[1]);
		} else {
			// Follow Ecma 15.3.4.4
			if (L <= 1) {
				callArgs = ScriptRuntime.EMPTY_OBJECTS;
			} else {
				callArgs = new Object[L - 1];
				System.arraycopy(args, 1, callArgs, 0, L - 1);
			}
		}

		return function.call(cx, scope, callThis, callArgs);
	}

	/**
	 * @return true if the passed in Scriptable looks like an array
	 */
	private static boolean isArrayLike(Context cx, Scriptable obj) {
		return obj != null && (obj instanceof NativeArray || obj instanceof Arguments || ScriptableObject.hasProperty(obj, "length", cx));
	}

	static Object[] getApplyArguments(Context cx, Object arg1) {
		if (arg1 == null || arg1 == Undefined.INSTANCE) {
			return ScriptRuntime.EMPTY_OBJECTS;
		} else if (arg1 instanceof Scriptable && isArrayLike(cx, (Scriptable) arg1)) {
			return ScriptRuntime.getArrayElements(cx, (Scriptable) arg1);
		} else if (arg1 instanceof ScriptableObject) {
			return ScriptRuntime.EMPTY_OBJECTS;
		} else {
			throw ScriptRuntime.typeError0(cx, "msg.arg.isnt.array");
		}
	}

	static Callable getCallable(Context cx, Scriptable thisObj) {
		Callable function;
		if (thisObj instanceof Callable) {
			function = (Callable) thisObj;
		} else {
			Object value = thisObj.getDefaultValue(cx, DefaultValueTypeHint.FUNCTION);
			if (!(value instanceof Callable)) {
				throw ScriptRuntime.notFunctionError(cx, value, thisObj);
			}
			function = (Callable) value;
		}
		return function;
	}

	/**
	 * The eval function property of the global object.
	 * <p>
	 * See ECMA 15.1.2.1
	 */
	public static Object evalSpecial(Context cx, Scriptable scope, Object thisArg, Object[] args, String filename, int lineNumber) {
		if (args.length < 1) {
			return Undefined.INSTANCE;
		}
		Object x = args[0];
		if (!(x instanceof CharSequence)) {
			String message = ScriptRuntime.getMessage0("msg.eval.nonstring");
			Context.reportWarning(message, cx);
			return x;
		}
		if (filename == null) {
			int[] linep = new int[1];
			filename = Context.getSourcePositionFromStack(cx, linep);
			if (filename != null) {
				lineNumber = linep[0];
			} else {
				filename = "";
			}
		}
		String sourceName = ScriptRuntime.makeUrlForGeneratedScript(true, filename, lineNumber);

		ErrorReporter reporter;
		reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());

		Evaluator evaluator = Context.createInterpreter();
		if (evaluator == null) {
			throw new JavaScriptException(cx, "Interpreter not present", filename, lineNumber);
		}

		// Compile with explicit interpreter instance to force interpreter
		// mode.
		Script script = cx.compileString(x.toString(), evaluator, reporter, sourceName, 1, null);
		evaluator.setEvalScriptFlag(script);
		Callable c = (Callable) script;
		return c.call(cx, scope, (Scriptable) thisArg, ScriptRuntime.EMPTY_OBJECTS);
	}

	/**
	 * The typeof operator
	 */
	public static MemberType typeof(Context cx, Object value) {
		return MemberType.get(value, cx);
	}

	/**
	 * The typeof operator that correctly handles the undefined case
	 */
	public static MemberType typeofName(Context cx, Scriptable scope, String id) {
		Scriptable val = bind(cx, scope, id);
		if (val == null) {
			return MemberType.UNDEFINED;
		}
		return typeof(cx, getObjectProp(cx, val, id));
	}

	// neg:
	// implement the '-' operator inline in the caller
	// as "-toNumber(val)"

	// not:
	// implement the '!' operator inline in the caller
	// as "!toBoolean(val)"

	// bitnot:
	// implement the '~' operator inline in the caller
	// as "~toInt32(val)"

	public static boolean isObject(Object value) {
		if (value == null) {
			return false;
		}
		if (Undefined.INSTANCE.equals(value)) {
			return false;
		}
		if (value instanceof ScriptableObject) {
			var type = ((ScriptableObject) value).getTypeOf();
			return type == MemberType.OBJECT || type == MemberType.FUNCTION;
		}
		if (value instanceof Scriptable) {
			return (!(value instanceof Callable));
		}
		return false;
	}

	public static Object add(Context cx, Object val1, Object val2) {
		if (val1 instanceof Number && val2 instanceof Number) {
			return wrapNumber(((Number) val1).doubleValue() + ((Number) val2).doubleValue());
		}
		if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
			throw typeError0(cx, "msg.not.a.number");
		}
		if (val1 instanceof Scriptable) {
			val1 = ((Scriptable) val1).getDefaultValue(cx, null);
		}
		if (val2 instanceof Scriptable) {
			val2 = ((Scriptable) val2).getDefaultValue(cx, null);
		}
		if (!(val1 instanceof CharSequence) && !(val2 instanceof CharSequence)) {
			if ((val1 instanceof Number) && (val2 instanceof Number)) {
				return wrapNumber(((Number) val1).doubleValue() + ((Number) val2).doubleValue());
			}
			return wrapNumber(toNumber(cx, val1) + toNumber(cx, val2));
		}
		return new ConsString(toCharSequence(cx, val1), toCharSequence(cx, val2));
	}

	public static CharSequence add(Context cx, CharSequence val1, Object val2) {
		return new ConsString(val1, toCharSequence(cx, val2));
	}

	public static CharSequence add(Context cx, Object val1, CharSequence val2) {
		return new ConsString(toCharSequence(cx, val1), val2);
	}

	public static Object nameIncrDecr(Context cx, Scriptable scopeChain, String id, int incrDecrMask) {
		Scriptable target;
		Object value;
		search:
		{
			do {
				target = scopeChain;
				do {
					value = target.get(cx, id, scopeChain);
					if (value != Scriptable.NOT_FOUND) {
						break search;
					}
					target = target.getPrototype(cx);
				} while (target != null);
				scopeChain = scopeChain.getParentScope();
			} while (scopeChain != null);
			throw notFoundError(cx, null, id);
		}
		return doScriptableIncrDecr(cx, target, id, scopeChain, value, incrDecrMask);
	}

	public static Object propIncrDecr(Context cx, Scriptable scope, Object obj, String id, int incrDecrMask) {
		Scriptable start = toObjectOrNull(cx, obj, scope);
		if (start == null) {
			throw undefReadError(cx, obj, id);
		}

		Scriptable target = start;
		Object value;
		search:
		{
			do {
				value = target.get(cx, id, start);
				if (value != Scriptable.NOT_FOUND) {
					break search;
				}
				target = target.getPrototype(cx);
			} while (target != null);
			start.put(cx, id, start, NaNobj);
			return NaNobj;
		}
		return doScriptableIncrDecr(cx, target, id, start, value, incrDecrMask);
	}

	private static Object doScriptableIncrDecr(Context cx, Scriptable target, String id, Scriptable protoChainStart, Object value, int incrDecrMask) {
		final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(cx, value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		target.put(cx, id, protoChainStart, result);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object elemIncrDecr(Context cx, Object obj, Object index, Scriptable scope, int incrDecrMask) {
		Object value = getObjectElem(cx, scope, obj, index);
		final boolean post = (incrDecrMask & Node.POST_FLAG) != 0;
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(cx, value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		setObjectElem(cx, scope, obj, index, result);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object refIncrDecr(Context cx, Scriptable scope, Ref ref, int incrDecrMask) {
		Object value = ref.get(cx);
		boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
		double number;
		if (value instanceof Number) {
			number = ((Number) value).doubleValue();
		} else {
			number = toNumber(cx, value);
			if (post) {
				// convert result to number
				value = wrapNumber(number);
			}
		}
		if ((incrDecrMask & Node.DECR_FLAG) == 0) {
			++number;
		} else {
			--number;
		}
		Number result = wrapNumber(number);
		ref.set(cx, scope, result);
		if (post) {
			return value;
		}
		return result;
	}

	public static Object toPrimitive(Context cx, Object val) {
		return toPrimitive(cx, val, null);
	}

	public static Object toPrimitive(Context cx, Object val, DefaultValueTypeHint typeHint) {
		if (!(val instanceof Scriptable s)) {
			return val;
		}
		Object result = s.getDefaultValue(cx, typeHint);
		if ((result instanceof Scriptable) && !isSymbol(result)) {
			throw typeError0(cx, "msg.bad.default.value");
		}
		return result;
	}

	/**
	 * Equality
	 * <p>
	 * See ECMA 11.9
	 */
	public static boolean eq(Context cx, Object x, Object y) {
		if (x == null || x == Undefined.INSTANCE) {
			if (y == null || y == Undefined.INSTANCE) {
				return true;
			}
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return false;
		} else if (x == y) {
			return true;
		}

		Object x1 = Wrapper.unwrapped(x);
		Object y1 = Wrapper.unwrapped(y);

		if (x1 == y1) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(x1, y1, false)) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(y1, x1, false)) {
			return true;
		} else if (x instanceof Number) {
			return eqNumber(cx, ((Number) x).doubleValue(), y);
		} else if (x instanceof CharSequence) {
			return eqString(cx, (CharSequence) x, y);
		} else if (x instanceof Boolean) {
			boolean b = (Boolean) x;
			if (y instanceof Boolean) {
				return b == (Boolean) y;
			}
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqNumber(cx, b ? 1.0 : 0.0, y);
		} else if (x instanceof Scriptable) {
			if (y instanceof Scriptable) {
				if (x instanceof ScriptableObject) {
					Object test = ((ScriptableObject) x).equivalentValues(y);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				if (y instanceof ScriptableObject) {
					Object test = ((ScriptableObject) y).equivalentValues(x);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				if (x instanceof Wrapper && y instanceof Wrapper) {
					// See bug 413838. Effectively an extension to ECMA for
					// the LiveConnect case.
					return x1 == y1 || (isPrimitive(x1) && isPrimitive(y1) && eq(cx, x1, y1));
				}
				return false;
			} else if (y instanceof Boolean) {
				if (x instanceof ScriptableObject) {
					Object test = ((ScriptableObject) x).equivalentValues(y);
					if (test != Scriptable.NOT_FOUND) {
						return (Boolean) test;
					}
				}
				double d = (Boolean) y ? 1.0 : 0.0;
				return eqNumber(cx, d, x);
			} else if (y instanceof Number) {
				return eqNumber(cx, ((Number) y).doubleValue(), x);
			} else if (y instanceof CharSequence) {
				return eqString(cx, (CharSequence) y, x);
			}
			// covers the case when y == Undefined.instance as well
			return false;
		} else {
			warnAboutNonJSObject(cx, x);
			return x == y;
		}
	}

	/*
	 * Implement "SameValue" as in ECMA 7.2.9. This is not the same as "eq" because it handles
	 * signed zeroes and NaNs differently.
	 */
	public static boolean same(Context cx, Object x, Object y) {
		if (typeof(cx, x) != typeof(cx, y)) {
			return false;
		}
		if (x instanceof Number) {
			if (isNaN(x) && isNaN(y)) {
				return true;
			}
			return x.equals(y);
		}
		return eq(cx, x, y);
	}

	/**
	 * Implement "SameValueZero" from ECMA 7.2.9
	 */
	public static boolean sameZero(Context cx, Object x, Object y) {
		x = Wrapper.unwrapped(x);
		y = Wrapper.unwrapped(y);

		if (typeof(cx, x) != typeof(cx, y)) {
			return false;
		}
		if (x instanceof Number) {
			if (isNaN(x) && isNaN(y)) {
				return true;
			}
			final double dx = ((Number) x).doubleValue();
			if (y instanceof Number) {
				final double dy = ((Number) y).doubleValue();
				if (((dx == negativeZero) && (dy == 0.0)) || ((dx == 0.0) && dy == negativeZero)) {
					return true;
				}
			}
			return eqNumber(cx, dx, y);
		}
		return eq(cx, x, y);
	}

	public static boolean isNaN(Object n) {
		if (n instanceof Double) {
			return ((Double) n).isNaN();
		}
		if (n instanceof Float) {
			return ((Float) n).isNaN();
		}
		return false;
	}

	public static boolean isPrimitive(Object obj) {
		return obj == null || obj == Undefined.INSTANCE || (obj instanceof Number) || (obj instanceof String) || (obj instanceof Boolean);
	}

	static boolean eqNumber(Context cx, double x, Object y) {
		if (y == null || y == Undefined.INSTANCE) {
			return false;
		} else if (y instanceof Number) {
			return x == ((Number) y).doubleValue();
		} else if (y instanceof CharSequence) {
			return x == toNumber(cx, y);
		} else if (y instanceof Boolean) {
			return x == ((Boolean) y ? 1.0 : +0.0);
		} else if (isSymbol(y)) {
			return false;
		} else if (y instanceof Scriptable) {
			if (y instanceof ScriptableObject) {
				Object xval = wrapNumber(x);
				Object test = ((ScriptableObject) y).equivalentValues(xval);
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqNumber(cx, x, toPrimitive(cx, y));
		} else {
			warnAboutNonJSObject(cx, y);
			return false;
		}
	}

	private static boolean eqString(Context cx, CharSequence x, Object y) {
		if (y == null || y == Undefined.INSTANCE) {
			return false;
		} else if (y instanceof CharSequence c) {
			return x.length() == c.length() && x.toString().equals(c.toString());
		} else if (y instanceof Number) {
			return toNumber(cx, x.toString()) == ((Number) y).doubleValue();
		} else if (y instanceof Boolean) {
			return toNumber(cx, x.toString()) == ((Boolean) y ? 1.0 : 0.0);
		} else if (isSymbol(y)) {
			return false;
		} else if (y instanceof Scriptable) {
			if (y instanceof ScriptableObject) {
				Object test = ((ScriptableObject) y).equivalentValues(x.toString());
				if (test != Scriptable.NOT_FOUND) {
					return (Boolean) test;
				}
			}
			return eqString(cx, x, toPrimitive(cx, y));
		} else {
			warnAboutNonJSObject(cx, y);
			return false;
		}
	}

	public static boolean shallowEq(Context cx, Object x, Object y) {
		if (x == y) {
			if (!(x instanceof Number)) {
				return true;
			}
			// NaN check
			double d = ((Number) x).doubleValue();
			return !Double.isNaN(d);
		} else if (x == null || x == Undefined.INSTANCE || x == Undefined.SCRIPTABLE_INSTANCE) {
			return (x == Undefined.INSTANCE && y == Undefined.SCRIPTABLE_INSTANCE) || (x == Undefined.SCRIPTABLE_INSTANCE && y == Undefined.INSTANCE);
		}

		Object x1 = Wrapper.unwrapped(x);
		Object y1 = Wrapper.unwrapped(y);

		if (x1 == y1) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(x1, y1, true)) {
			return true;
		} else if (SpecialEquality.checkSpecialEquality(y1, x1, true)) {
			return true;
		} else if (x1 instanceof Number) {
			if (y1 instanceof Number) {
				return ((Number) x1).doubleValue() == ((Number) y1).doubleValue();
			}
		} else if (x1 instanceof CharSequence) {
			return x1.toString().equals(String.valueOf(y1));
		} else if (y1 instanceof CharSequence) {
			return y1.toString().equals(String.valueOf(x1));
		} else if (x1 instanceof Boolean) {
			if (y1 instanceof Boolean) {
				return x1.equals(y1);
			}
		} else if (!(x1 instanceof Scriptable)) {
			warnAboutNonJSObject(cx, x1);
		}

		return false;
	}

	/**
	 * The instanceof operator.
	 *
	 * @return a instanceof b
	 */
	public static boolean instanceOf(Context cx, Object a, Object b) {
		// Check RHS is an object
		if (!(b instanceof Scriptable)) {
			throw typeError0(cx, "msg.instanceof.not.object");
		}

		// for primitive values on LHS, return false
		if (!(a instanceof Scriptable)) {
			return false;
		}

		return ((Scriptable) b).hasInstance(cx, (Scriptable) a);
	}

	/**
	 * Delegates to
	 *
	 * @return true iff rhs appears in lhs' proto chain
	 */
	public static boolean jsDelegatesTo(Context cx, Scriptable lhs, Scriptable rhs) {
		Scriptable proto = lhs.getPrototype(cx);

		while (proto != null) {
			if (proto.equals(rhs)) {
				return true;
			}
			proto = proto.getPrototype(cx);
		}

		return false;
	}

	/**
	 * The in operator.
	 * <p>
	 * This is a new JS 1.3 language feature.  The in operator mirrors
	 * the operation of the for .. in construct, and tests whether the
	 * rhs has the property given by the lhs.  It is different from the
	 * for .. in construct in that:
	 * <BR> - it doesn't perform ToObject on the right hand side
	 * <BR> - it returns true for DontEnum properties.
	 *
	 * @param a the left hand operand
	 * @param b the right hand operand
	 * @return true if property name or element number a is a property of b
	 */
	public static boolean in(Context cx, Object a, Object b) {
		if (!(b instanceof Scriptable)) {
			throw typeError0(cx, "msg.in.not.object");
		}

		return hasObjectElem(cx, (Scriptable) b, a);
	}

	public static boolean cmp_LT(Context cx, Object val1, Object val2) {
		double d1, d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
				throw typeError0(cx, "msg.compare.symbol");
			}
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(cx, DefaultValueTypeHint.NUMBER);
			}
			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(cx, DefaultValueTypeHint.NUMBER);
			}
			if (val1 instanceof CharSequence && val2 instanceof CharSequence) {
				return val1.toString().compareTo(val2.toString()) < 0;
			}
			d1 = toNumber(cx, val1);
			d2 = toNumber(cx, val2);
		}
		return d1 < d2;
	}

	// ------------------
	// Statements
	// ------------------

	public static boolean cmp_LE(Context cx, Object val1, Object val2) {
		double d1, d2;
		if (val1 instanceof Number && val2 instanceof Number) {
			d1 = ((Number) val1).doubleValue();
			d2 = ((Number) val2).doubleValue();
		} else {
			if ((val1 instanceof Symbol) || (val2 instanceof Symbol)) {
				throw typeError0(cx, "msg.compare.symbol");
			}
			if (val1 instanceof Scriptable) {
				val1 = ((Scriptable) val1).getDefaultValue(cx, DefaultValueTypeHint.NUMBER);
			}
			if (val2 instanceof Scriptable) {
				val2 = ((Scriptable) val2).getDefaultValue(cx, DefaultValueTypeHint.NUMBER);
			}
			if (val1 instanceof CharSequence && val2 instanceof CharSequence) {
				return val1.toString().compareTo(val2.toString()) <= 0;
			}
			d1 = toNumber(cx, val1);
			d2 = toNumber(cx, val2);
		}
		return d1 <= d2;
	}

	public static void initScript(Context cx, Scriptable scope, NativeFunction funObj, Scriptable thisObj, boolean evalScript) {
		if (!cx.hasTopCallScope()) {
			throw new IllegalStateException();
		}

		int varCount = funObj.getParamAndVarCount();
		if (varCount != 0) {

			Scriptable varScope = scope;
			// Never define any variables from var statements inside with
			// object. See bug 38590.
			while (varScope instanceof NativeWith) {
				varScope = varScope.getParentScope();
			}

			for (int i = varCount; i-- != 0; ) {
				String name = funObj.getParamOrVarName(i);
				boolean isConst = funObj.getParamOrVarConst(i);
				// Don't overwrite existing def if already defined in object
				// or prototypes of object.
				if (!ScriptableObject.hasProperty(scope, name, cx)) {
					if (isConst) {
						ScriptableObject.defineConstProperty(varScope, name, cx);
					} else if (!evalScript) {
						if (!(funObj instanceof InterpretedFunction) || ((InterpretedFunction) funObj).hasFunctionNamed(name)) {
							// Global var definitions are supposed to be DONTDELETE
							ScriptableObject.defineProperty(varScope, name, Undefined.INSTANCE, ScriptableObject.PERMANENT, cx);
						}
					} else {
						varScope.put(cx, name, varScope, Undefined.INSTANCE);
					}
				} else {
					ScriptableObject.redefineProperty(scope, name, isConst, cx);
				}
			}
		}
	}

	public static Scriptable createFunctionActivation(Context cx, Scriptable scope, NativeFunction funObj, Object[] args, boolean isStrict) {
		return new NativeCall(funObj, scope, args, false, isStrict, cx);
	}

	public static Scriptable createArrowFunctionActivation(Context cx, Scriptable scope, NativeFunction funObj, Object[] args, boolean isStrict) {
		return new NativeCall(funObj, scope, args, true, isStrict, cx);
	}

	public static void enterActivationFunction(Context cx, Scriptable scope) {
		if (!cx.hasTopCallScope()) {
			throw new IllegalStateException();
		}
		NativeCall call = (NativeCall) scope;
		call.parentActivationCall = cx.currentActivationCall;
		cx.currentActivationCall = call;
		call.defineAttributesForArguments(cx);
	}

	public static void exitActivationFunction(Context cx) {
		NativeCall call = cx.currentActivationCall;
		cx.currentActivationCall = call.parentActivationCall;
		call.parentActivationCall = null;
	}

	static NativeCall findFunctionActivation(Context cx, Function f) {
		NativeCall call = cx.currentActivationCall;
		while (call != null) {
			if (call.function == f) {
				return call;
			}
			call = call.parentActivationCall;
		}
		return null;
	}

	public static Scriptable newCatchScope(Context cx, Scriptable scope, Throwable t, Scriptable lastCatchScope, String exceptionName) {
		Object obj;
		boolean cacheObj;

		getObj:
		if (t instanceof JavaScriptException) {
			cacheObj = false;
			obj = ((JavaScriptException) t).getValue();
		} else {
			cacheObj = true;

			// Create wrapper object unless it was associated with
			// the previous scope object

			if (lastCatchScope != null) {
				NativeObject last = (NativeObject) lastCatchScope;
				obj = last.getAssociatedValue(t);
				if (obj == null) {
					Kit.codeBug();
				}
				break getObj;
			}

			RhinoException re;
			TopLevel.NativeErrors type;
			String errorMsg;
			Throwable javaException = null;

			if (t instanceof EcmaError ee) {
				re = ee;
				type = TopLevel.NativeErrors.valueOf(ee.getName());
				errorMsg = ee.getErrorMessage();
			} else if (t instanceof WrappedException we) {
				re = we;
				javaException = we.getWrappedException();
				type = TopLevel.NativeErrors.JavaException;
				errorMsg = javaException.getClass().getName() + ": " + javaException.getMessage();
			} else if (t instanceof EvaluatorException ee) {
				// Pure evaluator exception, nor WrappedException instance
				re = ee;
				type = TopLevel.NativeErrors.InternalError;
				errorMsg = ee.getMessage();
			} else {
				// Script can catch only instances of JavaScriptException,
				// EcmaError and EvaluatorException
				throw Kit.codeBug();
			}

			String sourceUri = re.sourceName();
			if (sourceUri == null) {
				sourceUri = "";
			}
			int line = re.lineNumber();
			Object[] args;
			if (line > 0) {
				args = new Object[]{errorMsg, sourceUri, line};
			} else {
				args = new Object[]{errorMsg, sourceUri};
			}

			Scriptable errorObject = newNativeError(cx, scope, type, args);
			// set exception in Error objects to enable non-ECMA "stack" property
			if (errorObject instanceof NativeError) {
				((NativeError) errorObject).setStackProvider(re, cx);
			}

			if (javaException != null && cx.visibleToScripts(javaException.getClass().getName(), ClassVisibilityContext.EXCEPTION)) {
				Object wrap = cx.wrap(scope, javaException);
				ScriptableObject.defineProperty(errorObject, "javaException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM, cx);
			}
			if (cx.visibleToScripts(re.getClass().getName(), ClassVisibilityContext.EXCEPTION)) {
				Object wrap = cx.wrap(scope, re);
				ScriptableObject.defineProperty(errorObject, "rhinoException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM, cx);
			}
			obj = errorObject;
		}

		NativeObject catchScopeObject = new NativeObject(cx.factory);
		// See ECMA 12.4
		catchScopeObject.defineProperty(cx, exceptionName, obj, ScriptableObject.PERMANENT);

		if (cx.visibleToScripts(t.getClass().getName(), ClassVisibilityContext.EXCEPTION)) {
			// Add special Rhino object __exception__ defined in the catch
			// scope that can be used to retrieve the Java exception associated
			// with the JavaScript exception (to get stack trace info, etc.)
			catchScopeObject.defineProperty(cx, "__exception__", cx.javaToJS(t, scope), ScriptableObject.PERMANENT | ScriptableObject.DONTENUM);
		}

		if (cacheObj) {
			catchScopeObject.associateValue(t, obj);
		}
		return catchScopeObject;
	}

	public static Scriptable wrapException(Context cx, Scriptable scope, Throwable t) {
		RhinoException re;
		String errorName;
		String errorMsg;
		Throwable javaException = null;

		if (t instanceof EcmaError ee) {
			re = ee;
			errorName = ee.getName();
			errorMsg = ee.getErrorMessage();
		} else if (t instanceof WrappedException we) {
			re = we;
			javaException = we.getWrappedException();
			errorName = "JavaException";
			errorMsg = javaException.getClass().getName() + ": " + javaException.getMessage();
		} else if (t instanceof EvaluatorException ee) {
			// Pure evaluator exception, nor WrappedException instance
			re = ee;
			errorName = "InternalError";
			errorMsg = ee.getMessage();
		} else {
			// Script can catch only instances of JavaScriptException,
			// EcmaError and EvaluatorException
			throw Kit.codeBug();
		}

		String sourceUri = re.sourceName();
		if (sourceUri == null) {
			sourceUri = "";
		}
		int line = re.lineNumber();
		Object[] args;
		if (line > 0) {
			args = new Object[]{errorMsg, sourceUri, line};
		} else {
			args = new Object[]{errorMsg, sourceUri};
		}

		Scriptable errorObject = cx.newObject(scope, errorName, args);
		ScriptableObject.putProperty(errorObject, "name", errorName, cx);
		// set exception in Error objects to enable non-ECMA "stack" property
		if (errorObject instanceof NativeError) {
			((NativeError) errorObject).setStackProvider(re, cx);
		}

		if (javaException != null && cx.visibleToScripts(javaException.getClass().getName(), ClassVisibilityContext.EXCEPTION)) {
			Object wrap = cx.wrap(scope, javaException);
			ScriptableObject.defineProperty(errorObject, "javaException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM, cx);
		}
		if (cx.visibleToScripts(re.getClass().getName(), ClassVisibilityContext.EXCEPTION)) {
			Object wrap = cx.wrap(scope, re);
			ScriptableObject.defineProperty(errorObject, "rhinoException", wrap, ScriptableObject.PERMANENT | ScriptableObject.READONLY | ScriptableObject.DONTENUM, cx);
		}
		return errorObject;
	}

	public static Scriptable enterWith(Context cx, Scriptable scope, Object obj) {
		Scriptable sobj = toObjectOrNull(cx, obj, scope);
		if (sobj == null) {
			throw typeError1(cx, "msg.undef.with", toString(cx, obj));
		}
		return new NativeWith(scope, sobj);
	}

	public static Scriptable leaveWith(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static Scriptable enterDotQuery(Object value, Scriptable scope, Context cx) {
		throw notXmlError(cx, value);
	}

	public static Object updateDotQuery(boolean value, Scriptable scope) {
		// Return null to continue looping
		NativeWith nw = (NativeWith) scope;
		return nw.updateDotQuery(value);
	}

	public static Scriptable leaveDotQuery(Scriptable scope) {
		NativeWith nw = (NativeWith) scope;
		return nw.getParentScope();
	}

	public static void setFunctionProtoAndParent(Context cx, Scriptable scope, BaseFunction fn) {
		setFunctionProtoAndParent(cx, scope, fn, false);
	}

	public static void setFunctionProtoAndParent(Context cx, Scriptable scope, BaseFunction fn, boolean es6GeneratorFunction) {
		fn.setParentScope(scope);
		if (es6GeneratorFunction) {
			fn.setPrototype(ScriptableObject.getGeneratorFunctionPrototype(scope, cx));
		} else {
			fn.setPrototype(ScriptableObject.getFunctionPrototype(scope, cx));
		}
	}

	public static void setObjectProtoAndParent(Context cx, Scriptable scope, ScriptableObject object) {
		// Compared with function it always sets the scope to top scope
		scope = ScriptableObject.getTopLevelScope(scope);
		object.setParentScope(scope);
		Scriptable proto = ScriptableObject.getClassPrototype(scope, object.getClassName(), cx);
		object.setPrototype(proto);
	}

	public static void setBuiltinProtoAndParent(Context cx, Scriptable scope, ScriptableObject object, TopLevel.Builtins type) {
		scope = ScriptableObject.getTopLevelScope(scope);
		object.setParentScope(scope);
		object.setPrototype(TopLevel.getBuiltinPrototype(scope, type, cx));
	}

	public static void initFunction(Context cx, Scriptable scope, NativeFunction function, int type, boolean fromEvalCode) {
		if (type == FunctionNode.FUNCTION_STATEMENT) {
			String name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				if (!fromEvalCode) {
					// ECMA specifies that functions defined in global and
					// function scope outside eval should have DONTDELETE set.
					ScriptableObject.defineProperty(scope, name, function, ScriptableObject.PERMANENT, cx);
				} else {
					scope.put(cx, name, scope, function);
				}
			}
		} else if (type == FunctionNode.FUNCTION_EXPRESSION_STATEMENT) {
			String name = function.getFunctionName();
			if (name != null && name.length() != 0) {
				// Always put function expression statements into initial
				// activation object ignoring the with statement to follow
				// SpiderMonkey
				while (scope instanceof NativeWith) {
					scope = scope.getParentScope();
				}
				scope.put(cx, name, scope, function);
			}
		} else {
			throw Kit.codeBug();
		}
	}

	public static Scriptable newArrayLiteral(Context cx, Scriptable scope, Object[] objects, int[] skipIndices) {
		final int SKIP_DENSITY = 2;
		int count = objects.length;
		int skipCount = 0;
		if (skipIndices != null) {
			skipCount = skipIndices.length;
		}
		int length = count + skipCount;
		if (length > 1 && skipCount * SKIP_DENSITY < length) {
			// If not too sparse, create whole array for constructor
			Object[] sparse;
			if (skipCount == 0) {
				sparse = objects;
			} else {
				sparse = new Object[length];
				int skip = 0;
				for (int i = 0, j = 0; i != length; ++i) {
					if (skip != skipCount && skipIndices[skip] == i) {
						sparse[i] = Scriptable.NOT_FOUND;
						++skip;
						continue;
					}
					sparse[i] = objects[j];
					++j;
				}
			}
			return cx.newArray(scope, sparse);
		}

		Scriptable array = cx.newArray(scope, length);

		int skip = 0;
		for (int i = 0, j = 0; i != length; ++i) {
			if (skip != skipCount && skipIndices[skip] == i) {
				++skip;
				continue;
			}
			array.put(cx, i, array, objects[j]);
			++j;
		}
		return array;
	}

	public static Scriptable newObjectLiteral(Context cx, Scriptable scope, Object[] propertyIds, Object[] propertyValues, int[] getterSetters) {
		Scriptable object = cx.newObject(scope);
		for (int i = 0, end = propertyIds.length; i != end; ++i) {
			Object id = propertyIds[i];
			int getterSetter = getterSetters == null ? 0 : getterSetters[i];
			Object value = propertyValues[i];
			if (id instanceof String) {
				if (getterSetter == 0) {
					if (isSpecialProperty((String) id)) {
						Ref ref = specialRef(cx, scope, object, (String) id);
						ref.set(cx, scope, value);
					} else {
						object.put(cx, (String) id, object, value);
					}
				} else {
					ScriptableObject so = (ScriptableObject) object;
					Callable getterOrSetter = (Callable) value;
					boolean isSetter = getterSetter == 1;
					so.setGetterOrSetter(cx, (String) id, 0, getterOrSetter, isSetter);
				}
			} else {
				int index = (Integer) id;
				object.put(cx, index, object, value);
			}
		}
		return object;
	}

	public static boolean isArrayObject(Object obj) {
		return obj instanceof NativeArray || obj instanceof Arguments;
	}

	public static Object[] getArrayElements(Context cx, Scriptable object) {
		long longLen = NativeArray.getLengthProperty(cx, object, false);
		if (longLen > Integer.MAX_VALUE) {
			// arrays beyond  MAX_INT is not in Java in any case
			throw new IllegalArgumentException();
		}
		int len = (int) longLen;
		if (len == 0) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}
		Object[] result = new Object[len];
		for (int i = 0; i < len; i++) {
			Object elem = ScriptableObject.getProperty(object, i, cx);
			result[i] = (elem == Scriptable.NOT_FOUND) ? Undefined.INSTANCE : elem;
		}
		return result;
	}

	static void checkDeprecated(Context cx, String name) {
		throw Context.reportRuntimeError(getMessage1("msg.deprec.ctor", name), cx);
	}

	public static String getMessage0(String messageId) {
		return getMessage(messageId, null);
	}

	public static String getMessage1(String messageId, Object arg1) {
		Object[] arguments = {arg1};
		return getMessage(messageId, arguments);
	}

	public static String getMessage2(String messageId, Object arg1, Object arg2) {
		Object[] arguments = {arg1, arg2};
		return getMessage(messageId, arguments);
	}

	public static String getMessage3(String messageId, Object arg1, Object arg2, Object arg3) {
		Object[] arguments = {arg1, arg2, arg3};
		return getMessage(messageId, arguments);
	}

	public static String getMessage4(String messageId, Object arg1, Object arg2, Object arg3, Object arg4) {
		Object[] arguments = {arg1, arg2, arg3, arg4};
		return getMessage(messageId, arguments);
	}

	public static String getMessage(String messageId, Object[] arguments) {
		return messageProvider.getMessage(messageId, arguments);
	}

	public static EcmaError constructError(Context cx, String error, String message) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(cx, linep);
		return constructError(cx, error, message, filename, linep[0], null, 0);
	}

	public static EcmaError constructError(Context cx, String error, String message, int lineNumberDelta) {
		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(cx, linep);
		if (linep[0] != 0) {
			linep[0] += lineNumberDelta;
		}
		return constructError(cx, error, message, filename, linep[0], null, 0);
	}

	public static EcmaError constructError(Context cx, String error, String message, String sourceName, int lineNumber, String lineSource, int columnNumber) {
		return new EcmaError(cx, error, message, sourceName, lineNumber, lineSource, columnNumber);
	}

	public static EcmaError rangeError(Context cx, String message) {
		return constructError(cx, "RangeError", message);
	}

	public static EcmaError typeError(Context cx, String message) {
		return constructError(cx, "TypeError", message);
	}

	public static EcmaError typeError0(Context cx, String messageId) {
		String msg = getMessage0(messageId);
		return typeError(cx, msg);
	}

	public static EcmaError typeError1(Context cx, String messageId, Object arg1) {
		String msg = getMessage1(messageId, arg1);
		return typeError(cx, msg);
	}

	public static EcmaError typeError2(Context cx, String messageId, Object arg1, Object arg2) {
		String msg = getMessage2(messageId, arg1, arg2);
		return typeError(cx, msg);
	}

	public static EcmaError typeError3(Context cx, String messageId, String arg1, String arg2, String arg3) {
		String msg = getMessage3(messageId, arg1, arg2, arg3);
		return typeError(cx, msg);
	}

	public static RuntimeException undefReadError(Context cx, Object object, Object id) {
		return typeError2(cx, "msg.undef.prop.read", toString(cx, object), toString(cx, id));
	}

	public static RuntimeException undefCallError(Context cx, Object object, Object id) {
		return typeError2(cx, "msg.undef.method.call", toString(cx, object), toString(cx, id));
	}

	public static RuntimeException undefWriteError(Context cx, Object object, Object id, Object value) {
		return typeError3(cx, "msg.undef.prop.write", toString(cx, object), toString(cx, id), toString(cx, value));
	}

	private static RuntimeException undefDeleteError(Context cx, Object object, Object id) {
		throw typeError2(cx, "msg.undef.prop.delete", toString(cx, object), toString(cx, id));
	}

	public static RuntimeException notFoundError(Context cx, Scriptable object, String property) {
		// XXX: use object to improve the error message
		String msg = getMessage1("msg.is.not.defined", property);
		throw constructError(cx, "ReferenceError", msg);
	}

	public static RuntimeException notFunctionError(Context cx, Object value) {
		return notFunctionError(cx, value, value);
	}

	public static RuntimeException notFunctionError(Context cx, Object value, Object messageHelper) {
		// Use value for better error reporting
		String msg = (messageHelper == null) ? "null" : messageHelper.toString();
		if (value == Scriptable.NOT_FOUND) {
			return typeError1(cx, "msg.function.not.found", msg);
		}
		return typeError2(cx, "msg.isnt.function", msg, typeof(cx, value));
	}

	public static RuntimeException notFunctionError(Context cx, Object obj, Object value, String propertyName) {
		// Use obj and value for better error reporting
		String objString = toString(cx, obj);
		if (obj instanceof NativeFunction) {
			// Omit function body in string representations of functions
			int paren = objString.indexOf(')');
			int curly = objString.indexOf('{', paren);
			if (curly > -1) {
				objString = objString.substring(0, curly + 1) + "...}";
			}
		}
		if (value == Scriptable.NOT_FOUND) {
			return typeError2(cx, "msg.function.not.found.in", propertyName, objString);
		}
		return typeError3(cx, "msg.isnt.function.in", propertyName, objString, typeof(cx, value).toString());
	}

	private static RuntimeException notXmlError(Context cx, Object value) {
		throw typeError1(cx, "msg.isnt.xml.object", toString(cx, value));
	}

	private static void warnAboutNonJSObject(Context cx, Object nonJSObject) {
		final String omitParam = ScriptRuntime.getMessage0("params.omit.non.js.object.warning");
		if (!"true".equals(omitParam)) {
			String message = ScriptRuntime.getMessage2("msg.non.js.object.warning", nonJSObject, nonJSObject.getClass().getName());
			Context.reportWarning(message, cx);
			// Just to be sure that it would be noticed
			System.err.println(message);
		}
	}

	public static void setRegExpProxy(Context cx, RegExp proxy) {
		if (proxy == null) {
			throw new IllegalArgumentException();
		}
		cx.regExp = proxy;
	}

	public static Scriptable wrapRegExp(Context cx, Scriptable scope, Object compiled) {
		return cx.getRegExp().wrapRegExp(cx, scope, compiled);
	}

	public static Scriptable getTemplateLiteralCallSite(Context cx, Scriptable scope, Object[] strings, int index) {
		/* step 1 */
		Object callsite = strings[index];
		if (callsite instanceof Scriptable) {
			return (Scriptable) callsite;
		}
		assert callsite instanceof String[];
		String[] vals = (String[]) callsite;
		assert (vals.length & 1) == 0;
		final int FROZEN = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
		/* step 2-7 */
		ScriptableObject siteObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);
		ScriptableObject rawObj = (ScriptableObject) cx.newArray(scope, vals.length >>> 1);
		for (int i = 0, n = vals.length; i < n; i += 2) {
			/* step 8 a-f */
			int idx = i >>> 1;
			siteObj.put(cx, idx, siteObj, vals[i]);
			siteObj.setAttributes(cx, idx, FROZEN);
			rawObj.put(cx, idx, rawObj, vals[i + 1]);
			rawObj.setAttributes(cx, idx, FROZEN);
		}
		/* step 9 */
		// TODO: call abstract operation FreezeObject
		rawObj.setAttributes(cx, "length", FROZEN);
		rawObj.preventExtensions();
		/* step 10 */
		siteObj.put(cx, "raw", siteObj, rawObj);
		siteObj.setAttributes(cx, "raw", FROZEN | ScriptableObject.DONTENUM);
		/* step 11 */
		// TODO: call abstract operation FreezeObject
		siteObj.setAttributes(cx, "length", FROZEN);
		siteObj.preventExtensions();
		/* step 12 */
		strings[index] = siteObj;
		return siteObj;
	}

	public static void storeUint32Result(Context cx, long value) {
		if ((value >>> 32) != 0) {
			throw new IllegalArgumentException();
		}
		cx.scratchUint32 = value;
	}

	public static long lastUint32Result(Context cx) {
		long value = cx.scratchUint32;
		if ((value >>> 32) != 0) {
			throw new IllegalStateException();
		}
		return value;
	}

	static String makeUrlForGeneratedScript(boolean isEval, String masterScriptUrl, int masterScriptLine) {
		if (isEval) {
			return masterScriptUrl + '#' + masterScriptLine + "(eval)";
		}
		return masterScriptUrl + '#' + masterScriptLine + "(Function)";
	}

	/**
	 * Not all "NativeSymbol" instances are actually symbols. So account for that here rather than just
	 * by using an "instanceof" check.
	 */
	static boolean isSymbol(Object obj) {
		return (((obj instanceof NativeSymbol) && ((NativeSymbol) obj).isSymbol())) || (obj instanceof SymbolKey);
	}

	public static RuntimeException errorWithClassName(String msg, Object val, Context cx) {
		return Context.reportRuntimeError1(msg, val.getClass().getName(), cx);
	}

	/**
	 * Equivalent to executing "new Error(message, sourceFileName, sourceLineNo)" from JavaScript.
	 *
	 * @param cx      the current context
	 * @param scope   the current scope
	 * @param message the message
	 * @return a JavaScriptException you should throw
	 */
	public static JavaScriptException throwError(Context cx, Scriptable scope, String message) {
		int[] linep = {0};
		String filename = Context.getSourcePositionFromStack(cx, linep);
		final Scriptable error = newBuiltinObject(cx, scope, TopLevel.Builtins.Error, new Object[]{message, filename, linep[0]});
		return new JavaScriptException(cx, error, filename, linep[0]);
	}

	/**
	 * Equivalent to executing "new $constructorName(message, sourceFileName, sourceLineNo)" from JavaScript.
	 *
	 * @param cx      the current context
	 * @param scope   the current scope
	 * @param message the message
	 * @return a JavaScriptException you should throw
	 */
	public static JavaScriptException throwCustomError(Context cx, Scriptable scope, String constructorName, String message) {
		int[] linep = {0};
		String filename = Context.getSourcePositionFromStack(cx, linep);
		final Scriptable error = cx.newObject(scope, constructorName, new Object[]{message, filename, linep[0]});
		return new JavaScriptException(cx, error, filename, linep[0]);
	}

	/**
	 * No instances should be created.
	 */
	protected ScriptRuntime() {
	}


	/**
	 * This is an interface defining a message provider. Create your
	 * own implementation to override the default error message provider.
	 *
	 * @author Mike Harm
	 */
	public interface MessageProvider {

		/**
		 * Returns a textual message identified by the given messageId,
		 * parameterized by the given arguments.
		 *
		 * @param messageId the identifier of the message
		 * @param arguments the arguments to fill into the message
		 */
		String getMessage(String messageId, Object[] arguments);
	}
}

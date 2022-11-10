/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the Number native object.
 * <p>
 * See ECMA 15.7.
 *
 * @author Norris Boyd
 */
final class NativeNumber extends IdScriptableObject {
	/**
	 * @see https://www.ecma-international.org/ecma-262/6.0/#sec-number.max_safe_integer
	 */
	public static final double MAX_SAFE_INTEGER = 9007199254740991.0; // Math.pow(2, 53) - 1

	private static final Object NUMBER_TAG = "Number";

	private static final int MAX_PRECISION = 100;
	private static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;
	private static final int ConstructorId_isFinite = -1;
	private static final int ConstructorId_isNaN = -2;
	private static final int ConstructorId_isInteger = -3;
	private static final int ConstructorId_isSafeInteger = -4;
	private static final int ConstructorId_parseFloat = -5;
	private static final int ConstructorId_parseInt = -6;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toLocaleString = 3;
	private static final int Id_toSource = 4;
	private static final int Id_valueOf = 5;
	private static final int Id_toFixed = 6;
	private static final int Id_toExponential = 7;
	private static final int Id_toPrecision = 8;
	private static final int MAX_PROTOTYPE_ID = 8;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeNumber obj = new NativeNumber(cx, 0.0);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	private static Object execConstructorCall(Context cx, int id, Object[] args) {
		switch (id) {
			case ConstructorId_isFinite:
				if ((args.length == 0) || (Undefined.instance == args[0])) {
					return Boolean.FALSE;
				}
				if (args[0] instanceof Number) {
					// Match ES6 polyfill, which only works for "number" types
					return isFinite(args[0], cx);
				}
				return Boolean.FALSE;

			case ConstructorId_isNaN:
				if ((args.length == 0) || (Undefined.instance == args[0])) {
					return Boolean.FALSE;
				}
				if (args[0] instanceof Number) {
					return isNaN((Number) args[0]);
				}
				return Boolean.FALSE;

			case ConstructorId_isInteger:
				if ((args.length == 0) || (Undefined.instance == args[0])) {
					return Boolean.FALSE;
				}
				if (args[0] instanceof Number) {
					return isInteger((Number) args[0]);
				}
				return Boolean.FALSE;

			case ConstructorId_isSafeInteger:
				if ((args.length == 0) || (Undefined.instance == args[0])) {
					return Boolean.FALSE;
				}
				if (args[0] instanceof Number) {
					return isSafeInteger((Number) args[0]);
				}
				return Boolean.FALSE;

			case ConstructorId_parseFloat:
				return NativeGlobal.js_parseFloat(cx, args);

			case ConstructorId_parseInt:
				return NativeGlobal.js_parseInt(args, cx);

			default:
				throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	private static String num_to(Context cx, double val, Object[] args, int zeroArgMode, int oneArgMode, int precisionMin, int precisionOffset) {
		int precision;
		if (args.length == 0) {
			precision = 0;
			oneArgMode = zeroArgMode;
		} else {
            /* We allow a larger range of precision than
               ECMA requires; this is permitted by ECMA. */
			double p = ScriptRuntime.toInteger(cx, args[0]);
			if (p < precisionMin || p > MAX_PRECISION) {
				String msg = ScriptRuntime.getMessage1("msg.bad.precision", ScriptRuntime.toString(cx, args[0]));
				throw ScriptRuntime.rangeError(cx, msg);
			}
			precision = ScriptRuntime.toInt32(p);
		}
		StringBuilder sb = new StringBuilder();
		DToA.JS_dtostr(sb, oneArgMode, precision + precisionOffset, val);
		return sb.toString();
	}

	static Object isFinite(Object val, Context cx) {
		double d = ScriptRuntime.toNumber(cx, val);
		Double nd = d;
		return !nd.isInfinite() && !nd.isNaN();
	}

	private static Boolean isNaN(Number val) {
		if (val instanceof Double) {
			return ((Double) val).isNaN();
		}

		double d = val.doubleValue();
		return Double.isNaN(d);
	}

	private static boolean isInteger(Number val) {
		if (val instanceof Double) {
			return isDoubleInteger((Double) val);
		}
		return isDoubleInteger(val.doubleValue());
	}

	private static boolean isDoubleInteger(Double d) {
		return !d.isInfinite() && !d.isNaN() && (Math.floor(d) == d);
	}

	private static boolean isDoubleInteger(double d) {
		return !Double.isInfinite(d) && !Double.isNaN(d) && (Math.floor(d) == d);
	}

	private static boolean isSafeInteger(Number val) {
		if (val instanceof Double) {
			return isDoubleSafeInteger((Double) val);
		}
		return isDoubleSafeInteger(val.doubleValue());
	}

	private static boolean isDoubleSafeInteger(Double d) {
		return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
	}

	private static boolean isDoubleSafeInteger(double d) {
		return isDoubleInteger(d) && (d <= MAX_SAFE_INTEGER) && (d >= MIN_SAFE_INTEGER);
	}

	private final Context localContext;
	private final double doubleValue;

	NativeNumber(Context cx, double number) {
		localContext = cx;
		doubleValue = number;
	}

	@Override
	public String getClassName() {
		return "Number";
	}

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor, Context cx) {
		final int attr = DONTENUM | PERMANENT | READONLY;

		ctor.defineProperty("NaN", ScriptRuntime.NaNobj, attr, cx);
		ctor.defineProperty("POSITIVE_INFINITY", ScriptRuntime.wrapNumber(Double.POSITIVE_INFINITY), attr, cx);
		ctor.defineProperty("NEGATIVE_INFINITY", ScriptRuntime.wrapNumber(Double.NEGATIVE_INFINITY), attr, cx);
		ctor.defineProperty("MAX_VALUE", ScriptRuntime.wrapNumber(Double.MAX_VALUE), attr, cx);
		ctor.defineProperty("MIN_VALUE", ScriptRuntime.wrapNumber(Double.MIN_VALUE), attr, cx);
		ctor.defineProperty("MAX_SAFE_INTEGER", ScriptRuntime.wrapNumber(MAX_SAFE_INTEGER), attr, cx);
		ctor.defineProperty("MIN_SAFE_INTEGER", ScriptRuntime.wrapNumber(MIN_SAFE_INTEGER), attr, cx);

		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_isFinite, "isFinite", 1, cx);
		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_isNaN, "isNaN", 1, cx);
		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_isInteger, "isInteger", 1, cx);
		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_isSafeInteger, "isSafeInteger", 1, cx);
		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_parseFloat, "parseFloat", 1, cx);
		addIdFunctionProperty(ctor, NUMBER_TAG, ConstructorId_parseInt, "parseInt", 1, cx);

		super.fillConstructorProperties(ctor, cx);
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		String s;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 1;
				s = "constructor";
			}
			case Id_toString -> {
				arity = 1;
				s = "toString";
			}
			case Id_toLocaleString -> {
				arity = 1;
				s = "toLocaleString";
			}
			case Id_toSource -> {
				arity = 0;
				s = "toSource";
			}
			case Id_valueOf -> {
				arity = 0;
				s = "valueOf";
			}
			case Id_toFixed -> {
				arity = 1;
				s = "toFixed";
			}
			case Id_toExponential -> {
				arity = 1;
				s = "toExponential";
			}
			case Id_toPrecision -> {
				arity = 1;
				s = "toPrecision";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(NUMBER_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(NUMBER_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		if (id == Id_constructor) {
			double val = (args.length >= 1) ? ScriptRuntime.toNumber(cx, args[0]) : 0.0;
			if (thisObj == null) {
				// new Number(val) creates a new Number object.
				return new NativeNumber(cx, val);
			}
			// Number(val) converts val to a number value.
			return ScriptRuntime.wrapNumber(val);

		} else if (id < Id_constructor) {
			return execConstructorCall(cx, id, args);
		}

		// The rest of Number.prototype methods require thisObj to be Number

		if (!(thisObj instanceof NativeNumber)) {
			throw incompatibleCallError(f, cx);
		}
		double value = ((NativeNumber) thisObj).doubleValue;

		switch (id) {

			case Id_toString:
			case Id_toLocaleString: {
				// toLocaleString is just an alias for toString for now
				int base = (args.length == 0 || args[0] == Undefined.instance) ? 10 : ScriptRuntime.toInt32(cx, args[0]);
				return ScriptRuntime.numberToString(cx, value, base);
			}

			case Id_toSource:
				return "not_supported";

			case Id_valueOf:
				return ScriptRuntime.wrapNumber(value);

			case Id_toFixed:
				return num_to(cx, value, args, DToA.DTOSTR_FIXED, DToA.DTOSTR_FIXED, 0, 0);

			case Id_toExponential: {
				// Handle special values before range check
				if (Double.isNaN(value)) {
					return "NaN";
				}
				if (Double.isInfinite(value)) {
					if (value >= 0) {
						return "Infinity";
					}
					return "-Infinity";
				}
				// General case
				return num_to(cx, value, args, DToA.DTOSTR_STANDARD_EXPONENTIAL, DToA.DTOSTR_EXPONENTIAL, 0, 1);
			}

			case Id_toPrecision: {
				// Undefined precision, fall back to ToString()
				if (args.length == 0 || args[0] == Undefined.instance) {
					return ScriptRuntime.numberToString(cx, value, 10);
				}
				// Handle special values before range check
				if (Double.isNaN(value)) {
					return "NaN";
				}
				if (Double.isInfinite(value)) {
					if (value >= 0) {
						return "Infinity";
					}
					return "-Infinity";
				}
				return num_to(cx, value, args, DToA.DTOSTR_STANDARD, DToA.DTOSTR_PRECISION, 1, 0);
			}

			default:
				throw new IllegalArgumentException(String.valueOf(id));
		}
	}

	@Override
	public String toString() {
		return ScriptRuntime.numberToString(localContext, doubleValue, 10);
	}

	@Override
	public MemberType getTypeOf() {
		return MemberType.NUMBER;
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toLocaleString" -> Id_toLocaleString;
			case "toSource" -> Id_toSource;
			case "valueOf" -> Id_valueOf;
			case "toFixed" -> Id_toFixed;
			case "toExponential" -> Id_toExponential;
			case "toPrecision" -> Id_toPrecision;
			default -> super.findPrototypeId(s);
		};
	}
}

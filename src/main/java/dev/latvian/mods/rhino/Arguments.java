/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * This class implements the "arguments" object.
 * <p>
 * See ECMA 10.1.8
 *
 * @author Norris Boyd
 * @see NativeCall
 */
final class Arguments extends IdScriptableObject {
	private static final String FTAG = "Arguments";
	private static final int Id_callee = 1;
	private static final int Id_length = 2;
	private static final int Id_caller = 3;

	// the following helper methods assume that 0 < index < args.length
	private static final int MAX_INSTANCE_ID = Id_caller;
	private static final BaseFunction iteratorMethod = new BaseFunction() {
		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			// TODO : call %ArrayProto_values%
			// 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
			//  1. Perform DefinePropertyOrThrow(obj, @@iterator, PropertyDescriptor {[[Value]]:%ArrayProto_values%,
			//     [[Writable]]: true, [[Enumerable]]: false, [[Configurable]]: true}).
			return new NativeArrayIterator(cx, scope, thisObj, NativeArrayIterator.ArrayIteratorType.VALUES);
		}
	};

	private static class ThrowTypeError extends BaseFunction {
		private final String propertyName;

		ThrowTypeError(String propertyName) {
			this.propertyName = propertyName;
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			throw ScriptRuntime.typeError1(cx, "msg.arguments.not.access.strict", propertyName);
		}
	}

	private final NativeCall activation;

	// end helpers
	// Fields to hold caller, callee and length properties,
	// where NOT_FOUND value tags deleted properties.
	// In addition if callerObj == NULL_VALUE, it tags null for scripts, as
	// initial callerObj == null means access to caller arguments available
	// only in JS <= 1.3 scripts
	private Object callerObj;
	private Object calleeObj;
	private Object lengthObj;
	private int callerAttr = DONTENUM;
	private int calleeAttr = DONTENUM;
	private int lengthAttr = DONTENUM;

	// #string_id_map#
	// Initially args holds activation.getOriginalArgs(), but any modification
	// of its elements triggers creation of a copy. If its element holds NOT_FOUND,
	// it indicates deleted index, in which case super class is queried.
	private Object[] args;

	public Arguments(NativeCall activation, Context cx) {
		this.activation = activation;

		Scriptable parent = activation.getParentScope();
		setParentScope(parent);
		setPrototype(getObjectPrototype(parent, cx));

		args = activation.originalArgs;
		lengthObj = args.length;

		calleeObj = activation.function;
		callerObj = NOT_FOUND;

		defineProperty(cx, SymbolKey.ITERATOR, iteratorMethod, DONTENUM);
	}

	@Override
	public String getClassName() {
		return FTAG;
	}

	private Object arg(int index) {
		if (index < 0 || args.length <= index) {
			return NOT_FOUND;
		}
		return args[index];
	}

	private void putIntoActivation(int index, Object value, Context cx) {
		String argName = activation.function.getParamOrVarName(index);
		activation.put(cx, argName, activation, value);
	}

	private Object getFromActivation(int index, Context cx) {
		String argName = activation.function.getParamOrVarName(index);
		return activation.get(cx, argName, activation);
	}

	// #/string_id_map#

	private void replaceArg(int index, Object value, Context cx) {
		if (sharedWithActivation(index, cx)) {
			putIntoActivation(index, value, cx);
		}
		synchronized (this) {
			if (args == activation.originalArgs) {
				args = args.clone();
			}
			args[index] = value;
		}
	}

	private void removeArg(int index) {
		synchronized (this) {
			if (args[index] != NOT_FOUND) {
				if (args == activation.originalArgs) {
					args = args.clone();
				}
				args[index] = NOT_FOUND;
			}
		}
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (arg(index) != NOT_FOUND) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		final Object value = arg(index);
		if (value == NOT_FOUND) {
			return super.get(cx, index, start);
		}
		if (sharedWithActivation(index, cx)) {
			return getFromActivation(index, cx);
		}
		return value;
	}

	private boolean sharedWithActivation(int index, Context cx) {
		if (cx.isStrictMode()) {
			return false;
		}
		NativeFunction f = activation.function;
		int definedCount = f.getParamCount();
		if (index < definedCount) {
			// Check if argument is not hidden by later argument with the same
			// name as hidden arguments are not shared with activation
			if (index < definedCount - 1) {
				String argName = f.getParamOrVarName(index);
				for (int i = index + 1; i < definedCount; i++) {
					if (argName.equals(f.getParamOrVarName(i))) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (arg(index) == NOT_FOUND) {
			super.put(cx, index, start, value);
		} else {
			replaceArg(index, value, cx);
		}
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		super.put(cx, name, start, value);
	}

	@Override
	public void delete(Context cx, int index) {
		if (0 <= index && index < args.length) {
			removeArg(index);
		}
		super.delete(cx, index);
	}

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected int findInstanceIdInfo(String s, Context cx) {
		int id = switch (s) {
			case "callee" -> Id_callee;
			case "length" -> Id_length;
			case "caller" -> Id_caller;
			default -> 0;
		};

		if (cx.isStrictMode()) {
			if (id == Id_callee || id == Id_caller) {
				return super.findInstanceIdInfo(s, cx);
			}
		}

		if (id == 0) {
			return super.findInstanceIdInfo(s, cx);
		}

		int attr = switch (id) {
			case Id_callee -> calleeAttr;
			case Id_caller -> callerAttr;
			case Id_length -> lengthAttr;
			default -> throw new IllegalStateException();
		};
		return instanceIdInfo(attr, id);
	}

	@Override
	protected String getInstanceIdName(int id) {
		return switch (id) {
			case Id_callee -> "callee";
			case Id_length -> "length";
			case Id_caller -> "caller";
			default -> null;
		};
	}

	@Override
	protected Object getInstanceIdValue(int id, Context cx) {
		switch (id) {
			case Id_callee:
				return calleeObj;
			case Id_length:
				return lengthObj;
			case Id_caller: {
				Object value = callerObj;
				if (value == UniqueTag.NULL_VALUE) {
					value = null;
				} else if (value == null) {
					NativeCall caller = activation.parentActivationCall;
					if (caller != null) {
						value = caller.get(cx, "arguments", caller);
					}
				}
				return value;
			}
		}
		return super.getInstanceIdValue(id, cx);
	}

	@Override
	protected void setInstanceIdValue(int id, Object value, Context cx) {
		switch (id) {
			case Id_callee -> calleeObj = value;
			case Id_length -> lengthObj = value;
			case Id_caller -> callerObj = (value != null) ? value : UniqueTag.NULL_VALUE;
			default -> super.setInstanceIdValue(id, value, cx);
		}

	}

	@Override
	protected void setInstanceIdAttributes(int id, int attr, Context cx) {
		switch (id) {
			case Id_callee -> calleeAttr = attr;
			case Id_length -> lengthAttr = attr;
			case Id_caller -> callerAttr = attr;
			default -> super.setInstanceIdAttributes(id, attr, cx);
		}
	}

	@Override
	Object[] getIds(Context cx, boolean getNonEnumerable, boolean getSymbols) {
		Object[] ids = super.getIds(cx, getNonEnumerable, getSymbols);
		if (args.length != 0) {
			boolean[] present = new boolean[args.length];
			int extraCount = args.length;
			for (int i = 0; i != ids.length; ++i) {
				Object id = ids[i];
				if (id instanceof Integer) {
					int index = (Integer) id;
					if (0 <= index && index < args.length) {
						if (!present[index]) {
							present[index] = true;
							extraCount--;
						}
					}
				}
			}
			if (!getNonEnumerable) { // avoid adding args which were redefined to non-enumerable
				for (int i = 0; i < present.length; i++) {
					if (!present[i] && super.has(cx, i, this)) {
						present[i] = true;
						extraCount--;
					}
				}
			}
			if (extraCount != 0) {
				Object[] tmp = new Object[extraCount + ids.length];
				System.arraycopy(ids, 0, tmp, extraCount, ids.length);
				ids = tmp;
				int offset = 0;
				for (int i = 0; i != args.length; ++i) {
					if (!present[i]) {
						ids[offset] = i;
						++offset;
					}
				}
				if (offset != extraCount) {
					Kit.codeBug();
				}
			}
		}
		return ids;
	}

	@Override
	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		if (ScriptRuntime.isSymbol(id) || id instanceof Scriptable) {
			return super.getOwnPropertyDescriptor(cx, id);
		}

		double d = ScriptRuntime.toNumber(cx, id);
		int index = (int) d;
		if (d != index) {
			return super.getOwnPropertyDescriptor(cx, id);
		}
		Object value = arg(index);
		if (value == NOT_FOUND) {
			return super.getOwnPropertyDescriptor(cx, id);
		}
		if (sharedWithActivation(index, cx)) {
			value = getFromActivation(index, cx);
		}
		if (super.has(cx, index, this)) { // the descriptor has been redefined
			ScriptableObject desc = super.getOwnPropertyDescriptor(cx, id);
			desc.put(cx, "value", desc, value);
			return desc;
		}
		Scriptable scope = getParentScope();
		if (scope == null) {
			scope = this;
		}
		return buildDataDescriptor(scope, value, EMPTY, cx);
	}

	@Override
	protected void defineOwnProperty(Context cx, Object id, ScriptableObject desc, boolean checkValid) {
		super.defineOwnProperty(cx, id, desc, checkValid);
		if (ScriptRuntime.isSymbol(id)) {
			return;
		}

		double d = ScriptRuntime.toNumber(cx, id);
		int index = (int) d;
		if (d != index) {
			return;
		}

		Object value = arg(index);
		if (value == NOT_FOUND) {
			return;
		}

		if (isAccessorDescriptor(cx, desc)) {
			removeArg(index);
			return;
		}

		Object newValue = getProperty(desc, "value", cx);
		if (newValue == NOT_FOUND) {
			return;
		}

		replaceArg(index, newValue, cx);

		if (isFalse(getProperty(desc, "writable", cx), cx)) {
			removeArg(index);
		}
	}

	// ECMAScript2015
	// 9.4.4.6 CreateUnmappedArgumentsObject(argumentsList)
	//   8. Perform DefinePropertyOrThrow(obj, "caller", PropertyDescriptor {[[Get]]: %ThrowTypeError%,
	//      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
	//   9. Perform DefinePropertyOrThrow(obj, "callee", PropertyDescriptor {[[Get]]: %ThrowTypeError%,
	//      [[Set]]: %ThrowTypeError%, [[Enumerable]]: false, [[Configurable]]: false}).
	void defineAttributesForStrictMode(Context cx) {
		if (!cx.isStrictMode()) {
			return;
		}
		setGetterOrSetter(cx, "caller", 0, new ThrowTypeError("caller"), true);
		setGetterOrSetter(cx, "caller", 0, new ThrowTypeError("caller"), false);
		setGetterOrSetter(cx, "callee", 0, new ThrowTypeError("callee"), true);
		setGetterOrSetter(cx, "callee", 0, new ThrowTypeError("callee"), false);
		setAttributes(cx, "caller", DONTENUM | PERMANENT);
		setAttributes(cx, "callee", DONTENUM | PERMANENT);
		callerObj = null;
		calleeObj = null;
	}
}

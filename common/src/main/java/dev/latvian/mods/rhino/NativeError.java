/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * The class of error objects
 * <p>
 * ECMA 15.11
 */
final class NativeError extends IdScriptableObject {
	/**
	 * Default stack limit is set to "Infinity", here represented as a negative int
	 */
	public static final int DEFAULT_STACK_LIMIT = -1;
	private static final Object ERROR_TAG = "Error";
	private static final WrappedExecutable ERROR_DELEGATE_GET_STACK = (cx, scope, self, args) -> ((NativeError) self).getStackDelegated(cx, scope);
	private static final WrappedExecutable ERROR_DELEGATE_SET_STACK = (cx, scope, self, args) -> ((NativeError) self).setStackDelegated(cx, scope, args[0]);
	// This is used by "captureStackTrace"
	private static final String STACK_HIDE_KEY = "_stackHide";
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;
	private static final int ConstructorId_captureStackTrace = -1;
	private static final int MAX_PROTOTYPE_ID = 3;

	/**
	 * We will attch this object to the constructor and use it solely to store the constructor properties
	 * that are "global." We can't make them static because there can be many contexts in the same JVM.
	 */
	private static final class ProtoProps {
		static final String KEY = "_ErrorPrototypeProps";

		static final WrappedExecutable GET_STACK_LIMIT = (cx, scope, self, args) -> ((ProtoProps) self).getStackTraceLimit(cx);
		static final WrappedExecutable SET_STACK_LIMIT = (cx, scope, self, args) -> ((ProtoProps) self).setStackTraceLimit(cx, args[0]);
		static final WrappedExecutable GET_PREPARE_STACK = (cx, scope, self, args) -> ((ProtoProps) self).getPrepareStackTrace(cx);
		static final WrappedExecutable SET_PREPARE_STACK = (cx, scope, self, args) -> ((ProtoProps) self).setPrepareStackTrace(cx, args[0]);

		private int stackTraceLimit = DEFAULT_STACK_LIMIT;
		private Function prepareStackTrace;

		public Object getStackTraceLimit(Context cx) {
			if (stackTraceLimit >= 0) {
				return stackTraceLimit;
			}
			return Double.POSITIVE_INFINITY;
		}

		public int getStackTraceLimit() {
			return stackTraceLimit;
		}

		public Object setStackTraceLimit(Context cx, Object value) {
			double limit = ScriptRuntime.toNumber(cx, value);
			if (Double.isNaN(limit) || Double.isInfinite(limit)) {
				stackTraceLimit = -1;
			} else {
				stackTraceLimit = (int) limit;
			}

			return null;
		}

		public Object getPrepareStackTrace(Context cx) {
			Object ps = getPrepareStackTrace();
			return (ps == null ? Undefined.instance : ps);
		}

		public Function getPrepareStackTrace() {
			return prepareStackTrace;
		}

		public Object setPrepareStackTrace(Context cx, Object value) {
			if ((value == null) || Undefined.instance.equals(value)) {
				prepareStackTrace = null;
			} else if (value instanceof Function) {
				prepareStackTrace = (Function) value;
			}

			return null;
		}
	}

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeError obj = new NativeError(cx);
		putProperty(obj, "name", "Error", cx);
		putProperty(obj, "message", "", cx);
		putProperty(obj, "fileName", "", cx);
		putProperty(obj, "lineNumber", 0, cx);
		obj.setAttributes(cx, "name", DONTENUM);
		obj.setAttributes(cx, "message", DONTENUM);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
		NativeCallSite.init(obj, sealed, cx);
	}

	static NativeError make(Context cx, Scriptable scope, IdFunctionObject ctorObj, Object[] args) {
		Scriptable proto = (Scriptable) (ctorObj.get(cx, "prototype", ctorObj));

		NativeError obj = new NativeError(cx);
		obj.setPrototype(proto);
		obj.setParentScope(scope);

		int arglen = args.length;
		if (arglen >= 1) {
			if (args[0] != Undefined.instance) {
				putProperty(obj, "message", ScriptRuntime.toString(cx, args[0]), cx);
			}
			if (arglen >= 2) {
				putProperty(obj, "fileName", args[1], cx);
				if (arglen >= 3) {
					int line = ScriptRuntime.toInt32(cx, args[2]);
					putProperty(obj, "lineNumber", line, cx);
				}
			}
		}
		return obj;
	}

	private static Object js_toString(Context cx, Scriptable thisObj) {
		Object name = getProperty(thisObj, "name", cx);
		if (name == NOT_FOUND || name == Undefined.instance) {
			name = "Error";
		} else {
			name = ScriptRuntime.toString(cx, name);
		}
		Object msg = getProperty(thisObj, "message", cx);
		if (msg == NOT_FOUND || msg == Undefined.instance) {
			msg = "";
		} else {
			msg = ScriptRuntime.toString(cx, msg);
		}
		if (name.toString().length() == 0) {
			return msg;
		} else if (msg.toString().length() == 0) {
			return name;
		} else {
			return name + ": " + msg;
		}
	}

	private static void js_captureStackTrace(Context cx, Scriptable thisObj, Object[] args) {
		ScriptableObject obj = (ScriptableObject) ScriptRuntime.toObjectOrNull(cx, args[0], thisObj);
		Function func = null;
		if (args.length > 1) {
			func = (Function) ScriptRuntime.toObjectOrNull(cx, args[1], thisObj);
		}

		// Create a new error that will have the correct prototype so we can re-use "getStackTrace"
		NativeError err = (NativeError) cx.newObject(thisObj, "Error");
		// Wire it up so that it will have an actual exception with a stack trace
		err.setStackProvider(new EvaluatorException(cx, "[object Object]"), cx);

		// Figure out if they passed a function used to hide part of the stack
		if (func != null) {
			Object funcName = func.get(cx, "name", func);
			if ((funcName != null) && !Undefined.instance.equals(funcName)) {
				err.associateValue(STACK_HIDE_KEY, ScriptRuntime.toString(cx, funcName));
			}
		}

		// Define a property on the specified object to get that stack
		// that delegates to our new error. Build the stack trace lazily
		// using the "getStack" code from NativeError.
		obj.defineProperty(cx, "stack", err, ERROR_DELEGATE_GET_STACK, ERROR_DELEGATE_SET_STACK, 0);
	}

	private RhinoException stackProvider;
	private final Context localContext;

	public NativeError(Context cx) {
		localContext = cx;
	}

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor, Context cx) {
		addIdFunctionProperty(ctor, ERROR_TAG, ConstructorId_captureStackTrace, "captureStackTrace", 2, cx);

		// This is running on the global "Error" object. Associate an object there that can store
		// default stack trace, etc.
		// This prevents us from having to add two additional fields to every Error object.
		ProtoProps protoProps = new ProtoProps();
		associateValue(ProtoProps.KEY, protoProps);

		// Define constructor properties that delegate to the ProtoProps object.
		ctor.defineProperty(cx, "stackTraceLimit", protoProps, ProtoProps.GET_STACK_LIMIT, ProtoProps.SET_STACK_LIMIT, 0);
		ctor.defineProperty(cx, "prepareStackTrace", protoProps, ProtoProps.GET_PREPARE_STACK, ProtoProps.SET_PREPARE_STACK, 0);

		super.fillConstructorProperties(ctor, cx);
	}

	@Override
	public String getClassName() {
		return "Error";
	}

	@Override
	public String toString() {
		// According to spec, Error.prototype.toString() may return undefined.
		Object toString = js_toString(localContext, this);
		return toString instanceof String ? (String) toString : super.toString();
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
				arity = 0;
				s = "toString";
			}
			case Id_toSource -> {
				arity = 0;
				s = "toSource";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(ERROR_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(ERROR_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor:
				return make(cx, scope, f, args);

			case Id_toString:
				return js_toString(cx, thisObj);

			case Id_toSource:
				return "not_supported";

			case ConstructorId_captureStackTrace:
				js_captureStackTrace(cx, thisObj, args);
				return Undefined.instance;
		}
		throw new IllegalArgumentException(String.valueOf(id));
	}

	public void setStackProvider(RhinoException re, Context cx) {
		// We go some extra miles to make sure the stack property is only
		// generated on demand, is cached after the first access, and is
		// overwritable like an ordinary property. Hence this setup with
		// the getter and setter below.
		if (stackProvider == null) {
			stackProvider = re;
			defineProperty(cx, "stack", this, ERROR_DELEGATE_GET_STACK, ERROR_DELEGATE_SET_STACK, DONTENUM);
		}
	}

	public Object getStackDelegated(Context cx, Scriptable target) {
		if (stackProvider == null) {
			return NOT_FOUND;
		}

		// Get the object where prototype stuff is stored.
		int limit = DEFAULT_STACK_LIMIT;
		Function prepare = null;
		NativeError cons = (NativeError) getPrototype(cx);
		ProtoProps pp = (ProtoProps) cons.getAssociatedValue(ProtoProps.KEY);

		if (pp != null) {
			limit = pp.getStackTraceLimit();
			prepare = pp.getPrepareStackTrace();
		}

		// This key is only set by captureStackTrace
		String hideFunc = (String) getAssociatedValue(STACK_HIDE_KEY);
		ScriptStackElement[] stack = stackProvider.getScriptStack(limit, hideFunc);

		// Determine whether to format the stack trace ourselves, or call the user's code to do it
		Object value;
		if (prepare == null) {
			value = RhinoException.formatStackTrace(stack, stackProvider.details());
		} else {
			value = callPrepareStack(prepare, stack, cx);
		}

		// We store the stack as local property both to cache it
		// and to make the property writable
		setStackDelegated(cx, target, value);
		return value;
	}

	public Object setStackDelegated(Context cx, Scriptable target, Object value) {
		target.delete(cx, "stack");
		stackProvider = null;
		target.put(cx, "stack", target, value);
		return null;
	}

	private Object callPrepareStack(Function prepare, ScriptStackElement[] stack, Context cx) {
		Object[] elts = new Object[stack.length];

		// The "prepareStackTrace" function takes an array of CallSite objects.
		for (int i = 0; i < stack.length; i++) {
			NativeCallSite site = (NativeCallSite) cx.newObject(this, "CallSite");
			site.setElement(stack[i]);
			elts[i] = site;
		}

		Scriptable eltArray = cx.newArray(this, elts);
		return prepare.call(cx, prepare, this, new Object[]{this, eltArray});
	}

	// #/string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toSource" -> Id_toSource;
			default -> super.findPrototypeId(s);
		};
	}
}

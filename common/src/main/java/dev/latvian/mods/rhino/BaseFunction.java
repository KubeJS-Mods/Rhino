/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * The base class for Function objects. That is one of two purposes. It is also
 * the prototype for every "function" defined except those that are used
 * as GeneratorFunctions via the ES6 "function *" syntax.
 * <p>
 * See ECMA 15.3.
 *
 * @author Norris Boyd
 */
public class BaseFunction extends IdScriptableObject implements Function {
	static final String GENERATOR_FUNCTION_CLASS = "__GeneratorFunction";
	private static final Object FUNCTION_TAG = "Function";
	private static final String FUNCTION_CLASS = "Function";
	private static final int Id_length = 1;
	private static final int Id_arity = 2;
	private static final int Id_name = 3;
	private static final int Id_prototype = 4;
	private static final int Id_arguments = 5;
	private static final int MAX_INSTANCE_ID = 5;
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_toSource = 3;

	// #string_id_map#
	private static final int Id_apply = 4;
	private static final int Id_call = 5;
	private static final int Id_bind = 6;
	private static final int MAX_PROTOTYPE_ID = Id_bind;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		BaseFunction obj = new BaseFunction();
		// Function.prototype attributes: see ECMA 15.3.3.1
		obj.prototypePropertyAttributes = DONTENUM | READONLY | PERMANENT;
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	static Object initAsGeneratorFunction(Scriptable scope, boolean sealed, Context cx) {
		BaseFunction obj = new BaseFunction(true);
		// Function.prototype attributes: see ECMA 15.3.3.1
		obj.prototypePropertyAttributes = READONLY | PERMANENT;
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
		// The "GeneratorFunction" name actually never appears in the global scope.
		// Return it here so it can be cached as a "builtin"
		return getProperty(scope, GENERATOR_FUNCTION_CLASS, cx);
	}

	static boolean isApply(IdFunctionObject f) {
		return f.hasTag(FUNCTION_TAG) && f.methodId() == Id_apply;
	}

	static boolean isApplyOrCall(IdFunctionObject f) {
		if (f.hasTag(FUNCTION_TAG)) {
			switch (f.methodId()) {
				case Id_apply:
				case Id_call:
					return true;
			}
		}
		return false;
	}

	private Object prototypeProperty;
	private Object argumentsObj = NOT_FOUND;
	private boolean isGeneratorFunction = false;
	// For function object instances, attributes are
	//  {configurable:false, enumerable:false};
	// see ECMA 15.3.5.2
	private int prototypePropertyAttributes = PERMANENT | DONTENUM;
	private int argumentsAttributes = PERMANENT | DONTENUM;

	public BaseFunction() {
	}

	public BaseFunction(boolean isGenerator) {
		this.isGeneratorFunction = isGenerator;
	}

	public BaseFunction(Scriptable scope, Scriptable prototype) {
		super(scope, prototype);
	}

	@Override
	public String getClassName() {
		return isGeneratorFunction() ? GENERATOR_FUNCTION_CLASS : FUNCTION_CLASS;
	}

	// Generated code will override this
	protected boolean isGeneratorFunction() {
		return isGeneratorFunction;
	}

	/**
	 * Gets the value returned by calling the typeof operator on this object.
	 *
	 * @return "function" or "undefined" if {@link #avoidObjectDetection()} returns <code>true</code>
	 * @see ScriptableObject#getTypeOf()
	 */
	@Override
	public MemberType getTypeOf() {
		return avoidObjectDetection() ? MemberType.UNDEFINED : MemberType.FUNCTION;
	}

	/**
	 * Implements the instanceof operator for JavaScript Function objects.
	 * <p>
	 * <code>
	 * foo = new Foo();<br>
	 * foo instanceof Foo;  // true<br>
	 * </code>
	 *
	 * @param instance The value that appeared on the LHS of the instanceof
	 *                 operator
	 * @param cx
	 * @return true if the "prototype" property of "this" appears in
	 * value's prototype chain
	 */
	@Override
	public boolean hasInstance(Scriptable instance, Context cx) {
		Object protoProp = getProperty(this, "prototype", cx);
		if (protoProp instanceof Scriptable) {
			return ScriptRuntime.jsDelegatesTo(cx, instance, (Scriptable) protoProp);
		}
		throw ScriptRuntime.typeError1(cx, "msg.instanceof.bad.prototype", getFunctionName());
	}

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected int findInstanceIdInfo(String s, Context cx) {
		int id = switch (s) {
			case "name" -> Id_name;
			case "length" -> Id_length;
			case "arity" -> Id_arity;
			case "prototype" -> Id_prototype;
			case "arguments" -> Id_arguments;
			default -> 0;
		};

		if (id == 0) {
			return super.findInstanceIdInfo(s, cx);
		}

		int attr;
		switch (id) {
			case Id_length, Id_arity, Id_name -> attr = DONTENUM | READONLY | PERMANENT;
			case Id_prototype -> {
				// some functions such as built-ins don't have a prototype property
				if (!hasPrototypeProperty()) {
					return 0;
				}
				attr = prototypePropertyAttributes;
			}
			case Id_arguments -> attr = argumentsAttributes;
			default -> throw new IllegalStateException();
		}
		return instanceIdInfo(attr, id);
	}

	@Override
	protected String getInstanceIdName(int id) {
		return switch (id) {
			case Id_length -> "length";
			case Id_arity -> "arity";
			case Id_name -> "name";
			case Id_prototype -> "prototype";
			case Id_arguments -> "arguments";
			default -> super.getInstanceIdName(id);
		};
	}

	@Override
	protected Object getInstanceIdValue(int id, Context cx) {
		return switch (id) {
			case Id_length -> getLength();
			case Id_arity -> getArity();
			case Id_name -> getFunctionName();
			case Id_prototype -> getPrototypeProperty(cx);
			case Id_arguments -> getArguments(cx);
			default -> super.getInstanceIdValue(id, cx);
		};
	}

	@Override
	protected void setInstanceIdValue(int id, Object value, Context cx) {
		switch (id) {
			case Id_prototype:
				if ((prototypePropertyAttributes & READONLY) == 0) {
					prototypeProperty = (value != null) ? value : UniqueTag.NULL_VALUE;
				}
				return;
			case Id_arguments:
				if (value == NOT_FOUND) {
					// This should not be called since "arguments" is PERMANENT
					Kit.codeBug();
				}
				if (defaultHas(cx, "arguments")) {
					defaultPut(cx, "arguments", value);
				} else if ((argumentsAttributes & READONLY) == 0) {
					argumentsObj = value;
				}
				return;
			case Id_name:
			case Id_arity:
			case Id_length:
				return;
		}
		super.setInstanceIdValue(id, value, cx);
	}

	@Override
	protected void setInstanceIdAttributes(int id, int attr, Context cx) {
		switch (id) {
			case Id_prototype -> {
				prototypePropertyAttributes = attr;
				return;
			}
			case Id_arguments -> {
				argumentsAttributes = attr;
				return;
			}
		}
		super.setInstanceIdAttributes(id, attr, cx);
	}

	@Override
	protected void fillConstructorProperties(IdFunctionObject ctor, Context cx) {
		// Fix up bootstrapping problem: getPrototype of the IdFunctionObject
		// can not return Function.prototype because Function object is not
		// yet defined.
		ctor.setPrototype(this);
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
				arity = 0;
				s = "toString";
			}
			case Id_toSource -> {
				arity = 1;
				s = "toSource";
			}
			case Id_apply -> {
				arity = 2;
				s = "apply";
			}
			case Id_call -> {
				arity = 1;
				s = "call";
			}
			case Id_bind -> {
				arity = 1;
				s = "bind";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(FUNCTION_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(FUNCTION_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor:
				return jsConstructor(cx, scope, args);

			case Id_toString:
			case Id_toSource:
				return toFunctionString(thisObj);

			case Id_apply:
			case Id_call:
				return ScriptRuntime.applyOrCall(cx, scope, id == Id_apply, thisObj, args);

			case Id_bind:
				if (!(thisObj instanceof Callable targetFunction)) {
					throw ScriptRuntime.notFunctionError(cx, thisObj);
				}
				int argc = args.length;
				final Scriptable boundThis;
				final Object[] boundArgs;
				if (argc > 0) {
					boundThis = ScriptRuntime.toObjectOrNull(cx, args[0], scope);
					boundArgs = new Object[argc - 1];
					System.arraycopy(args, 1, boundArgs, 0, argc - 1);
				} else {
					boundThis = null;
					boundArgs = ScriptRuntime.EMPTY_OBJECTS;
				}
				return new BoundFunction(cx, scope, targetFunction, boundThis, boundArgs);
		}
		throw new IllegalArgumentException(String.valueOf(id));
	}

	/**
	 * Make value as DontEnum, DontDelete, ReadOnly
	 * prototype property of this Function object
	 */
	public void setImmunePrototypeProperty(Object value) {
		if ((prototypePropertyAttributes & READONLY) != 0) {
			throw new IllegalStateException();
		}
		prototypeProperty = (value != null) ? value : UniqueTag.NULL_VALUE;
		prototypePropertyAttributes = DONTENUM | PERMANENT | READONLY;
	}

	protected Scriptable getClassPrototype(Context cx) {
		Object protoVal = getPrototypeProperty(cx);
		if (protoVal instanceof Scriptable) {
			return (Scriptable) protoVal;
		}
		return getObjectPrototype(this, cx);
	}

	/**
	 * Should be overridden.
	 */
	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return Undefined.instance;
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Scriptable result = createObject(cx, scope);
		if (result != null) {
			Object val = call(cx, scope, result, args);
			if (val instanceof Scriptable) {
				result = (Scriptable) val;
			}
		} else {
			Object val = call(cx, scope, null, args);
			if (!(val instanceof Scriptable)) {
				// It is program error not to return Scriptable from
				// the call method if createObject returns null.
				throw new IllegalStateException("Bad implementaion of call as constructor, name=" + getFunctionName() + " in " + getClass().getName());
			}
			result = (Scriptable) val;
			if (result.getPrototype(cx) == null) {
				Scriptable proto = getClassPrototype(cx);
				if (result != proto) {
					result.setPrototype(proto);
				}
			}
			if (result.getParentScope() == null) {
				Scriptable parent = getParentScope();
				if (result != parent) {
					result.setParentScope(parent);
				}
			}
		}
		return result;
	}

	/**
	 * Creates new script object.
	 * The default implementation of {@link #construct} uses the method to
	 * to get the value for <code>thisObj</code> argument when invoking
	 * {@link #call}.
	 * The methos is allowed to return <code>null</code> to indicate that
	 * {@link #call} will create a new object itself. In this case
	 * {@link #construct} will set scope and prototype on the result
	 * {@link #call} unless they are already set.
	 */
	public Scriptable createObject(Context cx, Scriptable scope) {
		Scriptable newInstance = new NativeObject(cx);
		newInstance.setPrototype(getClassPrototype(cx));
		newInstance.setParentScope(getParentScope());
		return newInstance;
	}

	public int getArity() {
		return 0;
	}

	public int getLength() {
		return 0;
	}

	public String getFunctionName() {
		return "";
	}

	protected String toFunctionString(Scriptable parent) {
		String s = getFunctionName();

		if (s.isEmpty()) {
			return parent != null ? parent.getClassName() : "Unknown";
		}

		return s;
	}

	@Override
	public String toString() {
		String s = getFunctionName();
		return s.isEmpty() ? "Unknown" : s;
	}

	protected boolean hasPrototypeProperty() {
		return prototypeProperty != null || this instanceof NativeFunction;
	}

	// #/string_id_map#

	protected Object getPrototypeProperty(Context cx) {
		Object result = prototypeProperty;
		if (result == null) {
			// only create default prototype on native JavaScript functions,
			// not on built-in functions, java methods, host objects etc.
			if (this instanceof NativeFunction) {
				result = setupDefaultPrototype(cx);
			} else {
				result = Undefined.instance;
			}
		} else if (result == UniqueTag.NULL_VALUE) {
			result = null;
		}
		return result;
	}

	private synchronized Object setupDefaultPrototype(Context cx) {
		if (prototypeProperty != null) {
			return prototypeProperty;
		}
		NativeObject obj = new NativeObject(cx);
		final int attr = DONTENUM;
		obj.defineProperty("constructor", this, attr, cx);
		// put the prototype property into the object now, then in the
		// wacky case of a user defining a function Object(), we don't
		// get an infinite loop trying to find the prototype.
		prototypeProperty = obj;
		Scriptable proto = getObjectPrototype(this, cx);
		if (proto != obj) {
			// not the one we just made, it must remain grounded
			obj.setPrototype(proto);
		}
		return obj;
	}

	private Object getArguments(Context cx) {
		// <Function name>.arguments is deprecated, so we use a slow
		// way of getting it that doesn't add to the invocation cost.
		// TODO: add warning, error based on version
		Object value = defaultHas(cx, "arguments") ? defaultGet(cx, "arguments") : argumentsObj;
		if (value != NOT_FOUND) {
			// Should after changing <Function name>.arguments its
			// activation still be available during Function call?
			// This code assumes it should not:
			// defaultGet("arguments") != NOT_FOUND
			// means assigned arguments
			return value;
		}
		NativeCall activation = ScriptRuntime.findFunctionActivation(cx, this);
		return (activation == null) ? null : activation.get("arguments", activation, cx);
	}

	private Object jsConstructor(Context cx, Scriptable scope, Object[] args) {
		int arglen = args.length;
		StringBuilder sourceBuf = new StringBuilder();

		sourceBuf.append("function ");
		if (isGeneratorFunction()) {
			sourceBuf.append("* ");
		}
		sourceBuf.append("anonymous");
		sourceBuf.append('(');

		// Append arguments as coma separated strings
		for (int i = 0; i < arglen - 1; i++) {
			if (i > 0) {
				sourceBuf.append(',');
			}
			sourceBuf.append(ScriptRuntime.toString(cx, args[i]));
		}
		sourceBuf.append(") {");
		if (arglen != 0) {
			// append function body
			String funBody = ScriptRuntime.toString(cx, args[arglen - 1]);
			sourceBuf.append(funBody);
		}
		sourceBuf.append("\n}");
		String source = sourceBuf.toString();

		int[] linep = new int[1];
		String filename = Context.getSourcePositionFromStack(cx, linep);
		if (filename == null) {
			filename = "<eval'ed string>";
			linep[0] = 1;
		}

		String sourceURI = ScriptRuntime.makeUrlForGeneratedScript(false, filename, linep[0]);

		Scriptable global = getTopLevelScope(scope);

		ErrorReporter reporter;
		reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());

		Evaluator evaluator = Context.createInterpreter();
		if (evaluator == null) {
			throw new JavaScriptException(cx, "Interpreter not present", filename, linep[0]);
		}

		// Compile with explicit interpreter instance to force interpreter
		// mode.
		return cx.compileFunction(global, source, evaluator, reporter, sourceURI, 1, null);
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "toSource" -> Id_toSource;
			case "apply" -> Id_apply;
			case "call" -> Id_call;
			case "bind" -> Id_bind;
			default -> super.findPrototypeId(s);
		};
	}
}


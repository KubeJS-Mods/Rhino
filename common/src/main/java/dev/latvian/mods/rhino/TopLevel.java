/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.EnumMap;

/**
 * A top-level scope object that provides special means to cache and preserve
 * the initial values of the built-in constructor properties for better
 * ECMAScript compliance.
 *
 * <p>ECMA 262 requires that most constructors used internally construct
 * objects with the original prototype object as value of their [[Prototype]]
 * internal property. Since built-in global constructors are defined as
 * writable and deletable, this means they should be cached to protect against
 * redefinition at runtime.</p>
 *
 * <p>In order to implement this efficiently, this class provides a mechanism
 * to access the original built-in global constructors and their prototypes
 * via numeric class-ids. To make use of this, the new
 * {@link ScriptRuntime#newBuiltinObject ScriptRuntime.newBuiltinObject} and
 * {@link ScriptableObject#getClassPrototype ScriptRuntime.setBuiltinProtoAndParent}
 * methods should be used to create and initialize objects of built-in classes
 * instead of their generic counterparts.</p>
 *
 * <p>Calling {@link Context#initStandardObjects()}
 * with an instance of this class as argument will automatically cache
 * built-in classes after initialization. For other setups involving
 * top-level scopes that inherit global properties from their proptotypes
 * (e.g. with dynamic scopes) embeddings should explicitly call
 * {@link #cacheBuiltins()} to initialize the class cache for each top-level
 * scope.</p>
 */
public class TopLevel extends IdScriptableObject {
	/**
	 * Static helper method to get a built-in object constructor with the given
	 * <code>type</code> from the given <code>scope</code>. If the scope is not
	 * an instance of this class or does have a cache of built-ins,
	 * the constructor is looked up via normal property lookup.
	 *
	 * @param cx    the current Context
	 * @param scope the top-level scope
	 * @param type  the built-in type
	 * @return the built-in constructor
	 */
	public static Function getBuiltinCtor(Context cx, Scriptable scope, Builtins type) {
		// must be called with top level scope
		assert scope.getParentScope() == null;
		if (scope instanceof TopLevel) {
			Function result = ((TopLevel) scope).getBuiltinCtor(type);
			if (result != null) {
				return result;
			}
		}
		// fall back to normal constructor lookup
		String typeName;
		if (type == Builtins.GeneratorFunction) {
			// GeneratorFunction isn't stored in scope with that name, but in case
			// we end up falling back to this value then we have to
			// look this up using a hidden name.
			typeName = BaseFunction.GENERATOR_FUNCTION_CLASS;
		} else {
			typeName = type.name();
		}
		return ScriptRuntime.getExistingCtor(cx, scope, typeName);
	}

	/**
	 * Static helper method to get a native error constructor with the given
	 * <code>type</code> from the given <code>scope</code>. If the scope is not
	 * an instance of this class or does have a cache of native errors,
	 * the constructor is looked up via normal property lookup.
	 *
	 * @param cx    the current Context
	 * @param scope the top-level scope
	 * @param type  the native error type
	 * @return the native error constructor
	 */
	static Function getNativeErrorCtor(Context cx, Scriptable scope, NativeErrors type) {
		// must be called with top level scope
		assert scope.getParentScope() == null;
		if (scope instanceof TopLevel) {
			Function result = ((TopLevel) scope).getNativeErrorCtor(type);
			if (result != null) {
				return result;
			}
		}
		// fall back to normal constructor lookup
		return ScriptRuntime.getExistingCtor(cx, scope, type.name());
	}

	/**
	 * Static helper method to get a built-in object prototype with the given
	 * <code>type</code> from the given <code>scope</code>. If the scope is not
	 * an instance of this class or does have a cache of built-ins,
	 * the prototype is looked up via normal property lookup.
	 *
	 * @param scope the top-level scope
	 * @param type  the built-in type
	 * @return the built-in prototype
	 */
	public static Scriptable getBuiltinPrototype(Scriptable scope, Builtins type, Context cx) {
		// must be called with top level scope
		assert scope.getParentScope() == null;
		if (scope instanceof TopLevel) {
			Scriptable result = ((TopLevel) scope).getBuiltinPrototype(cx, type);
			if (result != null) {
				return result;
			}
		}
		// fall back to normal prototype lookup
		String typeName;
		if (type == Builtins.GeneratorFunction) {
			// GeneratorFunction isn't stored in scope with that name, but in case
			// we end up falling back to this value then we have to
			// look this up using a hidden name.
			typeName = BaseFunction.GENERATOR_FUNCTION_CLASS;
		} else {
			typeName = type.name();
		}
		return getClassPrototype(scope, typeName, cx);
	}

	private EnumMap<Builtins, BaseFunction> ctors;
	private EnumMap<NativeErrors, BaseFunction> errors;

	public TopLevel() {
	}

	@Override
	public String getClassName() {
		return "global";
	}

	/**
	 * Cache the built-in ECMAScript objects to protect them against
	 * modifications by the script. This method is called automatically by
	 * {@link ScriptRuntime#initStandardObjects ScriptRuntime.initStandardObjects}
	 * if the scope argument is an instance of this class. It only has to be
	 * called by the embedding if a top-level scope is not initialized through
	 * <code>initStandardObjects()</code>.
	 */
	public void cacheBuiltins(Scriptable scope, boolean sealed, Context cx) {
		ctors = new EnumMap<>(Builtins.class);
		for (Builtins builtin : Builtins.values()) {
			Object value = getProperty(this, builtin.name(), cx);
			if (value instanceof BaseFunction) {
				ctors.put(builtin, (BaseFunction) value);
			} else if (builtin == Builtins.GeneratorFunction) {
				// Handle weird situation of "GeneratorFunction" being a real constructor
				// which is never registered in the top-level scope
				ctors.put(builtin, (BaseFunction) BaseFunction.initAsGeneratorFunction(scope, sealed, cx));
			}
		}
		errors = new EnumMap<>(NativeErrors.class);
		for (NativeErrors error : NativeErrors.values()) {
			Object value = getProperty(this, error.name(), cx);
			if (value instanceof BaseFunction) {
				errors.put(error, (BaseFunction) value);
			}
		}
	}

	/**
	 * Get the cached built-in object constructor from this scope with the
	 * given <code>type</code>. Returns null if {@link #cacheBuiltins()} has not
	 * been called on this object.
	 *
	 * @param type the built-in type
	 * @return the built-in constructor
	 */
	public BaseFunction getBuiltinCtor(Builtins type) {
		return ctors != null ? ctors.get(type) : null;
	}

	/**
	 * Get the cached native error constructor from this scope with the
	 * given <code>type</code>. Returns null if {@link #cacheBuiltins()} has not
	 * been called on this object.
	 *
	 * @param type the native error type
	 * @return the native error constructor
	 */
	BaseFunction getNativeErrorCtor(NativeErrors type) {
		return errors != null ? errors.get(type) : null;
	}

	/**
	 * Get the cached built-in object prototype from this scope with the
	 * given <code>type</code>. Returns null if {@link #cacheBuiltins()} has not
	 * been called on this object.
	 *
	 * @param type the built-in type
	 * @return the built-in prototype
	 */
	public Scriptable getBuiltinPrototype(Context cx, Builtins type) {
		BaseFunction func = getBuiltinCtor(type);
		Object proto = func != null ? func.getPrototypeProperty(cx) : null;
		return proto instanceof Scriptable ? (Scriptable) proto : null;
	}

	/**
	 * An enumeration of built-in ECMAScript objects.
	 */
	public enum Builtins {
		/**
		 * The built-in Object type.
		 */
		Object,
		/**
		 * The built-in Array type.
		 */
		Array,
		/**
		 * The built-in Function type.
		 */
		Function,
		/**
		 * The built-in String type.
		 */
		String,
		/**
		 * The built-in Number type.
		 */
		Number,
		/**
		 * The built-in Boolean type.
		 */
		Boolean,
		/**
		 * The built-in RegExp type.
		 */
		RegExp,
		/**
		 * The built-in Error type.
		 */
		Error,
		/**
		 * The built-in Symbol type.
		 */
		Symbol,
		/**
		 * The built-in GeneratorFunction type.
		 */
		GeneratorFunction
	}

	/**
	 * An enumeration of built-in native errors. [ECMAScript 5 - 15.11.6]
	 */
	enum NativeErrors {
		/**
		 * Basic Error
		 */
		Error,
		/**
		 * The native EvalError.
		 */
		EvalError,
		/**
		 * The native RangeError.
		 */
		RangeError,
		/**
		 * The native ReferenceError.
		 */
		ReferenceError,
		/**
		 * The native SyntaxError.
		 */
		SyntaxError,
		/**
		 * The native TypeError.
		 */
		TypeError,
		/**
		 * The native URIError.
		 */
		URIError,
		/**
		 * The native InternalError (non-standard).
		 */
		InternalError,
		/**
		 * The native JavaException (non-standard).
		 */
		JavaException
	}

}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.AstRoot;
import dev.latvian.mods.rhino.ast.ScriptNode;
import dev.latvian.mods.rhino.classfile.ClassFileWriter.ClassFileFormatException;
import dev.latvian.mods.rhino.regexp.RegExp;
import dev.latvian.mods.rhino.type.ArrayTypeInfo;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.ArrayValueProvider;
import dev.latvian.mods.rhino.util.ClassVisibilityContext;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.JavaSetWrapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

@SuppressWarnings("ThrowableNotThrown")
public class Context {
	public static final int CONVERSION_EXACT = 0;
	public static final int CONVERSION_TRIVIAL = 1;
	public static final int CONVERSION_NONE = 99;
	public static final int JSTYPE_UNDEFINED = 0; // undefined type
	public static final int JSTYPE_NULL = 1; // null
	public static final int JSTYPE_BOOLEAN = 2; // boolean
	public static final int JSTYPE_NUMBER = 3; // number
	public static final int JSTYPE_STRING = 4; // string
	public static final int JSTYPE_JAVA_CLASS = 5; // JavaClass
	public static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
	public static final int JSTYPE_JAVA_ARRAY = 7; // JavaArray
	public static final int JSTYPE_OBJECT = 8; // Scriptable

	public static void reportWarning(Context cx, String message, String sourceName, int lineno, String lineSource, int lineOffset) {
		cx.getErrorReporter().warning(message, sourceName, lineno, lineSource, lineOffset);
	}

	public static void reportWarning(String message, Context cx) {
		int[] linep = {0};
		String filename = getSourcePositionFromStack(cx, linep);
		Context.reportWarning(cx, message, filename, linep[0], null, 0);
	}

	public static void reportError(Context cx, String message, int lineno, String lineSource, int lineOffset, String sourceName) {
		if (cx != null) {
			cx.getErrorReporter().error(cx, message, sourceName, lineno, lineSource, lineOffset);
		} else {
			throw new EvaluatorException(cx, message, sourceName, lineno, lineSource, lineOffset);
		}
	}

	public static void reportError(Context cx, String message) {
		int[] linep = {0};
		String filename = getSourcePositionFromStack(cx, linep);
		Context.reportError(cx, message, linep[0], null, 0, filename);
	}

	/**
	 * Report a runtime error using the error reporter for the current thread.
	 *
	 * @param message    the error message to report
	 * @param sourceName a string describing the source, such as a filename
	 * @param lineno     the starting line number
	 * @param lineSource the text of the line (may be null)
	 * @param lineOffset the offset into lineSource where problem was detected
	 * @return a runtime exception that will be thrown to terminate the
	 * execution of the script
	 * @see ErrorReporter
	 */
	public static EvaluatorException reportRuntimeError(Context cx, String message, String sourceName, int lineno, String lineSource, int lineOffset) {
		if (cx != null) {
			return cx.getErrorReporter().runtimeError(cx, message, sourceName, lineno, lineSource, lineOffset);
		}
		throw new EvaluatorException(cx, message, sourceName, lineno, lineSource, lineOffset);
	}

	public static EvaluatorException reportRuntimeError0(String messageId, Context cx) {
		String msg = ScriptRuntime.getMessage0(messageId);
		return reportRuntimeError(msg, cx);
	}

	public static EvaluatorException reportRuntimeError1(String messageId, Object arg1, Context cx) {
		String msg = ScriptRuntime.getMessage1(messageId, arg1);
		return reportRuntimeError(msg, cx);
	}

	public static EvaluatorException reportRuntimeError2(String messageId, Object arg1, Object arg2, Context cx) {
		String msg = ScriptRuntime.getMessage2(messageId, arg1, arg2);
		return reportRuntimeError(msg, cx);
	}

	public static EvaluatorException reportRuntimeError3(String messageId, Object arg1, Object arg2, Object arg3, Context cx) {
		String msg = ScriptRuntime.getMessage3(messageId, arg1, arg2, arg3);
		return reportRuntimeError(msg, cx);
	}

	public static EvaluatorException reportRuntimeError4(String messageId, Object arg1, Object arg2, Object arg3, Object arg4, Context cx) {
		String msg = ScriptRuntime.getMessage4(messageId, arg1, arg2, arg3, arg4);
		return reportRuntimeError(msg, cx);
	}

	/**
	 * Report a runtime error using the error reporter for the current thread.
	 *
	 * @param message the error message to report
	 * @param cx
	 * @see ErrorReporter
	 */
	public static EvaluatorException reportRuntimeError(String message, Context cx) {
		int[] linep = {0};
		String filename = getSourcePositionFromStack(cx, linep);
		return Context.reportRuntimeError(cx, message, filename, linep[0], null, 0);
	}

	/**
	 * Get the singleton object that represents the JavaScript Undefined value.
	 */
	public static Object getUndefinedValue() {
		return Undefined.INSTANCE;
	}

	/**
	 * Rethrow the exception wrapping it as the script runtime exception.
	 * Unless the exception is instance of {@link EcmaError} or
	 * {@link EvaluatorException} it will be wrapped as
	 * {@link WrappedException}, a subclass of {@link EvaluatorException}.
	 * The resulting exception object always contains
	 * source name and line number of script that triggered exception.
	 * <p>
	 * This method always throws an exception, its return value is provided
	 * only for convenience to allow a usage like:
	 * <pre>
	 * throw Context.throwAsScriptRuntimeEx(ex);
	 * </pre>
	 * to indicate that code after the method is unreachable.
	 *
	 * @throws EvaluatorException
	 * @throws EcmaError
	 */
	public static RuntimeException throwAsScriptRuntimeEx(Throwable e, Context cx) {
		while ((e instanceof InvocationTargetException)) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		// special handling of Error so scripts would not catch them
		if (e instanceof Error) {
			throw (Error) e;
		}
		if (e instanceof RhinoException) {
			throw (RhinoException) e;
		}
		throw new WrappedException(cx, e);
	}

	static Evaluator createInterpreter() {
		return new Interpreter();
	}

	public static String getSourcePositionFromStack(Context cx, int[] linep) {
		if (cx == null) {
			return null;
		}
		if (cx.lastInterpreterFrame != null) {
			Evaluator evaluator = createInterpreter();
			if (evaluator != null) {
				return evaluator.getSourcePositionFromStack(cx, linep);
			}
		}
		/**
		 * A bit of a hack, but the only way to get filename and line
		 * number from an enclosing frame.
		 */
		StackTraceElement[] stackTrace = new Throwable().getStackTrace();
		for (StackTraceElement st : stackTrace) {
			String file = st.getFileName();
			if (!(file == null || file.endsWith(".java"))) {
				int line = st.getLineNumber();
				if (line >= 0) {
					linep[0] = line;
					return file;
				}
			}
		}

		return null;
	}

	public final ContextFactory factory;
	public final Object lock = new Object();

	// Generate an observer count on compiled code
	public boolean generateObserverCount = false;
	private Scriptable topCallScope;
	boolean isContinuationsTopCall;
	NativeCall currentActivationCall;
	BaseFunction typeErrorThrower;
	RegExp regExp;
	// For the interpreter to store the last frame for error reports etc.
	Object lastInterpreterFrame;
	// For the interpreter to store information about previous invocations
	// interpreter invocations
	ObjArray previousInterpreterInvocations;
	// For instruction counting (interpreter only)
	int instructionCount;
	int instructionThreshold;
	// It can be used to return the second uint32 result from function
	long scratchUint32;
	// It can be used to return the second Scriptable result from function
	private Scriptable scratchScriptable;
	boolean isTopLevelStrict;

	private Map<Object, Object> threadLocalMap;
	private ClassLoader applicationClassLoader;

	// custom data

	private transient Map<Class<?>, JavaMembers> classTable;
	private transient Map<JavaAdapter.JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient Map<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;

	/**
	 * Creates a new context. Provided as a preferred super constructor for
	 * subclasses in place of the deprecated default public constructor.
	 *
	 * @throws IllegalArgumentException if factory parameter is null.
	 */
	public Context(ContextFactory factory) {
		this.factory = factory;
	}

	/**
	 * Get the implementation version.
	 *
	 * <p>
	 * The implementation version is of the form
	 * <pre>
	 *    "<i>name langVer</i> <code>release</code> <i>relNum date</i>"
	 * </pre>
	 * where <i>name</i> is the name of the product, <i>langVer</i> is
	 * the language version, <i>relNum</i> is the release number, and
	 * <i>date</i> is the release date for that specific
	 * release in the form "yyyy mm dd".
	 *
	 * @return a string that encodes the product, language version, release
	 * number, and date.
	 */
	public final String getImplementationVersion() {
		return ImplementationVersion.get();
	}

	/**
	 * Get the current error reporter.
	 *
	 * @see ErrorReporter
	 */
	public final ErrorReporter getErrorReporter() {
		return DefaultErrorReporter.instance;
	}

	/**
	 * Initialize the standard objects.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.
	 *
	 * @return the initialized scope
	 */
	public final ScriptableObject initStandardObjects() {
		return initStandardObjects(null, false);
	}

	/**
	 * Initialize the standard objects, leaving out those that offer access directly
	 * to Java classes. This sets up "scope" to have access to all the standard
	 * JavaScript classes, but does not create global objects for any top-level
	 * Java packages. In addition, the "Packages," "JavaAdapter," and
	 * "JavaImporter" classes, and the "getClass" function, are not
	 * initialized.
	 * <p>
	 * The result of this function is a scope that may be safely used in a "sandbox"
	 * environment where it is not desirable to give access to Java code from JavaScript.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.
	 *
	 * @return the initialized scope
	 */
	public final ScriptableObject initSafeStandardObjects() {
		return initSafeStandardObjects(null, false);
	}

	/**
	 * Initialize the standard objects.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.
	 *
	 * @param scope the scope to initialize, or null, in which case a new
	 *              object will be created to serve as the scope
	 * @return the initialized scope. The method returns the value of the scope
	 * argument if it is not null or newly allocated scope object which
	 * is an instance {@link ScriptableObject}.
	 */
	public final Scriptable initStandardObjects(ScriptableObject scope) {
		return initStandardObjects(scope, false);
	}

	/**
	 * Initialize the standard objects, leaving out those that offer access directly
	 * to Java classes. This sets up "scope" to have access to all the standard
	 * JavaScript classes, but does not create global objects for any top-level
	 * Java packages. In addition, the "Packages," "JavaAdapter," and
	 * "JavaImporter" classes, and the "getClass" function, are not
	 * initialized.
	 * <p>
	 * The result of this function is a scope that may be safely used in a "sandbox"
	 * environment where it is not desirable to give access to Java code from JavaScript.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.
	 *
	 * @param scope the scope to initialize, or null, in which case a new
	 *              object will be created to serve as the scope
	 * @return the initialized scope. The method returns the value of the scope
	 * argument if it is not null or newly allocated scope object which
	 * is an instance {@link ScriptableObject}.
	 */
	public final Scriptable initSafeStandardObjects(ScriptableObject scope) {
		return initSafeStandardObjects(scope, false);
	}

	/**
	 * Initialize the standard objects.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.<p>
	 * <p>
	 * This form of the method also allows for creating "sealed" standard
	 * objects. An object that is sealed cannot have properties added, changed,
	 * or removed. This is useful to create a "superglobal" that can be shared
	 * among several top-level objects. Note that sealing is not allowed in
	 * the current ECMA/ISO language specification, but is likely for
	 * the next version.
	 *
	 * @param scope  the scope to initialize, or null, in which case a new
	 *               object will be created to serve as the scope
	 * @param sealed whether or not to create sealed standard objects that
	 *               cannot be modified.
	 * @return the initialized scope. The method returns the value of the scope
	 * argument if it is not null or newly allocated scope object.
	 * @since 1.4R3
	 */
	public ScriptableObject initStandardObjects(ScriptableObject scope, boolean sealed) {
		return ScriptRuntime.initStandardObjects(this, scope, sealed);
	}

	/**
	 * Initialize the standard objects, leaving out those that offer access directly
	 * to Java classes. This sets up "scope" to have access to all the standard
	 * JavaScript classes, but does not create global objects for any top-level
	 * Java packages. In addition, the "Packages," "JavaAdapter," and
	 * "JavaImporter" classes, and the "getClass" function, are not
	 * initialized.
	 * <p>
	 * The result of this function is a scope that may be safely used in a "sandbox"
	 * environment where it is not desirable to give access to Java code from JavaScript.
	 * <p>
	 * Creates instances of the standard objects and their constructors
	 * (Object, String, Number, Date, etc.), setting up 'scope' to act
	 * as a global object as in ECMA 15.1.<p>
	 * <p>
	 * This method must be called to initialize a scope before scripts
	 * can be evaluated in that scope.<p>
	 * <p>
	 * This method does not affect the Context it is called upon.<p>
	 * <p>
	 * This form of the method also allows for creating "sealed" standard
	 * objects. An object that is sealed cannot have properties added, changed,
	 * or removed. This is useful to create a "superglobal" that can be shared
	 * among several top-level objects. Note that sealing is not allowed in
	 * the current ECMA/ISO language specification, but is likely for
	 * the next version.
	 *
	 * @param scope  the scope to initialize, or null, in which case a new
	 *               object will be created to serve as the scope
	 * @param sealed whether or not to create sealed standard objects that
	 *               cannot be modified.
	 * @return the initialized scope. The method returns the value of the scope
	 * argument if it is not null or newly allocated scope object.
	 * @since 1.7.6
	 */
	public ScriptableObject initSafeStandardObjects(ScriptableObject scope, boolean sealed) {
		return ScriptRuntime.initSafeStandardObjects(this, scope, sealed);
	}

	/**
	 * Evaluate a JavaScript source string.
	 * <p>
	 * The provided source name and line number are used for error messages
	 * and for producing debug information.
	 *
	 * @param scope          the scope to execute in
	 * @param source         the JavaScript source
	 * @param sourceName     a string describing the source, such as a filename
	 * @param lineno         the starting line number
	 * @param securityDomain an arbitrary object that specifies security
	 *                       information about the origin or owner of the script. For
	 *                       implementations that don't care about security, this value
	 *                       may be null.
	 * @return the result of evaluating the string
	 */
	public final Object evaluateString(Scriptable scope, String source, String sourceName, int lineno, Object securityDomain) {
		Script script = compileString(source, sourceName, lineno, securityDomain);
		if (script != null) {
			return script.exec(this, scope);
		}
		return null;
	}

	/**
	 * Evaluate a reader as JavaScript source.
	 * <p>
	 * All characters of the reader are consumed.
	 *
	 * @param scope          the scope to execute in
	 * @param in             the Reader to get JavaScript source from
	 * @param sourceName     a string describing the source, such as a filename
	 * @param lineno         the starting line number
	 * @param securityDomain an arbitrary object that specifies security
	 *                       information about the origin or owner of the script. For
	 *                       implementations that don't care about security, this value
	 *                       may be null.
	 * @return the result of evaluating the source
	 * @throws IOException if an IOException was generated by the Reader
	 */
	public final Object evaluateReader(Scriptable scope, Reader in, String sourceName, int lineno, Object securityDomain) throws IOException {
		Script script = compileReader(in, sourceName, lineno, securityDomain);
		if (script != null) {
			return script.exec(this, scope);
		}
		return null;
	}

	/**
	 * Compiles the source in the given reader.
	 * <p>
	 * Returns a script that may later be executed.
	 * Will consume all the source in the reader.
	 *
	 * @param in             the input reader
	 * @param sourceName     a string describing the source, such as a filename
	 * @param lineno         the starting line number for reporting errors
	 * @param securityDomain an arbitrary object that specifies security
	 *                       information about the origin or owner of the script. For
	 *                       implementations that don't care about security, this value
	 *                       may be null.
	 * @return a script that may later be executed
	 * @throws IOException if an IOException was generated by the Reader
	 * @see Script
	 */
	public final Script compileReader(Reader in, String sourceName, int lineno, Object securityDomain) throws IOException {
		if (lineno < 0) {
			// For compatibility IllegalArgumentException can not be thrown here
			lineno = 0;
		}

		return (Script) compileImpl(null, Kit.readReader(in), sourceName, lineno, securityDomain, false, null, null);
	}

	/**
	 * Compiles the source in the given string.
	 * <p>
	 * Returns a script that may later be executed.
	 *
	 * @param source         the source string
	 * @param sourceName     a string describing the source, such as a filename
	 * @param lineno         the starting line number for reporting errors. Use
	 *                       0 if the line number is unknown.
	 * @param securityDomain an arbitrary object that specifies security
	 *                       information about the origin or owner of the script. For
	 *                       implementations that don't care about security, this value
	 *                       may be null.
	 * @return a script that may later be executed
	 * @see Script
	 */
	public final Script compileString(String source, String sourceName, int lineno, Object securityDomain) {
		if (lineno < 0) {
			// For compatibility IllegalArgumentException can not be thrown here
			lineno = 0;
		}
		return compileString(source, null, null, sourceName, lineno, securityDomain);
	}

	final Script compileString(String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
		try {
			return (Script) compileImpl(null, source, sourceName, lineno, securityDomain, false, compiler, compilationErrorReporter);
		} catch (IOException ioe) {
			// Should not happen when dealing with source as string
			throw new RuntimeException(ioe);
		}
	}

	final Function compileFunction(Scriptable scope, String source, Evaluator compiler, ErrorReporter compilationErrorReporter, String sourceName, int lineno, Object securityDomain) {
		try {
			return (Function) compileImpl(scope, source, sourceName, lineno, securityDomain, true, compiler, compilationErrorReporter);
		} catch (IOException ioe) {
			// Should never happen because we just made the reader
			// from a String
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * Create a new JavaScript object.
	 * <p>
	 * Equivalent to evaluating "new Object()".
	 *
	 * @param scope the scope to search for the constructor and to evaluate
	 *              against
	 * @return the new object
	 */
	public Scriptable newObject(Scriptable scope) {
		NativeObject result = new NativeObject(factory);
		ScriptRuntime.setBuiltinProtoAndParent(this, scope, result, TopLevel.Builtins.Object);
		return result;
	}

	/**
	 * Create a new JavaScript object by executing the named constructor.
	 * <p>
	 * The call <code>newObject(scope, "Foo")</code> is equivalent to
	 * evaluating "new Foo()".
	 *
	 * @param scope           the scope to search for the constructor and to evaluate against
	 * @param constructorName the name of the constructor to call
	 * @return the new object
	 */
	public Scriptable newObject(Scriptable scope, String constructorName) {
		return newObject(scope, constructorName, ScriptRuntime.EMPTY_OBJECTS);
	}

	/**
	 * Creates a new JavaScript object by executing the named constructor.
	 * <p>
	 * Searches <code>scope</code> for the named constructor, calls it with
	 * the given arguments, and returns the result.<p>
	 * <p>
	 * The code
	 * <pre>
	 * Object[] args = { "a", "b" };
	 * newObject(scope, "Foo", args)</pre>
	 * is equivalent to evaluating "new Foo('a', 'b')", assuming that the Foo
	 * constructor has been defined in <code>scope</code>.
	 *
	 * @param scope           The scope to search for the constructor and to evaluate
	 *                        against
	 * @param constructorName the name of the constructor to call
	 * @param args            the array of arguments for the constructor
	 * @return the new object
	 */
	public Scriptable newObject(Scriptable scope, String constructorName, Object[] args) {
		return ScriptRuntime.newObject(this, scope, constructorName, args);
	}

	/**
	 * Create an array with a specified initial length.
	 * <p>
	 *
	 * @param scope  the scope to create the object in
	 * @param length the initial length (JavaScript arrays may have
	 *               additional properties added dynamically).
	 * @return the new array object
	 */
	public Scriptable newArray(Scriptable scope, int length) {
		NativeArray result = new NativeArray(this, length);
		ScriptRuntime.setBuiltinProtoAndParent(this, scope, result, TopLevel.Builtins.Array);
		return result;
	}

	/**
	 * Create an array with a set of initial elements.
	 *
	 * @param scope    the scope to create the object in.
	 * @param elements the initial elements. Each object in this array
	 *                 must be an acceptable JavaScript type and type
	 *                 of array should be exactly Object[], not
	 *                 SomeObjectSubclass[].
	 * @return the new array object.
	 */
	public Scriptable newArray(Scriptable scope, Object[] elements) {
		if (elements.getClass().getComponentType() != ScriptRuntime.ObjectClass) {
			throw new IllegalArgumentException();
		}
		NativeArray result = new NativeArray(this, elements);
		ScriptRuntime.setBuiltinProtoAndParent(this, scope, result, TopLevel.Builtins.Array);
		return result;
	}

	//--------------------------
	// Static conversion methods
	//--------------------------

	/**
	 * Convert the value to a JavaScript boolean value.
	 * <p>
	 * See ECMA 9.2.
	 *
	 * @param value a JavaScript value
	 * @return the corresponding boolean value converted using
	 * the ECMA rules
	 */
	public static boolean toBoolean(Object value, Context cx) {
		return ScriptRuntime.toBoolean(cx, value);
	}

	/**
	 * Convert the value to a JavaScript Number value.
	 * <p>
	 * Returns a Java double for the JavaScript Number.
	 * <p>
	 * See ECMA 9.3.
	 *
	 * @param value a JavaScript value
	 * @return the corresponding double value converted using
	 * the ECMA rules
	 */
	public static double toNumber(Object value, Context cx) {
		return ScriptRuntime.toNumber(cx, value);
	}

	/**
	 * Convert the value to a JavaScript String value.
	 * <p>
	 * See ECMA 9.8.
	 * <p>
	 *
	 * @param value a JavaScript value
	 * @return the corresponding String value converted using
	 * the ECMA rules
	 */
	public static String toString(Object value, Context cx) {
		return ScriptRuntime.toString(cx, value);
	}

	/**
	 * Convert the value to an JavaScript object value.
	 * <p>
	 * Note that a scope must be provided to look up the constructors
	 * for Number, Boolean, and String.
	 * <p>
	 * See ECMA 9.9.
	 * <p>
	 * Additionally, arbitrary Java objects and classes will be
	 * wrapped in a Scriptable object with its Java fields and methods
	 * reflected as JavaScript properties of the object.
	 *
	 * @param value any Java object
	 * @param scope global scope containing constructors for Number,
	 *              Boolean, and String
	 * @return new JavaScript object
	 */
	public static Scriptable toObject(Object value, Scriptable scope, Context cx) {
		return ScriptRuntime.toObject(cx, scope, value);
	}


	/**
	 * Get a value corresponding to a key.
	 * <p>
	 * Since the Context is associated with a thread it can be
	 * used to maintain values that can be later retrieved using
	 * the current thread.
	 * <p>
	 * Note that the values are maintained with the Context, so
	 * if the Context is disassociated from the thread the values
	 * cannot be retrieved. Also, if private data is to be maintained
	 * in this manner the key should be a java.lang.Object
	 * whose reference is not divulged to untrusted code.
	 *
	 * @param key the key used to lookup the value
	 * @return a value previously stored using putThreadLocal.
	 */
	public final Object getThreadLocal(Object key) {
		if (threadLocalMap == null) {
			return null;
		}
		return threadLocalMap.get(key);
	}

	/**
	 * Put a value that can later be retrieved using a given key.
	 * <p>
	 *
	 * @param key   the key used to index the value
	 * @param value the value to save
	 */
	public synchronized final void putThreadLocal(Object key, Object value) {
		if (threadLocalMap == null) {
			threadLocalMap = new HashMap<>();
		}
		threadLocalMap.put(key, value);
	}

	/**
	 * Remove values from thread-local storage.
	 *
	 * @param key the key for the entry to remove.
	 * @since 1.5 release 2
	 */
	public final void removeThreadLocal(Object key) {
		if (threadLocalMap == null) {
			return;
		}
		threadLocalMap.remove(key);
	}

	/**
	 * Get threshold of executed instructions counter that triggers call to
	 * <code>observeInstructionCount()</code>.
	 * When the threshold is zero, instruction counting is disabled,
	 * otherwise each time the run-time executes at least the threshold value
	 * of script instructions, <code>observeInstructionCount()</code> will
	 * be called.
	 */
	public final int getInstructionObserverThreshold() {
		return instructionThreshold;
	}

	/**
	 * Set threshold of executed instructions counter that triggers call to
	 * <code>observeInstructionCount()</code>.
	 * When the threshold is zero, instruction counting is disabled,
	 * otherwise each time the run-time executes at least the threshold value
	 * of script instructions, <code>observeInstructionCount()</code> will
	 * be called.<br>
	 * Note that the meaning of "instruction" is not guaranteed to be
	 * consistent between compiled and interpretive modes: executing a given
	 * script or function in the different modes will result in different
	 * instruction counts against the threshold.
	 * {@link #setGenerateObserverCount} is called with true if
	 * <code>threshold</code> is greater than zero, false otherwise.
	 *
	 * @param threshold The instruction threshold
	 */
	public final void setInstructionObserverThreshold(int threshold) {
		if (threshold < 0) {
			throw new IllegalArgumentException();
		}
		instructionThreshold = threshold;
		setGenerateObserverCount(threshold > 0);
	}

	/**
	 * Turn on or off generation of code with callbacks to
	 * track the count of executed instructions.
	 * Currently only affects JVM byte code generation: this slows down the
	 * generated code, but code generated without the callbacks will not
	 * be counted toward instruction thresholds. Rhino's interpretive
	 * mode does instruction counting without inserting callbacks, so
	 * there is no requirement to compile code differently.
	 *
	 * @param generateObserverCount if true, generated code will contain
	 *                              calls to accumulate an estimate of the instructions executed.
	 */
	public void setGenerateObserverCount(boolean generateObserverCount) {
		this.generateObserverCount = generateObserverCount;
	}

	/**
	 * Allow application to monitor counter of executed script instructions
	 * in Context subclasses.
	 * Run-time calls this when instruction counting is enabled and the counter
	 * reaches limit set by <code>setInstructionObserverThreshold()</code>.
	 * The method is useful to observe long running scripts and if necessary
	 * to terminate them.
	 * <p>
	 *
	 * @param instructionCount amount of script instruction executed since
	 *                         last call to <code>observeInstructionCount</code>
	 * @throws Error to terminate the script
	 */
	protected void observeInstructionCount(int instructionCount) {
	}


	public final ClassLoader getApplicationClassLoader() {
		if (applicationClassLoader == null) {
			ClassLoader threadLoader = Thread.currentThread().getContextClassLoader();
			if (threadLoader != null && Kit.testIfCanLoadRhinoClasses(threadLoader)) {
				// Thread.getContextClassLoader is not cached since
				// its caching prevents it from GC which may lead to
				// a memory leak and hides updates to
				// Thread.getContextClassLoader
				return threadLoader;
			}
			// Thread.getContextClassLoader can not load Rhino classes,
			// try to use the loader of ContextFactory or Context
			// subclasses.
			applicationClassLoader = getClass().getClassLoader();
		}
		return applicationClassLoader;
	}

	public final void setApplicationClassLoader(ClassLoader loader) {
		if (loader == null) {
			// restore default behaviour
			applicationClassLoader = null;
			return;
		}
		if (!Kit.testIfCanLoadRhinoClasses(loader)) {
			throw new IllegalArgumentException("Loader can not resolve Rhino classes");
		}
		applicationClassLoader = loader;
	}

	private Object compileImpl(Scriptable scope, String sourceString, String sourceName, int lineno, Object securityDomain, boolean returnFunction, Evaluator compiler, ErrorReporter compilationErrorReporter) throws IOException {
		if (sourceName == null) {
			sourceName = "unnamed script";
		}
		if (securityDomain != null) {
			throw new IllegalArgumentException("securityDomain should be null if setSecurityController() was never called");
		}

		// scope should be given if and only if compiling function
		if ((scope == null) == returnFunction) {
			Kit.codeBug();
		}

		CompilerEnvirons compilerEnv = new CompilerEnvirons();
		compilerEnv.initFromContext(this);
		if (compilationErrorReporter == null) {
			compilationErrorReporter = compilerEnv.getErrorReporter();
		}

		ScriptNode tree = parse(sourceString, sourceName, lineno, compilerEnv, compilationErrorReporter, returnFunction);

		Object bytecode;
		try {
			if (compiler == null) {
				compiler = createCompiler();
			}

			bytecode = compiler.compile(compilerEnv, tree, returnFunction, this);
		} catch (ClassFileFormatException e) {
			// we hit some class file limit, fall back to interpreter or report

			// we have to recreate the tree because the compile call might have changed the tree already
			tree = parse(sourceString, sourceName, lineno, compilerEnv, compilationErrorReporter, returnFunction);

			compiler = createInterpreter();
			bytecode = compiler.compile(compilerEnv, tree, returnFunction, this);
		}

		Object result;
		if (returnFunction) {
			result = compiler.createFunctionObject(this, scope, bytecode, securityDomain);
		} else {
			result = compiler.createScriptObject(bytecode, securityDomain);
		}

		return result;
	}

	private ScriptNode parse(String sourceString, String sourceName, int lineno, CompilerEnvirons compilerEnv, ErrorReporter compilationErrorReporter, boolean returnFunction) throws IOException {
		Parser p = new Parser(this, compilerEnv, compilationErrorReporter);
		if (returnFunction) {
			p.calledByCompileFunction = true;
		}
		if (isStrictMode()) {
			p.setDefaultUseStrictDirective(true);
		}

		AstRoot ast = p.parse(sourceString, sourceName, lineno);
		if (returnFunction) {
			// parser no longer adds function to script node
			if (!(ast.getFirstChild() != null && ast.getFirstChild().getType() == Token.FUNCTION)) {
				// XXX: the check just looks for the first child
				// and allows for more nodes after it for compatibility
				// with sources like function() {};;;
				throw new IllegalArgumentException("compileFunction only accepts source with single JS function: " + sourceString);
			}
		}

		return new IRFactory(this, compilerEnv, compilationErrorReporter).transformTree(ast);
	}

	private Evaluator createCompiler() {
		return createInterpreter();
	}

	public RegExp getRegExp() {
		if (regExp == null) {
			regExp = new RegExp();
		}
		return regExp;
	}

	public final boolean isStrictMode() {
		return isTopLevelStrict || (currentActivationCall != null && currentActivationCall.isStrict);
	}

	public void addToScope(Scriptable scope, String name, Object value) {
		if (value instanceof Class<?> c) {
			ScriptableObject.putProperty(scope, name, new NativeJavaClass(this, scope, c), this);
		} else {
			ScriptableObject.putProperty(scope, name, javaToJS(value, scope), this);
		}
	}

	// custom data

	/**
	 * @return a map from classes to associated JavaMembers objects
	 */
	Map<Class<?>, JavaMembers> getClassCacheMap() {
		if (classTable == null) {
			// Use 1 as concurrency level here and for other concurrent hash maps
			// as we don't expect high levels of sustained concurrent writes.
			classTable = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		return classTable;
	}

	Map<JavaAdapter.JavaAdapterSignature, Class<?>> getInterfaceAdapterCacheMap() {
		if (classAdapterCache == null) {
			classAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}
		return classAdapterCache;
	}

	/**
	 * Internal engine method to return serial number for generated classes
	 * to ensure name uniqueness.
	 */
	public final synchronized int newClassSerialNumber() {
		return ++generatedClassSerial;
	}

	Object getInterfaceAdapter(Class<?> cl) {
		return interfaceAdapterCache == null ? null : interfaceAdapterCache.get(cl);
	}

	synchronized void cacheInterfaceAdapter(Class<?> cl, Object iadapter) {
		if (interfaceAdapterCache == null) {
			interfaceAdapterCache = new ConcurrentHashMap<>(16, 0.75f, 1);
		}

		interfaceAdapterCache.put(cl, iadapter);
	}

	/**
	 * Return true iff the Java class with the given name should be exposed
	 * to scripts.
	 * <p>
	 * An embedding may filter which Java classes are exposed through
	 * LiveConnect to JavaScript scripts.
	 * <p>
	 * Due to the fact that there is no package reflection in Java,
	 * this method will also be called with package names. There
	 * is no way for Rhino to tell if "Packages.a.b" is a package name
	 * or a class that doesn't exist. What Rhino does is attempt
	 * to load each segment of "Packages.a.b.c": It first attempts to
	 * load class "a", then attempts to load class "a.b", then
	 * finally attempts to load class "a.b.c". On a Rhino installation
	 * without any ClassShutter set, and without any of the
	 * above classes, the expression "Packages.a.b.c" will result in
	 * a [JavaPackage a.b.c] and not an error.
	 * <p>
	 * With ClassShutter supplied, Rhino will first call
	 * visibleToScripts before attempting to look up the class name. If
	 * visibleToScripts returns false, the class name lookup is not
	 * performed and subsequent Rhino execution assumes the class is
	 * not present. So for "java.lang.System.out.println" the lookup
	 * of "java.lang.System" is skipped and thus Rhino assumes that
	 * "java.lang.System" doesn't exist. So then for "java.lang.System.out",
	 * Rhino attempts to load the class "java.lang.System.out" because
	 * it assumes that "java.lang.System" is a package name.
	 * <p>
	 *
	 * @param fullClassName the full name of the class (including the package
	 *                      name, with '.' as a delimiter). For example the
	 *                      standard string class is "java.lang.String"
	 * @return whether or not to reveal this class to scripts
	 */
	public boolean visibleToScripts(String fullClassName, ClassVisibilityContext type) {
		return true;
	}

	public Object wrap(Scriptable scope, Object obj, TypeInfo target) {
		if (obj == null || obj == Undefined.INSTANCE || obj instanceof Scriptable) {
			return obj;
		} else if (target.isVoid()) {
			return Undefined.INSTANCE;
		} else if (target.isCharacter()) { // is this necessary?
			return (int) (Character) obj;
		} else if (target.isPrimitive()) {
			return obj;
		} else if (target instanceof ArrayTypeInfo array) {
			return new NativeJavaArray(scope, obj, array, this);
		} else {
			return wrapAsJavaObject(scope, obj, target);
		}
	}

	// Wraps an object if needed
	public Object wrapAny(Scriptable scope, Object obj) {
		if (obj instanceof String ||
			obj instanceof Boolean ||
			obj instanceof Integer ||
			obj instanceof Short ||
			obj instanceof Long ||
			obj instanceof Float ||
			obj instanceof Double) {
			return obj;
		} else if (obj instanceof Character) {
			return String.valueOf(((Character) obj).charValue());
		}
		Class<?> cls = obj.getClass();
		TypeInfo ti = TypeInfo.NONE;
		if (cls.isArray()) {
			ti = TypeInfo.of(cls);
			//return NativeJavaArray.wrap(scope, obj);
		}

		return wrap(scope, obj, ti);
	}

	public Object wrap(Scriptable scope, Object obj) {
		return wrap(scope, obj, TypeInfo.NONE);
	}

	public boolean hasTopCallScope() {
		synchronized (lock) {
			return topCallScope != null;
		}
	}

	public Scriptable getTopCallScope() {
		synchronized (lock) {
			return topCallScope;
		}
	}

	public Scriptable getTopCallOrThrow() {
		synchronized (lock) {
			if (topCallScope == null) {
				throw new IllegalStateException();
			}

			return topCallScope;
		}
	}

	public void setTopCall(Scriptable scope) {
		synchronized (lock) {
			topCallScope = scope;
		}
	}

	public void storeScriptable(Scriptable value) {
		synchronized (lock) {
			// The previously stored scratchScriptable should be consumed
			if (scratchScriptable != null) {
				throw new IllegalStateException();
			}
			scratchScriptable = value;
		}
	}

	public Scriptable lastStoredScriptable() {
		synchronized (lock) {
			Scriptable result = scratchScriptable;
			scratchScriptable = null;
			return result;
		}
	}

	/**
	 * Call {@link
	 * Callable#call(Context cx, Scriptable scope, Scriptable thisObj,
	 * Object[] args)}
	 * using the Context instance associated with the current thread.
	 * If no Context is associated with the thread, then makeContext() will be called to construct
	 * new Context instance. The instance will be temporary associated
	 * with the thread during call to {@link ContextAction#run(Context)}.
	 * <p>
	 * It is allowed but not advisable to use null for <code>factory</code>
	 * argument in which case the global static singleton ContextFactory
	 * instance will be used to create new context instances.
	 */
	public Object callSync(Callable callable, Scriptable scope, Scriptable thisObj, Object[] args) {
		synchronized (lock) {
			return callable.call(this, scope, thisObj, args);
		}
	}

	public Object doTopCall(Scriptable scope, Callable callable, Scriptable thisObj, Object[] args, boolean isTopLevelStrict) {
		if (scope == null) {
			throw new IllegalArgumentException();
		}
		if (hasTopCallScope()) {
			throw new IllegalStateException();
		}

		Object result;
		setTopCall(ScriptableObject.getTopLevelScope(scope));
		boolean previousTopLevelStrict = this.isTopLevelStrict;
		this.isTopLevelStrict = isTopLevelStrict;
		try {
			result = callSync(callable, scope, thisObj, args);
		} finally {
			setTopCall(null);
			// Cleanup cached references
			this.isTopLevelStrict = previousTopLevelStrict;

			if (currentActivationCall != null) {
				// Function should always call exitActivationFunction
				// if it creates activation record
				throw new IllegalStateException();
			}
		}
		return result;
	}

	/**
	 * Wrap a Java class as Scriptable instance to allow access to its static
	 * members and fields and use as constructor from JavaScript.
	 * <p>
	 * Subclasses can override this method to provide custom wrappers for
	 * Java classes.
	 *
	 * @param scope     the scope of the executing script
	 * @param javaClass the class to be wrapped
	 * @return the wrapped value which shall not be null
	 * @since 1.7R3
	 */
	public Scriptable wrapJavaClass(Scriptable scope, Class<?> javaClass) {
		return new NativeJavaClass(this, scope, javaClass);
	}

	/**
	 * Wrap Java object as Scriptable instance to allow full access to its
	 * methods and fields from JavaScript.
	 * <p>
	 * {@link #wrap(Scriptable, Object, TypeInfo)} and
	 * {@link #wrapNewObject(Scriptable, Object, TypeInfo)} call this method
	 * when they can not convert <code>javaObject</code> to JavaScript primitive
	 * value or JavaScript array.
	 * <p>
	 * Subclasses can override the method to provide custom wrappers
	 * for Java objects.
	 *
	 * @param scope      the scope of the executing script
	 * @param javaObject the object to be wrapped
	 * @param target     type hint. If security restrictions prevent to wrap
	 *                   object based on its class, staticType will be used instead.
	 * @return the wrapped value which shall not be null
	 */
	public Scriptable wrapAsJavaObject(Scriptable scope, Object javaObject, TypeInfo target) {
		if (javaObject instanceof CustomJavaToJsWrapper w) {
			return w.convertJavaToJs(this, scope, target);
		}

		if (javaObject instanceof Map map) {
			return new NativeJavaMap(this, scope, map, map, target);
		} else if (javaObject instanceof List list) {
			return new NativeJavaList(this, scope, list, list, target);
		} else if (javaObject instanceof Set<?> set) {
			return new NativeJavaList(this, scope, set, new JavaSetWrapper<>(set), target);
		}

		// TODO: Wrap Gson
		return new NativeJavaObject(scope, javaObject, target, this);
	}

	/**
	 * Wrap an object newly created by a constructor call.
	 *
	 * @param scope the scope of the executing script
	 * @param obj   the object to be wrapped
	 * @return the wrapped value.
	 */
	public Scriptable wrapNewObject(Scriptable scope, Object obj, TypeInfo objType) {
		if (obj instanceof Scriptable) {
			return (Scriptable) obj;
		}
		if (objType instanceof ArrayTypeInfo) {
			return new NativeJavaArray(scope, obj, objType, this);
		}
		return wrapAsJavaObject(scope, obj, TypeInfo.NONE);
	}

	public int internalConversionWeight(Object fromObj, TypeInfo target) {
		if (factory.getTypeWrappers().hasWrapper(fromObj, target)) {
			return CONVERSION_EXACT;
		}

		return CONVERSION_NONE;
	}

	public int internalConversionWeightLast(Object fromObj, TypeInfo target) {
		return CONVERSION_NONE;
	}

	/**
	 * Create class loader for generated classes.
	 */
	public GeneratedClassLoader createClassLoader(ClassLoader parent) {
		return new DefiningClassLoader(parent);
	}

	public String getMappedClass(Class<?> from) {
		return "";
	}

	public String getUnmappedClass(String from) {
		return "";
	}

	public String getMappedField(Class<?> from, Field field) {
		return "";
	}

	public String getMappedMethod(Class<?> from, Method method) {
		return "";
	}

	public int getMaximumInterpreterStackDepth() {
		return Integer.MAX_VALUE;
	}

	public ArrayValueProvider arrayValueProviderOf(Object value) {
		if (value instanceof Object[] arr) {
			return arr.length == 0 ? ArrayValueProvider.EMPTY : new ArrayValueProvider.FromPlainJavaArray(arr);
		} else if (value != null && value.getClass().isArray()) {
			int len = Array.getLength(value);
			return len == 0 ? ArrayValueProvider.EMPTY : new ArrayValueProvider.FromJavaArray(value, len);
		}

		return switch (value) {
			case NativeArray array -> ArrayValueProvider.fromNativeArray(array);
			case NativeJavaList list -> ArrayValueProvider.fromJavaList(list.list, list);
			case List<?> list -> ArrayValueProvider.fromJavaList(list, list);
			case Iterable<?> itr -> ArrayValueProvider.fromIterable(itr);
			case null, default -> value == null ? ArrayValueProvider.FromObject.FROM_NULL : new ArrayValueProvider.FromObject(value);
		};
	}

	public Object arrayOf(@Nullable Object from, TypeInfo target) {
		if (from instanceof Object[] arr) {
			if (target == null) {
				return from;
			}

			return arr.length == 0 ? target.newArray(0) : new ArrayValueProvider.FromPlainJavaArray(arr).createArray(this, target);
		} else if (from != null && from.getClass().isArray()) {
			if (target == null) {
				return from;
			}

			int len = Array.getLength(from);
			return len == 0 ? target.newArray(0) : new ArrayValueProvider.FromJavaArray(from, len).createArray(this, target);
		}

		return arrayValueProviderOf(from).createArray(this, target);
	}

	public Object listOf(@Nullable Object from, TypeInfo target) {
		if (from instanceof NativeJavaList n) {
			if (target == null) {
				// No conversion necessary
				return n.list;
			} else if (target.equals(n.listType)) {
				// No conversion necessary
				return n.list;
			} else {
				var list = new ArrayList<>(n.list.size());

				for (var o : n.list) {
					list.add(jsToJava(o, target));
				}

				return list;
			}
		}

		return arrayValueProviderOf(from).createList(this, target);
	}

	public boolean isListLike(Object from) {
		return from instanceof NativeJavaList || from instanceof NativeJavaArray || from instanceof List<?>;
	}

	@Nullable
	public <K> List<K> optionalListOf(@Nullable Object from, TypeInfo target) {
		return isListLike(from) ? (List) listOf(from, target) : null;
	}

	@Nullable
	public List<Object> optionalListOf(@Nullable Object from) {
		return optionalListOf(from, TypeInfo.NONE);
	}

	public Object setOf(@Nullable Object from, TypeInfo target) {
		if (from instanceof NativeJavaList n) {
			if (target == null) {
				// No conversion necessary
				return new LinkedHashSet<>(n.list);
			} else if (target.equals(n.listType)) {
				// No conversion necessary
				return new LinkedHashSet<>(n.list);
			} else {
				var set = new LinkedHashSet<>(n.list.size());

				for (var o : n.list) {
					set.add(jsToJava(o, target));
				}

				return set;
			}
		}

		return arrayValueProviderOf(from).createSet(this, target);
	}

	public Object mapOf(@Nullable Object from, TypeInfo kTarget, TypeInfo vTarget) {
		if (from instanceof NativeJavaMap n) {
			if (!kTarget.shouldConvert() && !vTarget.shouldConvert()) {
				// No conversion necessary
				return n.map;
			} else if (kTarget.equals(n.mapKeyType) && vTarget.equals(n.mapValueType)) {
				// No conversion necessary
				return n.map;
			} else {
				if (n.map.isEmpty()) {
					return Map.of();
				}

				var map = new LinkedHashMap<>(n.map.size());

				for (var entry : ((Map<?, ?>) n.map).entrySet()) {
					map.put(jsToJava(entry.getKey(), kTarget), jsToJava(entry.getValue(), vTarget));
				}

				return map;
			}
		} else if (from instanceof NativeObject obj) {
			var keys = obj.getIds(this);
			var map = new LinkedHashMap<>(keys.length);

			for (var key : keys) {
				map.put(jsToJava(key, kTarget), jsToJava(obj.get(this, key), vTarget));
			}

			return map;
		} else if (from instanceof Map<?, ?> m) {
			if (!kTarget.shouldConvert() && !vTarget.shouldConvert()) {
				// No conversion necessary
				return m;
			}

			var map = new LinkedHashMap<>(m.size());

			for (var entry : m.entrySet()) {
				map.put(jsToJava(entry.getKey(), kTarget), jsToJava(entry.getValue(), vTarget));
			}

			return map;
		} else {
			return reportConversionError(from, TypeInfo.RAW_MAP);
		}
	}

	public boolean isMapLike(Object from) {
		return from instanceof NativeJavaMap || from instanceof Map<?, ?>;
	}

	@Nullable
	public <K, V> Map<K, V> optionalMapOf(@Nullable Object from, TypeInfo kTarget, TypeInfo vTarget) {
		return isMapLike(from) ? (Map) mapOf(from, kTarget, vTarget) : null;
	}

	@Nullable
	public Map<String, Object> optionalMapOf(@Nullable Object from) {
		return optionalMapOf(from, TypeInfo.STRING, TypeInfo.NONE);
	}

	public Object classOf(Object from) {
		if (from instanceof NativeJavaClass n) {
			return n.getClassObject();
		} else if (from instanceof Class<?> c) {
			if (visibleToScripts(c.getName(), ClassVisibilityContext.ARGUMENT)) {
				return c;
			} else {
				throw reportRuntimeError("Class " + c.getName() + " not allowed", this);
			}
		} else {
			var s = ScriptRuntime.toString(this, from);

			if (visibleToScripts(s, ClassVisibilityContext.ARGUMENT)) {
				try {
					return Class.forName(s);
				} catch (ClassNotFoundException e) {
					throw reportRuntimeError("Failed to load class " + s, this);
				}
			} else {
				throw reportRuntimeError("Class " + from + " not allowed", this);
			}
		}
	}

	public Object createInterfaceAdapter(TypeInfo type, ScriptableObject so) {
		var cl = type.asClass();

		if (cl == null) {
			throw new NullPointerException("type.asClass() must not be null");
		}

		// XXX: Currently only instances of ScriptableObject are
		// supported since the resulting interface proxies should
		// be reused next time conversion is made and generic
		// Callable has no storage for it. Weak references can
		// address it but for now use this restriction.

		Object key = Kit.makeHashKeyFromPair("Coerced Interface", cl);
		Object old = so.getAssociatedValue(key);
		if (old != null) {
			// Function was already wrapped
			return old;
		}
		Object glue = InterfaceAdapter.create(this, cl, so);
		// Store for later retrieval
		glue = so.associateValue(key, glue);
		return glue;
	}

	public Object javaToJS(Object value, Scriptable scope) {
		return javaToJS(value, scope, TypeInfo.NONE);
	}

	public Object javaToJS(Object value, Scriptable scope, TypeInfo target) {
		if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Scriptable) {
			return value;
		} else if (value instanceof Character) {
			return String.valueOf(((Character) value).charValue());
		} else {
			return wrap(scope, value, target);
		}
	}

	public final Object jsToJava(@Nullable Object from, TypeInfo target) throws EvaluatorException {
		if (target == null || !target.shouldConvert()) {
			return Wrapper.unwrapped(from);
		} else if (target.is(TypeInfo.RAW_SET)) {
			return setOf(from, target.param(0));
		} else if (target.is(TypeInfo.RAW_MAP)) {
			return mapOf(from, target.param(0), target.param(1));
		} else if (target instanceof ArrayTypeInfo) {
			return arrayOf(from, target.componentType());
		} else if (List.class.isAssignableFrom(target.asClass())) {
			return listOf(from, target.param(0));
		} else if (target.is(TypeInfo.CLASS)) {
			return classOf(from);
		}

		if (from == null || from.getClass() == target.asClass()) {
			return from;
		}

		return internalJsToJava(from, target);
	}

	private static int getJSTypeCode(Object from) {
		if (from == null) {
			return JSTYPE_NULL;
		} else if (from == Undefined.INSTANCE) {
			return JSTYPE_UNDEFINED;
		} else if (from instanceof CharSequence) {
			return JSTYPE_STRING;
		} else if (from instanceof Number) {
			return JSTYPE_NUMBER;
		} else if (from instanceof Boolean) {
			return JSTYPE_BOOLEAN;
		} else if (from instanceof Scriptable) {
			return switch (from) {
				case NativeJavaClass ignore -> JSTYPE_JAVA_CLASS;
				case NativeJavaArray ignore -> JSTYPE_JAVA_ARRAY;
				case Wrapper ignore -> JSTYPE_JAVA_OBJECT;
				default -> JSTYPE_OBJECT;
			};
		} else if (from instanceof Class) {
			return JSTYPE_JAVA_CLASS;
		} else {
			Class<?> valueClass = from.getClass();
			if (valueClass.isArray()) {
				return JSTYPE_JAVA_ARRAY;
			}
			return JSTYPE_JAVA_OBJECT;
		}
	}

	protected Object internalJsToJava(Object from, TypeInfo target) {
		if (target instanceof ArrayTypeInfo) {
			// Make a new java array, and coerce the JS array components to the target (component) type.
			var arrayType = target.componentType();

			if (from instanceof NativeArray array) {
				long length = array.getLength();

				Object result = arrayType.newArray((int) length);
				for (int i = 0; i < length; ++i) {
					try {
						Array.set(result, i, jsToJava(array.get(this, i, array), arrayType));
					} catch (EvaluatorException ee) {
						return reportConversionError(from, target);
					}
				}

				return result;
			} else {
				// Convert a single value to an array
				Object result = arrayType.newArray(1);
				Array.set(result, 0, jsToJava(from, arrayType));
				return result;
			}
		}

		Object unwrappedValue = Wrapper.unwrapped(from);

		var typeWrapper = factory.getTypeWrappers().getWrapperFactory(unwrappedValue, target);

		if (typeWrapper != null) {
			return typeWrapper.wrap(this, unwrappedValue, target);
		}

		switch (getJSTypeCode(from)) {
			case JSTYPE_NULL -> {
				// raise error if type.isPrimitive()
				if (target.isPrimitive()) {
					return reportConversionError(from, target);
				}
				return null;
			}
			case JSTYPE_UNDEFINED -> {
				if (target == TypeInfo.STRING || target == TypeInfo.OBJECT) {
					return "undefined";
				}
				return reportConversionError(from, target);
			}
			case JSTYPE_BOOLEAN -> {
				// Under LC3, only JS Booleans can be coerced into a Boolean value
				if (target.isBoolean() || target == TypeInfo.OBJECT) {
					return from;
				} else if (target == TypeInfo.STRING) {
					return from.toString();
				} else {
					return internalJsToJavaLast(from, target);
				}
			}
			case JSTYPE_NUMBER -> {
				if (target == TypeInfo.STRING) {
					return ScriptRuntime.toString(this, from);
				} else if (target == TypeInfo.OBJECT) {
					return coerceToNumber(TypeInfo.DOUBLE, from);
				} else if ((target.isPrimitive() && !target.isBoolean()) || ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
					return coerceToNumber(target, from);
				} else {
					return internalJsToJavaLast(from, target);
				}
			}
			case JSTYPE_STRING -> {
				if (target == TypeInfo.STRING || target.asClass().isInstance(from)) {
					return from.toString();
				} else if (target.isCharacter()) {
					// Special case for converting a single char string to a
					// character
					// Placed here because it applies *only* to JS strings,
					// not other JS objects converted to strings
					if (((CharSequence) from).length() == 1) {
						return ((CharSequence) from).charAt(0);
					}
					return coerceToNumber(target, from);
				} else if ((target.isPrimitive() && !target.isBoolean()) || ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
					return coerceToNumber(target, from);
				} else {
					return internalJsToJavaLast(from, target);
				}
			}
			case JSTYPE_JAVA_CLASS -> {
				if (target == TypeInfo.CLASS || target == TypeInfo.OBJECT) {
					return unwrappedValue;
				} else if (target == TypeInfo.STRING) {
					return unwrappedValue.toString();
				} else {
					return internalJsToJavaLast(unwrappedValue, target);
				}
			}
			case JSTYPE_JAVA_OBJECT, JSTYPE_JAVA_ARRAY -> {
				if (target.isPrimitive()) {
					if (target.isBoolean()) {
						return internalJsToJavaLast(unwrappedValue, target);
					}
					return coerceToNumber(target, unwrappedValue);
				}
				if (target == TypeInfo.STRING) {
					return unwrappedValue.toString();
				}
				if (target.asClass().isInstance(unwrappedValue)) {
					return unwrappedValue;
				}
				return internalJsToJavaLast(unwrappedValue, target);
			}
			case JSTYPE_OBJECT -> {
				if (target == TypeInfo.STRING) {
					return ScriptRuntime.toString(this, from);
				} else if (target.isPrimitive()) {
					if (target.isBoolean()) {
						return internalJsToJavaLast(from, target);
					}
					return coerceToNumber(target, from);
				} else if (target.asClass().isInstance(from)) {
					return from;
				} else if (target == TypeInfo.DATE && from instanceof NativeDate) {
					double time = ((NativeDate) from).getJSTimeValue();
					// XXX: This will replace NaN by 0
					return new Date((long) time);
				} else if (from instanceof Wrapper) {
					if (target.asClass().isInstance(unwrappedValue)) {
						return unwrappedValue;
					}
					return internalJsToJavaLast(unwrappedValue, target);
				} else if (target.asClass().isInterface() && (from instanceof NativeObject || from instanceof NativeFunction || from instanceof ArrowFunction)) {
					// Try to use function/object as implementation of Java interface.
					return createInterfaceAdapter(target, (ScriptableObject) from);
				} else {
					return internalJsToJavaLast(from, target);
				}
			}
		}

		return internalJsToJavaLast(from, target);
	}

	protected Object internalJsToJavaLast(Object from, TypeInfo target) {
		if (target instanceof TypeWrapperFactory<?> f) {
			return f.wrap(this, from, target);
		}

		return reportConversionError(from, target);
	}

	public final boolean canConvert(Object from, TypeInfo target) {
		return getConversionWeight(from, target) < CONVERSION_NONE;
	}

	public final int getConversionWeight(Object from, TypeInfo target) {
		int fcw = internalConversionWeight(from, target);

		if (fcw != CONVERSION_NONE) {
			return fcw;
		} else if (target instanceof ArrayTypeInfo || Collection.class.isAssignableFrom(target.asClass())) {
			return CONVERSION_EXACT;
		} else if (target.is(TypeInfo.CLASS)) {
			return from instanceof Class<?> || from instanceof NativeJavaClass ? CONVERSION_TRIVIAL : CONVERSION_EXACT;
		} else if (from == null) {
			if (!target.isPrimitive()) {
				return CONVERSION_TRIVIAL;
			}
		} else if (from == Undefined.INSTANCE) {
			if (target == TypeInfo.STRING || target == TypeInfo.OBJECT) {
				return CONVERSION_TRIVIAL;
			}
		} else if (from instanceof CharSequence) {
			if (target == TypeInfo.STRING) {
				return CONVERSION_TRIVIAL;
			} else if (target.asClass().isInstance(from)) {
				return 2;
			} else if (target.isPrimitive()) {
				if (target.isCharacter()) {
					return 3;
				} else if (!target.isBoolean()) {
					return 4;
				}
			}
		} else if (from instanceof Number) {
			if (target.isPrimitive()) {
				if (target.isDouble()) {
					return CONVERSION_TRIVIAL;
				} else if (!target.isBoolean()) {
					return CONVERSION_TRIVIAL + getSizeRank(target);
				}
			} else {
				if (target == TypeInfo.STRING) {
					// native numbers are #1-8
					return 9;
				} else if (target == TypeInfo.OBJECT) {
					return 10;
				} else if (ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
					// "double" is #1
					return 2;
				}
			}
		} else if (from instanceof Boolean) {
			// "boolean" is #1
			if (target.isBoolean()) {
				return CONVERSION_TRIVIAL;
			} else if (target == TypeInfo.OBJECT) {
				return 3;
			} else if (target == TypeInfo.STRING) {
				return 4;
			}
		} else if (from instanceof Class || from instanceof NativeJavaClass) {
			if (target.is(TypeInfo.CLASS)) {
				return CONVERSION_EXACT;
			} else if (target == TypeInfo.OBJECT) {
				return 3;
			} else if (target == TypeInfo.STRING) {
				return 4;
			}
		}

		int fromCode = getJSTypeCode(from);

		switch (fromCode) {
			case JSTYPE_JAVA_OBJECT, JSTYPE_JAVA_ARRAY -> {
				Object javaObj = Wrapper.unwrapped(from);
				if (target.asClass().isInstance(javaObj)) {
					return CONVERSION_EXACT;
				} else if (target == TypeInfo.STRING) {
					return 2;
				} else if (target.isPrimitive() && !target.isBoolean()) {
					return (fromCode == JSTYPE_JAVA_ARRAY) ? CONVERSION_NONE : 2 + getSizeRank(target);
				} else if (target instanceof ArrayTypeInfo) {
					return 3;
				} else {
					return internalConversionWeightLast(from, target);
				}
			}
			case JSTYPE_OBJECT -> {
				// Other objects takes #1-#3 spots
				if (target != TypeInfo.OBJECT && target.asClass().isInstance(from)) {
					// No conversion required, but don't apply for java.lang.Object
					return CONVERSION_TRIVIAL;
				}
				if (target instanceof ArrayTypeInfo) {
					if (from instanceof NativeArray) {
						// This is a native array conversion to a java array
						// Array conversions are all equal, and preferable to object
						// and string conversion, per LC3.
						return 2;
					} else {
						return CONVERSION_TRIVIAL;
					}
				} else if (target == TypeInfo.OBJECT) {
					return 3;
				} else if (target == TypeInfo.STRING) {
					return 4;
				} else if (target == TypeInfo.DATE) {
					if (from instanceof NativeDate) {
						// This is a native date to java date conversion
						return CONVERSION_TRIVIAL;
					}
				} else if (target.isFunctionalInterface()) {
					if (from instanceof NativeFunction) {
						// See comments in createInterfaceAdapter
						return CONVERSION_TRIVIAL;
					}
					if (from instanceof NativeObject) {
						return 2;
					}
					return 12;
				} else if (target.isPrimitive() && !target.isBoolean()) {
					return 4 + getSizeRank(target);
				}
			}
		}

		return internalConversionWeightLast(from, target);
	}

	public static int getSizeRank(TypeInfo aType) {
		if (aType.isDouble()) {
			return 1;
		} else if (aType.isFloat()) {
			return 2;
		} else if (aType.isLong()) {
			return 3;
		} else if (aType.isInt()) {
			return 4;
		} else if (aType.isShort()) {
			return 5;
		} else if (aType.isCharacter()) {
			return 6;
		} else if (aType.isByte()) {
			return 7;
		} else if (aType.isBoolean()) {
			return CONVERSION_NONE;
		} else {
			return 8;
		}
	}

	protected Object coerceToNumber(TypeInfo target, Object value) {
		Class<?> valueClass = value.getClass();

		// Character
		if (target.isCharacter()) {
			if (valueClass == ScriptRuntime.CharacterClass) {
				return value;
			}
			return (char) toInteger(value, target, Character.MIN_VALUE, Character.MAX_VALUE);
		}

		// Double, Float
		if (target == TypeInfo.OBJECT || target.isDouble()) {
			return valueClass == ScriptRuntime.DoubleClass ? value : Double.valueOf(toDouble(value));
		}

		if (target.isFloat()) {
			if (valueClass == ScriptRuntime.FloatClass) {
				return value;
			}
			double number = toDouble(value);
			if (Double.isInfinite(number) || Double.isNaN(number) || number == 0.0) {
				return (float) number;
			}

			double absNumber = Math.abs(number);
			if (absNumber < Float.MIN_VALUE) {
				return (number > 0.0) ? +0.0f : -0.0f;
			} else if (absNumber > Float.MAX_VALUE) {
				return (number > 0.0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
			} else {
				return (float) number;
			}
		}

		// Integer, Long, Short, Byte
		if (target.isInt()) {
			if (valueClass == ScriptRuntime.IntegerClass) {
				return value;
			}
			return (int) toInteger(value, target, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}

		if (target.isLong()) {
			if (valueClass == ScriptRuntime.LongClass) {
				return value;
			}
			/* Long values cannot be expressed exactly in doubles.
			 * We thus use the largest and smallest double value that
			 * has a value expressible as a long value. We build these
			 * numerical values from their hexidecimal representations
			 * to avoid any problems caused by attempting to parse a
			 * decimal representation.
			 */
			final double max = Double.longBitsToDouble(0x43dfffffffffffffL);
			final double min = Double.longBitsToDouble(0xc3e0000000000000L);
			return toInteger(value, target, min, max);
		}

		if (target.isShort()) {
			if (valueClass == ScriptRuntime.ShortClass) {
				return value;
			}
			return (short) toInteger(value, target, Short.MIN_VALUE, Short.MAX_VALUE);
		}

		if (target.isByte()) {
			if (valueClass == ScriptRuntime.ByteClass) {
				return value;
			}
			return (byte) toInteger(value, target, Byte.MIN_VALUE, Byte.MAX_VALUE);
		}

		return toDouble(value);
	}

	protected double toDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return ScriptRuntime.toNumber(this, (String) value);
		} else if (value instanceof Scriptable) {
			if (value instanceof Wrapper) {
				// XXX: optimize tail-recursion?
				return toDouble(((Wrapper) value).unwrap());
			}
			return ScriptRuntime.toNumber(this, value);
		} else if (value instanceof DoubleSupplier) {
			return ((DoubleSupplier) value).getAsDouble();
		} else {
			return ScriptRuntime.toNumber(this, value.toString());
		}
	}

	protected long toInteger(Object value, TypeInfo type, double min, double max) {
		double d = toDouble(value);

		if (Double.isInfinite(d) || Double.isNaN(d)) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(this, value), type);
		}

		if (d > 0.0) {
			d = Math.floor(d);
		} else {
			d = Math.ceil(d);
		}

		if (d < min || d > max) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(this, value), type);
		}
		return (long) d;
	}

	public Object reportConversionError(Object value, TypeInfo type) {
		throw Context.reportRuntimeError2("msg.conversion.not.allowed", String.valueOf(value), type.signature(), this);
	}

	public String defaultObjectToSource(Scriptable scope, Scriptable thisObj, Object[] args) {
		return "not_supported";
	}

	public Object[] insertContextArg(Object[] args) {
		if (args.length == 0) {
			return new Object[]{this};
		} else if (!(args[0] instanceof Context)) {
			Object[] newArgs = new Object[args.length + 1];
			newArgs[0] = this;
			System.arraycopy(args, 0, newArgs, 1, args.length);
			return newArgs;
		} else {
			return args;
		}
	}
}

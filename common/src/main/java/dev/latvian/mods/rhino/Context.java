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
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProvider;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProviderHolder;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * This class represents the runtime context of an executing script.
 * <p>
 * Before executing a script, an instance of Context must be created
 * and associated with the thread that will be executing the script.
 * The Context will be used to store information about the executing
 * of the script such as the call stack. Contexts are associated with
 * the current thread  using the {@link #enter()} method.<p>
 * <p>
 * Different forms of script execution are supported. Scripts may be
 * evaluated from the source directly, or first compiled and then later
 * executed. Interactive execution is also supported.<p>
 * <p>
 * Some aspects of script execution, such as type conversions and
 * object creation, may be accessed directly through methods of
 * Context.
 *
 * @author Norris Boyd
 * @author Brendan Eich
 * @see Scriptable
 */

@SuppressWarnings("ThrowableNotThrown")
public class Context {
	public static Context enter() {
		return new Context();
	}

	/**
	 * Report a warning using the error reporter for the current thread.
	 *
	 * @param message    the warning message to report
	 * @param sourceName a string describing the source, such as a filename
	 * @param lineno     the starting line number
	 * @param lineSource the text of the line (may be null)
	 * @param lineOffset the offset into lineSource where problem was detected
	 * @see ErrorReporter
	 */
	public static void reportWarning(Context cx, String message, String sourceName, int lineno, String lineSource, int lineOffset) {
		cx.getErrorReporter().warning(message, sourceName, lineno, lineSource, lineOffset);
	}

	/**
	 * Report a warning using the error reporter for the current thread.
	 *
	 * @param message the warning message to report
	 * @param cx
	 * @see ErrorReporter
	 */
	public static void reportWarning(String message, Context cx) {
		int[] linep = {0};
		String filename = getSourcePositionFromStack(cx, linep);
		Context.reportWarning(cx, message, filename, linep[0], null, 0);
	}

	/**
	 * Report an error using the error reporter for the current thread.
	 *
	 * @param cx
	 * @param message    the error message to report
	 * @param lineno     the starting line number
	 * @param lineSource the text of the line (may be null)
	 * @param lineOffset the offset into lineSource where problem was detected
	 * @param sourceName a string describing the source, such as a filename
	 * @see ErrorReporter
	 */
	public static void reportError(Context cx, String message, int lineno, String lineSource, int lineOffset, String sourceName) {
		if (cx != null) {
			cx.getErrorReporter().error(cx, message, sourceName, lineno, lineSource, lineOffset);
		} else {
			throw new EvaluatorException(cx, message, sourceName, lineno, lineSource, lineOffset);
		}
	}

	/**
	 * Report an error using the error reporter for the current thread.
	 *
	 * @param message the error message to report
	 * @see ErrorReporter
	 */
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
		return Undefined.instance;
	}

	/**
	 * Convenient method to convert java value to its closest representation
	 * in JavaScript.
	 * <p>
	 * If value is an instance of String, Number, Boolean, Function or
	 * Scriptable, it is returned as it and will be treated as the corresponding
	 * JavaScript type of string, number, boolean, function and object.
	 * <p>
	 * Note that for Number instances during any arithmetic operation in
	 * JavaScript the engine will always use the result of
	 * <code>Number.doubleValue()</code> resulting in a precision loss if
	 * the number can not fit into double.
	 * <p>
	 * If value is an instance of Character, it will be converted to string of
	 * length 1 and its JavaScript type will be string.
	 * <p>
	 * The rest of values will be wrapped as LiveConnect objects
	 * by calling {@link Scriptable#getPrototype(Context)} as in:
	 * <pre>
	 *    Context cx = Context.getCurrentContext();
	 *    return cx.getWrapFactory().wrap(cx, scope, value, null);
	 * </pre>
	 *
	 * @param value any Java object
	 * @param scope top scope object
	 * @return value suitable to pass to any API that takes JavaScript values.
	 */
	public static Object javaToJS(Context cx, Object value, Scriptable scope) {
		if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Scriptable) {
			return value;
		} else if (value instanceof Character) {
			return String.valueOf(((Character) value).charValue());
		} else {
			return cx.getWrapFactory().wrap(cx, scope, value, null);
		}
	}

	/**
	 * Convert a JavaScript value into the desired type.
	 * Uses the semantics defined with LiveConnect3 and throws an
	 * Illegal argument exception if the conversion cannot be performed.
	 *
	 * @param value       the JavaScript value to convert
	 * @param desiredType the Java type to convert to. Primitive Java
	 *                    types are represented using the TYPE fields in the corresponding
	 *                    wrapper class in java.lang.
	 * @return the converted value
	 * @throws EvaluatorException if the conversion cannot be performed
	 */
	public static Object jsToJava(Context cx, Object value, Class<?> desiredType) throws EvaluatorException {
		if (desiredType == null) {
			return value;
		}

		return NativeJavaObject.coerceTypeImpl(cx.hasTypeWrappers() ? cx.getTypeWrappers() : null, desiredType, value, cx);
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

	public final Object lock = new Object();

	// Generate an observer count on compiled code
	public boolean generateObserverCount = false;
	private Scriptable topCallScope;
	boolean isContinuationsTopCall;
	NativeCall currentActivationCall;
	BaseFunction typeErrorThrower;
	// for Objects, Arrays to tag themselves as being printed out,
	// so they don't print themselves out recursively.
	// Use ObjToIntMap instead of java.util.HashSet for JDK 1.1 compatibility
	ObjToIntMap iterating;
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
	private int maximumInterpreterStackDepth;
	private Map<Object, Object> threadLocalMap;
	private ClassLoader applicationClassLoader;

	// custom data

	final List<CustomJavaToJsWrapperProviderHolder<?>> customScriptableWrappers = new ArrayList<>();
	final Map<Class<?>, CustomJavaToJsWrapperProvider> customScriptableWrapperCache = new HashMap<>();
	private final Map<String, Object> properties = new HashMap<>();
	TypeWrappers typeWrappers;
	Remapper remapper = DefaultRemapper.INSTANCE;
	private transient Map<Class<?>, JavaMembers> classTable;
	private transient Map<JavaAdapter.JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient Map<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;
	private ClassShutter classShutter;
	private WrapFactory wrapFactory;

	/**
	 * Creates a new context. Provided as a preferred super constructor for
	 * subclasses in place of the deprecated default public constructor.
	 *
	 * @throws IllegalArgumentException if factory parameter is null.
	 */
	protected Context() {
		maximumInterpreterStackDepth = Integer.MAX_VALUE;
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
		NativeObject result = new NativeObject(this);
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

	/**
	 * Returns the maximum stack depth (in terms of number of call frames)
	 * allowed in a single invocation of interpreter. If the set depth would be
	 * exceeded, the interpreter will throw an EvaluatorException in the script.
	 * Defaults to Integer.MAX_VALUE. The setting only has effect for
	 * interpreted functions (those compiled with optimization level set to -1).
	 * As the interpreter doesn't use the Java stack but rather manages its own
	 * stack in the heap memory, a runaway recursion in interpreted code would
	 * eventually consume all available memory and cause OutOfMemoryError
	 * instead of a StackOverflowError limited to only a single thread. This
	 * setting helps prevent such situations.
	 *
	 * @return The current maximum interpreter stack depth.
	 */
	public final int getMaximumInterpreterStackDepth() {
		return maximumInterpreterStackDepth;
	}

	/**
	 * Sets the maximum stack depth (in terms of number of call frames)
	 * allowed in a single invocation of interpreter. If the set depth would be
	 * exceeded, the interpreter will throw an EvaluatorException in the script.
	 * Defaults to Integer.MAX_VALUE. The setting only has effect for
	 * interpreted functions (those compiled with optimization level set to -1).
	 * As the interpreter doesn't use the Java stack but rather manages its own
	 * stack in the heap memory, a runaway recursion in interpreted code would
	 * eventually consume all available memory and cause OutOfMemoryError
	 * instead of a StackOverflowError limited to only a single thread. This
	 * setting helps prevent such situations.
	 *
	 * @param max the new maximum interpreter stack depth
	 * @throws IllegalStateException    if this context's optimization level is not
	 *                                  -1
	 * @throws IllegalArgumentException if the new depth is not at least 1
	 */
	public final void setMaximumInterpreterStackDepth(int max) {
		if (max < 1) {
			throw new IllegalArgumentException("Cannot set maximumInterpreterStackDepth to less than 1");
		}
		maximumInterpreterStackDepth = max;
	}

	//--------------------------
	// Primitive conversion methods
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
	public boolean toBoolean(Object value) {
		return ScriptRuntime.toBoolean(this, value);
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
	public double toNumber(Object value) {
		return ScriptRuntime.toNumber(this, value);
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
	public String toString(Object value) {
		return ScriptRuntime.toString(this, value);
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
	public Scriptable toObject(Object value, Scriptable scope) {
		return ScriptRuntime.toObject(this, scope, value);
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
	 * The default implementation calls
	 * {@link ContextFactory#observeInstructionCount(Context cx,
	 * int instructionCount)}
	 * that allows to customize Context behavior without introducing
	 * Context subclasses.
	 *
	 * @param instructionCount amount of script instruction executed since
	 *                         last call to <code>observeInstructionCount</code>
	 * @throws Error to terminate the script
	 */
	protected void observeInstructionCount(int instructionCount) {
	}

	/********** end of API **********/

	/**
	 * Create class loader for generated classes.
	 * The method calls {@link ContextFactory#createClassLoader(ClassLoader)}
	 * using the result of {@link #getFactory()}.
	 */
	public GeneratedClassLoader createClassLoader(ClassLoader parent) {
		return new DefiningClassLoader(parent);
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
			ScriptableObject.putProperty(scope, name, Context.javaToJS(this, value, scope), this);
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

	public TypeWrappers getTypeWrappers() {
		if (typeWrappers == null) {
			typeWrappers = new TypeWrappers();
		}

		return typeWrappers;
	}

	public boolean hasTypeWrappers() {
		return typeWrappers != null;
	}

	public Remapper getRemapper() {
		return remapper;
	}

	public void setRemapper(Remapper remapper) {
		this.remapper = remapper;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public CustomJavaToJsWrapper wrapCustomJavaToJs(Object javaObject) {
		if (customScriptableWrappers.isEmpty()) {
			return null;
		}

		var provider = customScriptableWrapperCache.get(javaObject.getClass());

		if (provider == null) {
			for (CustomJavaToJsWrapperProviderHolder wrapper : customScriptableWrappers) {
				provider = wrapper.create(javaObject);

				if (provider != null) {
					break;
				}
			}

			if (provider == null) {
				provider = CustomJavaToJsWrapperProvider.NONE;
			}

			customScriptableWrapperCache.put(javaObject.getClass(), provider);
		}

		return provider.create(javaObject);
	}

	public <T> void addCustomJavaToJsWrapper(Predicate<T> predicate, CustomJavaToJsWrapperProvider<T> provider) {
		customScriptableWrappers.add(new CustomJavaToJsWrapperProviderHolder<>(predicate, provider));
	}

	public <T> void addCustomJavaToJsWrapper(Class<T> type, CustomJavaToJsWrapperProvider<T> provider) {
		addCustomJavaToJsWrapper(new CustomJavaToJsWrapperProviderHolder.PredicateFromClass<>(type), provider);
	}

	public void setProperty(String key, @Nullable Object value) {
		if (value == null) {
			properties.remove(key);
		} else {
			properties.put(key, value);
		}
	}

	@Nullable
	public Object getProperty(String key) {
		return properties.get(key);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key, T def) {
		return (T) properties.getOrDefault(key, def);
	}

	@Nullable
	public final synchronized ClassShutter getClassShutter() {
		return classShutter;
	}

	/**
	 * Set the LiveConnect access filter for this context.
	 * <p> {@link ClassShutter} may only be set if it is currently null.
	 * Otherwise a SecurityException is thrown.
	 *
	 * @param shutter a ClassShutter object
	 * @throws SecurityException if there is already a ClassShutter
	 *                           object for this Context
	 */
	public synchronized final void setClassShutter(ClassShutter shutter) {
		if (shutter == null) {
			throw new IllegalArgumentException();
		}

		if (classShutter != null) {
			throw new SecurityException("Cannot overwrite existing " + "ClassShutter object");
		}

		classShutter = shutter;
	}

	/**
	 * Return the current WrapFactory, or null if none is defined.
	 *
	 * @see WrapFactory
	 * @since 1.5 Release 4
	 */
	public final WrapFactory getWrapFactory() {
		if (wrapFactory == null) {
			wrapFactory = new WrapFactory();
		}
		return wrapFactory;
	}

	/**
	 * Set a WrapFactory for this Context.
	 * <p>
	 * The WrapFactory allows custom object wrapping behavior for
	 * Java object manipulated with JavaScript.
	 *
	 * @see WrapFactory
	 * @since 1.5 Release 4
	 */
	public final void setWrapFactory(WrapFactory wrapFactory) {
		if (wrapFactory == null) {
			throw new IllegalArgumentException();
		}
		this.wrapFactory = wrapFactory;
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

			if (result instanceof ConsString) {
				result = result.toString();
			}
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
}
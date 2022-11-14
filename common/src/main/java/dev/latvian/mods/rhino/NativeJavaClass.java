/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * This class reflects Java classes into the JavaScript environment, mainly
 * for constructors and static members.  We lazily reflect properties,
 * and currently do not guarantee that a single j.l.Class is only
 * reflected once into the JS environment, although we should.
 * The only known case where multiple reflections
 * are possible occurs when a j.l.Class is wrapped as part of a
 * method return or property access, rather than by walking the
 * Packages/java tree.
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaObject
 */

public class NativeJavaClass extends NativeJavaObject implements Function {
	// Special property for getting the underlying Java class object.
	static final String javaClassPropertyName = "__javaObject__";

	static Scriptable constructSpecific(Context cx, Scriptable scope, Object[] args, MemberBox ctor) {
		Object instance = constructInternal(cx, scope, args, ctor);
		// we need to force this to be wrapped, because construct _has_
		// to return a scriptable
		Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
		return cx.getWrapFactory().wrapNewObject(topLevel, instance, cx);
	}

	static Object constructInternal(Context cx, Scriptable scope, Object[] args, MemberBox ctor) {
		Class<?>[] argTypes = ctor.argTypes;

		if (ctor.vararg) {
			// marshall the explicit parameter
			Object[] newArgs = new Object[argTypes.length];
			for (int i = 0; i < argTypes.length - 1; i++) {
				newArgs[i] = Context.jsToJava(cx, args[i], argTypes[i]);
			}

			Object varArgs;

			// Handle special situation where a single variable parameter
			// is given and it is a Java or ECMA array.
			if (args.length == argTypes.length && (args[args.length - 1] == null || args[args.length - 1] instanceof NativeArray || args[args.length - 1] instanceof NativeJavaArray)) {
				// convert the ECMA array into a native array
				varArgs = Context.jsToJava(cx, args[args.length - 1], argTypes[argTypes.length - 1]);
			} else {
				// marshall the variable parameter
				Class<?> componentType = argTypes[argTypes.length - 1].getComponentType();
				varArgs = Array.newInstance(componentType, args.length - argTypes.length + 1);
				for (int i = 0; i < Array.getLength(varArgs); i++) {
					Object value = Context.jsToJava(cx, args[argTypes.length - 1 + i], componentType);
					Array.set(varArgs, i, value);
				}
			}

			// add varargs
			newArgs[argTypes.length - 1] = varArgs;
			// replace the original args with the new one
			args = newArgs;
		} else {
			Object[] origArgs = args;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				Object x = Context.jsToJava(cx, arg, argTypes[i]);
				if (x != arg) {
					if (args == origArgs) {
						args = origArgs.clone();
					}
					args[i] = x;
				}
			}
		}

		return ctor.newInstance(args, cx, scope);
	}

	private static Class<?> findNestedClass(Class<?> parentClass, String name) {
		String nestedClassName = parentClass.getName() + '$' + name;
		ClassLoader loader = parentClass.getClassLoader();
		if (loader == null) {
			// ALERT: if loader is null, nested class should be loaded
			// via system class loader which can be different from the
			// loader that brought Rhino classes that Class.forName() would
			// use, but ClassLoader.getSystemClassLoader() is Java 2 only
			return Kit.classOrNull(nestedClassName);
		}
		return Kit.classOrNull(loader, nestedClassName);
	}

	private Map<String, FieldAndMethods> staticFieldAndMethods;

	public NativeJavaClass() {
	}

	public NativeJavaClass(Context cx, Scriptable scope, Class<?> cl) {
		this(cx, scope, cl, false);
	}

	public NativeJavaClass(Context cx, Scriptable scope, Class<?> cl, boolean isAdapter) {
		super(scope, cl, null, isAdapter, cx);
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> cl = (Class<?>) javaObject;
		members = JavaMembers.lookupClass(cx, scope, cl, cl, isAdapter);
		staticFieldAndMethods = members.getFieldAndMethodsObjects(this, cl, true, cx);
	}

	@Override
	public String getClassName() {
		return "JavaClass";
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return members.has(name, true) || javaClassPropertyName.equals(name);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		// When used as a constructor, ScriptRuntime.newObject() asks
		// for our prototype to create an object of the correct type.
		// We don't really care what the object is, since we're returning
		// one constructed out of whole cloth, so we return null.
		if (name.equals("prototype")) {
			return null;
		}

		if (staticFieldAndMethods != null) {
			Object result = staticFieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}

		if (members.has(name, true)) {
			return members.get(this, name, javaObject, true, cx);
		}

		Scriptable scope = ScriptableObject.getTopLevelScope(start);

		if (javaClassPropertyName.equals(name)) {
			return cx.getWrapFactory().wrap(cx, scope, javaObject, ScriptRuntime.ClassClass);
		}

		// experimental:  look for nested classes by appending $name to
		// current class' name.
		Class<?> nestedClass = findNestedClass(getClassObject(), name);
		if (nestedClass != null) {
			Scriptable nestedValue = cx.getWrapFactory().wrapJavaClass(cx, scope, nestedClass);
			nestedValue.setParentScope(this);
			return nestedValue;
		}

		throw members.reportMemberNotFound(name, cx);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		members.put(this, name, javaObject, value, true, cx);
	}

	@Override
	public Object[] getIds(Context cx) {
		return members.getIds(true);
	}

	public Class<?> getClassObject() {
		return (Class<?>) super.unwrap();
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		if (hint == null || hint == ScriptRuntime.StringClass) {
			return this.toString();
		}
		if (hint == ScriptRuntime.BooleanClass) {
			return Boolean.TRUE;
		}
		if (hint == ScriptRuntime.NumberClass) {
			return ScriptRuntime.NaNobj;
		}
		return this;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// If it looks like a "cast" of an object to this class type,
		// walk the prototype chain to see if there's a wrapper of a
		// object that's an instanceof this class.
		if (args.length == 1 && args[0] instanceof Scriptable p) {
			Class<?> c = getClassObject();
			do {
				if (p instanceof Wrapper) {
					Object o = ((Wrapper) p).unwrap();
					if (c.isInstance(o)) {
						return p;
					}
				}
				p = p.getPrototype(cx);
			} while (p != null);
		}
		return construct(cx, scope, args);
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Class<?> classObject = getClassObject();
		int modifiers = classObject.getModifiers();
		if (!(Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))) {
			NativeJavaMethod ctors = members.ctors;
			int index = ctors.findCachedFunction(cx, args);
			if (index < 0) {
				String sig = NativeJavaMethod.scriptSignature(args);
				throw Context.reportRuntimeError2("msg.no.java.ctor", classObject.getName(), sig, cx);
			}

			// Found the constructor, so try invoking it.
			return constructSpecific(cx, scope, args, ctors.methods[index]);
		}
		if (args.length == 0) {
			throw Context.reportRuntimeError0("msg.adapter.zero.args", cx);
		}
		Scriptable topLevel = ScriptableObject.getTopLevelScope(this);
		String msg = "";
		try {
			// When running on Android create an InterfaceAdapter since our
			// bytecode generation won't work on Dalvik VM.
			if ("Dalvik".equals(System.getProperty("java.vm.name")) && classObject.isInterface()) {
				Object obj = createInterfaceAdapter(cx, classObject, ScriptableObject.ensureScriptableObject(args[0], cx));
				return cx.getWrapFactory().wrapAsJavaObject(cx, scope, obj, null);
			}
			// use JavaAdapter to construct a new class on the fly that
			// implements/extends this interface/abstract class.
			Object v = topLevel.get(cx, "JavaAdapter", topLevel);
			if (v != NOT_FOUND) {
				Function f = (Function) v;
				// Args are (interface, js object)
				Object[] adapterArgs = {this, args[0]};
				return f.construct(cx, topLevel, adapterArgs);
			}
		} catch (Exception ex) {
			// fall through to error
			String m = ex.getMessage();
			if (m != null) {
				msg = m;
			}
		}
		throw Context.reportRuntimeError2("msg.cant.instantiate", msg, classObject.getName(), cx);
	}

	@Override
	public String toString() {
		return "[JavaClass " + getClassObject().getName() + "]";
	}

	/**
	 * Determines if prototype is a wrapped Java object and performs
	 * a Java "instanceof".
	 * Exception: if value is an instance of NativeJavaClass, it isn't
	 * considered an instance of the Java class; this forestalls any
	 * name conflicts between java.lang.Class's methods and the
	 * static methods exposed by a JavaNativeClass.
	 */
	@Override
	public boolean hasInstance(Context cx, Scriptable value) {

		if (value instanceof Wrapper && !(value instanceof NativeJavaClass)) {
			Object instance = ((Wrapper) value).unwrap();

			return getClassObject().isInstance(instance);
		}

		// value wasn't something we understand
		return false;
	}
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.DefaultValueTypeHint;
import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.JavaIteratorWrapper;
import org.openjdk.nashorn.internal.runtime.NativeJavaPackage;

import java.util.HashMap;
import java.util.Map;

/**
 * This class reflects non-Array Java objects into the JavaScript environment.  It
 * reflect fields directly, and uses NativeJavaMethod objects to reflect (possibly
 * overloaded) methods.<p>
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */

public class NativeJavaObject implements Scriptable, SymbolScriptable, Wrapper {
	/**
	 * The prototype of this object.
	 */
	protected Scriptable prototype;
	/**
	 * The parent scope of this object.
	 */
	protected Scriptable parent;
	protected transient Object javaObject;
	protected transient Class<?> staticType;
	protected transient JavaMembers members;
	protected transient Map<String, FieldAndMethods> fieldAndMethods;
	protected transient Map<String, Object> customMembers;
	protected transient boolean isAdapter;

	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, Context cx) {
		this(scope, javaObject, staticType, false, cx);
	}

	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, boolean isAdapter, Context cx) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.staticType = staticType;
		this.isAdapter = isAdapter;
		initMembers(cx, scope);
	}

	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> dynamicType;
		if (javaObject != null) {
			dynamicType = javaObject.getClass();
		} else {
			dynamicType = staticType;
		}
		members = JavaMembers.lookupClass(cx, scope, dynamicType, staticType, isAdapter);
		fieldAndMethods = members.getFieldAndMethodsObjects(this, javaObject, false, cx);
		customMembers = null;
	}

	protected void addCustomMember(String name, Object fm) {
		if (customMembers == null) {
			customMembers = new HashMap<>();
		}

		customMembers.put(name, fm);
	}

	protected void addCustomFunction(String name, CustomFunction.Func func, Class<?>... argTypes) {
		addCustomMember(name, new CustomFunction(name, func, argTypes));
	}

	protected void addCustomFunction(String name, CustomFunction.NoArgFunc func) {
		addCustomFunction(name, func, CustomFunction.NO_ARGS);
	}

	public void addCustomProperty(String name, CustomProperty getter) {
		addCustomMember(name, getter);
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return members.has(name, false) || customMembers != null && customMembers.containsKey(name);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return false;
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return javaObject instanceof Iterable<?> && SymbolKey.ITERATOR.equals(key);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}

		if (customMembers != null) {
			Object result = customMembers.get(name);

			if (result != null) {
				if (result instanceof CustomProperty) {
					Object r = ((CustomProperty) result).get(cx);

					if (r == null) {
						return Undefined.INSTANCE;
					}

					Object r1 = cx.wrap(this, r, r.getClass());

					if (r1 instanceof Scriptable) {
						return ((Scriptable) r1).getDefaultValue(cx, null);
					}

					return r1;
				}

				return result;
			}
		}

		// TODO: passing 'this' as the scope is bogus since it has
		//  no parent scope
		return members.get(this, name, javaObject, false, cx);
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (javaObject instanceof Iterable<?> itr && SymbolKey.ITERATOR.equals(key)) {
			return new JavaIteratorWrapper(itr.iterator());
		}

		// Native Java objects have no Symbol members
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		throw members.reportMemberNotFound(Integer.toString(index), cx);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false, cx);
		} else {
			prototype.put(cx, name, prototype, value);
		}
	}

	@Override
	public void put(Context cx, Symbol symbol, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		String name = symbol.toString();
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false, cx);
		} else if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).put(cx, symbol, prototype, value);
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		throw members.reportMemberNotFound(Integer.toString(index), cx);
	}

	@Override
	public boolean hasInstance(Context cx, Scriptable value) {
		// This is an instance of a Java class, so always return false
		return false;
	}

	@Override
	public void delete(Context cx, String name) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		if (customMembers != null) {
			Object result = customMembers.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		Deletable.deleteObject(members.get(this, name, javaObject, false, cx));
	}

	@Override
	public void delete(Context cx, Symbol key) {
	}

	@Override
	public void delete(Context cx, int index) {
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		if (prototype == null && javaObject instanceof String) {
			return TopLevel.getBuiltinPrototype(ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.String, cx);
		}
		return prototype;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Scriptable m) {
		prototype = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parent;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parent = m;
	}

	@Override
	public Object[] getIds(Context cx) {
		if (customMembers != null) {
			Object[] c = customMembers.keySet().toArray(ScriptRuntime.EMPTY_OBJECTS);
			Object[] m = members.getIds(false);
			Object[] result = new Object[c.length + m.length];
			System.arraycopy(c, 0, result, 0, c.length);
			System.arraycopy(m, 0, result, c.length, m.length);
			return result;
		}

		return members.getIds(false);
	}

	@Override
	public Object unwrap() {
		return javaObject;
	}

	@Override
	public String getClassName() {
		return "JavaObject";
	}

	@Override
	public Object getDefaultValue(Context cx, DefaultValueTypeHint hint) {
		Object value;
		if (hint == null) {
			if (javaObject instanceof Boolean) {
				hint = DefaultValueTypeHint.BOOLEAN;
			}
			if (javaObject instanceof Number) {
				hint = DefaultValueTypeHint.NUMBER;
			}
		}
		if (hint == null || hint == DefaultValueTypeHint.STRING) {
			value = javaObject.toString();
		} else {
			String converterName;
			if (hint == DefaultValueTypeHint.BOOLEAN) {
				converterName = "booleanValue";
			} else if (hint == DefaultValueTypeHint.NUMBER) {
				converterName = "doubleValue";
			} else {
				throw Context.reportRuntimeError0("msg.default.value", cx);
			}
			Object converterObject = get(cx, converterName, this);
			if (converterObject instanceof Function f) {
				value = f.call(cx, f.getParentScope(), this, ScriptRuntime.EMPTY_OBJECTS);
			} else {
				if (hint == DefaultValueTypeHint.NUMBER && javaObject instanceof Boolean) {
					boolean b = (Boolean) javaObject;
					value = b ? ScriptRuntime.wrapNumber(1.0) : ScriptRuntime.zeroObj;
				} else {
					value = javaObject.toString();
				}
			}
		}
		return value;
	}
}

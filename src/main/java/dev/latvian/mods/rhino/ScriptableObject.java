/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.DefaultValueTypeHint;
import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.WrappedReflectionMethod;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the default implementation of the Scriptable interface. This
 * class provides convenient default behavior that makes it easier to
 * define host objects.
 * <p>
 * Various properties and methods of JavaScript objects can be conveniently
 * defined using methods of ScriptableObject.
 * <p>
 * Classes extending ScriptableObject must define the getClassName method.
 *
 * @author Norris Boyd
 * @see Scriptable
 */

public abstract class ScriptableObject implements Scriptable, SymbolScriptable, ConstProperties {
	/**
	 * The empty property attribute.
	 * <p>
	 * Used by getAttributes() and setAttributes().
	 *
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#checkNotSealed(Context, Object, int)
	 */
	public static final int EMPTY = 0x00;

	/**
	 * Property attribute indicating assignment to this property is ignored.
	 *
	 * @see ScriptableObject
	 * #put(String, Scriptable, Object)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#checkNotSealed(Context, Object, int)
	 */
	public static final int READONLY = 0x01;

	/**
	 * Property attribute indicating property is not enumerated.
	 * <p>
	 * Only enumerated properties will be returned by getIds().
	 *
	 * @see Scriptable#getIds(Context)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#checkNotSealed(Context, Object, int)
	 */
	public static final int DONTENUM = 0x02;

	/**
	 * Property attribute indicating property cannot be deleted.
	 *
	 * @see Scriptable#get(Context, String, Scriptable)
	 * @see ScriptableObject#getAttributes(Context, String)
	 * @see ScriptableObject#checkNotSealed(Context, Object, int)
	 */
	public static final int PERMANENT = 0x04;

	/**
	 * Property attribute indicating that this is a const property that has not
	 * been assigned yet.  The first 'const' assignment to the property will
	 * clear this bit.
	 */
	public static final int UNINITIALIZED_CONST = 0x08;

	public static final int CONST = PERMANENT | READONLY | UNINITIALIZED_CONST;
	private static final WrappedExecutable GET_ARRAY_LENGTH = (cx, scope, self, args) -> ((ScriptableObject) self).getExternalArrayLength();
	private static final Comparator<Object> KEY_COMPARATOR = new KeyComparator();

	/**
	 * This is the object that is stored in the SlotMap. For historical reasons it remains
	 * inside this class. SlotMap references a number of members of this class directly.
	 */
	static class Slot {
		Object name; // This can change due to caching
		int indexOrHash;
		Object value;
		transient Slot next; // next in hash table bucket
		transient Slot orderedNext; // next in linked list
		private short attributes;

		Slot(Object name, int indexOrHash, int attributes) {
			this.name = name;
			this.indexOrHash = indexOrHash;
			this.attributes = (short) attributes;
		}

		boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx) {
			if ((attributes & READONLY) != 0) {
				if (cx.isStrictMode()) {
					throw ScriptRuntime.typeError1(cx, "msg.modify.readonly", name);
				}
				return true;
			}
			if (owner == start) {
				this.value = value;
				return true;
			}
			return false;
		}

		Object getValue(Scriptable start, Context cx) {
			return value;
		}

		int getAttributes() {
			return attributes;
		}

		synchronized void setAttributes(int value) {
			checkValidAttributes(value);
			attributes = (short) value;
		}

		ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
			return buildDataDescriptor(scope, value, attributes, cx);
		}

	}

	/**
	 * A GetterSlot is a specialication of a Slot for properties that are assigned functions
	 * via Object.defineProperty() and its friends instead of regular values.
	 */
	static final class GetterSlot extends Slot {
		Object getter;
		Object setter;

		GetterSlot(Object name, int indexOrHash, int attributes) {
			super(name, indexOrHash, attributes);
		}

		@Override
		ScriptableObject getPropertyDescriptor(Context cx, Scriptable scope) {
			int attr = getAttributes();
			ScriptableObject desc = new NativeObject(cx.factory);
			ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
			desc.defineProperty(cx, "enumerable", (attr & DONTENUM) == 0, EMPTY);
			desc.defineProperty(cx, "configurable", (attr & PERMANENT) == 0, EMPTY);
			if (getter == null && setter == null) {
				desc.defineProperty(cx, "writable", (attr & READONLY) == 0, EMPTY);
			}

			String fName = name == null ? "f" : name.toString();
			if (getter != null) {
				if (getter instanceof MemberBox box) {
					desc.defineProperty(cx, "get", new FunctionObject(fName, box.executableInfo, scope, cx), EMPTY);
				} else if (getter instanceof CachedExecutableInfo ex) {
					desc.defineProperty(cx, "get", new FunctionObject(fName, ex, scope, cx), EMPTY);
				} else {
					desc.defineProperty(cx, "get", getter, EMPTY);
				}
			}
			if (setter != null) {
				if (setter instanceof MemberBox box) {
					desc.defineProperty(cx, "set", new FunctionObject(fName, box.executableInfo, scope, cx), EMPTY);
				} else if (setter instanceof CachedExecutableInfo ex) {
					desc.defineProperty(cx, "set", new FunctionObject(fName, ex, scope, cx), EMPTY);
				} else {
					desc.defineProperty(cx, "set", setter, EMPTY);
				}
			}
			return desc;
		}

		@Override
		boolean setValue(Object value, Scriptable owner, Scriptable start, Context cx) {
			if (setter == null) {
				if (getter != null) {
					// Based on TC39 ES3.1 Draft of 9-Feb-2009, 8.12.4, step 2,
					// we should throw a TypeError in this case.
					if (cx.isStrictMode()) {
						String prop = "";
						if (name != null) {
							prop = "[" + start.getClassName() + "]." + name;
						}
						throw ScriptRuntime.typeError2(cx, "msg.set.prop.no.setter", prop, ScriptRuntime.toString(cx, value));
					}
					// Assignment to a property with only a getter defined. The
					// assignment is ignored. See bug 478047.
					return true;
				}
			} else {
				if (setter instanceof MemberBox nativeSetter) {
					var pTypes = nativeSetter.parameters().types();
					// XXX: cache tag since it is already calculated in
					// defineProperty ?
					Class<?> valueType = pTypes.getLast();
					int tag = FunctionObject.getTypeTag(valueType);
					Object actualArg = FunctionObject.convertArg(cx, start, value, tag);
					Object setterThis;
					Object[] args;
					if (nativeSetter.delegateTo == null) {
						setterThis = start;
						args = new Object[]{actualArg};
					} else {
						setterThis = nativeSetter.delegateTo;
						args = new Object[]{cx, start, actualArg};
					}
					nativeSetter.invoke(setterThis, args, cx, start);
				} else if (setter instanceof Function f) {
					f.call(cx, f.getParentScope(), start, new Object[]{value});
				}
				return true;
			}
			return super.setValue(value, owner, start, cx);
		}

		@Override
		Object getValue(Scriptable start, Context cx) {
			if (getter != null) {
				if (getter instanceof MemberBox nativeGetter) {
					Object getterThis;
					Object[] args;
					if (nativeGetter.delegateTo == null) {
						getterThis = start;
						args = ScriptRuntime.EMPTY_OBJECTS;
					} else {
						getterThis = nativeGetter.delegateTo;
						args = new Object[]{cx, start};
					}
					return nativeGetter.invoke(getterThis, args, cx, start);
				} else if (getter instanceof Function f) {
					return f.call(cx, f.getParentScope(), start, ScriptRuntime.EMPTY_OBJECTS);
				}
			}
			return this.value;
		}
	}

	/**
	 * This comparator sorts property fields in spec-compliant order. Numeric ids first, in numeric
	 * order, followed by string ids, in insertion order. Since this class already keeps string keys
	 * in insertion-time order, we treat all as equal. The "Arrays.sort" method will then not
	 * change their order, but simply move all the numeric properties to the front, since this
	 * method is defined to be stable.
	 */
	public static final class KeyComparator implements Comparator<Object>, Serializable {
		@Serial
		private static final long serialVersionUID = 6411335891523988149L;

		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof Integer) {
				if (o2 instanceof Integer) {
					int i1 = (Integer) o1;
					int i2 = (Integer) o2;
					if (i1 < i2) {
						return -1;
					}
					if (i1 > i2) {
						return 1;
					}
					return 0;
				}
				return -1;
			}
			if (o2 instanceof Integer) {
				return 1;
			}
			return 0;
		}
	}

	protected static ScriptableObject buildDataDescriptor(Scriptable scope, Object value, int attributes, Context cx) {
		ScriptableObject desc = new NativeObject(cx.factory);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, desc, TopLevel.Builtins.Object);
		desc.defineProperty(cx, "value", value, EMPTY);
		desc.defineProperty(cx, "writable", (attributes & READONLY) == 0, EMPTY);
		desc.defineProperty(cx, "enumerable", (attributes & DONTENUM) == 0, EMPTY);
		desc.defineProperty(cx, "configurable", (attributes & PERMANENT) == 0, EMPTY);
		return desc;
	}

	static void checkValidAttributes(int attributes) {
		final int mask = READONLY | DONTENUM | PERMANENT | UNINITIALIZED_CONST;
		if ((attributes & ~mask) != 0) {
			throw new IllegalArgumentException(String.valueOf(attributes));
		}
	}

	private static SlotMapContainer createSlotMap(int initialSize) {
		return new SlotMapContainer(initialSize);
	}

	public static Object getDefaultValue(Scriptable object, DefaultValueTypeHint typeHint, Context cx) {
		for (int i = 0; i < 2; i++) {
			boolean tryToString;
			if (typeHint == DefaultValueTypeHint.STRING) {
				tryToString = (i == 0);
			} else {
				tryToString = (i == 1);
			}

			String methodName;
			if (tryToString) {
				methodName = "toString";
			} else {
				methodName = "valueOf";
			}
			Object v = getProperty(object, methodName, cx);
			if (!(v instanceof Function fun)) {
				continue;
			}
			v = fun.call(cx, fun.getParentScope(), object, ScriptRuntime.EMPTY_OBJECTS);
			if (v != null) {
				if (!(v instanceof Scriptable)) {
					return v;
				}
				if (typeHint == DefaultValueTypeHint.CLASS || typeHint == DefaultValueTypeHint.FUNCTION) {
					return v;
				}
				if (tryToString && v instanceof Wrapper) {
					// Let a wrapped java.lang.String pass for a primitive
					// string.
					Object u = ((Wrapper) v).unwrap();
					if (u instanceof String) {
						return u;
					}
				}
			}
		}
		// fall through to error
		throw ScriptRuntime.typeError1(cx, "msg.default.value", (typeHint == null ? "undefined" : typeHint.name));
	}

	@SuppressWarnings({"unchecked"})
	private static <T extends Scriptable> Class<T> extendsScriptable(Class<?> c) {
		if (ScriptRuntime.ScriptableClass.isAssignableFrom(c)) {
			return (Class<T>) c;
		}
		return null;
	}

	/**
	 * Utility method to add properties to arbitrary Scriptable object.
	 * If destination is instance of ScriptableObject, calls
	 * defineProperty there, otherwise calls put in destination
	 * ignoring attributes
	 *
	 * @param destination  ScriptableObject to define the property on
	 * @param propertyName the name of the property to define.
	 * @param value        the initial value of the property
	 * @param attributes   the attributes of the JavaScript property
	 */
	public static void defineProperty(Scriptable destination, String propertyName, Object value, int attributes, Context cx) {
		if (!(destination instanceof ScriptableObject so)) {
			destination.put(cx, propertyName, destination, value);
			return;
		}
		so.defineProperty(cx, propertyName, value, attributes);
	}

	/**
	 * Utility method to add properties to arbitrary Scriptable object.
	 * If destination is instance of ScriptableObject, calls
	 * defineProperty there, otherwise calls put in destination
	 * ignoring attributes
	 *
	 * @param destination  ScriptableObject to define the property on
	 * @param propertyName the name of the property to define.
	 */
	public static void defineConstProperty(Scriptable destination, String propertyName, Context cx) {
		if (destination instanceof ConstProperties cp) {
			cp.defineConst(cx, propertyName, destination);
		} else {
			defineProperty(destination, propertyName, Undefined.INSTANCE, CONST, cx);
		}
	}

	protected static boolean isTrue(Object value, Context cx) {
		return (value != NOT_FOUND) && ScriptRuntime.toBoolean(cx, value);
	}

	protected static boolean isFalse(Object value, Context cx) {
		return !isTrue(value, cx);
	}

	protected static Scriptable ensureScriptable(Object arg, Context cx) {
		if (!(arg instanceof Scriptable)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, arg));
		}
		return (Scriptable) arg;
	}

	protected static SymbolScriptable ensureSymbolScriptable(Object arg, Context cx) {
		if (!(arg instanceof SymbolScriptable)) {
			throw ScriptRuntime.typeError1(cx, "msg.object.not.symbolscriptable", ScriptRuntime.typeof(cx, arg));
		}
		return (SymbolScriptable) arg;
	}

	protected static ScriptableObject ensureScriptableObject(Object arg, Context cx) {
		if (!(arg instanceof ScriptableObject)) {
			throw ScriptRuntime.typeError1(cx, "msg.arg.not.object", ScriptRuntime.typeof(cx, arg));
		}
		return (ScriptableObject) arg;
	}

	/**
	 * Get the Object.prototype property.
	 * See ECMA 15.2.4.
	 *
	 * @param scope an object in the scope chain
	 */
	public static Scriptable getObjectPrototype(Scriptable scope, Context cx) {
		return TopLevel.getBuiltinPrototype(getTopLevelScope(scope), TopLevel.Builtins.Object, cx);
	}

	/**
	 * Get the Function.prototype property.
	 * See ECMA 15.3.4.
	 *
	 * @param scope an object in the scope chain
	 */
	public static Scriptable getFunctionPrototype(Scriptable scope, Context cx) {
		return TopLevel.getBuiltinPrototype(getTopLevelScope(scope), TopLevel.Builtins.Function, cx);
	}

	public static Scriptable getGeneratorFunctionPrototype(Scriptable scope, Context cx) {
		return TopLevel.getBuiltinPrototype(getTopLevelScope(scope), TopLevel.Builtins.GeneratorFunction, cx);
	}

	public static Scriptable getArrayPrototype(Scriptable scope, Context cx) {
		return TopLevel.getBuiltinPrototype(getTopLevelScope(scope), TopLevel.Builtins.Array, cx);
	}

	/**
	 * Get the prototype for the named class.
	 * <p>
	 * For example, <code>getClassPrototype(s, "Date")</code> will first
	 * walk up the parent chain to find the outermost scope, then will
	 * search that scope for the Date constructor, and then will
	 * return Date.prototype. If any of the lookups fail, or
	 * the prototype is not a JavaScript object, then null will
	 * be returned.
	 *
	 * @param scope     an object in the scope chain
	 * @param className the name of the constructor
	 * @param cx
	 * @return the prototype for the named class, or null if it
	 * cannot be found.
	 */
	public static Scriptable getClassPrototype(Scriptable scope, String className, Context cx) {
		scope = getTopLevelScope(scope);
		Object ctor = getProperty(scope, className, cx);
		Object proto;
		if (ctor instanceof BaseFunction) {
			proto = ((BaseFunction) ctor).getPrototypeProperty(cx);
		} else if (ctor instanceof Scriptable ctorObj) {
			proto = ctorObj.get(cx, "prototype", ctorObj);
		} else {
			return null;
		}
		if (proto instanceof Scriptable) {
			return (Scriptable) proto;
		}
		return null;
	}

	/**
	 * Get the global scope.
	 *
	 * <p>Walks the parent scope chain to find an object with a null
	 * parent scope (the global object).
	 *
	 * @param obj a JavaScript object
	 * @return the corresponding global scope
	 */
	public static Scriptable getTopLevelScope(Scriptable obj) {
		for (; ; ) {
			Scriptable parent = obj.getParentScope();
			if (parent == null) {
				return obj;
			}
			obj = parent;
		}
	}

	/**
	 * Gets a named property from an object or any object in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property named <code>name</code>.
	 * <p>
	 *
	 * @param obj  a JavaScript object
	 * @param name a property name
	 * @return the value of a property with name <code>name</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * <code>Scriptable.NOT_FOUND</code> if not found
	 * @since 1.5R2
	 */
	public static Object getProperty(Scriptable obj, String name, Context cx) {
		Scriptable start = obj;
		Object result;
		do {
			result = obj.get(cx, name, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * This is a version of getProperty that works with Symbols.
	 */
	public static Object getProperty(Scriptable obj, Symbol key, Context cx) {
		Scriptable start = obj;
		Object result;
		do {
			result = ensureSymbolScriptable(obj, cx).get(cx, key, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * Gets an indexed property from an object or any object in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property with integral index
	 * <code>index</code>. Note that if you wish to look for properties with numerical
	 * but non-integral indicies, you should use getProperty(Scriptable,String) with
	 * the string value of the index.
	 * <p>
	 *
	 * @param obj   a JavaScript object
	 * @param index an integral index
	 * @return the value of a property with index <code>index</code> found in
	 * <code>obj</code> or any object in its prototype chain, or
	 * <code>Scriptable.NOT_FOUND</code> if not found
	 * @since 1.5R2
	 */
	public static Object getProperty(Scriptable obj, int index, Context cx) {
		Scriptable start = obj;
		Object result;
		do {
			result = obj.get(cx, index, start);
			if (result != NOT_FOUND) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return result;
	}

	/**
	 * Returns whether a named property is defined in an object or any object
	 * in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property named <code>name</code>.
	 * <p>
	 *
	 * @param obj  a JavaScript object
	 * @param name a property name
	 * @return the true if property was found
	 * @since 1.5R2
	 */
	public static boolean hasProperty(Scriptable obj, String name, Context cx) {
		return null != getBase(obj, name, cx);
	}

	/**
	 * If hasProperty(obj, name) would return true, then if the property that
	 * was found is compatible with the new property, this method just returns.
	 * If the property is not compatible, then an exception is thrown.
	 * <p>
	 * A property redefinition is incompatible if the first definition was a
	 * const declaration or if this one is.  They are compatible only if neither
	 * was const.
	 */
	public static void redefineProperty(Scriptable obj, String name, boolean isConst, Context cx) {
		Scriptable base = getBase(obj, name, cx);
		if (base == null) {
			return;
		}
		if (base instanceof ConstProperties cp) {

			if (cp.isConst(name)) {
				throw ScriptRuntime.typeError1(cx, "msg.const.redecl", name);
			}
		}
		if (isConst) {
			throw ScriptRuntime.typeError1(cx, "msg.var.redecl", name);
		}
	}

	/**
	 * Returns whether an indexed property is defined in an object or any object
	 * in its prototype chain.
	 * <p>
	 * Searches the prototype chain for a property with index <code>index</code>.
	 * <p>
	 *
	 * @param obj   a JavaScript object
	 * @param index a property index
	 * @return the true if property was found
	 * @since 1.5R2
	 */
	public static boolean hasProperty(Scriptable obj, int index, Context cx) {
		return null != getBase(cx, obj, index);
	}

	/**
	 * A version of hasProperty for properties with Symbol keys.
	 */
	public static boolean hasProperty(Scriptable obj, Symbol key, Context cx) {
		return null != getBase(cx, obj, key);
	}

	/**
	 * Puts a named property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the named property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#get(Context, String, Scriptable)} on the
	 * prototype passing <code>obj</code> as the <code>start</code> argument.
	 * This allows the prototype to veto the property setting in case the
	 * prototype defines the property with [[ReadOnly]] attribute. If the
	 * property is not found, it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param name  a property name
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putProperty(Scriptable obj, String name, Object value, Context cx) {
		Scriptable base = getBase(obj, name, cx);
		if (base == null) {
			base = obj;
		}
		base.put(cx, name, obj, value);
	}

	/**
	 * This is a version of putProperty for Symbol keys.
	 */
	public static void putProperty(Scriptable obj, Symbol key, Object value, Context cx) {
		Scriptable base = getBase(cx, obj, key);
		if (base == null) {
			base = obj;
		}
		ensureSymbolScriptable(base, cx).put(cx, key, obj, value);
	}

	/**
	 * Puts a named property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the named property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#get(Context, String, Scriptable)} on the
	 * prototype passing <code>obj</code> as the <code>start</code> argument.
	 * This allows the prototype to veto the property setting in case the
	 * prototype defines the property with [[ReadOnly]] attribute. If the
	 * property is not found, it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param name  a property name
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putConstProperty(Scriptable obj, String name, Object value, Context cx) {
		Scriptable base = getBase(obj, name, cx);
		if (base == null) {
			base = obj;
		}
		if (base instanceof ConstProperties) {
			((ConstProperties) base).putConst(cx, name, obj, value);
		}
	}

	/**
	 * Puts an indexed property in an object or in an object in its prototype chain.
	 * <p>
	 * Searches for the indexed property in the prototype chain. If it is found,
	 * the value of the property in <code>obj</code> is changed through a call
	 * to {@link Scriptable#get(Context, int, Scriptable)} on the prototype
	 * passing <code>obj</code> as the <code>start</code> argument. This allows
	 * the prototype to veto the property setting in case the prototype defines
	 * the property with [[ReadOnly]] attribute. If the property is not found,
	 * it is added in <code>obj</code>.
	 *
	 * @param obj   a JavaScript object
	 * @param index a property index
	 * @param value any JavaScript value accepted by Scriptable.put
	 * @since 1.5R2
	 */
	public static void putProperty(Scriptable obj, int index, Object value, Context cx) {
		Scriptable base = getBase(cx, obj, index);
		if (base == null) {
			base = obj;
		}
		base.put(cx, index, obj, value);
	}

	/**
	 * Removes the property from an object or its prototype chain.
	 * <p>
	 * Searches for a property with <code>name</code> in obj or
	 * its prototype chain. If it is found, the object's delete
	 * method is called.
	 *
	 * @param obj  a JavaScript object
	 * @param name a property name
	 * @return true if the property doesn't exist or was successfully removed
	 * @since 1.5R2
	 */
	public static boolean deleteProperty(Scriptable obj, String name, Context cx) {
		Scriptable base = getBase(obj, name, cx);
		if (base == null) {
			return true;
		}
		base.delete(cx, name);
		return !base.has(cx, name, obj);
	}

	/**
	 * Removes the property from an object or its prototype chain.
	 * <p>
	 * Searches for a property with <code>index</code> in obj or
	 * its prototype chain. If it is found, the object's delete
	 * method is called.
	 *
	 * @param obj   a JavaScript object
	 * @param index a property index
	 * @return true if the property doesn't exist or was successfully removed
	 * @since 1.5R2
	 */
	public static boolean deleteProperty(Scriptable obj, int index, Context cx) {
		Scriptable base = getBase(cx, obj, index);
		if (base == null) {
			return true;
		}
		base.delete(cx, index);
		return !base.has(cx, index, obj);
	}

	/**
	 * Returns an array of all ids from an object and its prototypes.
	 * <p>
	 *
	 * @param cx
	 * @param obj a JavaScript object
	 * @return an array of all ids from all object in the prototype chain.
	 * If a given id occurs multiple times in the prototype chain,
	 * it will occur only once in this list.
	 * @since 1.5R2
	 */
	public static Object[] getPropertyIds(Context cx, Scriptable obj) {
		if (obj == null) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}
		Object[] result = obj.getIds(cx);
		ObjToIntMap map = null;
		for (; ; ) {
			obj = obj.getPrototype(cx);
			if (obj == null) {
				break;
			}
			Object[] ids = obj.getIds(cx);
			if (ids.length == 0) {
				continue;
			}
			if (map == null) {
				if (result.length == 0) {
					result = ids;
					continue;
				}
				map = new ObjToIntMap(result.length + ids.length);
				for (int i = 0; i != result.length; ++i) {
					map.intern(result[i]);
				}
				result = null; // Allow to GC the result
			}
			for (int i = 0; i != ids.length; ++i) {
				map.intern(ids[i]);
			}
		}
		if (map != null) {
			result = map.getKeys();
		}
		return result;
	}

	private static Scriptable getBase(Scriptable obj, String name, Context cx) {
		do {
			if (obj.has(cx, name, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	private static Scriptable getBase(Context cx, Scriptable obj, int index) {
		do {
			if (obj.has(cx, index, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	private static Scriptable getBase(Context cx, Scriptable obj, Symbol key) {
		do {
			if (ensureSymbolScriptable(obj, cx).has(cx, key, obj)) {
				break;
			}
			obj = obj.getPrototype(cx);
		} while (obj != null);
		return obj;
	}

	/**
	 * Get arbitrary application-specific value associated with the top scope
	 * of the given scope.
	 * The method first calls {@link #getTopLevelScope(Scriptable scope)}
	 * and then searches the prototype chain of the top scope for the first
	 * object containing the associated value with the given key.
	 *
	 * @param scope the starting scope.
	 * @param key   key object to select particular value.
	 * @see #getAssociatedValue(Object key)
	 */
	public static Object getTopScopeValue(Scriptable scope, Object key, Context cx) {
		scope = ScriptableObject.getTopLevelScope(scope);
		for (; ; ) {
			if (scope instanceof ScriptableObject so) {
				Object value = so.getAssociatedValue(key);
				if (value != null) {
					return value;
				}
			}
			scope = scope.getPrototype(cx);
			if (scope == null) {
				return null;
			}
		}
	}

	/**
	 * This holds all the slots. It may or may not be thread-safe, and may expand itself to
	 * a different data structure depending on the size of the object.
	 */
	private final transient SlotMapContainer slotMap;
	/**
	 * The prototype of this object.
	 */
	private Scriptable prototypeObject;
	/**
	 * The parent scope of this object.
	 */
	private Scriptable parentScopeObject;
	// Where external array data is stored.
	private transient ExternalArrayData externalData;
	private volatile Map<Object, Object> associatedValues;
	private boolean isExtensible = true;
	private boolean isSealed = false;

	public ScriptableObject() {
		slotMap = createSlotMap(0);
	}

	public ScriptableObject(Scriptable scope, Scriptable prototype) {
		if (scope == null) {
			throw new IllegalArgumentException();
		}

		parentScopeObject = scope;
		prototypeObject = prototype;
		slotMap = createSlotMap(0);
	}

	/**
	 * Gets the value that will be returned by calling the typeof operator on this object.
	 *
	 * @return default is "object" unless {@link #avoidObjectDetection()} is <code>true</code> in which
	 * case it returns "undefined"
	 */
	@Override
	public MemberType getTypeOf() {
		return avoidObjectDetection() ? MemberType.UNDEFINED : MemberType.OBJECT;
	}

	/**
	 * Return the name of the class.
	 * <p>
	 * This is typically the same name as the constructor.
	 * Classes extending ScriptableObject must implement this abstract
	 * method.
	 */
	@Override
	public abstract String getClassName();

	/**
	 * Returns true if the named property is defined.
	 *
	 * @param name  the name of the property
	 * @param start the object in which the lookup began
	 * @return true if and only if the property was found in the object
	 */
	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return null != slotMap.query(name, 0);
	}

	/**
	 * Returns true if the property index is defined.
	 *
	 * @param index the numeric index for the property
	 * @param start the object in which the lookup began
	 * @return true if and only if the property was found in the object
	 */
	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (externalData != null) {
			return (index < externalData.getArrayLength());
		}
		return null != slotMap.query(null, index);
	}

	/**
	 * A version of "has" that supports symbols.
	 */
	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return null != slotMap.query(key, 0);
	}

	/**
	 * Returns the value of the named property or NOT_FOUND.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate getter method is called.
	 *
	 * @param cx
	 * @param name  the name of the property
	 * @param start the object in which the lookup began
	 * @return the value of the property (may be null), or NOT_FOUND
	 */
	@Override
	public Object get(Context cx, String name, Scriptable start) {
		Slot slot = slotMap.query(name, 0);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(start, cx);
	}

	/**
	 * Returns the value of the indexed property or NOT_FOUND.
	 *
	 * @param cx
	 * @param index the numeric index for the property
	 * @param start the object in which the lookup began
	 * @return the value of the property (may be null), or NOT_FOUND
	 */
	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (externalData != null) {
			if (index < externalData.getArrayLength()) {
				return externalData.getArrayElement(index);
			}
			return NOT_FOUND;
		}

		Slot slot = slotMap.query(null, index);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(start, cx);
	}

	/**
	 * Another version of Get that supports Symbol keyed properties.
	 */
	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		Slot slot = slotMap.query(key, 0);
		if (slot == null) {
			return NOT_FOUND;
		}
		return slot.getValue(start, cx);
	}

	/**
	 * Sets the value of the named property, creating it if need be.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate setter method is called. <p>
	 * <p>
	 * If the property's attributes include READONLY, no action is
	 * taken.
	 * This method will actually set the property in the start
	 * object.
	 *
	 * @param name  the name of the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		if (putImpl(cx, name, 0, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		start.put(cx, name, start, value);
	}

	/**
	 * Sets the value of the indexed property, creating it if need be.
	 *
	 * @param index the numeric index for the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		if (externalData != null) {
			if (index < externalData.getArrayLength()) {
				externalData.setArrayElement(index, value);
			} else {
				throw new JavaScriptException(cx, ScriptRuntime.newNativeError(cx, this, TopLevel.NativeErrors.RangeError, new Object[]{"External array index out of bounds "}), null, 0);
			}
			return;
		}

		if (putImpl(cx, null, index, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		start.put(cx, index, start, value);
	}

	/**
	 * Implementation of put required by SymbolScriptable objects.
	 */
	@Override
	public void put(Context cx, Symbol key, Scriptable start, Object value) {
		if (putImpl(cx, key, 0, start, value)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		ensureSymbolScriptable(start, cx).put(cx, key, start, value);
	}

	/**
	 * Removes a named property from the object.
	 * <p>
	 * If the property is not found, or it has the PERMANENT attribute,
	 * no action is taken.
	 *
	 * @param name the name of the property
	 */
	@Override
	public void delete(Context cx, String name) {
		checkNotSealed(cx, name, 0);
		Slot s = slotMap.query(name, 0);
		slotMap.remove(name, 0, cx);
		Deletable.deleteObject(s == null ? null : s.value);
	}

	/**
	 * Removes the indexed property from the object.
	 * <p>
	 * If the property is not found, or it has the PERMANENT attribute,
	 * no action is taken.
	 *
	 * @param index the numeric index for the property
	 */
	@Override
	public void delete(Context cx, int index) {
		checkNotSealed(cx, null, index);
		Slot s = slotMap.query(null, index);
		slotMap.remove(null, index, cx);
		Deletable.deleteObject(s == null ? null : s.value);
	}

	/**
	 * Removes an object like the others, but using a Symbol as the key.
	 */
	@Override
	public void delete(Context cx, Symbol key) {
		checkNotSealed(cx, key, 0);
		slotMap.remove(key, 0, cx);
	}

	/**
	 * Sets the value of the named const property, creating it if need be.
	 * <p>
	 * If the property was created using defineProperty, the
	 * appropriate setter method is called. <p>
	 * <p>
	 * If the property's attributes include READONLY, no action is
	 * taken.
	 * This method will actually set the property in the start
	 * object.
	 *
	 * @param name  the name of the property
	 * @param start the object whose property is being set
	 * @param value value to set the property to
	 */
	@Override
	public void putConst(Context cx, String name, Scriptable start, Object value) {
		if (putConstImpl(cx, name, 0, start, value, READONLY)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		if (start instanceof ConstProperties) {
			((ConstProperties) start).putConst(cx, name, start, value);
		} else {
			start.put(cx, name, start, value);
		}
	}

	@Override
	public void defineConst(Context cx, String name, Scriptable start) {
		if (putConstImpl(cx, name, 0, start, Undefined.INSTANCE, UNINITIALIZED_CONST)) {
			return;
		}

		if (start == this) {
			throw Kit.codeBug();
		}
		if (start instanceof ConstProperties) {
			((ConstProperties) start).defineConst(cx, name, start);
		}
	}

	/**
	 * Returns true if the named property is defined as a const on this object.
	 *
	 * @param name
	 * @return true if the named property is defined as a const, false
	 * otherwise.
	 */
	@Override
	public boolean isConst(String name) {
		Slot slot = slotMap.query(name, 0);
		if (slot == null) {
			return false;
		}
		return (slot.getAttributes() & (PERMANENT | READONLY)) == (PERMANENT | READONLY);

	}

	/**
	 * Get the attributes of a named property.
	 * <p>
	 * The property is specified by <code>name</code>
	 * as defined for <code>has</code>.<p>
	 *
	 * @param cx
	 * @param name the identifier for the property
	 * @return the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see Scriptable#get(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public int getAttributes(Context cx, String name) {
		return findAttributeSlot(cx, name, 0, SlotAccess.QUERY).getAttributes();
	}

	/**
	 * Get the attributes of an indexed property.
	 *
	 * @param cx
	 * @param index the numeric index for the property
	 * @return the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 *                            is not found
	 * @see Scriptable#get(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public int getAttributes(Context cx, int index) {
		return findAttributeSlot(cx, null, index, SlotAccess.QUERY).getAttributes();
	}

	public int getAttributes(Context cx, Symbol sym) {
		return findAttributeSlot(cx, sym, SlotAccess.QUERY).getAttributes();
	}

	/**
	 * Set the attributes of a named property.
	 * <p>
	 * The property is specified by <code>name</code>
	 * as defined for <code>has</code>.<p>
	 * <p>
	 * The possible attributes are READONLY, DONTENUM,
	 * and PERMANENT. Combinations of attributes
	 * are expressed by the bitwise OR of attributes.
	 * EMPTY is the state of no attributes set. Any unused
	 * bits are reserved for future use.
	 *
	 * @param name       the name of the property
	 * @param attributes the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see Scriptable#get(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public void setAttributes(Context cx, String name, int attributes) {
		checkNotSealed(cx, name, 0);
		findAttributeSlot(cx, name, 0, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * Set the attributes of an indexed property.
	 *
	 * @param index      the numeric index for the property
	 * @param attributes the bitset of attributes
	 * @throws EvaluatorException if the named property is not found
	 * @see Scriptable#get(Context, String, Scriptable)
	 * @see ScriptableObject#READONLY
	 * @see ScriptableObject#DONTENUM
	 * @see ScriptableObject#PERMANENT
	 * @see ScriptableObject#EMPTY
	 */
	public void setAttributes(Context cx, int index, int attributes) {
		checkNotSealed(cx, null, index);
		findAttributeSlot(cx, null, index, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * Set attributes of a Symbol-keyed property.
	 */
	public void setAttributes(Context cx, Symbol key, int attributes) {
		checkNotSealed(cx, key, 0);
		findAttributeSlot(cx, key, SlotAccess.MODIFY).setAttributes(attributes);
	}

	/**
	 * XXX: write docs.
	 */
	public void setGetterOrSetter(Context cx, String name, int index, Callable getterOrSetter, boolean isSetter) {
		setGetterOrSetter(cx, name, index, getterOrSetter, isSetter, false);
	}

	private void setGetterOrSetter(Context cx, String name, int index, Callable getterOrSetter, boolean isSetter, boolean force) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		}

		if (!force) {
			checkNotSealed(cx, name, index);
		}

		final GetterSlot gslot;
		if (isExtensible()) {
			gslot = (GetterSlot) slotMap.get(name, index, SlotAccess.MODIFY_GETTER_SETTER);
		} else {
			Slot slot = slotMap.query(name, index);
			if (!(slot instanceof GetterSlot)) {
				return;
			}
			gslot = (GetterSlot) slot;
		}

		if (!force) {
			int attributes = gslot.getAttributes();
			if ((attributes & READONLY) != 0) {
				throw Context.reportRuntimeError1("msg.modify.readonly", name, cx);
			}
		}
		if (isSetter) {
			gslot.setter = getterOrSetter;
		} else {
			gslot.getter = getterOrSetter;
		}
		gslot.value = Undefined.INSTANCE;
	}

	/**
	 * Get the getter or setter for a given property. Used by __lookupGetter__
	 * and __lookupSetter__.
	 *
	 * @param name     Name of the object. If nonnull, index must be 0.
	 * @param index    Index of the object. If nonzero, name must be null.
	 * @param isSetter If true, return the setter, otherwise return the getter.
	 * @return Null if the property does not exist. Otherwise returns either
	 * the getter or the setter for the property, depending on
	 * the value of isSetter (may be undefined if unset).
	 * @throws IllegalArgumentException if both name and index are nonnull
	 *                                  and nonzero respectively.
	 */
	public Object getGetterOrSetter(String name, int index, boolean isSetter) {
		if (name != null && index != 0) {
			throw new IllegalArgumentException(name);
		}
		Slot slot = slotMap.query(name, index);
		if (slot == null) {
			return null;
		}
		if (slot instanceof GetterSlot gslot) {
			Object result = isSetter ? gslot.setter : gslot.getter;
			return result != null ? result : Undefined.INSTANCE;
		}
		return Undefined.INSTANCE;
	}

	/**
	 * Returns whether a property is a getter or a setter
	 *
	 * @param name   property name
	 * @param index  property index
	 * @param setter true to check for a setter, false for a getter
	 * @return whether the property is a getter or a setter
	 */
	protected boolean isGetterOrSetter(String name, int index, boolean setter) {
		Slot slot = slotMap.query(name, index);
		if (slot instanceof GetterSlot) {
			if (setter && ((GetterSlot) slot).setter != null) {
				return true;
			}
			return !setter && ((GetterSlot) slot).getter != null;
		}
		return false;
	}

	/**
	 * Return the array that was previously set by the call to "setExternalArrayData".
	 *
	 * @return the array, or null if it was never set
	 * @since 1.7.6
	 */
	public ExternalArrayData getExternalArrayData() {
		return externalData;
	}

	/**
	 * Attach the specified object to this object, and delegate all indexed property lookups to it. In other words,
	 * if the object has 3 elements, then an attempt to look up or modify "[0]", "[1]", or "[2]" will be delegated
	 * to this object. Additional indexed properties outside the range specified, and additional non-indexed
	 * properties, may still be added. The object specified must implement the ExternalArrayData interface.
	 *
	 * @param array the List to use for delegated property access. Set this to null to revert back to regular
	 *              property access.
	 * @since 1.7.6
	 */
	public void setExternalArrayData(Context cx, ExternalArrayData array) {
		externalData = array;

		if (array == null) {
			delete(cx, "length");
		} else {
			// Define "length" to return whatever length the List gives us.
			defineProperty(cx, "length", null, GET_ARRAY_LENGTH, null, READONLY | DONTENUM);
		}
	}

	/**
	 * This is a function used by setExternalArrayData to dynamically get the "length" property value.
	 */
	public Object getExternalArrayLength() {
		return externalData == null ? 0 : externalData.getArrayLength();
	}

	/**
	 * Returns the prototype of the object.
	 */
	@Override
	public Scriptable getPrototype(Context cx) {
		return prototypeObject;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Scriptable m) {
		prototypeObject = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parentScopeObject;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parentScopeObject = m;
	}

	/**
	 * Returns an array of ids for the properties of the object.
	 *
	 * <p>Any properties with the attribute DONTENUM are not listed. <p>
	 *
	 * @return an array of java.lang.Objects with an entry for every
	 * listed property. Properties accessed via an integer index will
	 * have a corresponding
	 * Integer entry in the returned array. Properties accessed by
	 * a String will have a String entry in the returned array.
	 */
	@Override
	public Object[] getIds(Context cx) {
		return getIds(cx, false, false);
	}

	/**
	 * Returns an array of ids for the properties of the object.
	 *
	 * <p>All properties, even those with attribute DONTENUM, are listed. <p>
	 *
	 * @return an array of java.lang.Objects with an entry for every
	 * listed property. Properties accessed via an integer index will
	 * have a corresponding
	 * Integer entry in the returned array. Properties accessed by
	 * a String will have a String entry in the returned array.
	 */
	@Override
	public Object[] getAllIds(Context cx) {
		return getIds(cx, true, false);
	}

	/**
	 * Implements the [[DefaultValue]] internal method.
	 *
	 * <p>Note that the toPrimitive conversion is a no-op for
	 * every type other than Object, for which [[DefaultValue]]
	 * is called. See ECMA 9.1.<p>
	 * <p>
	 * A <code>hint</code> of null means "no hint".
	 *
	 * @param cx
	 * @param typeHint the type hint
	 * @return the default value for the object
	 * <p>
	 * See ECMA 8.6.2.6.
	 */
	@Override
	public Object getDefaultValue(Context cx, DefaultValueTypeHint typeHint) {
		return getDefaultValue(this, typeHint, cx);
	}

	/**
	 * Implements the instanceof operator.
	 *
	 * <p>This operator has been proposed to ECMA.
	 *
	 * @param cx
	 * @param instance The value that appeared on the LHS of the instanceof
	 *                 operator
	 * @return true if "this" appears in value's prototype chain
	 */
	@Override
	public boolean hasInstance(Context cx, Scriptable instance) {
		// Default for JS objects (other than Function) is to do prototype
		// chasing.  This will be overridden in NativeFunction and non-JS
		// objects.

		return ScriptRuntime.jsDelegatesTo(cx, instance, this);
	}

	/**
	 * Emulate the SpiderMonkey (and Firefox) feature of allowing
	 * custom objects to avoid detection by normal "object detection"
	 * code patterns. This is used to implement document.all.
	 * See https://bugzilla.mozilla.org/show_bug.cgi?id=412247.
	 * This is an analog to JOF_DETECTING from SpiderMonkey; see
	 * https://bugzilla.mozilla.org/show_bug.cgi?id=248549.
	 * Other than this special case, embeddings should return false.
	 *
	 * @return true if this object should avoid object detection
	 * @since 1.7R1
	 */
	public boolean avoidObjectDetection() {
		return false;
	}

	/**
	 * Custom <code>==</code> operator.
	 * Must return {@link Scriptable#NOT_FOUND} if this object does not
	 * have custom equality operator for the given value,
	 * <code>Boolean.TRUE</code> if this object is equivalent to <code>value</code>,
	 * <code>Boolean.FALSE</code> if this object is not equivalent to
	 * <code>value</code>.
	 * <p>
	 * The default implementation returns Boolean.TRUE
	 * if <code>this == value</code> or {@link Scriptable#NOT_FOUND} otherwise.
	 * It indicates that by default custom equality is available only if
	 * <code>value</code> is <code>this</code> in which case true is returned.
	 */
	protected Object equivalentValues(Object value) {
		return (this == value) ? Boolean.TRUE : NOT_FOUND;
	}

	/**
	 * Define a JavaScript property.
	 * <p>
	 * Creates the property with an initial value and sets its attributes.
	 *
	 * @param cx
	 * @param propertyName the name of the property to define.
	 * @param value        the initial value of the property
	 * @param attributes   the attributes of the JavaScript property
	 * @see Scriptable#get(Context, String, Scriptable)
	 */
	public void defineProperty(Context cx, String propertyName, Object value, int attributes) {
		checkNotSealed(cx, propertyName, 0);
		put(cx, propertyName, this, value);
		setAttributes(cx, propertyName, attributes);
	}

	/**
	 * A version of defineProperty that uses a Symbol key.
	 *
	 * @param key        symbol of the property to define.
	 * @param value      the initial value of the property
	 * @param attributes the attributes of the JavaScript property
	 */
	public void defineProperty(Context cx, Symbol key, Object value, int attributes) {
		checkNotSealed(cx, key, 0);
		put(cx, key, this, value);
		setAttributes(cx, key, attributes);
	}

	/**
	 * Define a JavaScript property with getter and setter side effects.
	 * <p>
	 * If the setter is not found, the attribute READONLY is added to
	 * the given attributes. <p>
	 * <p>
	 * The getter must be a method with zero parameters, and the setter, if
	 * found, must be a method with one parameter.<p>
	 *
	 * @param propertyName the name of the property to define. This name
	 *                     also affects the name of the setter and getter
	 *                     to search for. If the propertyId is "foo", then
	 *                     <code>clazz</code> will be searched for "getFoo"
	 *                     and "setFoo" methods.
	 * @param clazz        the Java class to search for the getter and setter
	 * @param attributes   the attributes of the JavaScript property
	 * @see Scriptable#get(Context, String, Scriptable)
	 */
	public void defineProperty(Context cx, String propertyName, Class<?> clazz, int attributes) {
		int length = propertyName.length();
		if (length == 0) {
			throw new IllegalArgumentException();
		}
		char[] buf = new char[3 + length];
		propertyName.getChars(0, length, buf, 3);
		buf[3] = Character.toUpperCase(buf[3]);
		buf[0] = 'g';
		buf[1] = 'e';
		buf[2] = 't';
		String getterName = new String(buf);
		buf[0] = 's';
		String setterName = new String(buf);

		var methods = FunctionObject.getMethodList(cx, clazz);
		var getter = WrappedReflectionMethod.of(FunctionObject.findSingleMethod(methods, getterName, cx));
		var setter = WrappedReflectionMethod.of(FunctionObject.findSingleMethod(methods, setterName, cx));
		if (setter == null) {
			attributes |= ScriptableObject.READONLY;
		}
		defineProperty(cx, propertyName, null, getter, setter, attributes);
	}

	/**
	 * Define a JavaScript property.
	 * <p>
	 * Use this method only if you wish to define getters and setters for
	 * a given property in a ScriptableObject. To create a property without
	 * special getter or setter side effects, use
	 * <code>defineProperty(String,int)</code>.
	 * <p>
	 * If <code>setter</code> is null, the attribute READONLY is added to
	 * the given attributes.<p>
	 * <p>
	 * Several forms of getters or setters are allowed. In all cases the
	 * type of the value parameter can be any one of the following types:
	 * Object, String, boolean, Scriptable, byte, short, int, long, float,
	 * or double. The runtime will perform appropriate conversions based
	 * upon the type of the parameter (see description in FunctionObject).
	 * The first forms are nonstatic methods of the class referred to
	 * by 'this':
	 * <pre>
	 * Object getFoo();
	 * void setFoo(SomeType value);</pre>
	 * Next are static methods that may be of any class; the object whose
	 * property is being accessed is passed in as an extra argument:
	 * <pre>
	 * static Object getFoo(Scriptable obj);
	 * static void setFoo(Scriptable obj, SomeType value);</pre>
	 * Finally, it is possible to delegate to another object entirely using
	 * the <code>delegateTo</code> parameter. In this case the methods are
	 * nonstatic methods of the class delegated to, and the object whose
	 * property is being accessed is passed in as an extra argument:
	 * <pre>
	 * Object getFoo(Scriptable obj);
	 * void setFoo(Scriptable obj, SomeType value);</pre>
	 *
	 * @param cx
	 * @param propertyName the name of the property to define.
	 * @param delegateTo   an object to call the getter and setter methods on,
	 *                     or null, depending on the form used above.
	 * @param getter       the method to invoke to get the value of the property
	 * @param setter       the method to invoke to set the value of the property
	 * @param attributes   the attributes of the JavaScript property
	 */
	public void defineProperty(Context cx, String propertyName, Object delegateTo, WrappedExecutable getter, WrappedExecutable setter, int attributes) {
		MemberBox getterBox = null;
		if (getter != null) {
			getterBox = new MemberBox(getter);

			if (!getter.isStatic()) {
				getterBox.delegateTo = delegateTo;
			} else {
				// Ignore delegateTo for static getter but store
				// non-null delegateTo indicator.
				getterBox.delegateTo = Void.TYPE;
			}
		}

		MemberBox setterBox = null;
		if (setter != null) {
			if (setter.getReturnType() != TypeInfo.PRIMITIVE_VOID) {
				throw Context.reportRuntimeError1("msg.setter.return", setter.toString(), cx);
			}

			setterBox = new MemberBox(setter);

			if (!setter.isStatic()) {
				setterBox.delegateTo = delegateTo;
			} else {
				// Ignore delegateTo for static setter but store
				// non-null delegateTo indicator.
				setterBox.delegateTo = Void.TYPE;
			}
		}

		GetterSlot gslot = (GetterSlot) slotMap.get(propertyName, 0, SlotAccess.MODIFY_GETTER_SETTER);
		gslot.setAttributes(attributes);
		gslot.getter = getterBox;
		gslot.setter = setterBox;
	}

	/**
	 * Defines one or more properties on this object.
	 *
	 * @param cx    the current Context
	 * @param props a map of property ids to property descriptors
	 */
	public void defineOwnProperties(Context cx, ScriptableObject props) {
		Object[] ids = props.getIds(cx, false, true);
		ScriptableObject[] descs = new ScriptableObject[ids.length];
		for (int i = 0, len = ids.length; i < len; ++i) {
			Object descObj = ScriptRuntime.getObjectElem(cx, props, ids[i]);
			ScriptableObject desc = ensureScriptableObject(descObj, cx);
			checkPropertyDefinition(cx, desc);
			descs[i] = desc;
		}
		for (int i = 0, len = ids.length; i < len; ++i) {
			defineOwnProperty(cx, ids[i], descs[i]);
		}
	}

	/**
	 * Defines a property on an object.
	 *
	 * @param cx   the current Context
	 * @param id   the name/index of the property
	 * @param desc the new property descriptor, as described in 8.6.1
	 */
	public void defineOwnProperty(Context cx, Object id, ScriptableObject desc) {
		checkPropertyDefinition(cx, desc);
		defineOwnProperty(cx, id, desc, true);
	}

	/**
	 * Defines a property on an object.
	 * <p>
	 * Based on [[DefineOwnProperty]] from 8.12.10 of the spec.
	 *
	 * @param cx         the current Context
	 * @param id         the name/index of the property
	 * @param desc       the new property descriptor, as described in 8.6.1
	 * @param checkValid whether to perform validity checks
	 */
	protected void defineOwnProperty(Context cx, Object id, ScriptableObject desc, boolean checkValid) {

		Slot slot = getSlot(cx, id, SlotAccess.QUERY);
		boolean isNew = slot == null;

		if (checkValid) {
			ScriptableObject current = slot == null ? null : slot.getPropertyDescriptor(cx, this);
			checkPropertyChange(cx, id, current, desc);
		}

		boolean isAccessor = isAccessorDescriptor(cx, desc);
		final int attributes;

		if (slot == null) { // new slot
			slot = getSlot(cx, id, isAccessor ? SlotAccess.MODIFY_GETTER_SETTER : SlotAccess.MODIFY);
			attributes = applyDescriptorToAttributeBitset(cx, DONTENUM | READONLY | PERMANENT, desc);
		} else {
			attributes = applyDescriptorToAttributeBitset(cx, slot.getAttributes(), desc);
		}

		if (isAccessor) {
			if (!(slot instanceof GetterSlot)) {
				slot = getSlot(cx, id, SlotAccess.MODIFY_GETTER_SETTER);
			}

			GetterSlot gslot = (GetterSlot) slot;

			Object getter = getProperty(desc, "get", cx);
			if (getter != NOT_FOUND) {
				gslot.getter = getter;
			}
			Object setter = getProperty(desc, "set", cx);
			if (setter != NOT_FOUND) {
				gslot.setter = setter;
			}

			gslot.value = Undefined.INSTANCE;
			gslot.setAttributes(attributes);
		} else {
			if (slot instanceof GetterSlot && isDataDescriptor(desc, cx)) {
				slot = getSlot(cx, id, SlotAccess.CONVERT_ACCESSOR_TO_DATA);
			}

			Object value = getProperty(desc, "value", cx);
			if (value != NOT_FOUND) {
				slot.value = value;
			} else if (isNew) {
				slot.value = Undefined.INSTANCE;
			}
			slot.setAttributes(attributes);
		}
	}

	protected void checkPropertyDefinition(Context cx, ScriptableObject desc) {
		Object getter = getProperty(desc, "get", cx);
		if (getter != NOT_FOUND && getter != Undefined.INSTANCE && !(getter instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(cx, getter);
		}
		Object setter = getProperty(desc, "set", cx);
		if (setter != NOT_FOUND && setter != Undefined.INSTANCE && !(setter instanceof Callable)) {
			throw ScriptRuntime.notFunctionError(cx, setter);
		}
		if (isDataDescriptor(desc, cx) && isAccessorDescriptor(cx, desc)) {
			throw ScriptRuntime.typeError0(cx, "msg.both.data.and.accessor.desc");
		}
	}

	protected void checkPropertyChange(Context cx, Object id, ScriptableObject current, ScriptableObject desc) {
		if (current == null) { // new property
			if (!isExtensible()) {
				throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
			}
		} else {
			if (isFalse(current.get(cx, "configurable", current), cx)) {
				if (isTrue(getProperty(desc, "configurable", cx), cx)) {
					throw ScriptRuntime.typeError1(cx, "msg.change.configurable.false.to.true", id);
				}
				if (isTrue(current.get(cx, "enumerable", current), cx) != isTrue(getProperty(desc, "enumerable", cx), cx)) {
					throw ScriptRuntime.typeError1(cx, "msg.change.enumerable.with.configurable.false", id);
				}
				boolean isData = isDataDescriptor(desc, cx);
				boolean isAccessor = isAccessorDescriptor(cx, desc);
				if (!isData && !isAccessor) {
					// no further validation required for generic descriptor
				} else if (isData && isDataDescriptor(current, cx)) {
					if (isFalse(current.get(cx, "writable", current), cx)) {
						if (isTrue(getProperty(desc, "writable", cx), cx)) {
							throw ScriptRuntime.typeError1(cx, "msg.change.writable.false.to.true.with.configurable.false", id);
						}

						if (!sameValue(cx, getProperty(desc, "value", cx), current.get(cx, "value", current))) {
							throw ScriptRuntime.typeError1(cx, "msg.change.value.with.writable.false", id);
						}
					}
				} else if (isAccessor && isAccessorDescriptor(cx, current)) {
					if (!sameValue(cx, getProperty(desc, "set", cx), current.get(cx, "set", current))) {
						throw ScriptRuntime.typeError1(cx, "msg.change.setter.with.configurable.false", id);
					}

					if (!sameValue(cx, getProperty(desc, "get", cx), current.get(cx, "get", current))) {
						throw ScriptRuntime.typeError1(cx, "msg.change.getter.with.configurable.false", id);
					}
				} else {
					if (isDataDescriptor(current, cx)) {
						throw ScriptRuntime.typeError1(cx, "msg.change.property.data.to.accessor.with.configurable.false", id);
					}
					throw ScriptRuntime.typeError1(cx, "msg.change.property.accessor.to.data.with.configurable.false", id);
				}
			}
		}
	}

	/**
	 * Implements SameValue as described in ES5 9.12, additionally checking
	 * if new value is defined.
	 *
	 * @param newValue     the new value
	 * @param currentValue the current value
	 * @return true if values are the same as defined by ES5 9.12
	 */
	protected boolean sameValue(Context cx, Object newValue, Object currentValue) {
		if (newValue == NOT_FOUND) {
			return true;
		}
		if (currentValue == NOT_FOUND) {
			currentValue = Undefined.INSTANCE;
		}
		// Special rules for numbers: NaN is considered the same value,
		// while zeroes with different signs are considered different.
		if (currentValue instanceof Number && newValue instanceof Number) {
			double d1 = ((Number) currentValue).doubleValue();
			double d2 = ((Number) newValue).doubleValue();
			if (Double.isNaN(d1) && Double.isNaN(d2)) {
				return true;
			}
			if (d1 == 0.0 && Double.doubleToLongBits(d1) != Double.doubleToLongBits(d2)) {
				return false;
			}
		}
		return ScriptRuntime.shallowEq(cx, currentValue, newValue);
	}

	protected int applyDescriptorToAttributeBitset(Context cx, int attributes, ScriptableObject desc) {
		Object enumerable = getProperty(desc, "enumerable", cx);
		if (enumerable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, enumerable) ? attributes & ~DONTENUM : attributes | DONTENUM;
		}

		Object writable = getProperty(desc, "writable", cx);
		if (writable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, writable) ? attributes & ~READONLY : attributes | READONLY;
		}

		Object configurable = getProperty(desc, "configurable", cx);
		if (configurable != NOT_FOUND) {
			attributes = ScriptRuntime.toBoolean(cx, configurable) ? attributes & ~PERMANENT : attributes | PERMANENT;
		}

		return attributes;
	}

	/**
	 * Implements IsDataDescriptor as described in ES5 8.10.2
	 *
	 * @param desc a property descriptor
	 * @return true if this is a data descriptor.
	 */
	protected boolean isDataDescriptor(ScriptableObject desc, Context cx) {
		return hasProperty(desc, "value", cx) || hasProperty(desc, "writable", cx);
	}

	/**
	 * Implements IsAccessorDescriptor as described in ES5 8.10.1
	 *
	 * @param desc a property descriptor
	 * @return true if this is an accessor descriptor.
	 */
	protected boolean isAccessorDescriptor(Context cx, ScriptableObject desc) {
		return hasProperty(desc, "get", cx) || hasProperty(desc, "set", cx);
	}

	/**
	 * Implements IsGenericDescriptor as described in ES5 8.10.3
	 *
	 * @param desc a property descriptor
	 * @return true if this is a generic descriptor.
	 */
	protected boolean isGenericDescriptor(Context cx, ScriptableObject desc) {
		return !isDataDescriptor(desc, cx) && !isAccessorDescriptor(cx, desc);
	}

	/**
	 * Search for names in a class, adding the resulting methods
	 * as properties.
	 *
	 * <p> Uses reflection to find the methods of the given names. Then
	 * FunctionObjects are constructed from the methods found, and
	 * are added to this object as properties with the given names.
	 *
	 * @param names      the names of the Methods to add as function properties
	 * @param clazz      the class to search for the Methods
	 * @param attributes the attributes of the new properties
	 * @see FunctionObject
	 */
	public void defineFunctionProperties(Context cx, String[] names, Class<?> clazz, int attributes) {
		var methods = FunctionObject.getMethodList(cx, clazz);
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			var m = FunctionObject.findSingleMethod(methods, name, cx);
			if (m == null) {
				throw Context.reportRuntimeError2("msg.method.not.found", name, clazz.getName(), cx);
			}
			FunctionObject f = new FunctionObject(name, m, this, cx);
			defineProperty(cx, name, f, attributes);
		}
	}

	public boolean isExtensible() {
		return isExtensible;
	}

	public void preventExtensions() {
		isExtensible = false;
	}

	/**
	 * Seal this object.
	 * <p>
	 * It is an error to add properties to or delete properties from
	 * a sealed object. It is possible to change the value of an
	 * existing property. Once an object is sealed it may not be unsealed.
	 *
	 * @since 1.4R3
	 */
	public void sealObject(Context cx) {
		if (!isSealed) {
			final long stamp = slotMap.readLock();
			try {
				isSealed = true;
			} finally {
				slotMap.unlockRead(stamp);
			}
		}
	}

	/**
	 * Return true if this object is sealed.
	 *
	 * @return true if sealed, false otherwise.
	 * @since 1.4R3
	 */
	public final boolean isSealed(Context cx) {
		return isSealed;
	}

	private void checkNotSealed(Context cx, Object key, int index) {
		if (!isSealed(cx)) {
			return;
		}

		String str = (key != null) ? key.toString() : Integer.toString(index);
		throw Context.reportRuntimeError1("msg.modify.sealed", str, cx);
	}

	/**
	 * Get arbitrary application-specific value associated with this object.
	 *
	 * @param key key object to select particular value.
	 * @see #associateValue(Object key, Object value)
	 */
	public final Object getAssociatedValue(Object key) {
		Map<Object, Object> h = associatedValues;
		if (h == null) {
			return null;
		}
		return h.get(key);
	}

	/**
	 * Associate arbitrary application-specific value with this object.
	 * Value can only be associated with the given object and key only once.
	 * The method ignores any subsequent attempts to change the already
	 * associated value.
	 * <p> The associated values are not serialized.
	 *
	 * @param key   key object to select particular value.
	 * @param value the value to associate
	 * @return the passed value if the method is called first time for the
	 * given key or old value for any subsequent calls.
	 * @see #getAssociatedValue(Object key)
	 */
	public synchronized final Object associateValue(Object key, Object value) {
		if (value == null) {
			throw new IllegalArgumentException();
		}
		Map<Object, Object> h = associatedValues;
		if (h == null) {
			h = new HashMap<>();
			associatedValues = h;
		}
		return Kit.initHash(h, key, value);
	}

	/**
	 * @param key
	 * @param index
	 * @param start
	 * @param value
	 * @return false if this != start and no slot was found.  true if this == start
	 * or this != start and a READONLY slot was found.
	 */
	private boolean putImpl(Context cx, Object key, int index, Scriptable start, Object value) {
		// This method is very hot (basically called on each assignment)
		// so we inline the extensible/sealed checks below.
		Slot slot;
		if (this != start) {
			slot = slotMap.query(key, index);
			if (!isExtensible && (slot == null || (!(slot instanceof GetterSlot) && (slot.getAttributes() & READONLY) != 0)) && cx.isStrictMode()) {
				throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
			}
			if (slot == null) {
				return false;
			}
		} else if (!isExtensible) {
			slot = slotMap.query(key, index);
			if ((slot == null || (!(slot instanceof GetterSlot) && (slot.getAttributes() & READONLY) != 0)) && cx.isStrictMode()) {
				throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
			}
			if (slot == null) {
				return true;
			}
		} else {
			if (isSealed) {
				checkNotSealed(cx, key, index);
			}
			slot = slotMap.get(key, index, SlotAccess.MODIFY);
		}
		return slot.setValue(value, this, start, cx);
	}

	/**
	 * @param name
	 * @param index
	 * @param start
	 * @param value
	 * @param constFlag EMPTY means normal put.  UNINITIALIZED_CONST means
	 *                  defineConstProperty.  READONLY means const initialization expression.
	 * @return false if this != start and no slot was found.  true if this == start
	 * or this != start and a READONLY slot was found.
	 */
	private boolean putConstImpl(Context cx, String name, int index, Scriptable start, Object value, int constFlag) {
		assert (constFlag != EMPTY);
		if (!isExtensible) {
			if (cx.isStrictMode()) {
				throw ScriptRuntime.typeError0(cx, "msg.not.extensible");
			}
		}
		Slot slot;
		if (this != start) {
			slot = slotMap.query(name, index);
			if (slot == null) {
				return false;
			}
		} else if (!isExtensible()) {
			slot = slotMap.query(name, index);
			if (slot == null) {
				return true;
			}
		} else {
			checkNotSealed(cx, name, index);
			// either const hoisted declaration or initialization
			slot = slotMap.get(name, index, SlotAccess.MODIFY_CONST);
			int attr = slot.getAttributes();
			if ((attr & READONLY) == 0) {
				throw Context.reportRuntimeError1("msg.var.redecl", name, cx);
			}
			if ((attr & UNINITIALIZED_CONST) != 0) {
				slot.value = value;
				// clear the bit on const initialization
				if (constFlag != UNINITIALIZED_CONST) {
					slot.setAttributes(attr & ~UNINITIALIZED_CONST);
				}
			}
			return true;
		}
		return slot.setValue(value, this, start, cx);
	}

	private Slot findAttributeSlot(Context cx, String name, int index, SlotAccess accessType) {
		Slot slot = slotMap.get(name, index, accessType);
		if (slot == null) {
			String str = (name != null ? name : Integer.toString(index));
			throw Context.reportRuntimeError1("msg.prop.not.found", str, cx);
		}
		return slot;
	}

	private Slot findAttributeSlot(Context cx, Symbol key, SlotAccess accessType) {
		Slot slot = slotMap.get(key, 0, accessType);
		if (slot == null) {
			throw Context.reportRuntimeError1("msg.prop.not.found", key, cx);
		}
		return slot;
	}

	Object[] getIds(Context cx, boolean getNonEnumerable, boolean getSymbols) {
		Object[] a;
		int externalLen = (externalData == null ? 0 : externalData.getArrayLength());

		if (externalLen == 0) {
			a = ScriptRuntime.EMPTY_OBJECTS;
		} else {
			a = new Object[externalLen];
			for (int i = 0; i < externalLen; i++) {
				a[i] = i;
			}
		}
		if (slotMap.isEmpty()) {
			return a;
		}

		int c = externalLen;
		final long stamp = slotMap.readLock();
		try {
			for (Slot slot : slotMap) {
				if ((getNonEnumerable || (slot.getAttributes() & DONTENUM) == 0) && (getSymbols || !(slot.name instanceof Symbol))) {
					if (c == externalLen) {
						// Special handling to combine external array with additional properties
						Object[] oldA = a;
						a = new Object[slotMap.dirtySize() + externalLen];
						if (oldA != null) {
							System.arraycopy(oldA, 0, a, 0, externalLen);
						}
					}
					a[c++] = slot.name != null ? slot.name : Integer.valueOf(slot.indexOrHash);
				}
			}
		} finally {
			slotMap.unlockRead(stamp);
		}

		Object[] result;
		if (c == (a.length + externalLen)) {
			result = a;
		} else {
			result = new Object[c];
			System.arraycopy(a, 0, result, 0, c);
		}

		if (cx != null) {
			// Move all the numeric IDs to the front in numeric order
			Arrays.sort(result, KEY_COMPARATOR);
		}

		return result;
	}

	protected ScriptableObject getOwnPropertyDescriptor(Context cx, Object id) {
		Slot slot = getSlot(cx, id, SlotAccess.QUERY);
		if (slot == null) {
			return null;
		}
		Scriptable scope = getParentScope();
		return slot.getPropertyDescriptor(cx, (scope == null ? this : scope));
	}

	// Partial implementation of java.util.Map. See NativeObject for
	// a subclass that implements java.util.Map.

	protected Slot getSlot(Context cx, Object id, SlotAccess accessType) {
		if (id instanceof Symbol) {
			return slotMap.get(id, 0, accessType);
		}
		ScriptRuntime.StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(cx, id);
		if (s.stringId == null) {
			return slotMap.get(null, s.index, accessType);
		}
		return slotMap.get(s.stringId, 0, accessType);
	}

	public int size() {
		return slotMap.size();
	}

	public boolean isEmpty() {
		return slotMap.isEmpty();
	}

	public Object get(Context cx, Object key) {
		Object value = null;
		if (key instanceof String) {
			value = get(cx, (String) key, this);
		} else if (key instanceof Symbol) {
			value = get(cx, (Symbol) key, this);
		} else if (key instanceof Number) {
			value = get(cx, ((Number) key).intValue(), this);
		}
		if (value == NOT_FOUND || value == Undefined.INSTANCE) {
			return null;
		} else if (value instanceof Wrapper) {
			return ((Wrapper) value).unwrap();
		} else {
			return value;
		}
	}

	enum SlotAccess {
		QUERY, MODIFY, MODIFY_CONST, MODIFY_GETTER_SETTER, CONVERT_ACCESSOR_TO_DATA
	}
}

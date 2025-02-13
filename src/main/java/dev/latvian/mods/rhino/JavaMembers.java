/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.ClassVisibilityContext;
import dev.latvian.mods.rhino.util.HideFromJS;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Mike Shaver
 * @author Norris Boyd
 * @see NativeJavaObject
 * @see NativeJavaClass
 */
public class JavaMembers {
	public static String javaSignature(Class<?> type) {
		if (!type.isArray()) {
			return type.getName();
		}
		int arrayDimension = 0;
		do {
			++arrayDimension;
			type = type.getComponentType();
		} while (type.isArray());
		String name = type.getName();
		String suffix = "[]";
		if (arrayDimension == 1) {
			return name.concat(suffix);
		}
		int length = name.length() + arrayDimension * suffix.length();
		StringBuilder sb = new StringBuilder(length);
		sb.append(name);
		while (arrayDimension != 0) {
			--arrayDimension;
			sb.append(suffix);
		}
		return sb.toString();
	}

	public static String liveConnectSignature(List<Class<?>> argTypes) {
		int N = argTypes.size();
		if (N == 0) {
			return "()";
		}
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (int i = 0; i != N; ++i) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(javaSignature(argTypes.get(i)));
		}
		sb.append(')');
		return sb.toString();
	}

	private static MemberBox findGetter(boolean isStatic, Map<String, Object> ht, String prefix, String propertyName) {
		String getterName = prefix.concat(propertyName);
		if (ht.containsKey(getterName)) {
			// Check that the getter is a method.
			Object member = ht.get(getterName);
			if (member instanceof NativeJavaMethod njmGet) {
				return extractGetMethod(njmGet.methods, isStatic);
			}
		}
		return null;
	}

	private static MemberBox extractGetMethod(MemberBox[] methods, boolean isStatic) {
		// Inspect the list of all MemberBox for the only one having no
		// parameters
		for (MemberBox method : methods) {
			// Does getter method have an empty parameter list with a return
			// value (eg. a getSomething() or isSomething())?
			if (method.parameters().count() == 0 && (!isStatic || method.isStatic())) {
				if (method.getReturnType() != TypeInfo.PRIMITIVE_VOID) {
					return method;
				}
				break;
			}
		}
		return null;
	}

	private static MemberBox extractSetMethod(TypeInfo type0, MemberBox[] methods, boolean isStatic) {
		var type = type0.asClass();
		//
		// Note: it may be preferable to allow NativeJavaMethod.findFunction()
		//       to find the appropriate setter; unfortunately, it requires an
		//       instance of the target arg to determine that.
		//

		// Make two passes: one to find a method with direct type assignment,
		// and one to find a widening conversion.
		for (int pass = 1; pass <= 2; ++pass) {
			for (var method : methods) {
				if (!isStatic || method.isStatic()) {
					var params = method.parameters().types();
					if (params.size() == 1) {
						if (pass == 1) {
							if (params.getFirst() == type) {
								return method;
							}
						} else {
							if (pass != 2) {
								Kit.codeBug();
							}
							if (params.getFirst().isAssignableFrom(type)) {
								return method;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static MemberBox extractSetMethod(MemberBox[] methods, boolean isStatic) {
		for (MemberBox method : methods) {
			if (!isStatic || method.isStatic()) {
				if (method.getReturnType() == TypeInfo.PRIMITIVE_VOID) {
					if (method.parameters().count() == 1) {
						return method;
					}
				}
			}
		}
		return null;
	}

	public static JavaMembers lookupClass(Context cx, Scriptable scope, Class<?> dynamicType, Class<?> staticType, boolean includeProtected) {
		JavaMembers members;
		Map<Class<?>, JavaMembers> ct = cx.getClassCacheMap();

		Class<?> cl = dynamicType;
		for (; ; ) {
			members = ct.get(cl);
			if (members != null) {
				if (cl != dynamicType) {
					// member lookup for the original class failed because of
					// missing privileges, cache the result so we don't try again
					ct.put(dynamicType, members);
				}
				return members;
			}
			try {
				members = new JavaMembers(cl, includeProtected, cx, ScriptableObject.getTopLevelScope(scope));
				break;
			} catch (SecurityException e) {
				// Reflection may fail for objects that are in a restricted
				// access package (e.g. sun.*).  If we get a security
				// exception, try again with the static type if it is interface.
				// Otherwise, try superclass
				if (staticType != null && staticType.isInterface()) {
					cl = staticType;
					staticType = null; // try staticType only once
				} else {
					Class<?> parent = cl.getSuperclass();
					if (parent == null) {
						if (cl.isInterface()) {
							// last resort after failed staticType interface
							parent = ScriptRuntime.ObjectClass;
						} else {
							throw e;
						}
					}
					cl = parent;
				}
			}
		}

		ct.put(cl, members);
		if (cl != dynamicType) {
			// member lookup for the original class failed because of
			// missing privileges, cache the result so we don't try again
			ct.put(dynamicType, members);
		}

		return members;
	}

	private final Class<?> cl;
	private final Map<String, Object> members;
	private final Map<String, Object> staticMembers;
	NativeJavaMethod ctors; // we use NativeJavaMethod for ctor overload resolution
	private Map<String, FieldAndMethods> fieldAndMethods;
	private Map<String, FieldAndMethods> staticFieldAndMethods;

	JavaMembers(Class<?> cl, boolean includeProtected, Context cx, Scriptable scope) {
		if (!cx.visibleToScripts(cl.getName(), ClassVisibilityContext.MEMBER)) {
			throw Context.reportRuntimeError1("msg.access.prohibited", cl.getName(), cx);
		}
		this.members = new HashMap<>();
		this.staticMembers = new HashMap<>();
		this.cl = cl;
		reflect(scope, includeProtected, cx);
	}

	public boolean has(Context cx, String name, boolean isStatic) {
		var ht = isStatic ? staticMembers : members;
		Object obj = ht.get(name);
		if (obj != null) {
			return true;
		}
		return findExplicitFunction(cx, name, isStatic) != null;
	}

	public Object get(Scriptable scope, String name, Object javaObject, boolean isStatic, Context cx) {
		var ht = isStatic ? staticMembers : members;
		Object member = ht.get(name);
		if (!isStatic && member == null && cx.factory.getInstanceStaticFallback()) {
			// Try to get static member from instance (LC3)
			member = staticMembers.get(name);
		}
		if (member == null) {
			member = this.getExplicitFunction(scope, name, javaObject, isStatic, cx);
			if (member == null) {
				return Scriptable.NOT_FOUND;
			}
		}
		if (member instanceof Scriptable) {
			return member;
		}
		Object rval;
		TypeInfo type;
		try {
			if (member instanceof BeanProperty bp) {
				if (bp.getter == null) {
					return Scriptable.NOT_FOUND;
				}
				rval = bp.getter.invoke(javaObject, ScriptRuntime.EMPTY_OBJECTS, cx, scope);
				type = bp.getter.getReturnType();
			} else {
				CachedFieldInfo fieldInfo = (CachedFieldInfo) member;
				rval = fieldInfo.get(cx, isStatic ? null : javaObject);
				type = fieldInfo.getType();
			}
		} catch (Throwable ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		}
		// Need to wrap the object before we return it.
		scope = ScriptableObject.getTopLevelScope(scope);
		return cx.wrap(scope, rval, type);
	}

	public void put(Scriptable scope, String name, Object javaObject, Object value, boolean isStatic, Context cx) {
		var ht = isStatic ? staticMembers : members;
		Object member = ht.get(name);
		if (!isStatic && member == null && cx.factory.getInstanceStaticFallback()) {
			// Try to get static member from instance (LC3)
			member = staticMembers.get(name);
		}
		if (member == null) {
			throw reportMemberNotFound(name, cx);
		}
		if (member instanceof FieldAndMethods fam) {
			member = fam.fieldInfo;
		}

		// Is this a bean property "set"?
		if (member instanceof BeanProperty bp) {
			if (bp.setter == null) {
				throw reportMemberNotFound(name, cx);
			}
			// If there's only one setter or if the value is null, use the
			// main setter. Otherwise, let the NativeJavaMethod decide which
			// setter to use:
			if (bp.setters == null || value == null) {
				Object[] args = {cx.jsToJava(value, bp.setter.parameters().typeInfos().getFirst())};
				try {
					bp.setter.invoke(javaObject, args, cx, scope);
				} catch (Exception ex) {
					throw Context.throwAsScriptRuntimeEx(ex, cx);
				}
			} else {
				Object[] args = {value};
				cx.callSync(bp.setters, ScriptableObject.getTopLevelScope(scope), scope, args);
			}
		} else {
			if (!(member instanceof CachedFieldInfo fieldInfo)) {
				if (member == null) {
					throw Context.reportRuntimeError3("msg.java.internal.private.set", name, String.valueOf(javaObject), cl.getName(), cx);
				} else {
					throw Context.reportRuntimeError2("msg.java.method.assign", name, cl.getName(), cx);
				}
			}
			if (fieldInfo.isFinal) {
				// treat Java final the same as JavaScript [[READONLY]]
				throw Context.throwAsScriptRuntimeEx(new IllegalAccessException("Can't modify final field " + fieldInfo.getName()), cx);
			}

			var type = fieldInfo.getType();
			if (scope instanceof NativeJavaObject nativeJavaObject) {
				type = type.consolidate(nativeJavaObject.getTypeMapping());
			}

			try {
				fieldInfo.set(cx, javaObject, cx.jsToJava(value, type));
			} catch (IllegalArgumentException argEx) {
				throw Context.reportRuntimeError3("msg.java.internal.field.type", value.getClass().getName(), fieldInfo.getType(), javaObject.getClass().getName(), cx);
			} catch (Throwable accessEx) {
				throw Context.throwAsScriptRuntimeEx(accessEx, cx);
			}
		}
	}

	public Object[] getIds(boolean isStatic) {
		var map = isStatic ? staticMembers : members;
		return map.keySet().toArray(ScriptRuntime.EMPTY_OBJECTS);
	}

	private MemberBox findExplicitFunction(Context cx, String name, boolean isStatic) {
		int sigStart = name.indexOf('(');
		if (sigStart < 0) {
			return null;
		}

		var ht = isStatic ? staticMembers : members;
		MemberBox[] methodsOrCtors = null;
		boolean isCtor = (isStatic && sigStart == 0);

		if (isCtor) {
			// Explicit request for an overloaded constructor
			methodsOrCtors = ctors.methods;
		} else {
			// Explicit request for an overloaded method
			String trueName = name.substring(0, sigStart);
			Object obj = ht.get(trueName);
			if (!isStatic && obj == null && cx.factory.getInstanceStaticFallback()) {
				// Try to get static member from instance (LC3)
				obj = staticMembers.get(trueName);
			}
			if (obj instanceof NativeJavaMethod njm) {
				methodsOrCtors = njm.methods;
			}
		}

		if (methodsOrCtors != null) {
			for (var methodsOrCtor : methodsOrCtors) {
				var type = methodsOrCtor.parameters().types();
				String sig = liveConnectSignature(type);
				if (sigStart + sig.length() == name.length() && name.regionMatches(sigStart, sig, 0, sig.length())) {
					return methodsOrCtor;
				}
			}
		}

		return null;
	}

	private Object getExplicitFunction(Scriptable scope, String name, Object javaObject, boolean isStatic, Context cx) {
		var ht = isStatic ? staticMembers : members;
		Object member = null;
		MemberBox methodOrCtor = findExplicitFunction(cx, name, isStatic);

		if (methodOrCtor != null) {
			Scriptable prototype = ScriptableObject.getFunctionPrototype(scope, cx);

			if (methodOrCtor.isCtor()) {
				NativeJavaConstructor fun = new NativeJavaConstructor(methodOrCtor);
				fun.setPrototype(prototype);
				member = fun;
				ht.put(name, fun);
			} else {
				String trueName = methodOrCtor.getName();
				member = ht.get(trueName);

				if (member instanceof NativeJavaMethod && ((NativeJavaMethod) member).methods.length > 1) {
					NativeJavaMethod fun = new NativeJavaMethod(methodOrCtor, name);
					fun.setPrototype(prototype);
					ht.put(name, fun);
					member = fun;
				}
			}
		}

		return member;
	}

	private void reflect(Scriptable scope, boolean includeProtected, Context cx) {
		if (cl.isAnnotationPresent(HideFromJS.class)) {
			ctors = new NativeJavaMethod(new MemberBox[0], cl.getSimpleName());
			return;
		}

		// We reflect methods first, because we want overloaded field/method
		// names to be allocated to the NativeJavaMethod before the field
		// gets in the way.

		var storage = cx.getCachedClassStorage(includeProtected);
		var classInfo = storage.get(cl);

		for (var methodInfo0 : classInfo.getAccessibleMethods(includeProtected)) {
			var methodInfo = methodInfo0.getInfo();
			var name = methodInfo0.getName();
			var ht = methodInfo.isStatic ? staticMembers : members;
			var value = ht.get(name);

			if (value == null) {
				ht.put(name, methodInfo);
			} else {
				ObjArray overloadedMethods;
				if (value instanceof ObjArray) {
					overloadedMethods = (ObjArray) value;
				} else {
					if (!(value instanceof CachedMethodInfo)) {
						Kit.codeBug();
					}
					// value should be instance of Method as at this stage
					// staticMembers and members can only contain methods
					overloadedMethods = new ObjArray();
					overloadedMethods.add(value);
					ht.put(name, overloadedMethods);
				}
				overloadedMethods.add(methodInfo);
			}
		}

		// replace Method instances by wrapped NativeJavaMethod objects
		// first in staticMembers and then in members
		for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
			boolean isStatic = (tableCursor == 0);
			var ht = isStatic ? staticMembers : members;

			for (var entry : ht.entrySet()) {
				MemberBox[] methodBoxes;
				Object value = entry.getValue();
				if (value instanceof CachedMethodInfo methodInfo) {
					methodBoxes = new MemberBox[1];
					methodBoxes[0] = new MemberBox(methodInfo);
				} else {
					ObjArray overloadedMethods = (ObjArray) value;
					int N = overloadedMethods.size();
					if (N < 2) {
						Kit.codeBug();
					}
					methodBoxes = new MemberBox[N];
					for (int i = 0; i != N; ++i) {
						var method = (CachedMethodInfo) overloadedMethods.get(i);
						methodBoxes[i] = new MemberBox(method);
					}
				}
				NativeJavaMethod fun = new NativeJavaMethod(methodBoxes);
				if (scope != null) {
					ScriptRuntime.setFunctionProtoAndParent(cx, scope, fun);
				}
				ht.put(entry.getKey(), fun);
			}
		}

		// Reflect fields.
		for (var fieldInfo0 : classInfo.getAccessibleFields(includeProtected)) {
			var fieldInfo = fieldInfo0.getInfo();
			var name = fieldInfo0.getName();

			try {
				var ht = fieldInfo.isStatic ? staticMembers : members;
				Object member = ht.get(name);
				if (member == null) {
					ht.put(name, fieldInfo);
				} else if (member instanceof NativeJavaMethod method) {
					var fam = new FieldAndMethods(scope, method.methods, fieldInfo, cx);
					var fmht = fieldInfo.isStatic ? staticFieldAndMethods : fieldAndMethods;

					if (fmht == null) {
						fmht = new HashMap<>();
						if (fieldInfo.isStatic) {
							staticFieldAndMethods = fmht;
						} else {
							fieldAndMethods = fmht;
						}
					}

					fmht.put(name, fam);
					ht.put(name, fam);
				} else if (member instanceof CachedFieldInfo oldFieldInfo) {
					// If this newly reflected field shadows an inherited field,
					// then replace it. Otherwise, since access to the field
					// would be ambiguous from Java, no field should be
					// reflected.
					// For now, the first field found wins, unless another field
					// explicitly shadows it.
					if (oldFieldInfo.getDeclaringClass().type.isAssignableFrom(fieldInfo.getDeclaringClass().type)) {
						ht.put(name, fieldInfo);
					}
				} else {
					// "unknown member type"
					Kit.codeBug();
				}
			} catch (SecurityException e) {
				// skip this field
				Context.reportWarning("Could not access field " + name + " of class " + cl.getName() + " due to lack of privileges.", cx);
			}
		}

		// Create bean properties from corresponding get/set methods first for
		// static members and then for instance members
		for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
			boolean isStatic = (tableCursor == 0);
			var ht = isStatic ? staticMembers : members;

			Map<String, BeanProperty> toAdd = new HashMap<>();

			// Now, For each member, make "bean" properties.
			for (String name : ht.keySet()) {
				// Is this a getter?
				boolean memberIsGetMethod = name.startsWith("get");
				boolean memberIsSetMethod = name.startsWith("set");
				boolean memberIsIsMethod = name.startsWith("is");
				if (memberIsGetMethod || memberIsIsMethod || memberIsSetMethod) {
					// Double check name component.
					String nameComponent = name.substring(memberIsIsMethod ? 2 : 3);
					if (nameComponent.length() == 0) {
						continue;
					}

					// Make the bean property name.
					String beanPropertyName = nameComponent;
					char ch0 = nameComponent.charAt(0);
					if (Character.isUpperCase(ch0)) {
						if (nameComponent.length() == 1) {
							beanPropertyName = nameComponent.toLowerCase(Locale.ROOT);
						} else {
							char ch1 = nameComponent.charAt(1);
							if (!Character.isUpperCase(ch1)) {
								beanPropertyName = Character.toLowerCase(ch0) + nameComponent.substring(1);
							}
						}
					}

					// If we already have a member by this name, don't do this
					// property.
					if (toAdd.containsKey(beanPropertyName)) {
						continue;
					}
					Object v = ht.get(beanPropertyName);
					if (v != null) {
						// A private field shouldn't mask a public getter/setter
						continue;
					}

					// Find the getter method, or if there is none, the is-
					// method.
					MemberBox getter;
					getter = findGetter(isStatic, ht, "get", nameComponent);
					// If there was no valid getter, check for an is- method.
					if (getter == null) {
						getter = findGetter(isStatic, ht, "is", nameComponent);
					}

					// setter
					MemberBox setter = null;
					NativeJavaMethod setters = null;
					String setterName = "set".concat(nameComponent);

					if (ht.containsKey(setterName)) {
						// Is this value a method?
						Object member = ht.get(setterName);
						if (member instanceof NativeJavaMethod njmSet) {
							if (getter != null) {
								// We have a getter. Now, do we have a matching
								// setter?
								var type = getter.getReturnType();
								setter = extractSetMethod(type, njmSet.methods, isStatic);
							} else {
								// No getter, find any set method
								setter = extractSetMethod(njmSet.methods, isStatic);
							}
							if (njmSet.methods.length > 1) {
								setters = njmSet;
							}
						}
					}
					// Make the property.
					BeanProperty bp = new BeanProperty(getter, setter, setters);
					toAdd.put(beanPropertyName, bp);
				}
			}

			// Add the new bean properties.
			ht.putAll(toAdd);
		}

		// Reflect constructors
		var constructors = classInfo.getConstructors();
		MemberBox[] ctorMembers = new MemberBox[constructors.size()];
		for (int i = 0; i != constructors.size(); ++i) {
			ctorMembers[i] = new MemberBox(constructors.get(i));
		}
		ctors = new NativeJavaMethod(ctorMembers, cl.getSimpleName());
	}

	public List<Constructor<?>> getAccessibleConstructors() {
		List<Constructor<?>> constructorsList = new ArrayList<>();

		for (Constructor<?> c : cl.getConstructors()) {
			if (!c.isAnnotationPresent(HideFromJS.class)) {
				if (Modifier.isPublic(c.getModifiers())) {
					constructorsList.add(c);
				}
			}
		}

		return constructorsList;
	}

	@Deprecated(forRemoval = true)
	public Collection<FieldInfo> getAccessibleFields(Context cx, boolean includeProtected) {
		var list0 = cx.getCachedClassStorage(includeProtected).get(cl).getAccessibleFields(includeProtected);
		var list = new ArrayList<FieldInfo>(list0.size());

		for (var f : list0) {
			list.add(new FieldInfo(f));
		}

		return list;
	}

	@Deprecated(forRemoval = true)
	public Collection<MethodInfo> getAccessibleMethods(Context cx, boolean includeProtected) {
		var list0 = cx.getCachedClassStorage(includeProtected).get(cl).getAccessibleMethods(includeProtected);
		var list = new ArrayList<MethodInfo>(list0.size());

		for (var m : list0) {
			list.add(new MethodInfo(m));
		}

		return list;
	}

	public Map<String, FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope, Object javaObject, boolean isStatic, Context cx) {
		var ht = isStatic ? staticFieldAndMethods : fieldAndMethods;

		if (ht == null) {
			return null;
		}

		int len = ht.size();
		var result = new HashMap<String, FieldAndMethods>(len);

		for (var fam : ht.values()) {
			FieldAndMethods famNew = new FieldAndMethods(scope, fam.methods, fam.fieldInfo, cx);
			famNew.javaObject = javaObject;
			result.put(fam.fieldInfo.getName(), famNew);
		}

		return result;
	}

	RuntimeException reportMemberNotFound(String memberName, Context cx) {
		return Context.reportRuntimeError2("msg.java.member.not.found", cl.getName(), memberName, cx);
	}

	/**
	 * Temp compat class for Probe
	 */
	@Deprecated(forRemoval = true)
	public static class FieldInfo {
		public final CachedFieldInfo.Accessible cached;
		public final Field field;
		public final String name;

		public FieldInfo(CachedFieldInfo.Accessible cached) {
			this.cached = cached;
			this.field = cached.getInfo().field;
			this.name = cached.getName();
		}
	}

	/**
	 * Temp compat class for Probe
	 */
	@Deprecated(forRemoval = true)
	public static class MethodInfo {
		public final CachedMethodInfo.Accessible cached;
		public final Method method;
		public final String name;
		public final boolean hidden;

		public MethodInfo(CachedMethodInfo.Accessible cached) {
			this.cached = cached;
			this.method = cached.getInfo().method;
			this.name = cached.getName();
			this.hidden = cached.isHidden();
		}
	}
}


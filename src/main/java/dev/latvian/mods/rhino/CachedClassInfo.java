package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class CachedClassInfo {
	private static final HashSet<String> IGNORED_DEBUG_METHODS = new HashSet<>();

	static {
		IGNORED_DEBUG_METHODS.add("void wait()");
		IGNORED_DEBUG_METHODS.add("void wait(long, int)");
		IGNORED_DEBUG_METHODS.add("native void wait(long)");
		IGNORED_DEBUG_METHODS.add("boolean equals(Object)");
		IGNORED_DEBUG_METHODS.add("String toString()");
		IGNORED_DEBUG_METHODS.add("native int hashCode()");
		IGNORED_DEBUG_METHODS.add("native Class getClass()");
		IGNORED_DEBUG_METHODS.add("native void notify()");
		IGNORED_DEBUG_METHODS.add("native void notifyAll()");
	}

	public final CachedClassStorage storage;
	public final Class<?> type;
	public final int modifiers;
	public final boolean isInterface;
	private TypeInfo typeInfo;
	private Set<String> remapPrefixes;
	private CachedClassInfo superclass;
	private List<CachedClassInfo> interfaces;
	private List<CachedConstructorInfo> constructors;
	private List<CachedFieldInfo> declaredFields;
	private List<CachedMethodInfo> declaredMethods;
	private List<CachedFieldInfo.Accessible> accessibleFields;
	private List<CachedMethodInfo.Accessible> accessibleMethods;

	public CachedClassInfo(CachedClassStorage storage, Class<?> type) {
		this.storage = storage;
		this.type = type;
		this.modifiers = type.getModifiers();
		this.isInterface = type.isInterface();
	}

	public TypeInfo getTypeInfo() {
		if (typeInfo == null) {
			typeInfo = TypeInfo.of(type);
		}

		return typeInfo;
	}

	public Set<String> getRemapPrefixes() {
		if (remapPrefixes == null) {
			remapPrefixes = new HashSet<>(0);

			for (var r : type.getAnnotationsByType(RemapPrefixForJS.class)) {
				var s = r.value().trim();

				if (!s.isEmpty()) {
					remapPrefixes.add(s);
				}
			}

			remapPrefixes = Set.copyOf(remapPrefixes);
		}

		return remapPrefixes;
	}

	public CachedClassInfo getSuperclass() {
		if (superclass == null) {
			superclass = storage.get(type.getSuperclass());
		}

		return superclass;
	}

	public List<CachedClassInfo> getInterfaces() {
		var il = interfaces;

		if (il == null) {
			il = new ArrayList<>(0);

			for (var i : type.getInterfaces()) {
				il.add(storage.get(i));
			}

			interfaces = List.copyOf(il);
		}

		return il;
	}

	public List<CachedConstructorInfo> getConstructors() {
		var list = constructors;

		if (list == null) {
			if (!storage.isVisible(modifiers)) {
				return constructors = List.of();
			}

			list = new ArrayList<>(1);

			for (var constructor : type.getConstructors()) {
				var mods = constructor.getModifiers();

				if (Modifier.isPublic(mods) && !Modifier.isAbstract(mods)) {
					var info = new CachedConstructorInfo(this, constructor);

					if (!info.isHidden) {
						list.add(info);
					}
				}
			}

			constructors = List.copyOf(list);
		}

		return list;
	}

	public List<CachedFieldInfo> getDeclaredFields() {
		var list = declaredFields;

		if (list == null) {
			if (!storage.isVisible(modifiers)) {
				return declaredFields = List.of();
			}

			list = new ArrayList<>();

			try {
				for (var field : type.getDeclaredFields()) {
					if (storage.include(type, field)) {
						try {
							list.add(new CachedFieldInfo(this, field));
						} catch (Throwable ignored) {
						}
					}
				}
			} catch (Throwable ex) {
				System.err.println("[Rhino] Failed to get declared fields for " + type.getName() + ": " + ex);

				try {
					for (var field : type.getFields()) {
						int mods = field.getModifiers();

						if (storage.include(type, field)) {
							try {
								if (storage.includeProtected && Modifier.isProtected(mods) && !field.isAccessible()) {
									field.setAccessible(true);
								}

								list.add(new CachedFieldInfo(this, field));
							} catch (Throwable ignored) {
							}
						}
					}
				} catch (Throwable ex1) {
					System.err.println("[Rhino] Failed to get declared fields for " + type.getName() + " again: " + ex1);
				}
			}

			declaredFields = List.copyOf(list);
		}

		return list;
	}

	public List<CachedMethodInfo> getDeclaredMethods() {
		var list = declaredMethods;

		if (list == null) {
			if (!storage.isVisible(modifiers)) {
				return declaredMethods = List.of();
			}

			list = new ArrayList<>();

			try {
				for (var method : type.getDeclaredMethods()) {
					if (storage.include(type, method)) {
						list.add(new CachedMethodInfo(this, method));
					}
				}
			} catch (Throwable ex) {
				System.err.println("[Rhino] Failed to get declared methods for " + type.getName() + ": " + ex);
				list.clear();

				try {
					for (var method : type.getMethods()) {
						if (storage.include(type, method)) {
							list.add(new CachedMethodInfo(this, method));
						}
					}
				} catch (Throwable ex1) {
					System.err.println("[Rhino] Failed to get declared methods for " + type.getName() + " again: " + ex1);
				}
			}

			declaredMethods = List.copyOf(list);
		}

		return list;
	}

	public List<CachedFieldInfo.Accessible> getAccessibleFields(boolean cache) {
		var list = accessibleFields;

		if (list == null) {
			var map = new HashMap<String, CachedFieldInfo.Accessible>();
			boolean anyHidden = false;

			var current = this;

			while (current != storage.objectClass) {
				for (var info : current.getDeclaredFields()) {
					var accessible = map.get(info.originalName);

					if (accessible == null) {
						accessible = new CachedFieldInfo.Accessible();
						accessible.info = info;
						map.put(info.originalName, accessible);
					}

					if (info.isHidden) {
						anyHidden = true;
						accessible.hidden = true;
					}

					if (accessible.name.isEmpty()) {
						accessible.name = info.rename;
					}
				}

				current = current.getSuperclass();
			}

			if (anyHidden) {
				map.values().removeIf(CachedFieldInfo.Accessible::isHidden);
			}

			list = List.copyOf(map.values());

			for (var v : list) {
				if (v.name.isEmpty()) {
					v.name = v.getInfo().getName();
				}
			}

			if (cache) {
				accessibleFields = list;
			}
		}

		return list;
	}

	public List<CachedMethodInfo.Accessible> getAccessibleMethods(boolean cache) {
		var list = accessibleMethods;

		if (list == null) {
			var map = new LinkedHashMap<MethodSignature, CachedMethodInfo.Accessible>();
			boolean anyHidden = false;

			var stack = new ArrayDeque<CachedClassInfo>();
			stack.add(this);

			while (!stack.isEmpty()) {
				var current = stack.pop();

				for (var info : current.getDeclaredMethods()) {
					var signature = info.getSignature();

					var accessible = map.get(signature);

					if (accessible == null) {
						accessible = new CachedMethodInfo.Accessible();
						accessible.info = info;
						accessible.signature = signature;
						map.put(signature, accessible);
					}

					if (info.isHidden) {
						anyHidden = true;
						accessible.hidden = true;
					}

					if (accessible.name.isEmpty()) {
						accessible.name = info.rename;
					}
				}

				stack.addAll(current.getInterfaces());

				var parent = current.getSuperclass();

				if (parent != storage.objectClass) {
					stack.add(parent);
				}
			}

			if (anyHidden) {
				map.values().removeIf(CachedMethodInfo.Accessible::isHidden);
			}

			list = List.copyOf(map.values());

			for (var v : list) {
				if (v.name.isEmpty()) {
					v.name = v.getInfo().getName();
				}
			}

			if (cache) {
				accessibleMethods = list;
			}
		}

		return list;
	}

	@Override
	public String toString() {
		return type.getName();
	}

	public CachedMethodInfo getMethod(String name, Class<?>[] params) throws NoSuchMethodException {
		for (var method : getDeclaredMethods()) {
			if (method.originalName.equals(name) && method.getParameters().typesMatch(params)) {
				return method;
			}
		}

		throw new NoSuchMethodException("Method '" + name + "' not found in class " + type.getName());
	}

	public void appendDebugType(StringBuilder builder) {
		int arr = 0;
		var current = type;

		while (current.isArray()) {
			arr++;
			current = current.componentType();
		}

		builder.append(storage.getDebugClassName(current));

		if (arr > 0) {
			builder.append("[]".repeat(arr));
		}
	}

	public List<String> getDebugInfo() {
		int array = 0;
		var cl = type;

		while (cl.isArray()) {
			cl = cl.getComponentType();
			array++;
		}

		var clName = new StringBuilder(cl.getName());

		if (array > 0) {
			clName.append("[]".repeat(array));
		}

		var list = new ArrayList<String>();

		if (cl.isInterface()) {
			clName.insert(0, "interface ");
		} else if (cl.isAnnotation()) {
			clName.insert(0, "annotation ");
		} else if (cl.isEnum()) {
			clName.insert(0, "enum ");
		} else if (cl.isRecord()) {
			clName.insert(0, "record ");
		} else {
			clName.insert(0, "class ");
		}

		list.add(clName.toString());

		var shortName = cl.getName();
		int shortNameIndex = shortName.lastIndexOf('.');

		if (shortNameIndex > 0) {
			shortName = shortName.substring(shortNameIndex + 1);
		}

		for (var constructor : getConstructors()) {
			var builder1 = new StringBuilder("new ");
			builder1.append(shortName);
			constructor.appendDebugParams(builder1);
			list.add(builder1.toString());
		}

		for (var field : getAccessibleFields(false)) {
			var builder1 = new StringBuilder();

			if (field.getInfo().isStatic) {
				builder1.append("static ");
			}

			if (field.getInfo().isFinal) {
				builder1.append("final ");
			}

			if (field.getInfo().isNative) {
				builder1.append("native ");
			}

			storage.get(field.getInfo().getType().asClass()).appendDebugType(builder1);
			builder1.append(' ');
			builder1.append(field.getName());
			list.add(builder1.toString());
		}

		for (var method : getAccessibleMethods(false)) {
			var builder1 = new StringBuilder();

			if (method.getInfo().isStatic) {
				builder1.append("static ");
			}

			if (method.getInfo().isNative) {
				builder1.append("native ");
			}

			storage.get(method.getInfo().getReturnType().asClass()).appendDebugType(builder1);
			builder1.append(' ');
			builder1.append(method.getName());
			method.getInfo().appendDebugParams(builder1);

			var s = builder1.toString();

			if (!IGNORED_DEBUG_METHODS.contains(s)) {
				list.add(s);
			}
		}

		return list;
	}
}

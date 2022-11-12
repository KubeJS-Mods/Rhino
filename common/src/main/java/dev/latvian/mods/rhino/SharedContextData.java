/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.CustomJavaToJsWrapper;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProvider;
import dev.latvian.mods.rhino.util.CustomJavaToJsWrapperProviderHolder;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Cache of generated classes and data structures to access Java runtime
 * from JavaScript.
 *
 * @author Igor Bukanov
 * @since Rhino 1.5 Release 5
 */
public class SharedContextData {
	public final Context context;
	final List<CustomJavaToJsWrapperProviderHolder<?>> customScriptableWrappers = new ArrayList<>();
	final Map<Class<?>, CustomJavaToJsWrapperProvider> customScriptableWrapperCache = new HashMap<>();
	private final Map<String, Object> extraProperties = new HashMap<>();
	TypeWrappers typeWrappers;
	Remapper remapper = DefaultRemapper.INSTANCE;
	private transient Map<Class<?>, JavaMembers> classTable;
	private transient Map<JavaAdapter.JavaAdapterSignature, Class<?>> classAdapterCache;
	private transient Map<Class<?>, Object> interfaceAdapterCache;
	private int generatedClassSerial;
	private ClassShutter classShutter;
	private WrapFactory wrapFactory;

	public SharedContextData(Context cx) {
		context = cx;
	}

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

	public void setExtraProperty(String key, @Nullable Object value) {
		if (value == null) {
			extraProperties.remove(key);
		} else {
			extraProperties.put(key, value);
		}
	}

	@Nullable
	public Object getExtraProperty(String key) {
		return extraProperties.get(key);
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
}

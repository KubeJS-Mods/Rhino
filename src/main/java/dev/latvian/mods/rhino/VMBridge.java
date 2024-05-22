/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

public class VMBridge {
	public static boolean tryToMakeAccessible(Object target, AccessibleObject accessible) {
		if (accessible.canAccess(target)) {
			return true;
		}

		try {
			accessible.setAccessible(true);
		} catch (Exception ex) {
		}

		return accessible.canAccess(target);
	}

	public static Object getInterfaceProxyHelper(Context cx, Class<?>[] interfaces) {
		// XXX: How to handle interfaces array withclasses from different
		// class loaders? Using cf.getApplicationClassLoader() ?
		ClassLoader loader = interfaces[0].getClassLoader();
		Class<?> cl = Proxy.getProxyClass(loader, interfaces);
		Constructor<?> c;
		try {
			c = cl.getConstructor(InvocationHandler.class);
		} catch (NoSuchMethodException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		}
		return c;
	}

	public static Object newInterfaceProxy(Object proxyHelper, final InterfaceAdapter adapter, final Object target, final Scriptable topScope, Context cx) {
		Constructor<?> c = (Constructor<?>) proxyHelper;

		InvocationHandler handler = (proxy, method, args) -> {
			// In addition to methods declared in the interface, proxies
			// also route some java.lang.Object methods through the
			// invocation handler.
			if (method.getDeclaringClass() == Object.class) {
				String methodName = method.getName();
				switch (methodName) {
					case "equals" -> {
						// Note: we could compare a proxy and its wrapped function
						// as equal here but that would break symmetry of equal().
						// The reason == suffices here is that proxies are cached
						// in ScriptableObject (see NativeJavaObject.coerceType())
						return proxy == args[0];
					}
					case "hashCode" -> {
						return target.hashCode();
					}
					case "toString" -> {
						return "Proxy[" + target + "]";
					}
				}
			}

			if (method.isDefault()) {
				return InvocationHandler.invokeDefault(proxy, method, args);
			} else {
				return adapter.invoke(cx, target, topScope, proxy, method, args);
			}
		};
		Object proxy;
		try {
			proxy = c.newInstance(handler);
		} catch (InvocationTargetException ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		} catch (IllegalAccessException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		} catch (InstantiationException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		}
		return proxy;
	}
}

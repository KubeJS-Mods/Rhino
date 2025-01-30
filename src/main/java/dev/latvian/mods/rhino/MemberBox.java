/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */

public final class MemberBox {
	private static CachedMethodInfo searchAccessibleMethod(CachedMethodInfo method, Class<?>[] params) {
		if (Modifier.isPublic(method.modifiers) && !method.isStatic) {
			var c = method.getDeclaringClass();

			if (!Modifier.isPublic(c.modifiers)) {
				String name = method.getName();
				var intfs = c.getInterfaces();
				for (int i = 0, N = intfs.size(); i != N; ++i) {
					var intf = intfs.get(i);
					if (Modifier.isPublic(intf.modifiers)) {
						try {
							return intf.getMethod(name, params);
						} catch (NoSuchMethodException ignored) {
						}
					}
				}
				for (; ; ) {
					c = c.getSuperclass();
					if (c == null) {
						break;
					}
					if (Modifier.isPublic(c.modifiers)) {
						try {
							var m = c.getMethod(name, params);
							if (Modifier.isPublic(m.modifiers) && !m.isStatic) {
								return m;
							}
						} catch (NoSuchMethodException ignored) {
						}
					}
				}
			}
		}
		return null;
	}

	transient CachedExecutableInfo executableInfo;
	transient Object delegateTo;
	public transient WrappedExecutable wrappedExecutable;

	MemberBox(CachedExecutableInfo executableInfo) {
		this.executableInfo = executableInfo;
	}

	MemberBox(WrappedExecutable wrappedExecutable) {
		var executable = wrappedExecutable.unwrap();

		if (executable != null) {
			this.executableInfo = executable;
		} else {
			this.wrappedExecutable = wrappedExecutable;
		}
	}

	@Nullable
	public CachedExecutableInfo getInfo() {
		return executableInfo;
	}

	public CachedParameters parameters() {
		return executableInfo == null ? CachedParameters.EMPTY : executableInfo.getParameters();
	}

	boolean isMethod() {
		return executableInfo instanceof CachedMethodInfo;
	}

	boolean isCtor() {
		return executableInfo instanceof CachedConstructorInfo;
	}

	boolean isStatic() {
		return executableInfo.isStatic;
	}

	String getName() {
		return wrappedExecutable != null ? wrappedExecutable.toString() : executableInfo.getName();
	}

	TypeInfo getReturnType() {
		return wrappedExecutable != null ? wrappedExecutable.getReturnType() : executableInfo.getReturnType();
	}

	String toJavaDeclaration() {
		return String.valueOf(getReturnType()) + ' ' + getName() + JavaMembers.liveConnectSignature(parameters().types());
	}

	@Override
	public String toString() {
		return getName();
	}

	Object invoke(Object target, Object[] args, Context cx, Scriptable scope) {
		if (wrappedExecutable != null) {
			try {
				return wrappedExecutable.invoke(cx, scope, target, args);
			} catch (Throwable ex) {
				throw Context.throwAsScriptRuntimeEx(ex, cx);
			}
		}

		try {
			return executableInfo.invoke(cx, scope, target, args);
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e, cx);
		} catch (Throwable ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		}
	}

	Object newInstance(Object[] args, Context cx, Scriptable scope) {
		if (wrappedExecutable != null) {
			try {
				return wrappedExecutable.construct(cx, scope, args);
			} catch (Throwable ex) {
				throw Context.throwAsScriptRuntimeEx(ex, cx);
			}
		}

		try {
			return executableInfo.invoke(cx, scope, null, args);
		} catch (Throwable ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		}
	}
}


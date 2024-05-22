/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */

public final class MemberBox {
	private static Method searchAccessibleMethod(Method method, Class<?>[] params) {
		int modifiers = method.getModifiers();
		if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
			Class<?> c = method.getDeclaringClass();
			if (!Modifier.isPublic(c.getModifiers())) {
				String name = method.getName();
				Class<?>[] intfs = c.getInterfaces();
				for (int i = 0, N = intfs.length; i != N; ++i) {
					Class<?> intf = intfs[i];
					if (Modifier.isPublic(intf.getModifiers())) {
						try {
							return intf.getMethod(name, params);
						} catch (NoSuchMethodException ex) {
						} catch (SecurityException ex) {
						}
					}
				}
				for (; ; ) {
					c = c.getSuperclass();
					if (c == null) {
						break;
					}
					if (Modifier.isPublic(c.getModifiers())) {
						try {
							Method m = c.getMethod(name, params);
							int mModifiers = m.getModifiers();
							if (Modifier.isPublic(mModifiers) && !Modifier.isStatic(mModifiers)) {
								return m;
							}
						} catch (NoSuchMethodException ex) {
						} catch (SecurityException ex) {
						}
					}
				}
			}
		}
		return null;
	}

	transient Class[] argTypes;
	transient TypeInfo[] argTypeInfos;
	transient TypeInfo returnType;
	transient Object delegateTo;
	transient boolean vararg;
	public transient Executable executable;
	public transient WrappedExecutable wrappedExecutable;

	MemberBox(Executable executable) {
		this.executable = executable;
		this.argTypes = executable.getParameterTypes();
		this.argTypeInfos = TypeInfo.ofArray(executable.getGenericParameterTypes());
		this.returnType = executable instanceof Method m ? TypeInfo.of(m.getGenericReturnType()) : executable instanceof Constructor<?> c ? TypeInfo.of(c.getDeclaringClass()) : TypeInfo.NONE;
		this.vararg = executable.isVarArgs();
	}

	MemberBox(WrappedExecutable wrappedExecutable) {
		var executable = wrappedExecutable.unwrap();

		if (executable != null) {
			this.executable = executable;
			this.argTypes = executable.getParameterTypes();
			this.argTypeInfos = TypeInfo.ofArray(executable.getGenericParameterTypes());
			this.returnType = executable instanceof Method m ? TypeInfo.of(m.getGenericReturnType()) : executable instanceof Constructor<?> c ? TypeInfo.of(c.getDeclaringClass()) : TypeInfo.NONE;
			this.vararg = executable.isVarArgs();
		} else {
			this.wrappedExecutable = wrappedExecutable;
			this.vararg = false;
		}
	}

	Constructor<?> ctor() {
		return (Constructor<?>) executable;
	}

	Member member() {
		return executable;
	}

	boolean isMethod() {
		return executable instanceof Method;
	}

	boolean isCtor() {
		return executable instanceof Constructor;
	}

	boolean isStatic() {
		return Modifier.isStatic(executable.getModifiers());
	}

	boolean isPublic() {
		return Modifier.isPublic(executable.getModifiers());
	}

	String getName() {
		return wrappedExecutable != null ? wrappedExecutable.toString() : executable.getName();
	}

	Class<?> getDeclaringClass() {
		return executable.getDeclaringClass();
	}

	Class<?> getReturnType() {
		return wrappedExecutable != null ? wrappedExecutable.getReturnType() : ((Method) executable).getReturnType();
	}

	Type getGenericReturnType() {
		return wrappedExecutable != null ? wrappedExecutable.getGenericReturnType() : ((Method) executable).getGenericReturnType();

	}

	String toJavaDeclaration() {
		StringBuilder sb = new StringBuilder();
		if (isMethod()) {
			sb.append(getReturnType());
			sb.append(' ');
			sb.append(getName());
		} else {
			String name = getDeclaringClass().getName();
			int lastDot = name.lastIndexOf('.');
			if (lastDot >= 0) {
				name = name.substring(lastDot + 1);
			}
			sb.append(name);
		}
		sb.append(JavaMembers.liveConnectSignature(argTypes));
		return sb.toString();
	}

	@Override
	public String toString() {
		return executable.toString();
	}

	Object invoke(Object target, Object[] args, Context cx, Scriptable scope) {
		if (wrappedExecutable != null) {
			try {
				return wrappedExecutable.invoke(cx, scope, target, args);
			} catch (Exception ex) {
				throw Context.throwAsScriptRuntimeEx(ex, cx);
			}
		}

		Method method = (Method) executable;
		try {
			try {
				return method.invoke(target, args);
			} catch (IllegalAccessException ex) {
				Method accessible = searchAccessibleMethod(method, argTypes);
				if (accessible != null) {
					executable = accessible;
					method = accessible;
				} else {
					if (!VMBridge.tryToMakeAccessible(target, method)) {
						throw Context.throwAsScriptRuntimeEx(ex, cx);
					}
				}
				// Retry after recovery
				return method.invoke(target, args);
			}
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite;
			do {
				e = ((InvocationTargetException) e).getTargetException();
			} while ((e instanceof InvocationTargetException));
			throw Context.throwAsScriptRuntimeEx(e, cx);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		}
	}

	Object newInstance(Object[] args, Context cx, Scriptable scope) {
		if (wrappedExecutable != null) {
			try {
				return wrappedExecutable.construct(cx, scope, args);
			} catch (Exception ex) {
				throw Context.throwAsScriptRuntimeEx(ex, cx);
			}
		}

		Constructor<?> ctor = ctor();
		try {
			try {
				return ctor.newInstance(args);
			} catch (IllegalAccessException ex) {
				if (!VMBridge.tryToMakeAccessible(null, ctor)) {
					throw Context.throwAsScriptRuntimeEx(ex, cx);
				}
			}
			return ctor.newInstance(args);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex, cx);
		}
	}
}


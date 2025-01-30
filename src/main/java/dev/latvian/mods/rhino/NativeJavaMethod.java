/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class reflects Java methods into the JavaScript environment and
 * handles overloading of methods.
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaClass
 */

public class NativeJavaMethod extends BaseFunction {
	/**
	 * Types are equal
	 */
	private static final int PREFERENCE_EQUAL = 0;
	private static final int PREFERENCE_FIRST_ARG = 1;
	private static final int PREFERENCE_SECOND_ARG = 2;
	/**
	 * No clear "easy" conversion
	 */
	private static final int PREFERENCE_AMBIGUOUS = 3;

	static String scriptSignature(Object[] values) {
		StringBuilder sig = new StringBuilder();
		for (int i = 0; i != values.length; ++i) {
			Object value = values[i];

			String s;
			if (value == null) {
				s = "null";
			} else if (value instanceof Boolean) {
				s = "boolean";
			} else if (value instanceof String) {
				s = "string";
			} else if (value instanceof Number) {
				s = "number";
			} else if (value instanceof Scriptable) {
				if (value instanceof Undefined) {
					s = "undefined";
				} else if (value instanceof Wrapper) {
					Object wrapped = ((Wrapper) value).unwrap();
					s = wrapped.getClass().getName();
				} else if (value instanceof Function) {
					s = "function";
				} else {
					s = "object";
				}
			} else {
				s = JavaMembers.javaSignature(value.getClass());
			}

			if (i != 0) {
				sig.append(',');
			}
			sig.append(s);
		}
		return sig.toString();
	}

	/**
	 * Find the index of the correct function to call given the set of methods
	 * or constructors and the arguments.
	 * If no function can be found to call, return -1.
	 */
	static int findFunction(Context cx, MemberBox[] methodsOrCtors, Object[] args) {
		if (methodsOrCtors.length == 0) {
			return -1;
		} else if (methodsOrCtors.length == 1) {
			MemberBox member = methodsOrCtors[0];
			var pars = member.parameters();
			int alength = member.parameters().count();

			if (pars.isVarArg()) {
				alength--;
				if (alength > args.length) {
					return -1;
				}
			} else {
				if (alength != args.length) {
					return -1;
				}
			}
			for (int j = 0; j != alength; ++j) {
				if (!cx.canConvert(args[j], pars.typeInfos().get(j))) {
					return -1;
				}
			}
			return 0;
		}

		int firstBestFit = -1;
		int[] extraBestFits = null;
		int extraBestFitsCount = 0;

		search:
		for (int i = 0; i < methodsOrCtors.length; i++) {
			MemberBox member = methodsOrCtors[i];
			var pars = member.parameters();
			int alength = pars.count();
			if (pars.isVarArg()) {
				alength--;
				if (alength > args.length) {
					continue search;
				}
			} else {
				if (alength != args.length) {
					continue search;
				}
			}
			for (int j = 0; j < alength; j++) {
				if (!cx.canConvert(args[j], pars.typeInfos().get(j))) {
					continue search;
				}
			}
			if (firstBestFit < 0) {
				firstBestFit = i;
			} else {
				// Compare with all currently fit methods.
				// The loop starts from -1 denoting firstBestFit and proceed
				// until extraBestFitsCount to avoid extraBestFits allocation
				// in the most common case of no ambiguity
				int betterCount = 0; // number of times member was prefered over
				// best fits
				int worseCount = 0;  // number of times best fits were prefered
				// over member
				for (int j = -1; j != extraBestFitsCount; ++j) {
					int bestFitIndex;
					if (j == -1) {
						bestFitIndex = firstBestFit;
					} else {
						bestFitIndex = extraBestFits[j];
					}
					MemberBox bestFit = methodsOrCtors[bestFitIndex];
					var bestFitPars = bestFit.parameters();

					int preference = preferSignature(cx, args, pars.typeInfos(), pars.isVarArg(), bestFitPars.typeInfos(), bestFitPars.isVarArg());
					if (preference == PREFERENCE_AMBIGUOUS) {
						break;
					} else if (preference == PREFERENCE_FIRST_ARG) {
						++betterCount;
					} else if (preference == PREFERENCE_SECOND_ARG) {
						++worseCount;
					} else {
						if (preference != PREFERENCE_EQUAL) {
							Kit.codeBug();
						}
						// This should not happen in theory
						// but on some JVMs, Class.getMethods will return all
						// static methods of the class hierarchy, even if
						// a derived class's parameters match exactly.
						// We want to call the derived class's method.
						if (bestFit.isStatic() && bestFit.executableInfo.getDeclaringClass().type.isAssignableFrom(member.executableInfo.getDeclaringClass().type)) {
							// On some JVMs, Class.getMethods will return all
							// static methods of the class hierarchy, even if
							// a derived class's parameters match exactly.
							// We want to call the derived class's method.
							if (j == -1) {
								firstBestFit = i;
							} else {
								extraBestFits[j] = i;
							}
						}
						continue search;
					}
				}
				if (betterCount == 1 + extraBestFitsCount) {
					// member was prefered over all best fits
					firstBestFit = i;
					extraBestFitsCount = 0;
				} else if (worseCount == 1 + extraBestFitsCount) {
					// all best fits were prefered over member, ignore it
				} else {
					// some ambiguity was present, add member to best fit set
					if (extraBestFits == null) {
						// Allocate maximum possible array
						extraBestFits = new int[methodsOrCtors.length - 1];
					}
					extraBestFits[extraBestFitsCount] = i;
					++extraBestFitsCount;
				}
			}
		}

		if (firstBestFit < 0) {
			// Nothing was found
			return -1;
		} else if (extraBestFitsCount == 0) {
			// single best fit
			return firstBestFit;
		}

		// report remaining ambiguity
		StringBuilder buf = new StringBuilder();
		for (int j = -1; j != extraBestFitsCount; ++j) {
			int bestFitIndex;
			if (j == -1) {
				bestFitIndex = firstBestFit;
			} else {
				bestFitIndex = extraBestFits[j];
			}
			buf.append("\n    ");
			buf.append(methodsOrCtors[bestFitIndex].toJavaDeclaration());
		}

		MemberBox firstFitMember = methodsOrCtors[firstBestFit];
		String memberName = firstFitMember.getName();
		String memberClass = firstFitMember.executableInfo.getDeclaringClass().type.getName();

		if (methodsOrCtors[0].isCtor()) {
			throw Context.reportRuntimeError3("msg.constructor.ambiguous", memberName, scriptSignature(args), buf.toString(), cx);
		}
		throw Context.reportRuntimeError4("msg.method.ambiguous", memberClass, memberName, scriptSignature(args), buf.toString(), cx);
	}

	/**
	 * Determine which of two signatures is the closer fit.
	 * Returns one of PREFERENCE_EQUAL, PREFERENCE_FIRST_ARG,
	 * PREFERENCE_SECOND_ARG, or PREFERENCE_AMBIGUOUS.
	 */
	private static int preferSignature(Context cx, Object[] args, List<TypeInfo> sig1, boolean vararg1, List<TypeInfo> sig2, boolean vararg2) {
		int totalPreference = 0;
		for (int j = 0; j < args.length; j++) {
			var type1 = vararg1 && j >= sig1.size() ? sig1.getLast() : sig1.get(j);
			var type2 = vararg2 && j >= sig2.size() ? sig2.getLast() : sig2.get(j);

			if (type1.equals(type2)) {
				continue;
			}

			Object arg = args[j];

			// Determine which of type1, type2 is easier to convert from arg.

			int rank1 = cx.getConversionWeight(arg, type1);
			int rank2 = cx.getConversionWeight(arg, type2);

			int preference;
			if (rank1 < rank2) {
				preference = PREFERENCE_FIRST_ARG;
			} else if (rank1 > rank2) {
				preference = PREFERENCE_SECOND_ARG;
			} else {
				// Equal ranks
				if (rank1 == Context.CONVERSION_EXACT) {
					if (type1.asClass().isAssignableFrom(type2.asClass())) {
						preference = PREFERENCE_SECOND_ARG;
					} else if (type2.asClass().isAssignableFrom(type1.asClass())) {
						preference = PREFERENCE_FIRST_ARG;
					} else {
						preference = PREFERENCE_AMBIGUOUS;
					}
				} else {
					preference = PREFERENCE_AMBIGUOUS;
				}
			}

			totalPreference |= preference;

			if (totalPreference == PREFERENCE_AMBIGUOUS) {
				break;
			}
		}
		return totalPreference;
	}

	private final String functionName;
	private transient final CopyOnWriteArrayList<ResolvedOverload> overloadCache = new CopyOnWriteArrayList<>();
	public transient MemberBox[] methods;

	NativeJavaMethod(MemberBox[] methods) {
		this.functionName = methods[0].getName();
		this.methods = methods;
	}

	NativeJavaMethod(MemberBox[] methods, String name) {
		this.functionName = name;
		this.methods = methods;
	}

	NativeJavaMethod(MemberBox method, String name) {
		this.functionName = name;
		this.methods = new MemberBox[]{method};
	}

	public NativeJavaMethod(CachedMethodInfo method, String name) {
		this(new MemberBox(method), name);
	}

	@Override
	public String getFunctionName() {
		return functionName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0, N = methods.length; i != N; ++i) {
			if (i > 0) {
				sb.append('\n');
			}

			// Check member type, we also use this for overloaded constructors
			if (methods[i].isMethod()) {
				sb.append(JavaMembers.javaSignature(methods[i].getReturnType().asClass()));
				sb.append(' ');
				sb.append(methods[i].getName());
			} else {
				sb.append(methods[i].getName());
			}
			sb.append(JavaMembers.liveConnectSignature(methods[i].parameters().types()));
		}
		return sb.toString();
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// Find a method that matches the types given.
		if (methods.length == 0) {
			throw new RuntimeException("No methods defined for call");
		}

		int index = findCachedFunction(cx, args);
		if (index < 0) {
			Class<?> c = methods[0].executableInfo.getDeclaringClass().getClass();
			String sig = c.getName() + '.' + getFunctionName() + '(' + scriptSignature(args) + ')';
			throw Context.reportRuntimeError1("msg.java.no_such_method", sig, cx);
		}

		MemberBox meth = methods[index];
		var pars = meth.parameters();
		var argTypes = pars.typeInfos();

		if (pars.isVarArg()) {
			// marshall the explicit parameters
			Object[] newArgs = new Object[argTypes.size()];
			for (int i = 0; i < argTypes.size() - 1; i++) {
				newArgs[i] = cx.jsToJava(args[i], argTypes.get(i));
			}

			Object varArgs;

			// Handle special situation where a single variable parameter
			// is given and it is a Java or ECMA array or is null.
			if (args.length == argTypes.size() && (args[args.length - 1] == null || args[args.length - 1] instanceof NativeArray || args[args.length - 1] instanceof NativeJavaArray)) {
				// convert the ECMA array into a native array
				varArgs = cx.jsToJava(args[args.length - 1], argTypes.getLast());
			} else {
				// marshall the variable parameters
				var componentType = argTypes.getLast().componentType();
				varArgs = Array.newInstance(componentType.asClass(), args.length - argTypes.size() + 1);
				int len = Array.getLength(varArgs);
				for (int i = 0; i < len; i++) {
					Object value = cx.jsToJava(args[argTypes.size() - 1 + i], componentType);
					Array.set(varArgs, i, value);
				}
			}

			// add varargs
			newArgs[argTypes.size() - 1] = varArgs;
			// replace the original args with the new one
			args = newArgs;
		} else {
			// First, we marshall the args.
			Object[] origArgs = args;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				Object coerced = arg;

				/*
				if (arg != null) {
					TypeWrapperFactory<?> factory = argTypes[i] != null && cx.hasTypeWrappers() ? cx.getTypeWrappers().getWrapperFactory(argTypes[i], arg) : null;

					if (factory != null) {
						coerced = factory.wrap(arg);
					}
				}
				 */

				coerced = cx.jsToJava(coerced, argTypes.get(i));

				if (coerced != arg) {
					if (origArgs == args) {
						args = args.clone();
					}
					args[i] = coerced;
				}
			}
		}
		Object javaObject;
		if (meth.isStatic()) {
			javaObject = null;  // don't need an object
		} else {
			Scriptable o = thisObj;
			Class<?> c = meth.executableInfo.getDeclaringClass().type;
			for (; ; ) {
				if (o == null) {
					throw Context.reportRuntimeError3("msg.nonjava.method", getFunctionName(), ScriptRuntime.toString(cx, thisObj), c.getName(), cx);
				}
				if (o instanceof Wrapper) {
					javaObject = ((Wrapper) o).unwrap();
					if (c.isInstance(javaObject)) {
						break;
					}
				}
				o = o.getPrototype(cx);
			}
		}

		Object retval = meth.invoke(javaObject, args, cx, scope);
		var staticType = meth.getReturnType();

		Object wrapped = cx.wrap(scope, retval, staticType);

		if (wrapped == null && staticType.isVoid()) {
			wrapped = Undefined.INSTANCE;
		}
		return wrapped;
	}

	int findCachedFunction(Context cx, Object[] args) {
		if (methods.length > 1) {
			for (ResolvedOverload ovl : overloadCache) {
				if (ovl.matches(args)) {
					return ovl.index;
				}
			}
			int index = findFunction(cx, methods, args);
			// As a sanity measure, don't let the lookup cache grow longer
			// than twice the number of overloaded methods
			if (overloadCache.size() < methods.length * 2) {
				ResolvedOverload ovl = new ResolvedOverload(args, index);
				overloadCache.addIfAbsent(ovl);
			}
			return index;
		}
		return findFunction(cx, methods, args);
	}
}


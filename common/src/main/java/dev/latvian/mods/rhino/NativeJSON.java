/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import dev.latvian.mods.rhino.json.JsonParser;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;

import java.io.Serial;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

/**
 * This class implements the JSON native object.
 * See ECMA 15.12.
 *
 * @author Matthew Crumley, Raphael Speyer
 */
public final class NativeJSON extends IdScriptableObject {
	@Serial
	private static final long serialVersionUID = -4567599697595654984L;

	private static final Object JSON_TAG = "JSON";

	private static final int MAX_STRINGIFY_GAP_LENGTH = 10;

	private static final HashSet<String> IGNORED_METHODS = new HashSet<>();

	static {
		IGNORED_METHODS.add("void wait()");
		IGNORED_METHODS.add("void wait(long, int)");
		IGNORED_METHODS.add("native void wait(long)");
		IGNORED_METHODS.add("boolean equals(Object)");
		IGNORED_METHODS.add("String toString()");
		IGNORED_METHODS.add("native int hashCode()");
		IGNORED_METHODS.add("native Class getClass()");
		IGNORED_METHODS.add("native void notify()");
		IGNORED_METHODS.add("native void notifyAll()");
	}

	static void init(Scriptable scope, boolean sealed) {
		NativeJSON obj = new NativeJSON();
		obj.activatePrototypeMap(MAX_ID);
		obj.setPrototype(getObjectPrototype(scope));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject();
		}
		defineProperty(scope, "JSON", obj, DONTENUM);
	}

	private NativeJSON() {
	}

	@Override
	public String getClassName() {
		return "JSON";
	}

	@Override
	protected void initPrototypeId(int id) {
		if (id <= LAST_METHOD_ID) {
			String name;
			int arity;
			switch (id) {
				case Id_toSource -> {
					arity = 0;
					name = "toSource";
				}
				case Id_parse -> {
					arity = 2;
					name = "parse";
				}
				case Id_stringify -> {
					arity = 3;
					name = "stringify";
				}
				default -> throw new IllegalStateException(String.valueOf(id));
			}
			initPrototypeMethod(JSON_TAG, id, name, arity);
		} else {
			throw new IllegalStateException(String.valueOf(id));
		}
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(JSON_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int methodId = f.methodId();
		switch (methodId) {
			case Id_toSource:
				return "JSON";

			case Id_parse: {
				String jtext = ScriptRuntime.toString(args, 0);
				Object reviver = null;
				if (args.length > 1) {
					reviver = args[1];
				}
				if (reviver instanceof Callable) {
					return parse(cx, scope, jtext, (Callable) reviver);
				}
				return parse(cx, scope, jtext);
			}

			case Id_stringify: {
				Object value = null, replacer = null, space = null;
				switch (args.length) {
					case 3:
						space = args[2];
						/* fall through */
					case 2:
						replacer = args[1];
						/* fall through */
					case 1:
						value = args[0];
						/* fall through */
					case 0:
						/* fall through */
					default:
				}
				return stringify(cx, scope, value, replacer, space);
			}

			default:
				throw new IllegalStateException(String.valueOf(methodId));
		}
	}

	private static Object parse(Context cx, Scriptable scope, String jtext) {
		try {
			return new JsonParser(cx, scope).parseValue(jtext);
		} catch (JsonParser.ParseException ex) {
			throw ScriptRuntime.constructError("SyntaxError", ex.getMessage());
		}
	}

	public static Object parse(Context cx, Scriptable scope, String jtext, Callable reviver) {
		Object unfiltered = parse(cx, scope, jtext);
		Scriptable root = cx.newObject(scope);
		root.put("", root, unfiltered);
		return walk(cx, scope, reviver, root, "");
	}

	private static Object walk(Context cx, Scriptable scope, Callable reviver, Scriptable holder, Object name) {
		final Object property;
		if (name instanceof Number) {
			property = holder.get(((Number) name).intValue(), holder);
		} else {
			property = holder.get(((String) name), holder);
		}

		if (property instanceof Scriptable val) {
			if (val instanceof NativeArray) {
				long len = ((NativeArray) val).getLength();
				for (long i = 0; i < len; i++) {
					// indices greater than MAX_INT are represented as strings
					if (i > Integer.MAX_VALUE) {
						String id = Long.toString(i);
						Object newElement = walk(cx, scope, reviver, val, id);
						if (newElement == Undefined.instance) {
							val.delete(id);
						} else {
							val.put(id, val, newElement);
						}
					} else {
						int idx = (int) i;
						Object newElement = walk(cx, scope, reviver, val, idx);
						if (newElement == Undefined.instance) {
							val.delete(idx);
						} else {
							val.put(idx, val, newElement);
						}
					}
				}
			} else {
				Object[] keys = val.getIds();
				for (Object p : keys) {
					Object newElement = walk(cx, scope, reviver, val, p);
					if (newElement == Undefined.instance) {
						if (p instanceof Number) {
							val.delete(((Number) p).intValue());
						} else {
							val.delete((String) p);
						}
					} else {
						if (p instanceof Number) {
							val.put(((Number) p).intValue(), val, newElement);
						} else {
							val.put((String) p, val, newElement);
						}
					}
				}
			}
		}

		return reviver.call(cx, scope, holder, new Object[]{name, property});
	}

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	public static String stringify(Context cx, Scriptable scope, Object value, Object replacer, Object space) {
		JsonElement e = stringify0(scope, value);

		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(stringWriter);

		String indent = null;

		if (space instanceof NativeNumber) {
			space = ScriptRuntime.toNumber(space);
		} else if (space instanceof NativeString) {
			space = ScriptRuntime.toString(space);
		}

		if (space instanceof Number) {
			int gapLength = (int) ScriptRuntime.toInteger(space);
			gapLength = Math.min(MAX_STRINGIFY_GAP_LENGTH, gapLength);
			indent = (gapLength > 0) ? repeat(' ', gapLength) : "";
		} else if (space instanceof String) {
			indent = (String) space;
			if (indent.length() > MAX_STRINGIFY_GAP_LENGTH) {
				indent = indent.substring(0, MAX_STRINGIFY_GAP_LENGTH);
			}
		}

		if (indent != null) {
			writer.setIndent(indent);
		}

		writer.setSerializeNulls(true);
		writer.setHtmlSafe(false);
		writer.setLenient(true);

		try {
			Streams.write(e, writer);
			return stringWriter.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return "error";
		}
	}

	private static void type(StringBuilder builder, Class<?> type) {
		String s = type.getName();

		if (s.startsWith("java.lang.") || s.startsWith("java.util.")) {
			builder.append(s.substring(10));
		} else {
			builder.append(s);
		}
	}

	private static void params(StringBuilder builder, Class<?>[] params) {
		builder.append('(');

		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}

			type(builder, params[i]);
		}

		builder.append(')');
	}

	private static JsonElement stringify0(Scriptable scope, Object v) {
		if (v == null) {
			return JsonNull.INSTANCE;
		} else if (v instanceof Boolean) {
			return new JsonPrimitive((Boolean) v);
		} else if (v instanceof CharSequence) {
			return new JsonPrimitive(v.toString());
		} else if (v instanceof Number) {
			return new JsonPrimitive((Number) v);
		} else if (v instanceof NativeString) {
			return new JsonPrimitive(ScriptRuntime.toString(v));
		} else if (v instanceof NativeNumber) {
			return new JsonPrimitive(ScriptRuntime.toNumber(v));
		} else if (v instanceof Map) {
			JsonObject json = new JsonObject();

			for (Map.Entry<?, ?> entry : ((Map<?, ?>) v).entrySet()) {
				json.add(entry.getKey().toString(), stringify0(scope, entry.getValue()));
			}

			return json;
		} else if (v instanceof Iterable) {
			JsonArray json = new JsonArray();

			for (Object o : (Iterable<?>) v) {
				json.add(stringify0(scope, o));

				return json;
			}
		}

		if (v instanceof Wrapper) {
			v = ((Wrapper) v).unwrap();
		}

		Class<?> cl = v.getClass();
		StringBuilder clName = new StringBuilder(cl.getName());

		while (cl.isArray()) {
			cl = cl.getComponentType();
			clName.append("[]");
		}

		JsonArray list = new JsonArray();

		if (cl.isInterface()) {
			clName.insert(0, "interface ");
		} else if (cl.isAnnotation()) {
			clName.insert(0, "annotation ");
		} else if (cl.isEnum()) {
			clName.insert(0, "enum ");
		} else {
			clName.insert(0, "class ");
		}

		list.add(clName.toString());

		for (Constructor<?> constructor : cl.getConstructors()) {
			if (constructor.isAnnotationPresent(HideFromJS.class)) {
				continue;
			}

			StringBuilder builder = new StringBuilder("new ");
			builder.append(cl.getSimpleName());
			params(builder, constructor.getParameterTypes());
			list.add(builder.toString());
		}

		for (Field field : cl.getFields()) {
			int mod = field.getModifiers();

			if (Modifier.isTransient(mod) || field.isAnnotationPresent(HideFromJS.class)) {
				continue;
			}

			StringBuilder builder = new StringBuilder();

			if (Modifier.isStatic(mod)) {
				builder.append("static ");
			}

			if (Modifier.isFinal(mod)) {
				builder.append("final ");
			}

			if (Modifier.isNative(mod)) {
				builder.append("native ");
			}

			type(builder, field.getType());
			builder.append(' ');

			RemapForJS remap = field.getAnnotation(RemapForJS.class);

			if (remap != null) {
				builder.append(remap.value());
			} else {
				builder.append(field.getName());
			}

			list.add(builder.toString());
		}

		for (Method method : cl.getMethods()) {
			if (method.isAnnotationPresent(HideFromJS.class)) {
				continue;
			}

			int mod = method.getModifiers();

			StringBuilder builder = new StringBuilder();

			if (Modifier.isStatic(mod)) {
				builder.append("static ");
			}

			if (Modifier.isNative(mod)) {
				builder.append("native ");
			}

			type(builder, method.getReturnType());
			builder.append(' ');

			RemapForJS remap = method.getAnnotation(RemapForJS.class);

			if (remap != null) {
				builder.append(remap.value());
			} else {
				builder.append(method.getName());
			}

			params(builder, method.getParameterTypes());

			String s = builder.toString();

			if (!IGNORED_METHODS.contains(s)) {
				list.add(s);
			}
		}

		return list;
	}

	// #string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "toSource" -> Id_toSource;
			case "parse" -> Id_parse;
			case "stringify" -> Id_stringify;
			default -> super.findPrototypeId(s);
		};
	}

	private static final int Id_toSource = 1;
	private static final int Id_parse = 2;
	private static final int Id_stringify = 3;
	private static final int LAST_METHOD_ID = 3;
	private static final int MAX_ID = 3;
}

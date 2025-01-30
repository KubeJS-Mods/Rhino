/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.json.JsonParser;

import java.util.Map;

/**
 * This class implements the JSON native object.
 * See ECMA 15.12.
 *
 * @author Matthew Crumley, Raphael Speyer
 */
public class NativeJSON extends IdScriptableObject {
	private static final Object JSON_TAG = "JSON";

	protected static final int MAX_STRINGIFY_GAP_LENGTH = 10;

	protected static final int Id_toSource = 1;
	protected static final int Id_parse = 2;
	protected static final int Id_stringify = 3;
	protected static final int LAST_METHOD_ID = 3;
	protected static final int MAX_ID = 3;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeJSON obj = new NativeJSON();
		obj.activatePrototypeMap(MAX_ID);
		obj.setPrototype(getObjectPrototype(scope, cx));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject(cx);
		}
		defineProperty(scope, "JSON", obj, DONTENUM, cx);
	}

	private static Object parse(Context cx, Scriptable scope, String jtext) {
		try {
			return new JsonParser(scope).parseValue(cx, jtext);
		} catch (JsonParser.ParseException ex) {
			throw ScriptRuntime.constructError(cx, "SyntaxError", ex.getMessage());
		}
	}

	public static Object parse(Context cx, Scriptable scope, String jtext, Callable reviver) {
		Object unfiltered = parse(cx, scope, jtext);
		Scriptable root = cx.newObject(scope);
		root.put(cx, "", root, unfiltered);
		return walk(cx, scope, reviver, root, "");
	}

	private static Object walk(Context cx, Scriptable scope, Callable reviver, Scriptable holder, Object name) {
		final Object property;
		if (name instanceof Number) {
			property = holder.get(cx, ((Number) name).intValue(), holder);
		} else {
			property = holder.get(cx, ((String) name), holder);
		}

		if (property instanceof Scriptable val) {
			if (val instanceof NativeArray) {
				long len = ((NativeArray) val).getLength();
				for (long i = 0; i < len; i++) {
					// indices greater than MAX_INT are represented as strings
					if (i > Integer.MAX_VALUE) {
						String id = Long.toString(i);
						Object newElement = walk(cx, scope, reviver, val, id);
						if (newElement == Undefined.INSTANCE) {
							val.delete(cx, id);
						} else {
							val.put(cx, id, val, newElement);
						}
					} else {
						int idx = (int) i;
						Object newElement = walk(cx, scope, reviver, val, idx);
						if (newElement == Undefined.INSTANCE) {
							val.delete(cx, idx);
						} else {
							val.put(cx, idx, val, newElement);
						}
					}
				}
			} else {
				Object[] keys = val.getIds(cx);
				for (Object p : keys) {
					Object newElement = walk(cx, scope, reviver, val, p);
					if (newElement == Undefined.INSTANCE) {
						if (p instanceof Number) {
							val.delete(cx, ((Number) p).intValue());
						} else {
							val.delete(cx, (String) p);
						}
					} else {
						if (p instanceof Number) {
							val.put(cx, ((Number) p).intValue(), val, newElement);
						} else {
							val.put(cx, (String) p, val, newElement);
						}
					}
				}
			}
		}

		return reviver.call(cx, scope, holder, new Object[]{name, property});
	}

	public static String stringify(Object value, Object replacer, Object space, Context cx) {
		var builder = new StringBuilder();
		stringify0(cx, value, builder);
		return builder.toString();
	}

	// #string_id_map#

	private static void escape(StringBuilder builder, String string) {
		builder.append('"');
		builder.append(string.replace("\"", "\\\""));
		builder.append('"');
	}

	private static void stringify0(Context cx, Object v, StringBuilder builder) {
		if (v == null || v instanceof Boolean || v instanceof Number) {
			builder.append(v);
		} else if (v instanceof CharSequence) {
			escape(builder, v.toString());
		} else if (v instanceof NativeString) {
			escape(builder, ScriptRuntime.toString(cx, v));
		} else if (v instanceof NativeNumber) {
			builder.append(ScriptRuntime.toNumber(cx, v));
		} else if (v instanceof Map<?, ?> map) {
			builder.append('{');
			boolean first = true;

			for (var entry : map.entrySet()) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				escape(builder, String.valueOf(entry.getKey()));
				builder.append(':');
				stringify0(cx, entry.getValue(), builder);
			}

			builder.append('}');
		} else if (v instanceof Iterable<?> itr) {
			builder.append('[');
			boolean first = true;

			for (var value : itr) {
				if (first) {
					first = false;
				} else {
					builder.append(',');
				}

				stringify0(cx, value, builder);
			}

			builder.append(']');
		} else {
			stringify0(cx, cx.getCachedClassStorage(false).get(Wrapper.unwrapped(v).getClass()).getDebugInfo(), builder);
		}
	}

	protected NativeJSON() {
	}

	@Override
	public String getClassName() {
		return "JSON";
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
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
			initPrototypeMethod(JSON_TAG, id, name, arity, cx);
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
				String jtext = ScriptRuntime.toString(cx, args, 0);
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
				return stringifyJSON(value, replacer, space, cx);
			}

			default:
				throw new IllegalStateException(String.valueOf(methodId));
		}
	}

	public String stringifyJSON(Object value, Object replacer, Object space, Context cx) {
		return stringify(value, replacer, space, cx);
	}

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "toSource" -> Id_toSource;
			case "parse" -> Id_parse;
			case "stringify" -> Id_stringify;
			default -> 0;
		};
	}
}

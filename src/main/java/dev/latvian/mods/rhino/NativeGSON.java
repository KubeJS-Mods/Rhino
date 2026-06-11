/* -*- Mode: java; tab-width: 4; indent-tabs-mode: 1; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;

/**
 * This class implements the JSON native object.
 */
public class NativeGSON extends NativeJSON {
	static void initGSON(Scriptable scope, boolean sealed, Context cx) {
		NativeGSON obj = new NativeGSON();
		obj.activatePrototypeMap(MAX_ID);
		obj.setPrototype(getObjectPrototype(scope, cx));
		obj.setParentScope(scope);
		if (sealed) {
			obj.sealObject(cx);
		}
		defineProperty(scope, "JSON", obj, DONTENUM, cx);
	}

	public static JsonElement stringify0(Context cx, Object v) {
		return switch (v) {
			case null -> JsonNull.INSTANCE;
			case JsonElement json -> json;
			case Boolean b -> new JsonPrimitive(b);
			case CharSequence ignore -> new JsonPrimitive(v.toString());
			case Number n -> numberPrimitive(n);
			case NativeString ignore -> new JsonPrimitive(ScriptRuntime.toString(cx, v));
			case NativeNumber ignore -> numberPrimitive(ScriptRuntime.toNumber(cx, v));
			case NativeObject obj -> {
				// don't use the Map view here, it can't distinguish undefined (skipped) from null (kept)
				var json = new JsonObject();

				for (Object id : obj.getIds(cx)) {
					Object value = id instanceof Integer index ? obj.get(cx, index, obj) : obj.get(cx, String.valueOf(id), obj);
					if (value == Scriptable.NOT_FOUND || isUnserializable(value)) {
						continue;
					} else if (value instanceof Wrapper w) {
						value = w.unwrap();
					}

					json.add(String.valueOf(id), stringify0(cx, value));
				}

				yield json;
			}
			case Map<?, ?> map -> {
				var json = new JsonObject();

				for (var entry : map.entrySet()) {
					if (!isUnserializable(entry.getValue())) {
						json.add(entry.getKey().toString(), stringify0(cx, entry.getValue()));
					}
				}

				yield json;
			}
			case Iterable<?> itr -> {
				var json = new JsonArray();

				for (var o : itr) {
					json.add(stringify0(cx, o));
				}

				yield json;
			}
			case Object o when isUnserializable(o) -> JsonNull.INSTANCE;
			default -> {
				var json = new JsonArray();
				cx.getCachedClassStorage(false).get(Wrapper.unwrapped(v).getClass()).getDebugInfo().forEach(json::add);
				yield json;
			}
		};
	}

	private static JsonElement numberPrimitive(Number n) {
		double d = n.doubleValue();
		if (!Double.isFinite(d)) {
			// JSON has no representation for non-finite numbers
			return JsonNull.INSTANCE;
		}
		long l = (long) d;
		// integral doubles must not print a ".0" suffix
		return l == d ? new JsonPrimitive(l) : new JsonPrimitive(n);
	}

	private final Gson gson;

	private NativeGSON() {
		this.gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setLenient().create();
	}

	@Override
	public String stringifyJSON(Object value, Object replacer, Object space, Context cx) {
		StringWriter stringWriter = new StringWriter();
		JsonWriter writer = new JsonWriter(stringWriter);

		String indent = null;

		if (space instanceof NativeNumber) {
			space = ScriptRuntime.toNumber(cx, space);
		} else if (space instanceof NativeString) {
			space = ScriptRuntime.toString(cx, space);
		}

		if (space instanceof Number) {
			int gapLength = (int) ScriptRuntime.toInteger(cx, space);
			gapLength = Math.min(MAX_STRINGIFY_GAP_LENGTH, gapLength);
			indent = (gapLength > 0) ? " ".repeat(gapLength) : "";
		} else if (space instanceof String) {
			indent = (String) space;
			if (indent.length() > MAX_STRINGIFY_GAP_LENGTH) {
				indent = indent.substring(0, MAX_STRINGIFY_GAP_LENGTH);
			}
		}

		writer.setIndent(Objects.requireNonNullElse(indent, ""));
		gson.toJson(stringify0(cx, value), writer);
		return stringWriter.toString();
	}
}

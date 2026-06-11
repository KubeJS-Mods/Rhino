/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeJavaMap extends NativeJavaObject {
	public final Map map;
	public final TypeInfo mapKeyType;
	public final TypeInfo mapValueType;

	public NativeJavaMap(Context cx, Scriptable scope, Object jo, Map map, TypeInfo type) {
		super(scope, jo, type, cx);
		this.map = map;
		this.mapKeyType = type.param(0);
		this.mapValueType = type.param(1);
	}

	@Override
	public String getClassName() {
		return "JavaMap";
	}

	/**
	 * Marker for JS property names that cannot be converted to this map's key type
	 * and therefore cannot be present in the map.
	 */
	private static final Object INVALID_KEY = new Object();

	private Object toMapKey(Context cx, Object jsKey) {
		try {
			return cx.jsToJava(jsKey, mapKeyType);
		} catch (Exception ex) {
			return INVALID_KEY;
		}
	}

	/**
	 * Checks key presence without propagating exceptions from maps that reject
	 * keys of an incompatible type (e.g. ClassCastException from a TreeMap with
	 * non-String comparable keys when a JS property name is looked up).
	 */
	private boolean containsKeySafe(Object key) {
		if (key == INVALID_KEY) {
			return false;
		}

		try {
			return map.containsKey(key);
		} catch (ClassCastException | NullPointerException ex) {
			return false;
		}
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		if (containsKeySafe(toMapKey(cx, name))) {
			return true;
		}
		return super.has(cx, name, start);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (containsKeySafe(toMapKey(cx, index))) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		Object key = toMapKey(cx, name);
		if (containsKeySafe(key)) {
			return cx.javaToJS(map.get(key), start, mapValueType);
		}
		return super.get(cx, name, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		Object key = toMapKey(cx, index);
		if (containsKeySafe(key)) {
			return cx.javaToJS(map.get(key), start, mapValueType);
		}
		return super.get(cx, index, start);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		map.put(cx.jsToJava(name, mapKeyType), cx.jsToJava(value, mapValueType));
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		map.put(cx.jsToJava(index, mapKeyType), cx.jsToJava(value, mapValueType));
	}

	@Override
	public Object[] getIds(Context cx) {
		List<Object> ids = new ArrayList<>(map.size());
		for (Object key : map.keySet()) {
			if (key instanceof Integer) {
				ids.add(key);
			} else {
				ids.add(ScriptRuntime.toString(cx, key));
			}
		}
		return ids.toArray();
	}

	@Override
	public void delete(Context cx, String name) {
		Object key = toMapKey(cx, name);
		if (containsKeySafe(key)) {
			Deletable.deleteObject(map.remove(key));
		}
	}

	@Override
	public void delete(Context cx, int index) {
		Object key = toMapKey(cx, index);
		if (containsKeySafe(key)) {
			Deletable.deleteObject(map.remove(key));
		}
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		super.initMembers(cx, scope);
		addCustomFunction("hasOwnProperty", TypeInfo.BOOLEAN, this::hasOwnProperty, TypeInfo.STRING);
	}

	private boolean hasOwnProperty(Context cx, Object[] args) {
		return containsKeySafe(toMapKey(cx, ScriptRuntime.toString(cx, args[0])));
	}
}

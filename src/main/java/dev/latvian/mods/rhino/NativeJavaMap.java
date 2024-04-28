/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.ValueUnwrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NativeJavaMap extends NativeJavaObject {
	private final Map map;
	private final Class<?> mapValueType;
	private final ValueUnwrapper valueUnwrapper;

	public NativeJavaMap(Context cx, Scriptable scope, Object jo, Map map, Class<?> mapValueType, ValueUnwrapper valueUnwrapper) {
		super(scope, jo, jo.getClass(), cx);
		this.map = map;
		this.mapValueType = mapValueType;
		this.valueUnwrapper = valueUnwrapper;
	}

	public NativeJavaMap(Context cx, Scriptable scope, Object jo, Map map) {
		this(cx, scope, jo, map, Object.class, ValueUnwrapper.DEFAULT);
	}

	@Override
	public String getClassName() {
		return "JavaMap";
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		if (map.containsKey(name)) {
			return true;
		}
		return super.has(cx, name, start);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		if (map.containsKey(index)) {
			return true;
		}
		return super.has(cx, index, start);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		if (map.containsKey(name)) {
			return valueUnwrapper.unwrap(cx, this, map.get(name));
		}
		return super.get(cx, name, start);
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		if (map.containsKey(index)) {
			return valueUnwrapper.unwrap(cx, this, map.get(index));
		}
		return super.get(cx, index, start);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		map.put(name, Context.jsToJava(cx, value, mapValueType));
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		map.put(index, Context.jsToJava(cx, value, mapValueType));
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
		Deletable.deleteObject(map.remove(name));
	}

	@Override
	public void delete(Context cx, int index) {
		Deletable.deleteObject(map.remove(index));
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		super.initMembers(cx, scope);
		addCustomFunction("hasOwnProperty", this::hasOwnProperty, String.class);
	}

	private boolean hasOwnProperty(Context cx, Object[] args) {
		return map.containsKey(ScriptRuntime.toString(cx, args[0]));
	}
}

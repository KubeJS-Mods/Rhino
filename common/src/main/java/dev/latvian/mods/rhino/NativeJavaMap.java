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
	private final SharedContextData contextData;
	private final Map map;
	private final Class<?> mapValueType;
	private final ValueUnwrapper valueUnwrapper;

	public NativeJavaMap(SharedContextData contextData, Scriptable scope, Object jo, Map map, Class<?> mapValueType, ValueUnwrapper valueUnwrapper) {
		super(scope, jo, jo.getClass());
		this.contextData = contextData;
		this.map = map;
		this.mapValueType = mapValueType;
		this.valueUnwrapper = valueUnwrapper;
	}

	public NativeJavaMap(SharedContextData contextData, Scriptable scope, Object jo, Map map) {
		this(contextData, scope, jo, map, Object.class, ValueUnwrapper.DEFAULT);
	}

	@Override
	public String getClassName() {
		return "JavaMap";
	}

	@Override
	public boolean has(String name, Scriptable start) {
		if (map.containsKey(name)) {
			return true;
		}
		return super.has(name, start);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (map.containsKey(index)) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public Object get(String name, Scriptable start) {
		if (map.containsKey(name)) {
			return valueUnwrapper.unwrap(contextData, this, map.get(name));
		}
		return super.get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (map.containsKey(index)) {
			return valueUnwrapper.unwrap(contextData, this, map.get(index));
		}
		return super.get(index, start);
	}

	@Override
	public void put(String name, Scriptable start, Object value) {
		map.put(name, Context.jsToJava(contextData, value, mapValueType));
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		map.put(index, Context.jsToJava(contextData, value, mapValueType));
	}

	@Override
	public Object[] getIds() {
		List<Object> ids = new ArrayList<>(map.size());
		for (Object key : map.keySet()) {
			if (key instanceof Integer) {
				ids.add(key);
			} else {
				ids.add(ScriptRuntime.toString(key));
			}
		}
		return ids.toArray();
	}

	@Override
	public void delete(String name) {
		Deletable.deleteObject(map.remove(name));
	}

	@Override
	public void delete(int index) {
		Deletable.deleteObject(map.remove(index));
	}

	@Override
	protected void initMembers() {
		super.initMembers();
		addCustomFunction("hasOwnProperty", this::hasOwnProperty, String.class);
	}

	private boolean hasOwnProperty(Object[] args) {
		return map.containsKey(ScriptRuntime.toString(args[0]));
	}
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.MapLike;

public class NativeJavaMapLike extends NativeJavaObject {
	private final MapLike<Object, Object> map;

	@SuppressWarnings("unchecked")
	public NativeJavaMapLike(Scriptable scope, Object map) {
		super(scope, map, map.getClass());
		assert map instanceof MapLike;
		this.map = (MapLike<Object, Object>) map;
	}

	@Override
	public String getClassName() {
		return "JavaMapLike";
	}

	@Override
	public boolean has(String name, Scriptable start) {
		if (map.containsKeyML(name)) {
			return true;
		}
		return super.has(name, start);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (map.containsKeyML(index)) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public Object get(String name, Scriptable start) {
		if (map.containsKeyML(name)) {
			Context cx = Context.getContext();
			Object obj = map.getML(name);
			return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
		}
		return super.get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (map.containsKeyML(index)) {
			Context cx = Context.getContext();
			Object obj = map.getML(index);
			return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
		}
		return super.get(index, start);
	}

	@Override
	public void put(String name, Scriptable start, Object value) {
		map.putML(name, Context.jsToJava(value, Object.class));
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		map.putML(index, Context.jsToJava(value, Object.class));
	}

	@Override
	public Object[] getIds() {
		Object[] k = map.keysML().toArray();

		for (int i = 0; i < k.length; i++) {
			if (!(k[i] instanceof Integer)) {
				k[i] = ScriptRuntime.toString(k[i]);
			}
		}

		return k;
	}

	@Override
	public void delete(String name) {
		Object obj = map.getML(name);
		map.removeML(name);
		Deletable.deleteObject(obj);
	}

	@Override
	public void delete(int index) {
		Object obj = map.getML(index);
		map.removeML(index);
		Deletable.deleteObject(obj);
	}
}

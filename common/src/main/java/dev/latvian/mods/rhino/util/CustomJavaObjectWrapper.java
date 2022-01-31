package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaList;
import dev.latvian.mods.rhino.NativeJavaMap;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.Wrapper;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface CustomJavaObjectWrapper {
	Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType);

	@FunctionalInterface
	interface AsList extends CustomJavaObjectWrapper {
		List<?> wrapAsJavaList();

		@Override
		default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
			List<?> list = wrapAsJavaList();
			Object jo = Wrapper.unwrapped(list);
			return new NativeJavaList(scope, jo, list);
		}
	}

	@FunctionalInterface
	interface AsMap extends CustomJavaObjectWrapper {
		Map<?, ?> wrapAsJavaMap();

		@Override
		default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
			Map<?, ?> map = wrapAsJavaMap();
			Object jo = Wrapper.unwrapped(map);
			return new NativeJavaMap(scope, jo, map);
		}
	}
}

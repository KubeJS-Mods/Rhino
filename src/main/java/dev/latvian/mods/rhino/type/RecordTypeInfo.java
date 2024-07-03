package dev.latvian.mods.rhino.type;

import dev.latvian.mods.rhino.Callable;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeJavaList;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.NativeMap;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RecordTypeInfo extends ClassTypeInfo implements TypeWrapperFactory<Object> {
	private static final Map<Class<?>, Object> GLOBAL_DEFAULT_VALUES = new IdentityHashMap<>();

	public static <T> void setGlobalDefaultValue(Class<T> type, T value) {
		GLOBAL_DEFAULT_VALUES.put(type, value);
	}

	static {
		setGlobalDefaultValue(Optional.class, Optional.empty());
		setGlobalDefaultValue(List.class, List.of());
		setGlobalDefaultValue(Set.class, Set.of());
		setGlobalDefaultValue(Map.class, Map.of());
	}

	public record Component(int index, String name, TypeInfo type) {
	}

	public record Data(Component[] components, Map<String, Component> componentMap, Object[] defaultArguments) {
	}

	static final Map<Class<?>, RecordTypeInfo> CACHE = new IdentityHashMap<>();

	private Data data;
	private Constructor<?> constructor;
	private JSObjectTypeInfo objectTypeInfo;
	private JSFixedArrayTypeInfo arrayTypeInfo;

	RecordTypeInfo(Class<?> type) {
		super(type);
	}

	public Data getData() {
		if (data == null) {
			var rc = asClass().getRecordComponents();
			var components = new Component[rc.length];
			var componentMap = new HashMap<String, Component>();
			var defaultArguments = new Object[rc.length];

			for (int i = 0; i < rc.length; i++) {
				var gt = rc[i].getGenericType();

				var rename = rc[i].getAccessor().getDeclaredAnnotation(RemapForJS.class);
				var c = new Component(i, rename != null ? rename.value() : rc[i].getName(), TypeInfo.of(gt));
				components[i] = c;
				componentMap.put(c.name, c);
				defaultArguments[i] = c.type.createDefaultValue();

				if (defaultArguments[i] == null) {
					defaultArguments[i] = GLOBAL_DEFAULT_VALUES.getOrDefault(rc[i].getType(), null);
				}
			}

			data = new Data(components, Map.copyOf(componentMap), defaultArguments);
		}

		return data;
	}

	public JSObjectTypeInfo getObjectTypeInfo() {
		if (objectTypeInfo == null) {
			var data = getData();
			var list = new ArrayList<JSOptionalParam>(data.components.length);

			for (var c : data.components) {
				list.add(new JSOptionalParam(c.name, c.type, true));
			}

			objectTypeInfo = new JSObjectTypeInfo(List.copyOf(list));
		}

		return objectTypeInfo;
	}

	public JSFixedArrayTypeInfo getArrayTypeInfo() {
		if (arrayTypeInfo == null) {
			var data = getData();
			var list = new ArrayList<JSOptionalParam>(data.components.length);

			for (var c : data.components) {
				list.add(new JSOptionalParam(c.name, c.type, true));
			}

			arrayTypeInfo = new JSFixedArrayTypeInfo(List.copyOf(list));
		}

		return arrayTypeInfo;
	}

	public TypeInfo createCombinedType(TypeInfo... preference) {
		var types = new ArrayList<TypeInfo>(2 + preference.length);
		types.addAll(Arrays.asList(preference));
		types.add(getObjectTypeInfo());
		types.add(getArrayTypeInfo());
		return new JSOrTypeInfo(types);
	}

	@Override
	public Map<String, Component> recordComponents() {
		return getData().componentMap;
	}

	public Constructor<?> getConstructor(Context cx) {
		if (constructor == null) {
			try {
				var data = getData();
				var types = new Class<?>[data.components.length];

				for (int i = 0; i < data.components.length; i++) {
					types[i] = data.components[i].type.asClass();
				}

				constructor = asClass().getConstructor(types);
			} catch (Exception ex) {
				throw Context.reportRuntimeError("Unable to find record '" + asClass().getName() + "' constructor", cx);
			}
		}

		return constructor;
	}

	public Object createInstance(Context cx, Map<?, ?> map) {
		var data = getData();
		var args = data.defaultArguments.clone();

		for (var entry : map.entrySet()) {
			var c = data.componentMap.get(String.valueOf(entry.getKey()));

			if (c != null) {
				if (args[c.index] == Optional.empty()) {
					args[c.index] = Optional.ofNullable(cx.jsToJava(entry.getValue(), c.type.param(0)));
				} else {
					args[c.index] = cx.jsToJava(entry.getValue(), c.type);
				}
			}
		}

		try {
			return getConstructor(cx).newInstance(args);
		} catch (RhinoException ignored) {
		} catch (Exception ex) {
			throw Context.reportRuntimeError("Unable to create record '" + asClass().getName() + "' instance from map " + map, cx);
		}

		return cx.reportConversionError(map, this);
	}

	public Object createInstance(Context cx, Object... objects) {
		var data = getData();
		var args = data.defaultArguments.clone();
		int alen = Math.min(args.length, objects.length);

		for (int i = 0; i < alen; i++) {
			if (args[i] == Optional.empty()) {
				args[i] = Optional.ofNullable(cx.jsToJava(objects[i], data.components[i].type.param(0)));
			} else {
				args[i] = cx.jsToJava(objects[i], data.components[i].type);
			}
		}

		try {
			return getConstructor(cx).newInstance(args);
		} catch (RhinoException ignored) {
		} catch (Exception ex) {
			throw Context.reportRuntimeError("Unable to create record '" + asClass().getName() + "' instance from array " + Arrays.toString(objects), cx);
		}

		return cx.reportConversionError(objects, this);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object wrap(Context cx, Object from, TypeInfo target) {
		if (asClass().isInstance(from)) {
			return from;
		} else if (from instanceof NativeArray || from instanceof NativeJavaList) {
			var arr = (Object[]) cx.arrayOf(from, TypeInfo.NONE);
			return createInstance(cx, arr);
		} else if (from instanceof Map<?, ?> || from instanceof NativeJavaObject || from instanceof NativeMap) {
			var map = (Map) cx.mapOf(from, TypeInfo.STRING, TypeInfo.NONE);
			return createInstance(cx, map);
		} else if (from instanceof Callable) {
			var map = new HashMap<>();
			var consumer = (Consumer) cx.jsToJava(from, TypeInfo.RAW_CONSUMER);
			consumer.accept(map);
			return createInstance(cx, map);
		}

		return cx.reportConversionError(from, target);
	}
}

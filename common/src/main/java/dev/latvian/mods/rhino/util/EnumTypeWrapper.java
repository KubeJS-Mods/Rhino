package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class EnumTypeWrapper<T> implements TypeWrapperFactory<T> {
	private static final Map<Class<?>, EnumTypeWrapper<?>> WRAPPERS = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <T> EnumTypeWrapper<T> get(Class<T> enumType) {
		if (!enumType.isEnum()) {
			throw new IllegalArgumentException("Class " + enumType.getName() + " is not an enum!");
		}

		return (EnumTypeWrapper<T>) WRAPPERS.computeIfAbsent(enumType, EnumTypeWrapper::new);
	}

	public static String getName(Class<?> enumType, Enum<?> e) {
		String name = e.name();

		if (e instanceof RemappedEnumConstant c) {
			String s = c.getRemappedEnumConstantName();

			if (!s.isEmpty()) {
				return s;
			}
		}

		try {
			Field field = enumType.getDeclaredField(name);
			field.setAccessible(true);
			String s = DefaultRemapper.INSTANCE.remapField(enumType, field, name);

			if (!s.isEmpty()) {
				return s;
			}
		} catch (Exception ex) {
		}

		return name;
	}

	public final Class<T> enumType;
	public final T[] indexValues;
	public final Map<String, T> nameValues;

	private EnumTypeWrapper(Class<T> enumType) {
		this.enumType = enumType;
		this.indexValues = enumType.getEnumConstants();
		this.nameValues = new HashMap<>();

		for (T t : indexValues) {
			nameValues.put(getName(enumType, (Enum<?>) t).toLowerCase(), t);
		}
	}

	@Override
	public T wrap(Object o) {
		if (o instanceof CharSequence) {
			String s = o.toString().toLowerCase();
			T t = nameValues.get(s);

			if (t == null) {
				throw new IllegalArgumentException("'" + s + "' is not a valid enum constant! Valid values are: " + nameValues.keySet().stream().map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
			}

			return t;
		} else if (o instanceof Number) {
			int index = ((Number) o).intValue();

			if (index < 0 || index >= indexValues.length) {
				throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (indexValues.length - 1));
			}

			return indexValues[index];
		}

		return (T) o;
	}
}

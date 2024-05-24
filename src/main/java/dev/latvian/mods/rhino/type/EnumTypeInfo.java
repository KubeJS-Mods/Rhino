package dev.latvian.mods.rhino.type;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemappedEnumConstant;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnumTypeInfo extends ClassTypeInfo implements TypeWrapperFactory<Object> {
	static final Map<Class<?>, EnumTypeInfo> CACHE = new IdentityHashMap<>();

	public static String getName(Object e) {
		if (e instanceof RemappedEnumConstant c) {
			String s = c.getRemappedEnumConstantName();

			if (!s.isEmpty()) {
				return s;
			}
		}

		return ((Enum) e).name();
	}

	private List<Object> constants;

	EnumTypeInfo(Class<?> type) {
		super(type);
	}

	@Override
	public List<Object> enumConstants() {
		if (constants == null) {
			constants = List.of(asClass().getEnumConstants());
		}

		return constants;
	}

	@Override
	public Object wrap(Context cx, Object from, TypeInfo target) {
		if (from instanceof CharSequence) {
			String s = from.toString();

			if (s.isEmpty()) {
				return null;
			}

			for (var entry : enumConstants()) {
				if (getName(entry).equalsIgnoreCase(s)) {
					return entry;
				}
			}

			throw new IllegalArgumentException("'" + s + "' is not a valid enum constant! Valid values are: " + enumConstants().stream().map(EnumTypeInfo::getName).map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
		} else if (from instanceof Number) {
			int index = ((Number) from).intValue();

			if (index < 0 || index >= enumConstants().size()) {
				throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (enumConstants().size() - 1));
			}

			return enumConstants().get(index);
		}

		return from;
	}
}

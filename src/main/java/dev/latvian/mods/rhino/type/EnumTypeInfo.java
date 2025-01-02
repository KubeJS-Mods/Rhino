package dev.latvian.mods.rhino.type;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.RemappedEnumConstant;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
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
	private Map<String, Object> constantMap;

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

			var constants = enumConstants();

			if (constantMap == null) {
				constantMap = new HashMap<>(constants.size());

				for (var entry : constants) {
					var name = getName(entry);
					constantMap.put(name.toLowerCase(Locale.ROOT), entry);
					constantMap.put(name, entry);
				}
			}

			var lookup = constantMap.get(s);

			if (lookup != null) {
				return lookup;
			}

			for (var entry : constants) {
				if (getName(entry).equalsIgnoreCase(s)) {
					return entry;
				}
			}

			throw new IllegalArgumentException("'" + s + "' is not a valid enum constant! Valid values are: " + constants.stream().map(EnumTypeInfo::getName).map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
		} else if (from instanceof Number) {
			int index = ((Number) from).intValue();

			var constants = enumConstants();

			if (index < 0 || index >= constants.size()) {
				throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (constants.size() - 1));
			}

			return constants.get(index);
		}

		return from;
	}
}

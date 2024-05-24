package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.EnumTypeInfo;

/**
 * Implement this on a class to override == != === and !== checks in JavaScript
 */
public interface SpecialEquality {
	static boolean checkSpecialEquality(Context cx, Object o, Object o1, boolean shallow) {
		if (o == o1) {
			return true;
		} else if (o instanceof SpecialEquality s) {
			return s.specialEquals(cx, o1, shallow);
		} else if (o1 != null && o instanceof Enum<?> e) {
			if (o1 instanceof Number) {
				return e.ordinal() == ((Number) o1).intValue();
			} else {
				return EnumTypeInfo.getName(e).equalsIgnoreCase(String.valueOf(o1));
			}
		}

		return false;
	}

	default boolean specialEquals(Context cx, Object o, boolean shallow) {
		return equals(o);
	}
}

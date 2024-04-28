package dev.latvian.mods.rhino.util;

/**
 * Implement this on a class to override == != === and !== checks in JavaScript
 */
public interface SpecialEquality {
	static boolean checkSpecialEquality(Object o, Object o1, boolean shallow) {
		if (o == o1) {
			return true;
		} else if (o instanceof SpecialEquality s) {
			return s.specialEquals(o1, shallow);
		} else if (o != null && o1 != null && o.getClass().isEnum()) {
			if (o1 instanceof Number) {
				return ((Enum<?>) o).ordinal() == ((Number) o1).intValue();
			} else {
				return EnumTypeWrapper.getName(o.getClass(), (Enum<?>) o, true).equalsIgnoreCase(String.valueOf(o1));
			}
		}

		return false;
	}

	default boolean specialEquals(Object o, boolean shallow) {
		return equals(o);
	}
}

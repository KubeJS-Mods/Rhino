package dev.latvian.mods.rhino.util;

/**
 * Implement this on a class to override == != === and !== checks in JavaScript
 */
public interface SpecialEquality {
	default boolean specialEquals(Object o, boolean shallow) {
		return equals(o);
	}
}

package dev.latvian.mods.rhino.util.wrap;

import java.lang.reflect.Type;

@FunctionalInterface
public interface TypeWrapperValidator {
	TypeWrapperValidator ALWAYS_VALID = (from, target, genericTarget) -> true;

	boolean isValid(Object from, Class<?> target, Type genericTarget);
}

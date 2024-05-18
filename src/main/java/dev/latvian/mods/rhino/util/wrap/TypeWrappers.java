package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.util.EnumTypeWrapper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class TypeWrappers {
	public final Map<Class<?>, TypeWrapper<?>> wrappers = new LinkedHashMap<>();

	public <T> void register(Class<T> target, TypeWrapperValidator validator, TypeWrapperFactory<T> factory) {
		if (target == null || target == Object.class) {
			throw new IllegalArgumentException("target can't be Object.class!");
		} else if (target.isArray()) {
			throw new IllegalArgumentException("target can't be an array!");
		} else if (wrappers.containsKey(target)) {
			throw new IllegalArgumentException("Wrapper for class " + target.getName() + " already exists!");
		} else {
			wrappers.put(target, new TypeWrapper<>(target, validator, factory));
		}
	}

	public <T> void register(Class<T> target, TypeWrapperFactory<T> factory) {
		register(target, TypeWrapperValidator.ALWAYS_VALID, factory);
	}

	public boolean hasWrapper(Object from, Class<?> target, Type genericTarget) {
		if (target.isEnum() || target.isRecord()) {
			return true;
		}

		var wrapper = wrappers.get(target);
		return wrapper != null && wrapper.validator().isValid(from, target, genericTarget);
	}

	@Nullable
	public TypeWrapperFactory<?> getWrapperFactory(@Nullable Object from, Class<?> target, Type genericTarget) {
		if (target == Object.class) {
			return null;
		}

		var wrapper = wrappers.get(target);

		if (wrapper != null && wrapper.validator().isValid(from, target, genericTarget)) {
			return wrapper.factory();
		} else if (target.isEnum()) {
			return EnumTypeWrapper.get(target);
		} else if (target.isRecord()) {

		}

		//else if (from != null && target.isArray() && !from.getClass().isArray() && target.getComponentType() == from.getClass() && !target.isPrimitive())
		//{
		//	return TypeWrapperFactory.OBJECT_TO_ARRAY;
		//}

		return null;
	}
}

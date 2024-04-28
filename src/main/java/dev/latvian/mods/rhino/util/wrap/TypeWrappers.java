package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.util.EnumTypeWrapper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class TypeWrappers {
	private final Map<Class<?>, TypeWrapper<?>> wrappers = new LinkedHashMap<>();

	@SuppressWarnings("unchecked")
	public <T> void register(Class<T> target, Predicate<Object> validator, TypeWrapperFactory<T> factory) {
		if (target == null || target == Object.class) {
			throw new IllegalArgumentException("target can't be Object.class!");
		} else if (target.isArray()) {
			throw new IllegalArgumentException("target can't be an array!");
		} else if (wrappers.containsKey(target)) {
			throw new IllegalArgumentException("Wrapper for class " + target.getName() + " already exists!");
		}

		TypeWrapper<T> typeWrapper0 = new TypeWrapper<>(target, validator, factory);
		wrappers.put(target, typeWrapper0);

		// I know this looks like cancer but it's actually pretty simple - grab T[].class, register ArrayTypeWrapperFactory
		// You may say that it would be better to just implement N-sized array checking directly in java parser, but this is way more efficient

		// 1D
		Class<T[]> target1 = (Class<T[]>) Array.newInstance(target, 0).getClass();
		TypeWrapper<T[]> typeWrapper1 = new TypeWrapper<>(target1, validator, new ArrayTypeWrapperFactory<>(typeWrapper0, target, target1));
		wrappers.put(target1, typeWrapper1);

		// 2D
		Class<T[][]> target2 = (Class<T[][]>) Array.newInstance(target1, 0).getClass();
		TypeWrapper<T[][]> typeWrapper2 = new TypeWrapper<>(target2, validator, new ArrayTypeWrapperFactory<>(typeWrapper1, target1, target2));
		wrappers.put(target2, typeWrapper2);

		// 3D
		Class<T[][][]> target3 = (Class<T[][][]>) Array.newInstance(target2, 0).getClass();
		TypeWrapper<T[][][]> typeWrapper3 = new TypeWrapper<>(target3, validator, new ArrayTypeWrapperFactory<>(typeWrapper2, target2, target3));
		wrappers.put(target3, typeWrapper3);

		// 4D.. yeah no. 3D already is an overkill
	}

	public <T> void register(Class<T> target, TypeWrapperFactory<T> factory) {
		register(target, TypeWrapper.ALWAYS_VALID, factory);
	}

	public <T> void registerSimple(Class<T> target, Predicate<Object> validator, TypeWrapperFactory.Simple<T> factory) {
		register(target, validator, factory);
	}

	public <T> void registerSimple(Class<T> target, TypeWrapperFactory.Simple<T> factory) {
		register(target, TypeWrapper.ALWAYS_VALID, factory);
	}

	@Nullable
	public TypeWrapperFactory<?> getWrapperFactory(Class<?> target, @Nullable Object from) {
		if (target == Object.class) {
			return null;
		}

		TypeWrapper<?> wrapper = wrappers.get(target);

		if (wrapper != null && wrapper.validator.test(from)) {
			return wrapper.factory;
		} else if (target.isEnum()) {
			return EnumTypeWrapper.get(target);
		}

		//else if (from != null && target.isArray() && !from.getClass().isArray() && target.getComponentType() == from.getClass() && !target.isPrimitive())
		//{
		//	return TypeWrapperFactory.OBJECT_TO_ARRAY;
		//}

		return null;
	}
}

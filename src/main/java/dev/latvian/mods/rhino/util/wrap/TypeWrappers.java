package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class TypeWrappers {
	public final Map<Class<?>, TypeWrapper<?>> wrappers = new IdentityHashMap<>();

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

	public <T> void registerDirect(Class<T> target, TypeWrapperValidator validator, DirectTypeWrapperFactory<T> factory) {
		register(target, validator, factory);
	}

	public <T> void registerDirect(Class<T> target, DirectTypeWrapperFactory<T> factory) {
		register(target, TypeWrapperValidator.ALWAYS_VALID, factory);
	}

	public boolean hasWrapper(Object from, TypeInfo target) {
		if (target instanceof TypeWrapperFactory<?>) {
			return true;
		}

		var wrapper = wrappers.get(target.asClass());
		return wrapper != null && wrapper.validator().isValid(from, target);
	}

	@Nullable
	public TypeWrapperFactory<?> getWrapperFactory(@Nullable Object from, TypeInfo target) {
		if (target == TypeInfo.OBJECT) {
			return null;
		}

		var cl = target.asClass();

		var wrapper = wrappers.get(cl);

		if (wrapper != null && wrapper.validator().isValid(from, target)) {
			return wrapper.factory();
		} else if (target instanceof TypeWrapperFactory<?> w) {
			return w;
		} else {
			return null;
		}
	}
}

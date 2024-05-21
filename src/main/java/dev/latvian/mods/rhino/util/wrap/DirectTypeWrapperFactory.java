package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;

import java.lang.reflect.Type;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface DirectTypeWrapperFactory<T> extends TypeWrapperFactory<T> {
	T wrap(Object from);

	default T wrap(Context cx, Object from, Class<?> target, Type genericTarget) {
		return wrap(from);
	}
}

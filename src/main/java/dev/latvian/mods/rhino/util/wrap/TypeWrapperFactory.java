package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;

import java.lang.reflect.Type;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TypeWrapperFactory<T> {
	T wrap(Context cx, Object from, Class<?> toType, Type toGenericType);
}

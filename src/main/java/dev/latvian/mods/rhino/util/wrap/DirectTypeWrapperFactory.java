package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface DirectTypeWrapperFactory<T> extends TypeWrapperFactory<T> {
	T wrap(Object from);

	default T wrap(Context cx, Object from, TypeInfo target) {
		return wrap(from);
	}
}

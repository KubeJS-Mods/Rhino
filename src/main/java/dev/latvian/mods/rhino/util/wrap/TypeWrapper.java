package dev.latvian.mods.rhino.util.wrap;

import java.util.function.Function;

/**
 * @author LatvianModder
 */
public final class TypeWrapper<T>
{
	public final Class<T> to;
	public final Function<Object, Object> function;

	TypeWrapper(Class<T> t, Function<Object, Object> f)
	{
		to = t;
		function = f;
	}
}

package dev.latvian.mods.rhino.util.wrap;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class TypeWrapper<T>
{
	public static final Predicate<Object> ALWAYS_VALID = o -> true;

	public final Class<T> target;
	public final Predicate<Object> validator;
	public final TypeWrapperFactory<T> factory;

	TypeWrapper(Class<T> t, Predicate<Object> v, TypeWrapperFactory<T> f)
	{
		target = t;
		validator = v;
		factory = f;
	}
}

package dev.latvian.mods.rhino.util.wrap;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TypeWrapperFactory<T>
{
	T wrap(Object o);
}

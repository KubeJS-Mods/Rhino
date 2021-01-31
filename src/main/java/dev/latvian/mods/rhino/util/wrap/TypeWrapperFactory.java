package dev.latvian.mods.rhino.util.wrap;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TypeWrapperFactory<T>
{
	//TypeWrapperFactory<Object> OBJECT_TO_ARRAY = o -> {
	//	Object array = Array.newInstance(o.getClass(), 1);
	//	Array.set(array, 0, o);
	//	return array;
	//};

	T wrap(Object o);
}

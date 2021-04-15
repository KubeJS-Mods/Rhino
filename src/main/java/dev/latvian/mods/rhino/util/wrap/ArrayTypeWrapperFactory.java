package dev.latvian.mods.rhino.util.wrap;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * @author LatvianModder
 */
public class ArrayTypeWrapperFactory<T> implements TypeWrapperFactory<T[]> {
	public final TypeWrapper<T> typeWrapper;
	public final Class<T> target;
	public final Class<T[]> arrayTarget;
	private final T[] emptyArray;

	@SuppressWarnings("unchecked")
	public ArrayTypeWrapperFactory(TypeWrapper<T> tw, Class<T> t, Class<T[]> at) {
		typeWrapper = tw;
		target = t;
		arrayTarget = at;
		emptyArray = (T[]) Array.newInstance(target, 0);
	}

	@Override
	@SuppressWarnings("all")
	public T[] wrap(Object o) {
		if (o instanceof Iterable) {
			int size;

			if (o instanceof List) {
				size = ((List) o).size();
			} else {
				size = 0;

				for (Object o1 : (Iterable<Object>) o) {
					size++;
				}
			}

			T[] array = (T[]) Array.newInstance(target, size);
			int index = 0;

			for (Object o1 : (Iterable<Object>) o) {
				if (typeWrapper.validator.test(o1)) {
					array[index] = typeWrapper.factory.wrap(o1);
					index++;
				}
			}

			return index == 0 ? emptyArray : index == array.length ? array : Arrays.copyOf(array, index, arrayTarget);
		}

		return emptyArray;
	}
}

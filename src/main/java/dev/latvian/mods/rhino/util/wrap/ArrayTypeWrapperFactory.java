package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

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
	public T[] wrap(Context cx, Object o) {
		if (o == null) {
			return emptyArray;
		} else if (o instanceof Iterable) {
			int size;

			if (o instanceof Collection) {
				size = ((Collection) o).size();
			} else {
				size = 0;

				for (Object o1 : (Iterable<Object>) o) {
					size++;
				}
			}

			if (size == 0) {
				return emptyArray;
			}

			T[] array = (T[]) Array.newInstance(target, size);
			int index = 0;

			for (Object o1 : (Iterable<Object>) o) {
				if (typeWrapper.validator.test(o1)) {
					array[index] = typeWrapper.factory.wrap(cx, o1);
					index++;
				}
			}

			return index == 0 ? emptyArray : index == array.length ? array : Arrays.copyOf(array, index, arrayTarget);
		} else if (o.getClass().isArray()) {
			int size = Array.getLength(o);

			if (size == 0) {
				return emptyArray;
			}

			T[] array = (T[]) Array.newInstance(target, size);
			int index = 0;

			for (int i = 0; i < array.length; i++) {
				Object o1 = Array.get(o, i);

				if (typeWrapper.validator.test(o1)) {
					array[index] = typeWrapper.factory.wrap(cx, o1);
					index++;
				}
			}

			return index == 0 ? emptyArray : index == array.length ? array : Arrays.copyOf(array, index, arrayTarget);
		} else if (typeWrapper.validator.test(o)) {
			T[] array = (T[]) Array.newInstance(target, 1);
			array[0] = typeWrapper.factory.wrap(cx, o);
			return array;
		}

		return emptyArray;
	}
}

package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.EvaluatorException;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeJavaList;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface ArrayValueProvider {
	ArrayValueProvider EMPTY = new ArrayValueProvider() {
		@Override
		public int getLength(Context cx) {
			return 0;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return null;
		}

		@Override
		public Object getErrorSource(Context cx) {
			return null;
		}
	};

	static ArrayValueProvider of(Object value) {
		return switch (value) {
			case NativeArray array -> fromNativeArray(array);
			case NativeJavaList list -> fromJavaList(list.list, list);
			case List<?> list -> fromJavaList(list, list);
			case Iterable<?> itr -> fromIterable(itr);
			case null, default -> value == null ? FromObject.FROM_NULL : new FromObject(value);
		};
	}

	int getLength(Context cx);

	Object getArrayValue(Context cx, int index);

	Object getErrorSource(Context cx);

	default Object createArray(Context cx, Class<?> target, Type genericTarget) {
		int len = getLength(cx);
		var arr = Array.newInstance(target, len);

		for (int i = 0; i < len; i++) {
			try {
				Array.set(arr, i, cx.jsToJava(getArrayValue(cx, i), target, genericTarget));
			} catch (EvaluatorException ee) {
				return cx.reportConversionError(getErrorSource(cx), target);
			}
		}

		return arr;
	}

	default Object createList(Context cx, Class<?> target, Type genericTarget) {
		int len = getLength(cx);

		if (len == 0) {
			return List.of();
		} else if (len == 1) {
			try {
				return List.of(cx.jsToJava(getArrayValue(cx, 0), target, genericTarget));
			} catch (EvaluatorException ee) {
				return cx.reportConversionError(getErrorSource(cx), target);
			}
		}

		var list = new ArrayList<>(len);

		for (int i = 0; i < len; i++) {
			try {
				list.add(cx.jsToJava(getArrayValue(cx, i), target, genericTarget));
			} catch (EvaluatorException ee) {
				return cx.reportConversionError(getErrorSource(cx), target);
			}
		}

		return list;
	}

	default Object createSet(Context cx, Class<?> target, Type genericTarget) {
		int len = getLength(cx);

		if (len == 0) {
			return Set.of();
		} else if (len == 1) {
			try {
				return Set.of(cx.jsToJava(getArrayValue(cx, 0), target, genericTarget));
			} catch (EvaluatorException ee) {
				return cx.reportConversionError(getErrorSource(cx), target);
			}
		}

		var set = new HashSet<>(len);

		for (int i = 0; i < len; i++) {
			try {
				set.add(cx.jsToJava(getArrayValue(cx, i), target, genericTarget));
			} catch (EvaluatorException ee) {
				return cx.reportConversionError(getErrorSource(cx), target);
			}
		}

		return set;
	}

	record FromObject(Object object) implements ArrayValueProvider {
		public static final FromObject FROM_NULL = new FromObject(null);

		@Override
		public int getLength(Context cx) {
			return 1;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return object;
		}

		@Override
		public Object getErrorSource(Context cx) {
			return object;
		}
	}

	static ArrayValueProvider fromNativeArray(NativeArray array) {
		return array.getLength() == 0 ? EMPTY : new FromNativeArray(array);
	}

	record FromNativeArray(NativeArray array) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return (int) array.getLength();
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return array.get(cx, index, array);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return array;
		}
	}

	static ArrayValueProvider fromJavaArray(Object array) {
		return Array.getLength(array) == 0 ? EMPTY : new FromJavaArray(array);
	}

	record FromJavaArray(Object array) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return Array.getLength(array);
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return Array.get(array, index);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return array;
		}
	}

	static ArrayValueProvider fromJavaList(List<?> list, Object errorSource) {
		return list.isEmpty() ? EMPTY : new FromJavaList(list, errorSource);
	}

	record FromJavaList(List<?> list, Object errorSource) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return list.size();
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return list.get(index);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return errorSource;
		}
	}

	static ArrayValueProvider fromIterable(Iterable<?> iterable) {
		int len;

		if (iterable instanceof Collection<?> c) {
			len = c.size();
		} else {
			len = 0;

			for (var ignored : iterable) {
				len++;
			}
		}

		return len == 0 ? EMPTY : new FromIterator(len, iterable.iterator(), iterable);
	}

	record FromIterator(int length, Iterator<?> iterator, Object errorSource) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return length;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return iterator.next();
		}

		@Override
		public Object getErrorSource(Context cx) {
			return errorSource;
		}
	}
}

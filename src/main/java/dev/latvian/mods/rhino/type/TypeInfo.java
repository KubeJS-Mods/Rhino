package dev.latvian.mods.rhino.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed interface TypeInfo permits NoTypeInfo, ClassTypeInfo, ArrayTypeInfo, ParameterizedTypeInfo {
	TypeInfo NONE = new NoTypeInfo();

	TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

	TypeInfo OBJECT = new ClassTypeInfo(Object.class);
	TypeInfo OBJECT_ARRAY = OBJECT.asArray();
	TypeInfo VOID = new ClassTypeInfo(Void.class);
	TypeInfo STRING = new ClassTypeInfo(String.class);
	TypeInfo STRING_ARRAY = STRING.asArray();
	TypeInfo BOOLEAN = new ClassTypeInfo(Boolean.class, true);
	TypeInfo NUMBER = new ClassTypeInfo(Number.class);
	TypeInfo BYTE = new ClassTypeInfo(Byte.class, true);
	TypeInfo SHORT = new ClassTypeInfo(Short.class, true);
	TypeInfo INT = new ClassTypeInfo(Integer.class, true);
	TypeInfo LONG = new ClassTypeInfo(Long.class, true);
	TypeInfo FLOAT = new ClassTypeInfo(Float.class, true);
	TypeInfo DOUBLE = new ClassTypeInfo(Double.class, true);
	TypeInfo CHARACTER = new ClassTypeInfo(Character.class, true);
	TypeInfo CLASS = new ClassTypeInfo(Class.class);
	TypeInfo DATE = new ClassTypeInfo(Date.class);

	TypeInfo RUNNABLE = new ClassTypeInfo(Runnable.class);
	TypeInfo RAW_CONSUMER = new ClassTypeInfo(Consumer.class);
	TypeInfo RAW_SUPPLIER = new ClassTypeInfo(Supplier.class);
	TypeInfo RAW_FUNCTION = new ClassTypeInfo(Function.class);
	TypeInfo RAW_PREDICATE = new ClassTypeInfo(Predicate.class);

	TypeInfo RAW_LIST = new ClassTypeInfo(List.class);
	TypeInfo RAW_SET = new ClassTypeInfo(Set.class);
	TypeInfo RAW_MAP = new ClassTypeInfo(Map.class);

	Class<?> asClass();

	default TypeInfo param(int index) {
		return NONE;
	}

	default boolean isPrimitive() {
		return false;
	}

	default boolean convert() {
		return true;
	}

	static TypeInfo of(Class<?> c) {
		if (c == null || c == Object.class) {
			return OBJECT;
		} else if (c == Void.class || c == Void.TYPE) {
			return VOID;
		} else if (c == String.class) {
			return STRING;
		} else if (c == Boolean.class || c == Boolean.TYPE) {
			return BOOLEAN;
		} else if (c == Number.class) {
			return NUMBER;
		} else if (c == Byte.class || c == Byte.TYPE) {
			return BYTE;
		} else if (c == Short.class || c == Short.TYPE) {
			return SHORT;
		} else if (c == Integer.class || c == Integer.TYPE) {
			return INT;
		} else if (c == Long.class || c == Long.TYPE) {
			return LONG;
		} else if (c == Float.class || c == Float.TYPE) {
			return FLOAT;
		} else if (c == Double.class || c == Double.TYPE) {
			return DOUBLE;
		} else if (c == Character.class || c == Character.TYPE) {
			return CHARACTER;
		} else if (c == Class.class) {
			return CLASS;
		} else if (c == Date.class) {
			return DATE;
		} else if (c == Runnable.class) {
			return RUNNABLE;
		} else if (c == Consumer.class) {
			return RAW_CONSUMER;
		} else if (c == Supplier.class) {
			return RAW_SUPPLIER;
		} else if (c == Function.class) {
			return RAW_FUNCTION;
		} else if (c == Predicate.class) {
			return RAW_PREDICATE;
		} else if (c == List.class) {
			return RAW_LIST;
		} else if (c == Set.class) {
			return RAW_SET;
		} else if (c == Map.class) {
			return RAW_MAP;
		} else if (c == Object[].class) {
			return OBJECT_ARRAY;
		} else if (c == String[].class) {
			return STRING_ARRAY;
		} else if (c.isArray()) {
			return of(c.getComponentType()).asArray();
		} else {
			synchronized (ClassTypeInfo.CACHE) {
				return ClassTypeInfo.CACHE.computeIfAbsent(c, ClassTypeInfo::new);
			}
		}
	}

	static TypeInfo of(Type type) {
		return switch (type) {
			case Class<?> clz -> of(clz);
			case ParameterizedType paramType -> of(paramType.getRawType()).withParams(ofArray(paramType.getActualTypeArguments()));
			case GenericArrayType arrType -> of(arrType.getGenericComponentType()).asArray();
			case TypeVariable<?> ignore -> NONE; // ClassTypeInfo.OBJECT
			case WildcardType wildcard -> of(wildcard.getUpperBounds()[0]);
			case null, default -> NONE;
		};
	}

	static TypeInfo[] ofArray(Type[] array) {
		if (array.length == 0) {
			return EMPTY_ARRAY;
		} else {
			var arr = new TypeInfo[array.length];

			for (int i = 0; i < array.length; i++) {
				arr[i] = of(array[i]);
			}

			return arr;
		}
	}

	default String signature() {
		return toString();
	}

	default TypeInfo componentType() {
		return NONE;
	}

	default Object newArray(int length) {
		return Array.newInstance(asClass(), length);
	}

	default TypeInfo asArray() {
		return new ArrayTypeInfo(this);
	}

	default TypeInfo withParams(TypeInfo... params) {
		if (params.length == 0) {
			return this;
		}

		return new ParameterizedTypeInfo(this, params);
	}
}

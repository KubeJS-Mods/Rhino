package dev.latvian.mods.rhino.type;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.IdentityHashMap;
import java.util.Map;

public class TypeUtils {
	static final Map<Class<?>, TypeInfo> IMMUTABLE_CACHE = new IdentityHashMap<>();

	private static void cacheType(TypeInfo... info) {
		for (var i : info) {
			IMMUTABLE_CACHE.putIfAbsent(i.asClass(), i);
		}
	}

	static {
		cacheType(
			TypeInfo.OBJECT_ARRAY,
			TypeInfo.PRIMITIVE_BOOLEAN,
			TypeInfo.PRIMITIVE_BOOLEAN_ARRAY,
			TypeInfo.PRIMITIVE_BYTE,
			TypeInfo.PRIMITIVE_BYTE_ARRAY,
			TypeInfo.PRIMITIVE_SHORT,
			TypeInfo.PRIMITIVE_SHORT_ARRAY,
			TypeInfo.PRIMITIVE_INT,
			TypeInfo.PRIMITIVE_INT_ARRAY,
			TypeInfo.PRIMITIVE_LONG,
			TypeInfo.PRIMITIVE_LONG_ARRAY,
			TypeInfo.PRIMITIVE_FLOAT,
			TypeInfo.PRIMITIVE_FLOAT_ARRAY,
			TypeInfo.PRIMITIVE_DOUBLE,
			TypeInfo.PRIMITIVE_DOUBLE_ARRAY,
			TypeInfo.PRIMITIVE_CHARACTER,
			TypeInfo.PRIMITIVE_CHARACTER_ARRAY
		);

		cacheType(
			TypeInfo.VOID,
			TypeInfo.BOOLEAN,
			TypeInfo.BYTE,
			TypeInfo.SHORT,
			TypeInfo.INT,
			TypeInfo.LONG,
			TypeInfo.FLOAT,
			TypeInfo.DOUBLE,
			TypeInfo.CHARACTER
		);

		cacheType(
			TypeInfo.NUMBER,
			TypeInfo.STRING,
			TypeInfo.STRING_ARRAY,
			TypeInfo.CLASS,
			TypeInfo.DATE,
			TypeInfo.CONTEXT,
			TypeInfo.SCRIPTABLE
		);

		cacheType(
			TypeInfo.RUNNABLE,
			TypeInfo.RAW_CONSUMER,
			TypeInfo.RAW_SUPPLIER,
			TypeInfo.RAW_FUNCTION,
			TypeInfo.RAW_PREDICATE,
			TypeInfo.RAW_LIST,
			TypeInfo.RAW_SET,
			TypeInfo.RAW_MAP,
			TypeInfo.RAW_OPTIONAL,
			TypeInfo.RAW_ENUM_SET
		);
	}

	public static Class<?> getRawType(Type type) {
		if (type instanceof Class<?> clz) {
			return clz;
		} else if (type instanceof ParameterizedType paramType) {
			var rawType = paramType.getRawType();

			if (rawType instanceof Class<?> clz) {
				return clz;
			}
		} else if (type instanceof GenericArrayType arrType) {
			var componentType = arrType.getGenericComponentType();
			return Array.newInstance(getRawType(componentType), 0).getClass();
		} else if (type instanceof TypeVariable) {
			return Object.class;
		} else if (type instanceof WildcardType wildcard) {
			return getRawType(wildcard.getUpperBounds()[0]);
		}

		return null;
	}

	public static Type getComponentType(Type type, Type fallback) {
		if (type instanceof Class<?> c) {
			return c.getComponentType();
		} else if (type instanceof GenericArrayType arr) {
			return arr.getGenericComponentType();
		} else {
			return fallback;
		}
	}
}

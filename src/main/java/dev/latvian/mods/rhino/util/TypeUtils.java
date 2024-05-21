package dev.latvian.mods.rhino.util;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class TypeUtils {
	public static final Type[] NO_TYPES = new Type[0];

	public static Type[] getGenericTypes(Type type) {
		return switch (type) {
			case ParameterizedType paramType -> paramType.getActualTypeArguments();
			case GenericArrayType arrayType -> new Type[]{arrayType.getGenericComponentType()};
			case WildcardType wildcard -> new Type[]{getRawType(wildcard.getUpperBounds()[0])};
			case null, default -> NO_TYPES;
		};
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

		var className = type == null ? "null" : type.getClass().getName();
		throw new IllegalArgumentException("Expected a Class, ParameterizedType, GenericArrayType, TypeVariable or WildcardType, but <" + type + "> is of type " + className);
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

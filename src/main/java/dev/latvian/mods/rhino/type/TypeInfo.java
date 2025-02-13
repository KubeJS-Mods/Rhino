package dev.latvian.mods.rhino.type;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TypeInfo {
	TypeInfo NONE = new NoTypeInfo();

	TypeInfo[] EMPTY_ARRAY = new TypeInfo[0];

	TypeInfo OBJECT = new BasicClassTypeInfo(Object.class);
	TypeInfo OBJECT_ARRAY = OBJECT.asArray();

	TypeInfo PRIMITIVE_VOID = new PrimitiveClassTypeInfo(Void.TYPE, null);
	TypeInfo PRIMITIVE_BOOLEAN = new PrimitiveClassTypeInfo(Boolean.TYPE, false);
	TypeInfo PRIMITIVE_BOOLEAN_ARRAY = PRIMITIVE_BOOLEAN.asArray();
	TypeInfo PRIMITIVE_BYTE = new PrimitiveClassTypeInfo(Byte.TYPE, (byte) 0);
	TypeInfo PRIMITIVE_BYTE_ARRAY = PRIMITIVE_BYTE.asArray();
	TypeInfo PRIMITIVE_SHORT = new PrimitiveClassTypeInfo(Short.TYPE, (short) 0);
	TypeInfo PRIMITIVE_SHORT_ARRAY = PRIMITIVE_SHORT.asArray();
	TypeInfo PRIMITIVE_INT = new PrimitiveClassTypeInfo(Integer.TYPE, 0);
	TypeInfo PRIMITIVE_INT_ARRAY = PRIMITIVE_INT.asArray();
	TypeInfo PRIMITIVE_LONG = new PrimitiveClassTypeInfo(Long.TYPE, 0L);
	TypeInfo PRIMITIVE_LONG_ARRAY = PRIMITIVE_LONG.asArray();
	TypeInfo PRIMITIVE_FLOAT = new PrimitiveClassTypeInfo(Float.TYPE, 0F);
	TypeInfo PRIMITIVE_FLOAT_ARRAY = PRIMITIVE_FLOAT.asArray();
	TypeInfo PRIMITIVE_DOUBLE = new PrimitiveClassTypeInfo(Double.TYPE, 0D);
	TypeInfo PRIMITIVE_DOUBLE_ARRAY = PRIMITIVE_DOUBLE.asArray();
	TypeInfo PRIMITIVE_CHARACTER = new PrimitiveClassTypeInfo(Character.TYPE, (char) 0);
	TypeInfo PRIMITIVE_CHARACTER_ARRAY = PRIMITIVE_CHARACTER.asArray();

	TypeInfo VOID = new BasicClassTypeInfo(Void.class);
	TypeInfo BOOLEAN = new BasicClassTypeInfo(Boolean.class);
	TypeInfo BYTE = new BasicClassTypeInfo(Byte.class);
	TypeInfo SHORT = new BasicClassTypeInfo(Short.class);
	TypeInfo INT = new BasicClassTypeInfo(Integer.class);
	TypeInfo LONG = new BasicClassTypeInfo(Long.class);
	TypeInfo FLOAT = new BasicClassTypeInfo(Float.class);
	TypeInfo DOUBLE = new BasicClassTypeInfo(Double.class);
	TypeInfo CHARACTER = new BasicClassTypeInfo(Character.class);

	TypeInfo NUMBER = new BasicClassTypeInfo(Number.class);
	TypeInfo STRING = new BasicClassTypeInfo(String.class);
	TypeInfo STRING_ARRAY = STRING.asArray();
	TypeInfo CLASS = new BasicClassTypeInfo(Class.class);
	TypeInfo DATE = new BasicClassTypeInfo(Date.class);
	TypeInfo CONTEXT = new BasicClassTypeInfo(Context.class);
	TypeInfo SCRIPTABLE = new BasicClassTypeInfo(Scriptable.class);

	TypeInfo RUNNABLE = new InterfaceTypeInfo(Runnable.class, Boolean.TRUE);
	TypeInfo RAW_CONSUMER = new InterfaceTypeInfo(Consumer.class, Boolean.TRUE);
	TypeInfo RAW_SUPPLIER = new InterfaceTypeInfo(Supplier.class, Boolean.TRUE);
	TypeInfo RAW_FUNCTION = new InterfaceTypeInfo(Function.class, Boolean.TRUE);
	TypeInfo RAW_PREDICATE = new InterfaceTypeInfo(Predicate.class, Boolean.TRUE);
	TypeInfo RAW_LIST = new InterfaceTypeInfo(List.class, Boolean.FALSE);
	TypeInfo RAW_SET = new InterfaceTypeInfo(Set.class, Boolean.FALSE);
	TypeInfo RAW_MAP = new InterfaceTypeInfo(Map.class, Boolean.FALSE);
	TypeInfo RAW_OPTIONAL = new BasicClassTypeInfo(Optional.class);
	TypeInfo RAW_ENUM_SET = new BasicClassTypeInfo(EnumSet.class);

	Class<?> asClass();

	default TypeInfo param(int index) {
		return NONE;
	}

	default boolean is(TypeInfo info) {
		return this == info;
	}

	default boolean isPrimitive() {
		return false;
	}

	default boolean shouldConvert() {
		return true;
	}

	static TypeInfo of(Class<?> c) {
		if (c == null || c == Object.class) {
			return OBJECT;
		} else if (c == Void.TYPE) {
			return PRIMITIVE_VOID;
		}

		var cached = TypeUtils.IMMUTABLE_CACHE.get(c);

		if (cached != null) {
			return cached;
		} else if (c.isArray()) {
			return of(c.getComponentType()).asArray();
		} else if (c.isEnum()) {
			synchronized (EnumTypeInfo.CACHE) {
				return EnumTypeInfo.CACHE.computeIfAbsent(c, EnumTypeInfo::new);
			}
		} else if (c.isRecord()) {
			synchronized (RecordTypeInfo.CACHE) {
				return RecordTypeInfo.CACHE.computeIfAbsent(c, RecordTypeInfo::new);
			}
		} else if (c.isInterface()) {
			synchronized (InterfaceTypeInfo.CACHE) {
				return InterfaceTypeInfo.CACHE.computeIfAbsent(c, InterfaceTypeInfo::new);
			}
		} else {
			synchronized (BasicClassTypeInfo.CACHE) {
				return BasicClassTypeInfo.CACHE.computeIfAbsent(c, BasicClassTypeInfo::new);
			}
		}
	}

	static VariableTypeInfo of(TypeVariable<?> typeVariable) {
		return (VariableTypeInfo) VariableTypeInfo.of(typeVariable);
	}

	static TypeInfo of(Type type) {
		return switch (type) {
			case Class<?> clz -> of(clz);
			case ParameterizedType paramType -> of(paramType.getRawType()).withParams(ofArray(paramType.getActualTypeArguments()));
			case GenericArrayType arrType -> of(arrType.getGenericComponentType()).asArray();
			case TypeVariable<?> variable -> VariableTypeInfo.of(variable); // ClassTypeInfo.OBJECT, or NONE?
			case WildcardType wildcard -> {
				var lower = wildcard.getLowerBounds();

				if (lower.length == 0) {
					var upper = wildcard.getUpperBounds();

					if (upper.length == 0 || upper[0] == Object.class) {
						yield NONE;
					}

					yield of(upper[0]);
				} else {
					yield of(lower[0]);
				}
			}
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

	static TypeInfo safeOf(Supplier<Type> supplier) {
		try {
			return of(supplier.get());
		} catch (Throwable ignored) {
			return TypeInfo.NONE;
		}
	}

	static TypeInfo[] safeOfArray(Supplier<Type[]> supplier) {
		try {
			return ofArray(supplier.get());
		} catch (Exception ignored) {
			return EMPTY_ARRAY;
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

	default boolean isFunctionalInterface() {
		return false;
	}

	default Map<String, RecordTypeInfo.Component> recordComponents() {
		return Map.of();
	}

	default List<Object> enumConstants() {
		return List.of();
	}

	default TypeInfo or(TypeInfo info) {
		return new JSOrTypeInfo(List.of(this, info));
	}

	default void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(this);
	}

	@Nullable
	default Object createDefaultValue() {
		return null;
	}

	default boolean isVoid() {
		return false;
	}

	default boolean isBoolean() {
		return false;
	}

	default boolean isByte() {
		return false;
	}

	default boolean isShort() {
		return false;
	}

	default boolean isInt() {
		return false;
	}

	default boolean isLong() {
		return false;
	}

	default boolean isFloat() {
		return false;
	}

	default boolean isDouble() {
		return false;
	}

	default boolean isCharacter() {
		return false;
	}

	default void collectContainedComponentClasses(Collection<Class<?>> classes) {
		classes.add(asClass());
	}

	default Set<Class<?>> getContainedComponentClasses() {
		var set = new LinkedHashSet<Class<?>>();
		collectContainedComponentClasses(set);
		return set;
	}

	/**
	 * @param mapping see {@link TypeConsolidator#getMapping(Class)}
	 * @return consolidated type, implementations aare encouraged to return {@code this} if the consolidated type
	 * is the same as original
	 */
	@NotNull
	default TypeInfo consolidate(@NotNull Map<VariableTypeInfo, TypeInfo> mapping) {
		return this;
	}
}

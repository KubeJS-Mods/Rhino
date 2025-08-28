package dev.latvian.mods.rhino.type;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ZZZank
 * @author Prunoideae
 */
public class VariableTypeInfo extends TypeInfoBase {
	static final Map<TypeVariable<?>, VariableTypeInfo> CACHE = new HashMap<>();

	private final TypeVariable<?> raw;
	// defer bound init to handle "T extends Enum<T>" properly
	private TypeInfo mainBound;

	VariableTypeInfo(TypeVariable<?> typeVariable) {
		this.raw = typeVariable;
	}

	/**
	 * ideally this method should never be called because all variable types should be consolidated
	 * via {@link VariableTypeInfo#consolidate(Map)} before being used.
	 *
	 * @see #consolidate(Map)
	 */
	@Override
	public Class<?> asClass() {
		return getMainBound().asClass();
	}

	@Override
	public boolean shouldConvert() {
		return asClass() != Object.class;
	}

	public String getName() {
		return raw.getName();
	}

	public TypeInfo getMainBound() {
		if (mainBound == null) {
			var bound = raw.getBounds()[0];
			if (bound == Object.class) {
				mainBound = NONE;
			} else {
				mainBound = TypeInfo.of(bound);
			}
		}
		return mainBound;
	}

	public TypeInfo[] getBounds() {
		var rawBounds = raw.getBounds();
		if (rawBounds.length == 1 && rawBounds[0] == Object.class) {
			// shortcut for most type variables with no bound
			return TypeInfo.EMPTY_ARRAY;
		}
		return Arrays.stream(rawBounds)
			.filter(t -> t != Object.class)
			.map(TypeInfo::of)
			.toArray(TypeInfo[]::new);
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public @NotNull TypeInfo consolidate(@NotNull Map<VariableTypeInfo, TypeInfo> mapping) {
		return mapping.getOrDefault(this, this);
	}
}

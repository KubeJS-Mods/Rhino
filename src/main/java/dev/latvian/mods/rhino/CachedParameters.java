package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CachedParameters(int count, List<Class<?>> types, List<TypeInfo> typeInfos, boolean firstArgContext, @Nullable TypeInfo varArgType) {
	public static final CachedParameters EMPTY = new CachedParameters(0, List.of(), List.of(), false, null);
	public static final CachedParameters EMPTY_FIRST_CX = new CachedParameters(0, List.of(), List.of(), true, null);

	public boolean typesMatch(Class<?>[] params) {
		if (params.length != types.size()) {
			return false;
		}

		for (int i = 0; i < params.length; i++) {
			if (types.get(i) != params[i]) {
				return false;
			}
		}

		return true;
	}

	public boolean isVarArg() {
		return varArgType != null;
	}
}

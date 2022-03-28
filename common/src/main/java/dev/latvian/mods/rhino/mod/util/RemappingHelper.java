package dev.latvian.mods.rhino.mod.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.FallbackRemapper;
import dev.latvian.mods.rhino.util.Remapper;

public class RemappingHelper {
	@ExpectPlatform
	public static MojangMappingRemapper getMojMapRemapper() {
		throw new AssertionError();
	}

	public static Remapper createModRemapper() {
		return new FallbackRemapper(DefaultRemapper.INSTANCE, getMojMapRemapper());
	}
}

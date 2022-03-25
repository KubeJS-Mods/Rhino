package dev.latvian.mods.rhino.mod.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.FallbackRemapper;
import dev.latvian.mods.rhino.util.Remapper;

public class RemappingHelper {
	@ExpectPlatform
	public static Remapper getMojMapRemapper() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean isServer() {
		throw new ArithmeticException();
	}

	@ExpectPlatform
	public static boolean isDev() {
		throw new ArithmeticException();
	}

	public static Remapper createModRemapper() {
		return new FallbackRemapper(DefaultRemapper.INSTANCE, getMojMapRemapper());
	}
}

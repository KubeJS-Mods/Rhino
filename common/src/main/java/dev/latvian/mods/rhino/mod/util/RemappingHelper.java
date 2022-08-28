package dev.latvian.mods.rhino.mod.util;

import com.google.common.base.Suppliers;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public class RemappingHelper {

	private static final Supplier<MinecraftRemapper> MINECRAFT_REMAPPER = Suppliers.memoize(() -> {
		var serviceLoader = ServiceLoader.load(MinecraftRemapper.class);
		return serviceLoader.findFirst().orElseThrow(() -> new RuntimeException("Could not find a MinecraftRemapper for your platform!"));
	});

	public static MinecraftRemapper getMinecraftRemapper() {
		return MINECRAFT_REMAPPER.get();
	}
}

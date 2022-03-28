package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;

public class RemappingHelperImpl {
	public static MinecraftRemapper getMinecraftRemapper() {
		return FabricRemapper.INSTANCE;
	}
}

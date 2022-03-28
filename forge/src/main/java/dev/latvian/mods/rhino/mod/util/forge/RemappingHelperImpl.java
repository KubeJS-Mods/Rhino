package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;

public class RemappingHelperImpl {
	public static MinecraftRemapper getMinecraftRemapper() {
		return ForgeRemapper.INSTANCE;
	}
}

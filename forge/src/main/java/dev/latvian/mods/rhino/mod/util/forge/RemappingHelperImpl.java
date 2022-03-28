package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MojangMappingRemapper;

public class RemappingHelperImpl {
	public static MojangMappingRemapper getMojMapRemapper() {
		return MMSRemapper.INSTANCE;
	}
}

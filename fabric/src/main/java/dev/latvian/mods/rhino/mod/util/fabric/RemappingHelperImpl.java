package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.mod.util.MojangMappingRemapper;

public class RemappingHelperImpl {
	public static MojangMappingRemapper getMojMapRemapper() {
		return MMIRemapper.INSTANCE;
	}
}

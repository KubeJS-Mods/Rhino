package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MojangMappingRemapper;

public class MMSRemapper extends MojangMappingRemapper {
	public static final MMSRemapper INSTANCE = new MMSRemapper();

	private MMSRemapper() {
		super("forge");
	}

	@Override
	public boolean isInvalid() {
		return true;
	}

	@Override
	public void init(MojMapClasses mojMapClasses) {
		// TODO: Implement SRG -> MM mapping
	}
}
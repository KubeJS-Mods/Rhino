package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MojangMappingRemapper;
import net.minecraftforge.fml.loading.FMLLoader;

public class MMSRemapper extends MojangMappingRemapper {
	public static final MMSRemapper INSTANCE = new MMSRemapper();

	private MMSRemapper() {
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public String getModLoader() {
		return "forge";
	}

	@Override
	public boolean isServer() {
		return FMLLoader.getDist().isDedicatedServer();
	}

	@Override
	public String getRuntimeMappings() {
		return "srg";
	}

	@Override
	public void init(MojMapClasses mojMapClasses) {
		// TODO: Implement SRG -> MM mapping
	}
}
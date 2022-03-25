package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.util.Remapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class RemappingHelperImpl {
	public static Remapper getMojMapRemapper() {
		return MMIRemapper.INSTANCE;
	}

	public static boolean isServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	public static boolean isDev() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}
}

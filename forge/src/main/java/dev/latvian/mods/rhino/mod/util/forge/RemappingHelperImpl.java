package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.util.Remapper;
import net.minecraftforge.fml.loading.FMLLoader;

public class RemappingHelperImpl {
	public static Remapper getMojMapRemapper() {
		return MMSRemapper.INSTANCE;
	}

	public static boolean isServer() {
		return FMLLoader.getDist().isDedicatedServer();
	}

	public static boolean isDev() {
		return !FMLLoader.isProduction();
	}
}

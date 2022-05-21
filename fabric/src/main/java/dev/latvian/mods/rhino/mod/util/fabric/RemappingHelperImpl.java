package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;

public class RemappingHelperImpl {
	private static MinecraftRemapper remapper;

	public static MinecraftRemapper getMinecraftRemapper() {
		if (remapper == null) {
			try {
				Class.forName("net.fabricmc.loader.impl.launch.FabricLauncherBase");
				remapper = getFabricRemapper();
			} catch (ClassNotFoundException ex) {
				try {
					Class.forName("net.fabricmc.loader.launch.common.FabricLauncherBase");
					remapper = getOldFabricRemapper();
				} catch (ClassNotFoundException ex2) {
					throw new RuntimeException("Rhino is not supported on this platform! Report this as a bug!");
				}
			}
		}

		return remapper;
	}

	private static MinecraftRemapper getFabricRemapper() {
		return new FabricRemapper();
	}

	private static MinecraftRemapper getOldFabricRemapper() {
		return new OldFabricRemapper();
	}
}

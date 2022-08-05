package dev.latvian.mods.rhino.mod.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class RhinoPropertiesImpl {
	public static Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}
}

package dev.latvian.mods.rhino.mod.util.forge;

import net.minecraftforge.fml.loading.FMLLoader;

import java.nio.file.Path;

public class RhinoPropertiesImpl {
	public static Path getGameDir() {
		return FMLLoader.getGamePath();
	}
}

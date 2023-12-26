package dev.latvian.mods.rhino.mod.util.forge;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class RhinoPropertiesImpl {
	public static Path getGameDir() {
		return FMLLoader.getGamePath();
	}

	public static boolean isDev() {
		return !FMLLoader.isProduction();
	}

	public static InputStream openResource(String path) throws Exception {
		return Files.newInputStream(ModList.get().getModFileById("rhino").getFile().findResource(path));
	}
}

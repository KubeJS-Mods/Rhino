package dev.latvian.mods.rhino.mod.util.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class RhinoPropertiesImpl {
	public static Path getGameDir() {
		return FabricLoader.getInstance().getGameDir();
	}

	public static boolean isDev() {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
	}

	public static boolean needsRemapping() {
		return true;
	}

	public static InputStream openResource(String path) throws Exception {
		return Files.newInputStream(FabricLoader.getInstance().getModContainer("rhino").get().findPath(path).get());
	}
}

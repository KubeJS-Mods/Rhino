package dev.latvian.mods.rhino.mod.util;

import com.mojang.logging.LogUtils;
import dev.architectury.injectables.annotations.ExpectPlatform;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public enum RhinoProperties {

	INSTANCE;

	@ExpectPlatform
	public static Path getGameDir() {
		throw new AssertionError();
	}

	private final Properties properties;
	public boolean forceLocalMappings;
	public String srgRemoteUrl;
	private boolean writeProperties;

	RhinoProperties() {
		properties = new Properties();

		try {
			var propertiesFile = getGameDir().resolve("rhino.local.properties");
			writeProperties = false;

			if (Files.exists(propertiesFile)) {
				try (Reader reader = Files.newBufferedReader(propertiesFile)) {
					properties.load(reader);
				}
			} else {
				writeProperties = true;
			}

			forceLocalMappings = get("forceLocalMappings", false);
			srgRemoteUrl = get("srgRemoteUrl", "https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/$version/joined.tsrg");

			if (writeProperties) {
				try (Writer writer = Files.newBufferedWriter(propertiesFile)) {
					properties.store(writer, "Local properties for Rhino, please do not push this to version control if you don't know what you're doing!");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		LogUtils.getLogger().info("Rhino properties loaded.");
	}

	private void remove(String key) {
		var s = properties.getProperty(key);

		if (s != null) {
			properties.remove(key);
			writeProperties = true;
		}
	}

	private String get(String key, String def) {
		var s = properties.getProperty(key);

		if (s == null) {
			properties.setProperty(key, def);
			writeProperties = true;
			return def;
		}

		return s;
	}

	private boolean get(String key, boolean def) {
		return get(key, def ? "true" : "false").equals("true");
	}
}

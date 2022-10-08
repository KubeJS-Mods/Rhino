package dev.latvian.mods.rhino.mod.util;

import com.google.common.base.Suppliers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.DefaultRemapper;
import dev.latvian.mods.rhino.util.Remapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RemappingHelper {
	public static final boolean GENERATE = System.getProperty("generaterhinomappings", "0").equals("1");
	private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().disableHtmlEscaping().create();
	public static final Logger LOGGER = LoggerFactory.getLogger("Rhino Script Remapper");

	public record MappingContext(String mcVersion, MojangMappings mappings) {
	}

	public interface Callback {
		void generateMappings(MappingContext context) throws Exception;
	}

	private static final Supplier<Remapper> MINECRAFT_REMAPPER = Suppliers.memoize(() -> {
		try (var in = Objects.requireNonNull(RemappingHelper.class.getResourceAsStream("/mm.jsmappings"))) {
			return MinecraftRemapper.load(new DataInputStream(new BufferedInputStream(new GZIPInputStream(in))));
		} catch (Exception ex) {
			ex.printStackTrace();
			LOGGER.error("Failed to load Rhino Minecraft remapper!", ex);
			return DefaultRemapper.INSTANCE;
		}
	});

	public static Remapper getMinecraftRemapper() {
		return MINECRAFT_REMAPPER.get();
	}

	public static Reader createReader(String url) throws Exception {
		LOGGER.info("Fetching " + url + "...");
		var connection = new URL(url).openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(10000);
		return new InputStreamReader(new BufferedInputStream(connection.getInputStream()), StandardCharsets.UTF_8);
	}

	public static void run(String mcVersion, Callback callback) {
		try {
			generate(mcVersion, callback);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void generate(String mcVersion, Callback callback) throws Exception {
		if (mcVersion.isEmpty()) {
			throw new RuntimeException("Invalid Minecraft version!");
		}

		try (var metaInfoReader = createReader("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")) {
			for (var metaInfo : GSON.fromJson(metaInfoReader, JsonObject.class).get("versions").getAsJsonArray()) {
				if (metaInfo.getAsJsonObject().get("id").getAsString().equals(mcVersion)) {
					String metaUrl = metaInfo.getAsJsonObject().get("url").getAsString();

					try (var metaReader = createReader(metaUrl)) {
						var meta = GSON.fromJson(metaReader, JsonObject.class);

						if (meta.get("downloads") instanceof JsonObject o && o.get("client_mappings") instanceof JsonObject cmap && cmap.has("url")) {
							try (var cmapReader = createReader(cmap.get("url").getAsString())) {
								var mojangMappings = MojangMappings.parse(IOUtils.readLines(cmapReader));
								callback.generateMappings(new MappingContext(mcVersion, mojangMappings));
								mojangMappings.cleanup();

								try (var out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(Path.of("mm.jsmappings")))))) {
									mojangMappings.write(out);
								}

								LOGGER.info("Finished generating mappings!");
								return;
							}
						} else {
							throw new RuntimeException("This Minecraft version doesn't have mappings!");
						}
					}
				}
			}
		}

		throw new RuntimeException("Failed for unknown reason!");
	}
}

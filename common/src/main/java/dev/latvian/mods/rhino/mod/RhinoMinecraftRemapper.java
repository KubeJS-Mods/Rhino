package dev.latvian.mods.rhino.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RhinoMinecraftRemapper {
	private static final Gson GSON = new GsonBuilder().setLenient().setPrettyPrinting().disableHtmlEscaping().create();

	private static Reader createReader(String url) throws Exception {
		var connection = new URL(url).openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(10000);
		return new InputStreamReader(new BufferedInputStream(connection.getInputStream()), StandardCharsets.UTF_8);
	}

	public static void run(Supplier<String> mcVersionGetter, Consumer<MappingContext> callback) {
		try {
			generate(mcVersionGetter, callback);
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private static void generate(Supplier<String> mcVersionGetter, Consumer<MappingContext> callback) throws Exception {
		String mcVersion = mcVersionGetter.get();

		if (mcVersion.isEmpty()) {
			throw new RuntimeException("Invalid Minecraft version!");
		}

		System.out.println("Fetching metadata json for " + mcVersion + "...");

		try (var metaInfoReader = createReader("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")) {
			for (var metaInfo : GSON.fromJson(metaInfoReader, JsonObject.class).get("versions").getAsJsonArray()) {
				if (metaInfo.getAsJsonObject().get("id").getAsString().equals(mcVersion)) {
					String metaUrl = metaInfo.getAsJsonObject().get("url").getAsString();
					System.out.println("Fetching " + metaUrl + "...");

					try (var metaReader = createReader(metaUrl)) {
						var meta = GSON.fromJson(metaReader, JsonObject.class);

						if (meta.get("downloads") instanceof JsonObject o && o.get("client_mappings") instanceof JsonObject cmap && cmap.has("url") && o.get("server_mappings") instanceof JsonObject smap && smap.has("url")) {
							try (var cmapReader = createReader(cmap.get("url").getAsString());
								 var smapReader = createReader(smap.get("url").getAsString())) {
								var cmappings = MojangMappings.parse(IOUtils.readLines(cmapReader));
								var smappings = MojangMappings.parse(IOUtils.readLines(smapReader));

								Files.write(Path.of("client.rhinomap"), cmappings.write());
								// Files.write(Path.of("server.rhinomap"), smappings.write());

								// TODO: finish this, actually remap to each modloader internals

								callback.accept(new MappingContext(cmappings, smappings));
								throw new RuntimeException("Finished generating mappings! Ignore this crash!");
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

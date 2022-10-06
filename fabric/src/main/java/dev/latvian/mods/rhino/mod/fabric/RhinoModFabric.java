package dev.latvian.mods.rhino.mod.fabric;

import dev.latvian.mods.rhino.mod.MappingContext;
import dev.latvian.mods.rhino.mod.RhinoMinecraftRemapper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class RhinoModFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		if (System.getProperty("generaterhinomappings", "0").equals("1")) {
			RhinoMinecraftRemapper.run(RhinoModFabric::getMcVersion, RhinoModFabric::generateMappings);
		}
	}

	public static String getMcVersion() {
		return FabricLoader.getInstance().getModContainer("minecraft").map(ModContainer::getMetadata).map(m -> m.getVersion().getFriendlyString()).orElse("");
	}

	public static void generateMappings(MappingContext context) {
	}
}

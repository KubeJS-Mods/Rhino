package dev.latvian.mods.rhino.mod.forge;

import dev.latvian.mods.rhino.mod.MappingContext;
import dev.latvian.mods.rhino.mod.RhinoMinecraftRemapper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod("rhino")
public class RhinoModForge {
	public RhinoModForge() {
		FMLJavaModLoadingContext.get().getModEventBus().register(RhinoModForge.class);
	}

	@SubscribeEvent
	public static void loaded(FMLCommonSetupEvent event) {
		if (System.getProperty("generaterhinomappings", "0").equals("1")) {
			RhinoMinecraftRemapper.run(RhinoModForge::getMcVersion, RhinoModForge::generateMappings);
		}
	}

	public static String getMcVersion() {
		return FMLLoader.versionInfo().mcVersion();
	}

	public static void generateMappings(MappingContext context) {
	}
}

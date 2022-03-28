package dev.latvian.mods.rhino.mod.fabric;

import dev.latvian.mods.rhino.mod.util.RemappingHelper;
import net.fabricmc.api.ModInitializer;

public class RhinoModFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		RemappingHelper.getMojMapRemapper();
	}
}

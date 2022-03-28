package dev.latvian.mods.rhino.mod.forge;

import dev.latvian.mods.rhino.mod.util.forge.ForgeRemapper;
import net.minecraftforge.fml.common.Mod;

@Mod("rhino")
public class RhinoModForge {
	public RhinoModForge() {
		ForgeRemapper.INSTANCE.getModLoader();
	}
}

package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ForgeRemapper extends MinecraftRemapper {
	public static final ForgeRemapper INSTANCE = new ForgeRemapper();

	private ForgeRemapper() {
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public String getModLoader() {
		return "forge";
	}

	@Override
	public boolean isServer() {
		return FMLLoader.getDist().isDedicatedServer();
	}

	@Override
	public String getRuntimeMappings() {
		return FMLLoader.isProduction() ? "srg" : "dev";
	}

	@Override
	public void init(MinecraftClasses minecraftClasses) throws Exception {
		// TODO: Implement SRG -> MM mapping

		for (String s : IOUtils.toString(new URL("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + getMcVersion() + "/joined.tsrg"), StandardCharsets.UTF_8).split("\n")) {
			s = s.trim();
		}
	}
}
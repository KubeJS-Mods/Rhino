package dev.latvian.mods.rhino.mod.util.forge;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForgeRemapper extends MinecraftRemapper {
	public static final ForgeRemapper INSTANCE = new ForgeRemapper();

	private ForgeRemapper() {
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
		RemappedClass current = null;
		RemappedClass mmCurrent = null;

		String[] srg = IOUtils.toString(new URL("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + getMcVersion() + "/joined.tsrg"), StandardCharsets.UTF_8).split("\n");
		Pattern pattern = Pattern.compile("[\t ]");
		Pattern argPattern = Pattern.compile("L([\\w/$]+);");

		for (int i = 1; i < srg.length; i++) {
			String[] s = pattern.split(srg[i]);

			if (s.length < 3 || s[1].isEmpty()) {
				continue;
			}

			if (!s[0].isEmpty()) {
				mmCurrent = minecraftClasses.rawLookup().get(s[0]);

				if (mmCurrent != null) {
					current = new RemappedClass(mmCurrent.mappedName, mmCurrent.mappedName);
					classMap.put(mmCurrent.mappedName, current);
				}
			} else if (current != null && mmCurrent != null) {
				if (s.length == 5) {
					if (s[1].equals("<init>") || s[1].equals("<clinit>")) {
						continue;
					}

					String a = s[2].substring(0, s[2].lastIndexOf(')') + 1);
					String m = mmCurrent.getChild(s[1] + a);

					if (!m.isEmpty()) {
						if (current.children == null) {
							current.children = new HashMap<>();
						}

						StringBuilder sb = new StringBuilder(s[3]);
						Matcher matcher = argPattern.matcher(a);

						while (matcher.find()) {
							String g = matcher.group(1);
							RemappedClass c = minecraftClasses.rawLookup().get(g);
							matcher.appendReplacement(sb, "L" + (c == null ? g : c.mappedName.replace('.', '/')).replace("$", "\\$") + ";");
						}

						matcher.appendTail(sb);
						current.children.put(sb.toString(), m);
					}
				} else if (s.length == 4) {
					String m = mmCurrent.getChild(s[1]);

					if (!m.isEmpty()) {
						if (current.children == null) {
							current.children = new HashMap<>();
						}

						current.children.put(s[2], m);
					}
				}
			}
		}

		classMap.entrySet().removeIf(RemappedClass::isUseless);
	}

	@Override
	public Path getLocalRhinoDir() {
		return FMLPaths.GAMEDIR.get().resolve("local/rhino");
	}
}
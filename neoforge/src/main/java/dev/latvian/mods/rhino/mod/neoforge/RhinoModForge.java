package dev.latvian.mods.rhino.mod.neoforge;

import dev.latvian.mods.rhino.mod.util.MojangMappings;
import dev.latvian.mods.rhino.mod.util.RemappingHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Mod("rhino")
public class RhinoModForge {
	public RhinoModForge(IEventBus bus) {
		bus.register(RhinoModForge.class);
	}

	@SubscribeEvent
	public static void loaded(FMLCommonSetupEvent event) {
		if (RemappingHelper.GENERATE) {
			RemappingHelper.run(FMLLoader.versionInfo().mcVersion(), RhinoModForge::generateMappings);
		}
	}

	private static void generateMappings(RemappingHelper.MappingContext context) throws Exception {
		MojangMappings.ClassDef current = null;

		var srg = new ArrayList<String>();

		try (var reader = new BufferedReader(RemappingHelper.createReader("https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/" + context.mcVersion() + "/joined.tsrg"))) {
			String line;

			while ((line = reader.readLine()) != null) {
				srg.add(line);
			}
		}

		var pattern = Pattern.compile("[\t ]");

		for (int i = 1; i < srg.size(); i++) {
			var s = pattern.split(srg.get(i));

			if (s.length < 3 || s[1].isEmpty()) {
				continue;
			}

			if (!s[0].isEmpty()) {
				s[0] = s[0].replace('/', '.');
				current = context.mappings().getClass(s[0]);

				if (current != null) {
					RemappingHelper.LOGGER.info("- Checking class " + s[0] + " ; " + current.displayName);
				} else {
					RemappingHelper.LOGGER.info("- Skipping class " + s[0]);
				}
			} else if (current != null) {
				if (s.length == 5) {
					if (s[1].equals("<init>") || s[1].equals("<clinit>")) {
						continue;
					}

					var sigs = s[2].substring(0, s[2].lastIndexOf(')') + 1).replace('/', '.');
					var sig = new MojangMappings.NamedSignature(s[1], context.mappings().readSignatureFromDescriptor(sigs));
					var m = current.members.get(sig);

					if (m != null && !m.mmName().equals(s[3])) {
						m.unmappedName().setValue(s[3]);
						RemappingHelper.LOGGER.info("Remapped method " + s[3] + sigs + " to " + m.mmName());
					} else if (m == null && !current.ignoredMembers.contains(sig)) {
						RemappingHelper.LOGGER.info("Method " + s[3] + " [" + sig + "] not found!");
					}
				} else if (s.length == 4) {
					var sig = new MojangMappings.NamedSignature(s[1], null);
					var m = current.members.get(sig);

					if (m != null) {
						if (!m.mmName().equals(s[2])) {
							m.unmappedName().setValue(s[2]);
							RemappingHelper.LOGGER.info("Remapped field " + s[2] + " [" + m.rawName() + "] to " + m.mmName());
						}
					} else if (!current.ignoredMembers.contains(sig)) {
						RemappingHelper.LOGGER.info("Field " + s[2] + " [" + s[1] + "] not found!");
					}
				}
			}
		}
	}
}
package dev.latvian.mods.rhino.mod.fabric;

import dev.latvian.mods.rhino.mod.util.MojangMappings;
import dev.latvian.mods.rhino.mod.util.RemappingHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

public class RhinoModFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		if (RemappingHelper.GENERATE) {
			RemappingHelper.run(FabricLoader.getInstance().getModContainer("minecraft").map(ModContainer::getMetadata).map(m -> m.getVersion().getFriendlyString()).orElse(""), RhinoModFabric::generateMappings);
		}
	}

	private static void generateMappings(RemappingHelper.MappingContext context) throws Exception {
		var runtimeNamespace = FabricLauncherBase.getLauncher().getTargetNamespace();
		var rawNamespace = "official";
		var tinyTree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings();

		for (var classDef : tinyTree.getClasses()) {
			var unmappedClassName = classDef.getName(runtimeNamespace).replace('/', '.');
			var rawClassName = classDef.getName(rawNamespace);

			RemappingHelper.LOGGER.info("- Checking class " + rawClassName);

			var mmClass = context.mappings().getClass(rawClassName.replace('/', '.'));

			if (mmClass != null) {
				if (!mmClass.mmName.equals(unmappedClassName)) {
					mmClass.unmappedName().setValue(unmappedClassName);
				}

				RemappingHelper.LOGGER.info("Remapped class " + unmappedClassName + " to " + mmClass.displayName);

				for (var fieldDef : classDef.getFields()) {
					var rawFieldName = fieldDef.getName(rawNamespace);
					var sig = new MojangMappings.NamedSignature(rawFieldName, null);
					var mmField = mmClass.members.get(sig);

					if (mmField != null) {
						var unmappedFieldName = fieldDef.getName(runtimeNamespace);

						if (!unmappedFieldName.equals(mmField.mmName())) {
							mmField.unmappedName().setValue(unmappedFieldName);
							RemappingHelper.LOGGER.info("Remapped field " + unmappedFieldName + " [" + mmField.rawName() + "] to " + mmField.mmName());
						}
					} else if (!mmClass.ignoredMembers.contains(sig)) {
						RemappingHelper.LOGGER.info("Field " + sig + " not found!");
					}
				}

				for (var methodDef : classDef.getMethods()) {
					var rawMethodName = methodDef.getName(rawNamespace);
					var rawMethodDesc = methodDef.getDescriptor(rawNamespace);
					var sig = new MojangMappings.NamedSignature(rawMethodName, context.mappings().readSignatureFromDescriptor(rawMethodDesc));
					var mmMethod = mmClass.members.get(sig);

					if (mmMethod != null) {
						var unmappedMethodName = methodDef.getName(runtimeNamespace);

						if (!unmappedMethodName.equals(mmMethod.mmName())) {
							mmMethod.unmappedName().setValue(unmappedMethodName);
							RemappingHelper.LOGGER.info("Remapped method " + unmappedMethodName + rawMethodDesc + " to " + mmMethod.mmName());
						}
					} else if (!mmClass.ignoredMembers.contains(sig)) {
						RemappingHelper.LOGGER.info("Method " + sig + " not found!");
					}
				}
			}
		}
	}
}

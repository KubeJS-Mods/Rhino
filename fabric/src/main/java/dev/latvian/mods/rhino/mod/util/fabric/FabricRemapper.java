package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.mod.util.MinecraftRemapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.TinyTree;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FabricRemapper extends MinecraftRemapper {

	@Override
	public String getModLoader() {
		return "fabric";
	}

	@Override
	public boolean isServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	@Override
	public String getRuntimeMappings() {
		// still doesnt know difference between mojamp and yarn in dev, but better than nothing
		return FabricLauncherBase.getLauncher().getTargetNamespace();
	}

	@Override
	public void init(MinecraftClasses minecraftClasses) {
		String runtimeNamespace = FabricLauncherBase.getLauncher().getTargetNamespace();
		String rawNamespace = "official";
		TinyTree tinyTree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings();
		Map<String, String> children = new HashMap<>();

		for (ClassDef classDef : tinyTree.getClasses()) {
			String runtimeClassName = classDef.getName(runtimeNamespace).replace('/', '.');
			String rawClassName = classDef.getName(rawNamespace).replace('/', '.');
			RemappedClass mm = minecraftClasses.rawLookup().get(rawClassName);

			if (mm != null) {
				children.clear();
				for (FieldDef fieldDef : classDef.getFields()) {
					String rawFieldName = fieldDef.getName(rawNamespace);
					String mappedFieldName = mm.getChild(rawFieldName);

					if (!mappedFieldName.isEmpty()) {
						String runtimeFieldName = fieldDef.getName(runtimeNamespace);

						if (!runtimeFieldName.equals(mappedFieldName)) {
							children.put(runtimeFieldName, mappedFieldName);
						}
					}
				}

				for (MethodDef methodDef : classDef.getMethods()) {
					String rawMethodName0 = methodDef.getName(rawNamespace) + methodDef.getDescriptor(rawNamespace);
					String rawMethodName = rawMethodName0.substring(0, rawMethodName0.lastIndexOf(')') + 1);
					String mappedMethodName = mm.getChild(rawMethodName);

					if (!mappedMethodName.isEmpty()) {
						String runtimeMethodName = methodDef.getName(runtimeNamespace);

						if (!runtimeMethodName.equals(mappedMethodName)) {
							String runtimeMethodDesc = methodDef.getDescriptor(runtimeNamespace);
							children.put(runtimeMethodName + runtimeMethodDesc.substring(0, runtimeMethodDesc.lastIndexOf(')') + 1), mappedMethodName);
						}
					}
				}

				if (!children.isEmpty() || !runtimeClassName.equals(mm.mappedName)) {
					RemappedClass rc = new RemappedClass(runtimeClassName, mm.mappedName);

					if (!children.isEmpty()) {
						rc.children = new HashMap<>(children);
					}

					classMap.put(runtimeClassName, rc);
				}
			}
		}
	}

	@Override
	public Path getLocalRhinoDir() {
		return FabricLoader.getInstance().getGameDir().resolve(".rhino_cache");
	}
}

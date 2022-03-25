package dev.latvian.mods.rhino.mod.util.fabric;

import dev.latvian.mods.rhino.mod.util.MojangMappingRemapper;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.FieldDef;
import net.fabricmc.mapping.tree.MethodDef;
import net.fabricmc.mapping.tree.TinyTree;

import java.util.HashMap;
import java.util.Map;

public class MMIRemapper extends MojangMappingRemapper {
	public static final MMIRemapper INSTANCE = new MMIRemapper();

	private MMIRemapper() {
		super("fabric");
	}

	@Override
	public boolean isInvalid() {
		return !FabricLauncherBase.getLauncher().getTargetNamespace().equals("intermediary");
	}

	@Override
	public void init(MojMapClasses mojMapClasses) {
		String runtimeNamespace = "intermediary";
		String rawNamespace = "official";
		TinyTree tinyTree = FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings();

		for (ClassDef classDef : tinyTree.getClasses()) {
			String runtimeClassName = classDef.getName(runtimeNamespace).replace('/', '.');
			String rawClassName = classDef.getName(rawNamespace).replace('/', '.');
			MojMapClass c = mojMapClasses.rawLookup().get(rawClassName);

			if (c != null) {
				Map<String, String> children = new HashMap<>();

				for (FieldDef fieldDef : classDef.getFields()) {
					String rawFieldName = fieldDef.getName(rawNamespace);
					String mappedFieldName = c.children().get(rawFieldName);

					if (mappedFieldName != null) {
						String runtimeFieldName = fieldDef.getName(runtimeNamespace);

						if (!runtimeFieldName.equals(mappedFieldName)) {
							children.put(runtimeFieldName, mappedFieldName);
						}
					}
				}

				for (MethodDef methodDef : classDef.getMethods()) {
					String rawMethodName = methodDef.getName(rawNamespace) + methodDef.getDescriptor(rawNamespace);
					String mappedMethodName = c.children().get(rawMethodName.substring(0, rawMethodName.lastIndexOf(')') + 1));

					if (mappedMethodName != null) {
						String runtimeMethodName = methodDef.getName(runtimeNamespace);

						if (!runtimeMethodName.equals(mappedMethodName)) {
							children.put(runtimeMethodName + methodDef.getDescriptor(runtimeNamespace), mappedMethodName);
						}
					}
				}

				if (!children.isEmpty() || !runtimeClassName.equals(c.mappedName())) {
					RemappedClass rc = new RemappedClass(c.mappedName());

					if (!children.isEmpty()) {
						rc.children = new HashMap<>(children);
					}

					classMap.put(runtimeClassName, rc);
				}
			}
		}
	}
}

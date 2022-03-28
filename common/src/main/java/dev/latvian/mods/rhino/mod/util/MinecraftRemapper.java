package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.util.Remapper;
import net.minecraft.SharedConstants;
import org.apache.commons.io.IOUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MinecraftRemapper implements Remapper {
	public static final int MM_VERSION = 1;
	public static final int VERSION = 1;

	public static class RemappedClass {
		public final String originalName;
		public final String mappedName;
		public Map<String, String> children;

		public RemappedClass(String on, String mn) {
			originalName = on;
			mappedName = mn;
		}

		@Override
		public String toString() {
			return mappedName;
		}

		public String getChild(String s) {
			return children == null || children.isEmpty() ? "" : children.getOrDefault(s, "");
		}
	}

	public record MinecraftClasses(Map<String, RemappedClass> rawLookup, Map<String, RemappedClass> mappedLookup) implements Function<String, String> {
		@Override
		public String apply(String s) {
			RemappedClass c = mappedLookup.get(s);
			return c == null ? s : c.originalName;
		}
	}

	public final Map<String, RemappedClass> classMap;
	private Map<String, String> inverseClassMap;
	private boolean empty;

	public MinecraftRemapper() {
		classMap = new HashMap<>();
		empty = true;

		if (!isValid()) {
			return;
		}

		try {
			Path remappedPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_" + getModLoader() + "_" + getRuntimeMappings() + "_remapped_" + getMcVersion() + "_v" + VERSION + (isServer() ? "_server.txt" : "_client.txt"));

			if (Files.exists(remappedPath)) {
				RemappedClass current = null;

				for (String line : Files.readAllLines(remappedPath, StandardCharsets.UTF_8)) {
					String[] l = line.split(" ");

					if (l[0].equals("*")) {
						current = new RemappedClass(l[1], l[2]);
						classMap.put(current.originalName, current);

						int cc = Integer.parseInt(l[3]);

						if (cc > 0) {
							current.children = new HashMap<>(cc);
						}
					} else if (current != null && !l[0].startsWith("#")) {
						current.children.put(l[0], l[1]);
					}
				}
			} else {
				MinecraftClasses minecraftClasses = loadMojMapClasses();
				init(minecraftClasses);

				List<String> list = new ArrayList<>();
				list.add("#version " + VERSION);

				for (var entry : classMap.entrySet()) {
					RemappedClass rc = entry.getValue();
					list.add("* " + rc.originalName + " " + rc.mappedName + " " + (rc.children == null ? 0 : rc.children.size()));

					if (rc.children != null) {
						for (var entry1 : rc.children.entrySet()) {
							list.add(entry1.getKey() + " " + entry1.getValue());
						}
					}
				}

				Files.write(remappedPath, list);
			}

			empty = false;
		} catch (Exception ex) {
			System.err.println("Failed to remap Rhino to Mojang Mappings:");
			ex.printStackTrace();
		}
	}

	public MinecraftClasses loadMojMapClasses() throws Exception {
		Path mojmapPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_mojang_mappings_" + getMcVersion() + "_v" + MM_VERSION + (isServer() ? "_server.txt" : "_client.txt"));

		if (Files.exists(mojmapPath)) {
			return readMojMapClasses(mojmapPath);
		} else {
			MinecraftClasses minecraftClasses = fetchMojMapClasses();

			List<String> list = new ArrayList<>();
			list.add("#version " + MM_VERSION);

			for (var entry : minecraftClasses.rawLookup.entrySet()) {
				RemappedClass rc = entry.getValue();
				list.add("* " + rc.originalName + " " + rc.mappedName + " " + (rc.children == null ? 0 : rc.children.size()));

				if (rc.children != null) {
					for (var entry1 : rc.children.entrySet()) {
						list.add(entry1.getKey() + " " + entry1.getValue());
					}
				}
			}

			Files.write(mojmapPath, list);
			return minecraftClasses;
		}
	}

	public MinecraftClasses readMojMapClasses(Path path) throws Exception {
		MinecraftClasses minecraftClasses = new MinecraftClasses(new HashMap<>(), new HashMap<>());

		RemappedClass current = null;

		for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
			String[] l = line.split(" ");

			if (l[0].equals("*")) {
				current = new RemappedClass(l[1], l[2]);
				minecraftClasses.rawLookup.put(current.originalName, current);
				minecraftClasses.mappedLookup.put(current.mappedName, current);

				int cc = Integer.parseInt(l[3]);

				if (cc > 0) {
					current.children = new HashMap<>(cc);
				}
			} else if (current != null && !l[0].startsWith("#")) {
				current.children.put(l[0], l[1]);
			}
		}

		return minecraftClasses;
	}

	public MinecraftClasses fetchMojMapClasses() throws Exception {
		MinecraftClasses minecraftClasses = new MinecraftClasses(new HashMap<>(), new HashMap<>());

		String str = IOUtils.toString(new URL("https://kubejs.com/mappings/" + getMcVersion() + (isServer() ? "/server.txt" : "/client.txt")), StandardCharsets.UTF_8);
		String[] mojmaps = IOUtils.toString(new URL(str), StandardCharsets.UTF_8).split("\n");

		for (String s : mojmaps) {
			s = s.trim();

			if (!s.startsWith("#") && s.endsWith(":")) {
				String[] s1 = s.substring(0, s.length() - 1).split(" -> ", 2);

				if (s1.length == 2 && !s1[0].endsWith(".package-info")) {
					RemappedClass c = new RemappedClass(s1[1], s1[0]);
					minecraftClasses.rawLookup.put(c.originalName, c);
					minecraftClasses.mappedLookup.put(c.mappedName, c);
				}
			}
		}

		Pattern pattern = Pattern.compile("([\\w$<>]+)(\\(.*\\))? -> ([\\w$<>]+)");
		RemappedClass current = null;

		for (String s : mojmaps) {
			s = s.trim();

			if (!s.isEmpty() && !s.startsWith("#")) {
				if (s.endsWith(":")) {
					String raw = s.substring(s.lastIndexOf(' ') + 1, s.length() - 1);
					current = minecraftClasses.rawLookup.get(raw);
				} else if (current != null) {
					Matcher matcher = pattern.matcher(s);

					if (matcher.find()) {
						String mappedName = matcher.group(1);
						String args = matcher.group(2);
						String rawName = matcher.group(3);

						if (!rawName.equals(mappedName)) {
							if (mappedName.startsWith("lambda$") || mappedName.startsWith("val$") || mappedName.startsWith("access$") || mappedName.startsWith("this$")) {
								continue;
							} else if (current.children == null) {
								current.children = new LinkedHashMap<>();
							}

							if (args != null && args.length() >= 2) {
								StringBuilder sb = new StringBuilder(rawName);
								sb.append('(');

								String a = args.substring(1, args.length() - 1);

								if (a.length() > 0) {
									String[] a1 = a.split(",");

									for (String value : a1) {
										sb.append(Remapper.getTypeName(value, minecraftClasses));
									}
								}

								sb.append(')');
								current.children.put(sb.toString(), mappedName);
							} else {
								current.children.put(rawName, mappedName);
							}
						}
					}
				}
			}
		}

		return minecraftClasses;
	}

	public boolean isValid() {
		return true;
	}

	public abstract String getModLoader();

	public abstract boolean isServer();

	public abstract String getRuntimeMappings();

	public String getMcVersion() {
		return SharedConstants.getCurrentVersion().getName();
	}

	public abstract void init(MinecraftClasses minecraftClasses) throws Exception;

	@Override
	public String remapClass(Class<?> from, String className) {
		RemappedClass c = empty ? null : classMap.get(className);
		return c == null ? "" : c.mappedName;
	}

	@Override
	public String unmapClass(String from) {
		if (empty) {
			return "";
		} else if (inverseClassMap == null) {
			inverseClassMap = new HashMap<>(classMap.size());

			for (var entry : classMap.entrySet()) {
				inverseClassMap.put(entry.getValue().mappedName, entry.getKey());
			}
		}

		return inverseClassMap.getOrDefault(from, "");
	}

	@Override
	public String remapField(Class<?> from, Field field, String fieldName) {
		RemappedClass c = empty ? null : classMap.get(from.getName());
		return c == null ? "" : c.getChild(fieldName);
	}

	@Override
	public String remapMethod(Class<?> from, Method method, String methodString) {
		RemappedClass c = empty ? null : classMap.get(from.getName());
		return c == null ? "" : c.getChild(methodString);
	}
}

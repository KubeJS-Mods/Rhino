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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class MojangMappingRemapper implements Remapper {
	public static final int VERSION = 2;

	public static class RemappedClass {
		public final String mappedName;
		public Map<String, String> children;

		public RemappedClass(String mn) {
			mappedName = mn;
		}

		@Override
		public String toString() {
			return mappedName;
		}

		public String getFieldName(Field field) {
			return children == null ? "" : children.getOrDefault(field.getName(), "");
		}

		public String getMethodName(Method method) {
			if (children == null) {
				return "";
			}

			StringBuilder sb = new StringBuilder(method.getName());
			sb.append('(');

			if (method.getParameterCount() > 0) {
				for (Class<?> param : method.getParameterTypes()) {
					sb.append(getTypeName(param.getTypeName(), Function.identity()));
				}
			}

			sb.append(')');
			return children.getOrDefault(sb.toString(), "");
		}
	}

	public record MojMapClass(String mappedName, String rawName, Map<String, String> children) {
	}

	public static String getTypeName(String type, Function<String, String> remap) {
		int array = 0;

		while (type.endsWith("[]")) {
			array++;
			type = type.substring(0, type.length() - 2);
		}

		String t = switch (type) {
			case "boolean" -> "Z";
			case "byte" -> "B";
			case "short" -> "S";
			case "int" -> "I";
			case "long" -> "J";
			case "float" -> "F";
			case "double" -> "D";
			case "char" -> "C";
			case "void" -> "V";
			default -> "L" + remap.apply(type.replace('/', '.')).replace('.', '/') + ";";
		};

		return array == 0 ? t : ("[".repeat(array) + t);
	}

	public record MojMapClasses(Map<String, MojMapClass> rawLookup, Map<String, MojMapClass> mappedLookup) implements Function<String, String> {
		public String getMappedTypeName(String type) {
			return getTypeName(type, this);
		}

		@Override
		public String apply(String s) {
			MojMapClass c = mappedLookup.get(s);
			return c == null ? s : c.mappedName;
		}
	}

	public final Map<String, RemappedClass> classMap;
	private Map<String, String> inverseClassMap;
	private boolean empty;

	public MojangMappingRemapper() {
		classMap = new HashMap<>();
		empty = true;

		if (!isValid()) {
			return;
		}

		try {
			boolean isServer = isServer();
			int version = 0;
			Path remappedPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_" + getModLoader() + "_" + getRuntimeMappings() + "_remapped_" + getMcVersion() + (isServer ? "_server.txt" : "_client.txt"));

			if (Files.exists(remappedPath)) {
				RemappedClass current = null;

				for (String line : Files.readAllLines(remappedPath, StandardCharsets.UTF_8)) {
					String[] l = line.split(" ");

					if (l[0].equals("#")) {
						version = Integer.parseInt(l[1]);
					} else if (l[0].equals("*")) {
						current = new RemappedClass(l[2]);
						classMap.put(l[1], current);

						int cc = Integer.parseInt(l[3]);

						if (cc > 0) {
							current.children = new HashMap<>(cc);
						}
					} else if (current != null) {
						current.children.put(l[0], l[1]);
					}
				}
			} else {
				Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_mojang_mappings_" + getMcVersion() + (isServer ? "_server.txt" : "_client.txt"));
				String[] mojmaps;

				if (Files.exists(tmpPath)) {
					mojmaps = Files.readString(tmpPath, StandardCharsets.UTF_8).split("\n");
				} else {
					String str = IOUtils.toString(new URL("https://kubejs.com/mappings/" + getMcVersion() + (isServer ? "/server.txt" : "/client.txt")), StandardCharsets.UTF_8);
					String mojmaps0 = IOUtils.toString(new URL(str), StandardCharsets.UTF_8);
					mojmaps = mojmaps0.split("\n");
					Files.writeString(tmpPath, mojmaps0, StandardCharsets.UTF_8);
				}

				MojMapClasses mojMapClasses = new MojMapClasses(new HashMap<>(), new HashMap<>());

				for (String s : mojmaps) {
					s = s.trim();

					if (!s.startsWith("#") && s.endsWith(":")) {
						String[] s1 = s.substring(0, s.length() - 1).split(" -> ", 2);

						if (s1.length == 2) {
							MojMapClass c = new MojMapClass(s1[0], s1[1], new HashMap<>());
							mojMapClasses.rawLookup.put(c.rawName, c);
							mojMapClasses.mappedLookup.put(c.mappedName, c);
						}
					}
				}

				Pattern pattern = Pattern.compile("([\\w$<>]+)(\\(.*\\))? -> ([\\w$<>]+)");
				MojMapClass current = null;

				for (String s : mojmaps) {
					s = s.trim();

					if (!s.isEmpty() && !s.startsWith("#")) {
						if (s.endsWith(":")) {
							String raw = s.substring(s.lastIndexOf(' ') + 1, s.length() - 1);
							current = mojMapClasses.rawLookup.get(raw);
						} else if (current != null) {
							Matcher matcher = pattern.matcher(s);

							if (matcher.find()) {
								String mappedName = matcher.group(1);
								String args = matcher.group(2);
								String rawName = matcher.group(3);

								if (!rawName.equals(mappedName)) {
									if (args != null && args.length() >= 2) {
										StringBuilder sb = new StringBuilder(rawName);
										sb.append('(');

										String a = args.substring(1, args.length() - 1);

										if (a.length() > 0) {
											String[] a1 = a.split(",");

											for (String value : a1) {
												sb.append(mojMapClasses.getMappedTypeName(value));
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

				init(mojMapClasses);
			}

			if (version != VERSION) {
				List<String> list = new ArrayList<>();
				list.add("# " + VERSION);

				for (var entry : classMap.entrySet()) {
					RemappedClass rc = entry.getValue();
					list.add("* " + entry.getKey() + " " + rc.mappedName + " " + (rc.children == null ? 0 : rc.children.size()));

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

	public boolean isValid() {
		return true;
	}

	public abstract String getModLoader();

	public abstract boolean isServer();

	public abstract String getRuntimeMappings();

	public String getMcVersion() {
		return SharedConstants.getCurrentVersion().getName();
	}

	public abstract void init(MojMapClasses mojMapClasses) throws Exception;

	@Override
	public String remapClass(Class<?> from) {
		RemappedClass c = empty ? null : classMap.get(from.getName());
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
	public String remapField(Class<?> from, Field field) {
		RemappedClass c = empty ? null : classMap.get(from.getName());
		return c == null ? "" : c.getFieldName(field);
	}

	@Override
	public String remapMethod(Class<?> from, Method method) {
		RemappedClass c = empty ? null : classMap.get(from.getName());
		return c == null ? "" : c.getMethodName(method);
	}

	public void clearCache() {
		try {
			Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_" + getModLoader() + "_" + getRuntimeMappings() + "_remapped_" + getMcVersion() + (isServer() ? "_server.txt" : "_client.txt")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			Files.deleteIfExists(Paths.get(System.getProperty("java.io.tmpdir")).resolve("rhino_mojang_mappings_" + getMcVersion() + (isServer() ? "_server.txt" : "_client.txt")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

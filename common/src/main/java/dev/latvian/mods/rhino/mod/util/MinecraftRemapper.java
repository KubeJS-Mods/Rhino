package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.util.Remapper;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MinecraftRemapper implements Remapper {
	private static final class RemappedClass {
		private final String realName;
		private final String remappedName;
		private final boolean remapped;
		private Map<String, String> fields;
		private Map<String, String> emptyMethods;
		private Map<String, String> methods;
		private String descriptorString;

		private RemappedClass(String realName, String remappedName, boolean remapped) {
			this.realName = realName;
			this.remappedName = remappedName;
			this.remapped = remapped;
			this.fields = null;
			this.emptyMethods = null;
			this.methods = null;
		}

		@Override
		public String toString() {
			if (remapped) {
				return remappedName + "[" + realName + "]";
			}

			return realName;
		}

		public String descriptorString() {
			if (descriptorString == null) {
				descriptorString = switch (realName) {
					case "boolean" -> "Z";
					case "byte" -> "B";
					case "char" -> "C";
					case "short" -> "S";
					case "int" -> "I";
					case "long" -> "J";
					case "float" -> "F";
					case "double" -> "D";
					case "void" -> "V";
					default -> "L" + realName.replace('.', '/') + ";";
				};
			}

			return descriptorString;
		}
	}

	@SuppressWarnings({"OptionalAssignedToNull", "OptionalUsedAsFieldOrParameterType"})
	private static final class RemappedType {
		private final RemappedClass parent;
		private final int array;
		private Optional<Class<?>> realClass;
		private String descriptorString;

		private RemappedType(RemappedClass parent, int array) {
			this.parent = parent;
			this.array = array;
			this.realClass = null;
		}

		@Override
		public String toString() {
			if (array == 0) {
				return parent.toString();
			}

			return parent.toString() + "[]".repeat(array);
		}

		public boolean isRemapped() {
			return array == 0 && parent.remapped;
		}

		@Nullable
		private Class<?> getRealClass(boolean debug) {
			if (realClass == null) {
				var r = RemappingHelper.getClass(parent.realName);

				if (r.isPresent()) {
					if (array > 0) {
						realClass = Optional.of(Array.newInstance(r.get(), array).getClass());
					} else {
						realClass = r;
					}
				} else {
					realClass = Optional.empty();

					if (debug) {
						RemappingHelper.LOGGER.error("Class " + parent.realName + " / " + parent.remappedName + " not found!");
					}
				}
			}

			return realClass.orElse(null);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof RemappedType type && type.parent == parent && type.array == array;
		}

		@Override
		public int hashCode() {
			return Objects.hash(parent, array);
		}

		public String descriptorString() {
			if (descriptorString == null) {
				if (array > 0) {
					descriptorString = "[".repeat(array) + parent.descriptorString();
				} else {
					descriptorString = parent.descriptorString();
				}
			}

			return descriptorString;
		}
	}

	private static int readVarInt(InputStream stream) throws Exception {
		return RemappingHelper.readVarInt(stream);
	}

	private static String readUtf(InputStream stream) throws Exception {
		return RemappingHelper.readUtf(stream);
	}

	public static MinecraftRemapper load(InputStream stream, boolean debug) throws Exception {
		var m = new MinecraftRemapper(new HashMap<>(), new HashMap<>());
		m.load0(stream, debug);
		return m;
	}

	private final Map<String, RemappedClass> classMap;
	private final Map<String, String> unmapClassMap;

	private void load0(InputStream stream, boolean debug) throws Exception {
		if (stream.read() != 0) {
			throw new RemapperException("Invalid Minecraft Remapper file!");
		}

		int version = stream.read();

		if (version > 1) {
			throw new RemapperException("Invalid Minecraft Remapper file version!");
		}

		RemappingHelper.LOGGER.info("Loading mappings for " + readUtf(stream));

		int unmappedTypes = readVarInt(stream);
		var mappedTypes = new RemappedType[readVarInt(stream)];
		int arrayTypes = readVarInt(stream);

		var types = new RemappedType[unmappedTypes + mappedTypes.length + arrayTypes];

		if (debug) {
			RemappingHelper.LOGGER.info("Unmapped Types: " + unmappedTypes);
			RemappingHelper.LOGGER.info("Mapped Types: " + mappedTypes.length);
			RemappingHelper.LOGGER.info("Array Types: " + arrayTypes);
			RemappingHelper.LOGGER.info("Total Types: " + types.length);
		}

		for (int i = 0; i < unmappedTypes; i++) {
			int index = readVarInt(stream);
			var name = readUtf(stream);
			types[index] = new RemappedType(new RemappedClass(name, name, false), 0);
		}

		for (int i = 0; i < mappedTypes.length; i++) {
			int index = readVarInt(stream);
			var realName = readUtf(stream);
			var remappedName = readUtf(stream);
			types[index] = new RemappedType(new RemappedClass(realName.isEmpty() ? remappedName : realName, remappedName, true), 0);
			mappedTypes[i] = types[index];
			classMap.put(types[index].parent.realName, types[index].parent);
		}

		for (int i = 0; i < arrayTypes; i++) {
			int index = readVarInt(stream);
			int type = readVarInt(stream);
			int array = readVarInt(stream);

			if (type < 0 || type >= types.length || types[type] == null) {
				throw new RemapperException("Invalid array index: " + type + "!");
			}

			types[index] = new RemappedType(types[type].parent, array);
		}

		var sig = new String[readVarInt(stream)];

		for (int i = 0; i < sig.length; i++) {
			int params = readVarInt(stream);
			var sb = new StringBuilder();
			sb.append('(');

			for (int j = 0; j < params; j++) {
				sb.append(types[readVarInt(stream)].descriptorString());
			}

			sig[i] = sb.toString();
		}

		for (var c : mappedTypes) {
			if (debug) {
				RemappingHelper.LOGGER.info(String.format("- %s -> %s", c.parent.realName, c.parent.remappedName));
			}

			int fields = readVarInt(stream);
			int arg0 = readVarInt(stream);
			int argN = readVarInt(stream);

			for (int i = 0; i < fields; i++) {
				var unmappedName = readUtf(stream);
				var mmName = readUtf(stream);

				if (unmappedName.isEmpty() || mmName.isEmpty() || unmappedName.equals(mmName)) {
					continue;
				}

				if (c.parent.fields == null) {
					c.parent.fields = new HashMap<>(arg0 + argN);
				}

				c.parent.fields.put(unmappedName, mmName);

				if (debug) {
					RemappingHelper.LOGGER.info(String.format("  %s -> %s", unmappedName, mmName));
				}
			}

			for (int i = 0; i < arg0; i++) {
				var realName = readUtf(stream);
				var remappedName = readUtf(stream);

				if (realName.isEmpty() || remappedName.isEmpty() || realName.equals(remappedName)) {
					continue;
				}

				if (c.parent.emptyMethods == null) {
					c.parent.emptyMethods = new HashMap<>(arg0);
				}

				c.parent.emptyMethods.put(realName, remappedName);

				if (debug) {
					RemappingHelper.LOGGER.info(String.format("  %s() -> %s", realName, remappedName));
				}
			}

			for (int i = 0; i < argN; i++) {
				var realName = readUtf(stream);
				var remappedName = readUtf(stream);

				if (realName.isEmpty() || remappedName.isEmpty() || realName.equals(remappedName)) {
					continue;
				}

				if (c.parent.methods == null) {
					c.parent.methods = new HashMap<>(argN);
				}

				int index = readVarInt(stream);
				var key = realName + sig[index];
				c.parent.methods.put(key, remappedName);

				if (debug) {
					RemappingHelper.LOGGER.info(String.format("  %s -> %s", key, remappedName));
				}
			}
		}
	}

	MinecraftRemapper(Map<String, RemappedClass> m1, Map<String, String> m2) {
		classMap = m1;
		unmapClassMap = m2;
	}

	@Override
	public String getMappedClass(Class<?> from) {
		var c = classMap.get(from.getName());
		return c == null ? "" : c.remappedName;
	}

	@Override
	public String getUnmappedClass(String mmName) {
		if (classMap.isEmpty()) {
			return "";
		}

		var s = unmapClassMap.get(mmName);

		if (s == null) {
			s = "";

			for (var c : classMap.values()) {
				if (c.remappedName.equals(mmName)) {
					s = c.realName;
				}
			}

			unmapClassMap.put(mmName, s);
		}

		return s;
	}

	@Override
	public String getMappedField(Class<?> from, Field field) {
		if (from == null || from == Object.class || from.getPackageName().startsWith("java.")) {
			return "";
		}

		var c = classMap.get(from.getName());
		return c == null || c.fields == null ? "" : c.fields.getOrDefault(field.getName(), "");
	}

	@Override
	public String getMappedMethod(Class<?> from, Method method) {
		if (from == null || from == Object.class || from.getPackageName().startsWith("java.")) {
			return "";
		}

		var c = classMap.get(from.getName());

		if (c == null) {
			return "";
		} else if (method.getParameterCount() == 0) {
			return c.emptyMethods == null ? "" : c.emptyMethods.getOrDefault(method.getName(), "");
		} else if (c.methods == null) {
			return "";
		}

		var sb = new StringBuilder();
		sb.append(method.getName());
		sb.append('(');

		for (var t : method.getParameterTypes()) {
			sb.append(t.descriptorString());
		}

		return c.methods.getOrDefault(sb.toString(), "");
	}
}

package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.util.Remapper;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MinecraftRemapper implements Remapper {
	private static final class RemappedClass {
		private final String unmappedName;
		private final String mmName;
		private Map<String, String> fields;
		private Map<RemappedMethodSignature, String> methods;

		private RemappedClass(String unmappedName, String mmName) {
			this.unmappedName = unmappedName;
			this.mmName = mmName;
			this.fields = null;
			this.methods = null;
		}

		@Override
		public String toString() {
			if (!mmName.isEmpty()) {
				return mmName + "[" + unmappedName + "]";
			}

			return unmappedName;
		}
	}

	private static final class RemappedType {
		private final RemappedClass parent;
		private final int array;
		private Optional<Class<?>> realClass;

		private RemappedType(RemappedClass parent, int array) {
			this.parent = parent;
			this.array = array;
		}

		@Override
		public String toString() {
			if (array == 0) {
				return parent.toString();
			}

			return parent.toString() + "[]".repeat(array);
		}

		@Nullable
		private Class<?> getRealClass(boolean debug) {
			if (realClass == null) {
				var r = RemappingHelper.getClass(parent.unmappedName);

				if (!r.isPresent() && !parent.mmName.isEmpty()) {
					r = RemappingHelper.getClass(parent.mmName);
				}

				if (r.isPresent()) {
					if (array > 0) {
						realClass = Optional.of(Array.newInstance(r.get(), array).getClass());
					} else {
						realClass = r;
					}
				} else {
					realClass = Optional.empty();

					if (debug) {
						RemappingHelper.LOGGER.error("Class " + parent.unmappedName + " / " + parent.mmName + " not found!");
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

	}

	private record RemappedMethodSignature(String name, Class<?>[] types) {
		public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

		public RemappedMethodSignature(Method m) {
			this(m.getName(), m.getParameterCount() == 0 ? EMPTY_CLASS_ARRAY : m.getParameterTypes());
		}

		@Override
		public int hashCode() {
			return name.hashCode() * 31 + Arrays.hashCode(types);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof RemappedMethodSignature s && s.name.equals(name) && Arrays.equals(s.types, types);
		}

		@Override
		public String toString() {
			var sb = new StringBuilder(name);
			sb.append('(');

			for (var t : types) {
				if (t == null) {
					sb.append('X');
				} else {
					sb.append(t.descriptorString());
				}
			}

			sb.append(')');
			return sb.toString();
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
			types[index] = new RemappedType(new RemappedClass(name, ""), 0);
		}

		for (int i = 0; i < mappedTypes.length; i++) {
			int index = readVarInt(stream);
			var unmappedName = readUtf(stream);
			var mmName = readUtf(stream);
			types[index] = new RemappedType(new RemappedClass(unmappedName.isEmpty() ? mmName : unmappedName, mmName), 0);
			mappedTypes[i] = types[index];
			classMap.put(types[index].parent.unmappedName, types[index].parent);
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

		var sig = new Class<?>[readVarInt(stream)][];

		for (int i = 0; i < sig.length; i++) {
			sig[i] = new Class<?>[readVarInt(stream)];

			for (int j = 0; j < sig[i].length; j++) {
				sig[i][j] = types[readVarInt(stream)].getRealClass(debug);
			}
		}

		for (var c : mappedTypes) {
			if (debug) {
				RemappingHelper.LOGGER.info(String.format("- %s -> %s", c.parent.unmappedName, c.parent.mmName));
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
				var unmappedName = readUtf(stream);
				var mmName = readUtf(stream);

				if (unmappedName.isEmpty() || mmName.isEmpty() || unmappedName.equals(mmName)) {
					continue;
				}

				if (c.parent.methods == null) {
					c.parent.methods = new HashMap<>(arg0 + argN);
				}

				c.parent.methods.put(new RemappedMethodSignature(unmappedName, RemappedMethodSignature.EMPTY_CLASS_ARRAY), mmName);

				if (debug) {
					RemappingHelper.LOGGER.info(String.format("  %s() -> %s", unmappedName, mmName));
				}
			}

			for (int i = 0; i < argN; i++) {
				var unmappedName = readUtf(stream);
				var mmName = readUtf(stream);

				if (unmappedName.isEmpty() || mmName.isEmpty() || unmappedName.equals(mmName)) {
					continue;
				}

				if (c.parent.methods == null) {
					c.parent.methods = new HashMap<>(arg0 + argN);
				}

				int index = readVarInt(stream);
				var key = new RemappedMethodSignature(unmappedName, sig[index]);
				c.parent.methods.put(key, mmName);

				if (debug) {
					RemappingHelper.LOGGER.info(String.format("  %s -> %s", key, mmName));
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
		return c == null ? "" : c.mmName;
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
				if (c.mmName.equals(mmName)) {
					s = c.unmappedName;
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
		return c == null || c.methods == null ? "" : c.methods.getOrDefault(new RemappedMethodSignature(method), "");
	}
}

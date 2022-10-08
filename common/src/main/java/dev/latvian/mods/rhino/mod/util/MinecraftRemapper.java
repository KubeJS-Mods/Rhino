package dev.latvian.mods.rhino.mod.util;

import dev.latvian.mods.rhino.util.Remapper;

import java.io.DataInput;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MinecraftRemapper implements Remapper {
	private static final class RemappedClass {
		private final String unmappedName;
		private final String mmName;
		private Map<String, String> fields;
		private Map<String, String> methods;

		private RemappedClass(String unmappedName, String mmName) {
			this.unmappedName = unmappedName;
			this.mmName = mmName;
			this.fields = null;
			this.methods = null;
		}
	}

	private record RemappedType(RemappedClass parent, int array) {
	}

	private static int readVarInt(DataInput input) throws Exception {
		int i = 0;
		int j = 0;

		byte b;
		do {
			b = input.readByte();
			i |= (b & 127) << j++ * 7;
			if (j > 5) {
				throw new RuntimeException("VarInt too big");
			}
		} while ((b & 128) == 128);

		return i;
	}

	private static String readUtf(DataInput input) throws Exception {
		byte[] bytes = new byte[readVarInt(input)];
		input.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static MinecraftRemapper load(DataInput input) throws Exception {
		return new MinecraftRemapper(input);
	}

	private final Map<String, RemappedClass> classMap;
	private final Map<String, String> unmapClassMap;

	private MinecraftRemapper(DataInput input) throws Exception {
		classMap = new HashMap<>();
		unmapClassMap = new HashMap<>();

		input.readByte();
		input.readByte();
		var types = new RemappedType[readVarInt(input)];

		int unmappedTypes = readVarInt(input);

		for (int i = 0; i < unmappedTypes; i++) {
			int index = readVarInt(input);
			var name = readUtf(input);
			types[index] = new RemappedType(new RemappedClass(name, ""), 0);
		}

		var mappedTypes = new RemappedType[readVarInt(input)];

		for (int i = 0; i < mappedTypes.length; i++) {
			int index = readVarInt(input);
			var unmappedName = readUtf(input);
			var mmName = readUtf(input);
			types[index] = new RemappedType(new RemappedClass(unmappedName, mmName), 0);
			mappedTypes[i] = types[index];
			classMap.put(unmappedName, types[index].parent);
		}

		int arrayTypes = readVarInt(input);

		for (int i = 0; i < arrayTypes; i++) {
			int index = readVarInt(input);
			int type = readVarInt(input);
			int array = readVarInt(input);
			types[index] = new RemappedType(types[type].parent, array);
		}

		var sig = new String[readVarInt(input)];

		for (int i = 0; i < sig.length; i++) {
			int sigTypes = readVarInt(input);
			var sb = new StringBuilder("(");

			for (int j = 0; j < sigTypes; j++) {
				sb.append(Remapper.getTypeName(types[readVarInt(input)].parent.unmappedName));
			}

			sb.append('(');
			sig[i] = sb.toString();
		}

		for (var c : mappedTypes) {
			int fields = readVarInt(input);

			if (fields > 0) {
				c.parent.fields = new HashMap<>();
			}

			for (int i = 0; i < fields; i++) {
				var unmappedName = readUtf(input);
				var mmName = readUtf(input);
				c.parent.fields.put(unmappedName, mmName);
			}

			int arg0methods = readVarInt(input);

			if (arg0methods > 0) {
				c.parent.methods = new HashMap<>();
			}

			for (int i = 0; i < arg0methods; i++) {
				var unmappedName = readUtf(input);
				var mmName = readUtf(input);
				c.parent.methods.put(unmappedName, mmName);
			}

			int argNmethods = readVarInt(input);

			if (arg0methods == 0 && argNmethods > 0) {
				c.parent.methods = new HashMap<>();
			}

			for (int i = 0; i < argNmethods; i++) {
				var unmappedName = readUtf(input);
				var mmName = readUtf(input);
				int index = readVarInt(input);
				c.parent.methods.put(unmappedName + sig[index], mmName);
			}
		}
	}

	@Override
	public String remapClass(Class<?> from, String className) {
		var c = classMap.get(className);
		return c == null ? "" : c.mmName;
	}

	@Override
	public String unmapClass(String mmName) {
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
	public String remapField(Class<?> from, Field field, String fieldName) {
		var c = classMap.get(from.getName());
		return c == null || c.fields == null ? "" : c.fields.getOrDefault(fieldName, "");
	}

	@Override
	public String remapMethod(Class<?> from, Method method, String methodString) {
		var c = classMap.get(from.getName());
		return c == null || c.methods == null ? "" : c.methods.getOrDefault(method.getParameterCount() == 0 ? method.getName() : methodString, "");
	}
}

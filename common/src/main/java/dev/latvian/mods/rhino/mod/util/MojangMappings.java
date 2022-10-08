package dev.latvian.mods.rhino.mod.util;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutput;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MojangMappings {
	private final Map<String, ClassDef> classes;
	private final Map<String, ClassDef> classesMM;
	private final Map<TypeDef, TypeDef> allTypes;
	private final Map<MethodDefSignature, MethodDefSignature> methodSignatures;

	private final ClassDef VOID = new ClassDef(this, "void").descriptor("V");
	private final ClassDef BOOLEAN = new ClassDef(this, "boolean").descriptor("Z");
	private final ClassDef CHAR = new ClassDef(this, "char").descriptor("C");
	private final ClassDef BYTE = new ClassDef(this, "byte").descriptor("B");
	private final ClassDef SHORT = new ClassDef(this, "short").descriptor("S");
	private final ClassDef INT = new ClassDef(this, "int").descriptor("I");
	private final ClassDef LONG = new ClassDef(this, "long").descriptor("J");
	private final ClassDef FLOAT = new ClassDef(this, "float").descriptor("F");
	private final ClassDef DOUBLE = new ClassDef(this, "double").descriptor("D");

	private final MethodDefSignature SIG_EMPTY = new MethodDefSignature();

	private MojangMappings() {
		classes = new HashMap<>();
		classesMM = new HashMap<>();
		allTypes = new HashMap<>();
		methodSignatures = new HashMap<>();

		for (var c : new ClassDef[]{
				VOID,
				BOOLEAN,
				CHAR,
				BYTE,
				SHORT,
				INT,
				LONG,
				FLOAT,
				DOUBLE,
		}) {
			classes.put(c.rawName, c);
			allTypes.put(c.noArrayType, c.noArrayType);
		}
	}

	public MethodDefSignature getSignature(TypeDef[] types) {
		if (types.length == 0) {
			return SIG_EMPTY;
		} else if (types.length == 1) {
			return types[0].getSingleArgumentSignature();
		}

		var sig = new MethodDefSignature(types);
		var cached = methodSignatures.get(sig);

		if (cached != null) {
			return cached;
		}

		methodSignatures.put(sig, sig);
		return sig;
	}

	public MethodDefSignature readSignatureFromDescriptor(String descriptor) throws Exception {
		if (descriptor.length() >= 2 && descriptor.charAt(0) == '(' && descriptor.charAt(1) == ')') {
			return SIG_EMPTY;
		}

		var reader = new StringReader(descriptor);
		var types = new ArrayList<TypeDef>();

		while (true) {
			var c = reader.read();

			if (c == '(') {
				continue;
			}

			int array = 0;

			while (c == '[') {
				array++;
				c = reader.read();
			}

			if (c == -1 || c == ')') {
				break;
			}

			if (c == 'L') {
				var sb = new StringBuilder();

				while (true) {
					c = reader.read();

					if (c == -1) {
						throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
					}

					if (c == ';') {
						break;
					}

					sb.append((char) c);
				}

				types.add(getType(sb.toString()).parent.array(array));
			} else if (c == 'Z') {
				types.add(BOOLEAN.array(array));
			} else if (c == 'C') {
				types.add(CHAR.array(array));
			} else if (c == 'B') {
				types.add(BYTE.array(array));
			} else if (c == 'S') {
				types.add(SHORT.array(array));
			} else if (c == 'I') {
				types.add(INT.array(array));
			} else if (c == 'J') {
				types.add(LONG.array(array));
			} else if (c == 'F') {
				types.add(FLOAT.array(array));
			} else if (c == 'D') {
				types.add(DOUBLE.array(array));
			} else if (c == 'V') {
				types.add(VOID.array(array));
			} else {
				throw new IllegalArgumentException("Invalid descriptor: " + descriptor);
			}
		}

		if (types.isEmpty()) {
			return SIG_EMPTY;
		} else if (types.size() == 1) {
			return types.get(0).getSingleArgumentSignature();
		}

		return getSignature(types.toArray(TypeDef[]::new));
	}

	@Nullable
	public ClassDef getClass(String name) {
		var mmc = classesMM.get(name);
		return mmc != null ? mmc : classes.get(name);
	}

	public TypeDef getType(String string) {
		int array = 0;

		while (string.endsWith("[]")) {
			array++;
			string = string.substring(0, string.length() - 2);
		}

		while (string.startsWith("[")) {
			array++;
			string = string.substring(1);
		}

		var c = getClass(string);

		if (c != null) {
			return c.array(array);
		}

		c = new ClassDef(this, string);
		classes.put(string, c);
		allTypes.put(c.noArrayType, c.noArrayType);
		return c.array(array);
	}

	private static boolean invalidLine(String s) {
		return s.isBlank() || s.startsWith("#") || s.endsWith("init>") || s.contains(".package-info ");
	}

	private void parse0(List<String> lines) {
		lines.removeIf(MojangMappings::invalidLine);

		for (var line : lines) {
			if (line.charAt(line.length() - 1) == ':') {
				var s = line.split(" -> ", 2); // replace with faster, last index of space check
				var c = new ClassDef(this, s[1].substring(0, s[1].length() - 1), s[0], new HashMap<>(0));
				c.mapped = true;
				classes.put(c.rawName, c);
				classesMM.put(c.mmName, c);
				allTypes.put(c.noArrayType, c.noArrayType);
			}
		}

		ClassDef currentClassDef = null;

		for (var line : lines) {
			if (line.charAt(0) == ' ') {
				if (currentClassDef == null) {
					throw new RuntimeException("Field or method without class! " + line);
				}

				line = line.substring(Math.max(4, line.lastIndexOf(':') + 1));
				int typeSpace = line.indexOf(' ');
				var type = getType(line.substring(0, typeSpace));
				line = line.substring(typeSpace + 1);
				var rawName = line.substring(line.lastIndexOf(' ') + 1);
				line = line.substring(0, line.indexOf(' '));
				String name;
				MethodDefSignature sig;

				if (line.charAt(line.length() - 1) == ')') {
					int lp = line.indexOf('(');
					name = line.substring(0, lp);
					line = line.substring(lp + 1, line.length() - 1);

					if (line.isEmpty()) {
						sig = SIG_EMPTY;
					} else {
						var sclasses = line.split(",");
						var types = new TypeDef[sclasses.length];
						for (int i = 0; i < sclasses.length; i++) {
							types[i] = getType(sclasses[i]);
						}

						sig = getSignature(types);
					}

					if (name.startsWith("lambda$") || name.startsWith("access$")) {
						continue;
					}
				} else {
					if (line.startsWith("val$") || line.startsWith("this$")) {
						continue;
					}

					name = line;
					sig = null;
				}

				var rawNameSig = new NamedSignature(rawName, sig);
				var m = new MemberDef(currentClassDef, rawNameSig, name, type, new MutableObject<>(""));
				currentClassDef.members.put(rawNameSig, m);
			} else if (line.charAt(line.length() - 1) == ':') {
				currentClassDef = classes.get(line.substring(line.lastIndexOf(' ') + 1, line.length() - 1));
			}
		}
	}

	public void cleanup() {
		classes.values().removeIf(ClassDef::cleanup);
	}

	public void updateOccurrences() {
		for (var c : allTypes.values()) {
			c.occurrences = 0;
		}

		for (var m : methodSignatures.values()) {
			m.occurrences = 0;
		}

		for (var c : classes.values()) {
			for (var m : c.members.values()) {
				if (m.rawName.signature != null && m.rawName.signature.types.length > 0) {
					m.rawName.signature.occurrences++;

					for (var t : m.rawName.signature.types) {
						t.occurrences++;
					}
				}
			}
		}
	}

	private static void writeVarInt(DataOutput out, int value) throws Exception {
		while ((value & -128) != 0) {
			out.writeByte(value & 127 | 128);
			value >>>= 7;
		}

		out.writeByte(value);
	}

	private static void writeUtf(DataOutput out, String value) throws Exception {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		writeVarInt(out, bytes.length);
		out.write(bytes);
	}

	public void write(DataOutput output) throws Exception {
		cleanup();
		updateOccurrences();

		var typeDefList = new ArrayList<>(allTypes.values());
		typeDefList.sort(TypeDef::compareTo);

		var unmappedTypes = new ArrayList<TypeDef>();
		var mappedTypes = new ArrayList<TypeDef>();
		var arrayTypes = new ArrayList<TypeDef>();

		for (int i = 0; i < typeDefList.size(); i++) {
			var c = typeDefList.get(i);
			c.index = i;

			if (c.array > 0) {
				arrayTypes.add(c);
			} else if (c.parent.mapped) {
				mappedTypes.add(c);
			} else {
				unmappedTypes.add(c);
			}
		}

		var sigList = new ArrayList<>(methodSignatures.values());
		sigList.sort(MethodDefSignature::compareTo);

		for (int i = 0; i < sigList.size(); i++) {
			sigList.get(i).index = i;
		}

		RemappingHelper.LOGGER.info("Total Types: " + typeDefList.size());
		RemappingHelper.LOGGER.info("Total Signatures: " + sigList.size());
		RemappingHelper.LOGGER.info("Unmapped Types: " + unmappedTypes.size());
		RemappingHelper.LOGGER.info("Mapped Types: " + mappedTypes.size());
		RemappingHelper.LOGGER.info("Array Types: " + arrayTypes.size());

		output.writeByte(0); // Binary indicator
		output.writeByte(1); // Version
		writeVarInt(output, typeDefList.size());

		writeVarInt(output, unmappedTypes.size());

		for (var c : unmappedTypes) {
			writeVarInt(output, c.index);
			output.writeUTF(c.parent.rawName);
		}

		writeVarInt(output, mappedTypes.size());

		for (var c : mappedTypes) {
			writeVarInt(output, c.index);
			writeUtf(output, c.parent.unmappedName.getValue());
			writeUtf(output, c.parent.mmName);
		}

		writeVarInt(output, arrayTypes.size());

		for (var c : arrayTypes) {
			writeVarInt(output, c.index);
			writeVarInt(output, c.parent.noArrayType.index);
			writeVarInt(output, c.array);
		}

		writeVarInt(output, sigList.size());

		for (var s : sigList) {
			writeVarInt(output, s.types.length);

			for (var c : s.types) {
				writeVarInt(output, c.index);
			}
		}

		for (var c : mappedTypes) {
			var fields = new ArrayList<MemberDef>();
			var arg0methods = new ArrayList<MemberDef>();
			var argNmethods = new ArrayList<MemberDef>();

			for (var f : c.parent.members.values()) {
				if (f.rawName.signature == null) {
					fields.add(f);
				} else if (f.rawName.signature.types.length == 0) {
					arg0methods.add(f);
				} else {
					argNmethods.add(f);
				}
			}

			writeVarInt(output, fields.size());

			for (var m : fields) {
				writeUtf(output, m.unmappedName.getValue());
				writeUtf(output, m.mmName);
			}

			writeVarInt(output, arg0methods.size());

			for (var m : arg0methods) {
				writeUtf(output, m.unmappedName.getValue());
				writeUtf(output, m.mmName);
			}

			writeVarInt(output, argNmethods.size());

			for (var m : argNmethods) {
				writeUtf(output, m.unmappedName.getValue());
				writeUtf(output, m.mmName);
				writeVarInt(output, m.rawName.signature.index);
			}
		}
	}

	public static MojangMappings parse(List<String> lines) throws Exception {
		var mappings = new MojangMappings();
		mappings.parse0(lines);
		return mappings;
	}

	public static class MethodDefSignature {
		public final TypeDef[] types;
		public int occurrences;
		public int index;

		public MethodDefSignature(TypeDef... types) {
			this.types = types;
			this.occurrences = 0;
			this.index = -1;
		}

		@Override
		public boolean equals(Object o) {
			return this == o || o instanceof MethodDefSignature sig && Arrays.equals(types, sig.types);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(types);
		}

		@Override
		public String toString() {
			if (types.length == 0) {
				return "";
			} else if (types.length == 1) {
				return types[0].toString();
			}

			var sb = new StringBuilder();

			for (int i = 0; i < types.length; i++) {
				if (i > 0) {
					sb.append(',');
				}

				sb.append(types[i]);
			}

			return sb.toString();
		}

		public int compareTo(MethodDefSignature other) {
			return Integer.compare(other.occurrences, occurrences);
		}
	}

	public record NamedSignature(String name, @Nullable MethodDefSignature signature) {
		@Override
		public String toString() {
			if (signature == null) {
				return name;
			}

			return name + "(" + signature + ")";
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof NamedSignature other && name.equals(other.name) && Objects.equals(signature, other.signature);
		}

		@Override
		public int hashCode() {
			return name.hashCode() * 31 + (signature == null ? 0 : signature.hashCode());
		}
	}

	public static final class ClassDef {
		public final MojangMappings mappings;
		public final String rawName;
		public final String mmName;
		public final String displayName;
		public final Map<NamedSignature, MemberDef> members;
		private final MutableObject<String> unmappedName;
		public boolean mapped;

		public TypeDef noArrayType;
		public String rawDescriptor;

		public ClassDef(MojangMappings mappings, String rawName, String mmName, Map<NamedSignature, MemberDef> members) {
			this.mappings = mappings;
			this.rawName = rawName;
			this.mmName = mmName;
			var dn = mmName.isEmpty() ? rawName : mmName;
			var dni = dn.lastIndexOf('.');
			this.displayName = dni == -1 ? dn : dn.substring(dni + 1);
			this.members = members;
			this.unmappedName = new MutableObject<>("");
			this.mapped = false;
			this.noArrayType = new TypeDef(this, 0);
		}

		public ClassDef(MojangMappings mappings, String name) {
			this(mappings, name, "", Map.of());
		}

		public ClassDef descriptor(String s) {
			this.rawDescriptor = s;
			return this;
		}

		private TypeDef array(int a) {
			if (a == 0) {
				return noArrayType;
			}

			var t = new TypeDef(this, a);
			var t1 = mappings.allTypes.get(t);

			if (t1 == null) {
				mappings.allTypes.put(t, t);
				return t;
			}

			return t1;
		}

		@Override
		public int hashCode() {
			return rawName.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof ClassDef other && rawName.equals(other.rawName);
		}

		@Override
		public String toString() {
			return displayName;
		}

		public MutableObject<String> unmappedName() {
			return unmappedName;
		}

		public String getRawDescriptor() {
			if (rawDescriptor == null) {
				rawDescriptor = 'L' + rawName.replace('.', '/') + ';';
			}

			return rawDescriptor;
		}

		public boolean cleanup() {
			if (mapped) {
				members.values().removeIf(MemberDef::cleanup);
				return members.isEmpty() && unmappedName.getValue().isEmpty();
			}

			return false;
		}
	}

	public static final class TypeDef {
		public final ClassDef parent;
		public final int array;
		public int occurrences;
		private MethodDefSignature singleArgumentSignature;
		private String rawDescriptor;
		public int index;

		public TypeDef(ClassDef parent, int array) {
			this.parent = parent;
			this.array = array;
			this.occurrences = 0;
			this.singleArgumentSignature = null;
			this.rawDescriptor = null;
			this.index = -1;
		}

		public String getRawDescriptor() {
			if (rawDescriptor == null) {
				if (array > 0) {
					rawDescriptor = "[".repeat(array) + parent.getRawDescriptor();
				} else {
					rawDescriptor = parent.getRawDescriptor();
				}
			}

			return rawDescriptor;
		}

		public MethodDefSignature getSingleArgumentSignature() {
			if (singleArgumentSignature == null) {
				singleArgumentSignature = new MethodDefSignature(this);
				parent.mappings.methodSignatures.put(singleArgumentSignature, singleArgumentSignature);
			}

			return singleArgumentSignature;
		}

		public int compareTo(TypeDef other) {
			return Integer.compare(other.occurrences, occurrences);
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof TypeDef t && parent.equals(t.parent) && array == t.array;
		}

		@Override
		public int hashCode() {
			return Objects.hash(parent, array);
		}

		@Override
		public String toString() {
			return getRawDescriptor();
		}

	}

	public record MemberDef(ClassDef parent, NamedSignature rawName, String mmName, TypeDef type, MutableObject<String> unmappedName) {
		public boolean cleanup() {
			return unmappedName.getValue().isEmpty();
		}
	}
}

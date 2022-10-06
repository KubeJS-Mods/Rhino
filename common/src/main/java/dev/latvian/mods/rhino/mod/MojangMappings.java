package dev.latvian.mods.rhino.mod;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.apache.commons.lang3.mutable.MutableObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class MojangMappings {
	public final Map<String, ClassDef> classes;
	public final Map<String, ClassDef> classesObf;
	private final Map<MethodDefSignature, MethodDefSignature> methodSignatureCache;

	private final MethodDefSignature SIG_EMPTY = new MethodDefSignature();
	private final MethodDefSignature SIG_BOOLEAN = new MethodDefSignature(ClassDef.BOOLEAN);
	private final MethodDefSignature SIG_CHAR = new MethodDefSignature(ClassDef.CHAR);
	private final MethodDefSignature SIG_BYTE = new MethodDefSignature(ClassDef.BYTE);
	private final MethodDefSignature SIG_SHORT = new MethodDefSignature(ClassDef.SHORT);
	private final MethodDefSignature SIG_INT = new MethodDefSignature(ClassDef.INT);
	private final MethodDefSignature SIG_LONG = new MethodDefSignature(ClassDef.LONG);
	private final MethodDefSignature SIG_FLOAT = new MethodDefSignature(ClassDef.FLOAT);
	private final MethodDefSignature SIG_DOUBLE = new MethodDefSignature(ClassDef.DOUBLE);
	private final MethodDefSignature SIG_STRING = new MethodDefSignature(ClassDef.STRING);
	private final MethodDefSignature SIG_OBJECT = new MethodDefSignature(ClassDef.OBJECT);

	private MojangMappings() {
		classes = new HashMap<>();
		classesObf = new HashMap<>();
		methodSignatureCache = new HashMap<>();

		for (var c : ClassDef.DEFAULT) {
			classes.put(c.name, c);
		}
	}

	public MethodDefSignature getSignature(ClassDef[] classes) {
		if (classes.length == 0) {
			return SIG_EMPTY;
		} else if (classes.length == 1) {
			if (classes[0] == ClassDef.BOOLEAN) {
				return SIG_BOOLEAN;
			} else if (classes[0] == ClassDef.CHAR) {
				return SIG_CHAR;
			} else if (classes[0] == ClassDef.BYTE) {
				return SIG_BYTE;
			} else if (classes[0] == ClassDef.SHORT) {
				return SIG_SHORT;
			} else if (classes[0] == ClassDef.INT) {
				return SIG_INT;
			} else if (classes[0] == ClassDef.LONG) {
				return SIG_LONG;
			} else if (classes[0] == ClassDef.FLOAT) {
				return SIG_FLOAT;
			} else if (classes[0] == ClassDef.DOUBLE) {
				return SIG_DOUBLE;
			} else if (classes[0] == ClassDef.STRING) {
				return SIG_STRING;
			} else if (classes[0] == ClassDef.OBJECT) {
				return SIG_OBJECT;
			}
		}

		var sig = new MethodDefSignature(classes);
		var cached = methodSignatureCache.get(sig);

		if (cached == null) {
			methodSignatureCache.put(sig, sig);
			return sig;
		}

		return cached;
	}

	public ClassDef getClass(String name) {
		var c = classes.get(name);

		if (c != null) {
			return c;
		}

		String tname = name;
		int array = 0;

		while (tname.endsWith("[]")) {
			tname = tname.substring(0, tname.length() - 2);
			array++;
		}

		if (array > 0) {
			var t = getClass(tname);
			c = t.array(array);
			classes.put(name, c);
		} else {
			c = new ClassDef(name);
			classes.put(name, c);
		}

		return c;
	}

	private void parse0(List<String> lines) {
		lines.removeIf(s -> s.isBlank() || s.startsWith("#") || s.endsWith("init>") || s.contains(".package-info "));

		for (var line : lines) {
			if (line.charAt(line.length() - 1) == ':') {
				var s = line.split(" -> ", 2); // replace with faster, last index of space check
				var c = new ClassDef(s[0], s[1].substring(0, s[1].length() - 1), new HashMap<>(), new HashMap<>());
				c.mapped = true;
				classes.put(c.name, c);
				classesObf.put(c.obfName, c);
				c.occurrences++;
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
				var type = getClass(line.substring(0, typeSpace));
				type.occurrences++;
				line = line.substring(typeSpace + 1);
				var obfName = line.substring(line.lastIndexOf(' ') + 1);
				line = line.substring(0, line.indexOf(' '));

				if (line.charAt(line.length() - 1) == ')') {
					int lp = line.indexOf('(');
					var name = line.substring(0, lp);
					line = line.substring(lp + 1, line.length() - 1);
					MethodDefSignature sig;

					if (line.isEmpty()) {
						sig = SIG_EMPTY;
					} else {
						var sclasses = line.split(",");
						var classes = new ClassDef[sclasses.length];
						for (int i = 0; i < sclasses.length; i++) {
							classes[i] = getClass(sclasses[i]);
							classes[i].occurrences++;
						}

						sig = getSignature(classes);
					}

					sig.occurrences++;
					var ns = new NamedSignature(obfName, sig);
					var m = new MethodDef(name, ns, type, new MutableObject<>(""));
					currentClassDef.methods.put(ns, m);
					System.out.println("M " + type + " | " + name + "(" + sig + ") -> " + m.getObfDescriptor(true));
				} else {
					currentClassDef.fields.put(obfName, new FieldDef(line, obfName, type, new MutableObject<>("")));
					System.out.println("F " + type + " | " + line + " -> " + obfName);
				}

				//200:204:void convertStereo(java.nio.FloatBuffer,java.nio.FloatBuffer,com.mojang.blaze3d.audio.OggAudioStream$OutputConcat) -> a
			} else if (line.charAt(line.length() - 1) == ':') {
				currentClassDef = classesObf.get(line.substring(line.lastIndexOf(' ') + 1, line.length() - 1));
				System.out.println("=== " + currentClassDef + " ===");
			}
		}
	}

	// Not actually used
	public byte[] write() throws Exception {
		var classDefList = new ArrayList<>(classes.values());
		classDefList.sort(ClassDef::compareTo);

		var sigList = new ArrayList<>(methodSignatureCache.values());
		sigList.sort(MethodDefSignature::compareTo);

		System.out.println("Total Classes: " + classDefList.size());
		System.out.println("Total Signatures: " + sigList.size());

		var unmappedClasses = new ArrayList<ClassDef>();
		var unmappedArrayClasses = new ArrayList<ClassDef>();
		var mappedClasses = new ArrayList<ClassDef>();
		var mappedArrayClasses = new ArrayList<ClassDef>();

		for (int i = 0; i < classDefList.size(); i++) {
			ClassDef c = classDefList.get(i);
			c.index = i;
			System.out.println(c + ":" + c.occurrences);

			if (c.mapped) {
				if (c.array > 0) {
					mappedArrayClasses.add(c);
				} else {
					mappedClasses.add(c);
				}
			} else {
				if (c.array > 0) {
					unmappedArrayClasses.add(c);
				} else {
					unmappedClasses.add(c);
				}
			}
		}

		for (int i = 0; i < sigList.size(); i++) {
			sigList.get(i).index = i;
		}

		System.out.println("Unmapped: " + unmappedClasses.size());
		System.out.println("Unmapped Arrays: " + unmappedArrayClasses.size());
		System.out.println("Mapped: " + mappedClasses.size());
		System.out.println("Mapped Arrays: " + mappedArrayClasses.size());

		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

		buf.writeByte(0); // Binary indicator
		buf.writeByte(1); // Version
		buf.writeVarInt(unmappedClasses.size());

		for (var c : unmappedClasses) {
			buf.writeVarInt(c.index);
			buf.writeUtf(c.name);
		}

		buf.writeVarInt(unmappedArrayClasses.size());

		for (var c : unmappedArrayClasses) {
			buf.writeVarInt(c.index);
			buf.writeVarInt(c.type.index);
			buf.writeVarInt(c.array);
		}

		buf.writeVarInt(mappedClasses.size());

		for (var c : mappedClasses) {
			buf.writeVarInt(c.index);
			buf.writeUtf(c.name);
			buf.writeUtf(c.obfName);
		}

		buf.writeVarInt(mappedArrayClasses.size());

		for (var c : mappedArrayClasses) {
			buf.writeVarInt(c.index);
			buf.writeVarInt(c.type.index);
			buf.writeVarInt(c.array);
		}

		buf.writeVarInt(sigList.size());

		for (var s : sigList) {
			buf.writeVarInt(s.classes.length);

			for (var c : s.classes) {
				buf.writeVarInt(c.index);
			}
		}

		for (var c : mappedClasses) {
			buf.writeVarInt(c.fields.size());

			for (var f : c.fields.values()) {
				buf.writeUtf(f.name);
				buf.writeUtf(f.obfName);
				buf.writeVarInt(f.type.index);
			}

			buf.writeVarInt(c.methods.size());

			for (var m : c.methods.values()) {
				buf.writeUtf(m.name);
				buf.writeUtf(m.obfName.name);
				buf.writeVarInt(m.returnType.index);
				buf.writeVarInt(m.obfName.signature.index);
			}
		}

		var bytes = new ByteArrayOutputStream();

		try (var out = new GZIPOutputStream(bytes)) {
			// out.write(buf.accessByteBufWithCorrectSize());
			buf.getBytes(0, out, buf.writerIndex());
		}

		var bytesOut = bytes.toByteArray();
		System.out.println(bytesOut.length + " bytes written"); // ~10x less than MM file
		return bytesOut;
	}

	public static MojangMappings parse(List<String> lines) throws Exception {
		var mappings = new MojangMappings();
		mappings.parse0(lines);
		return mappings;
	}

	public static class MethodDefSignature {
		public final ClassDef[] classes;
		public int occurrences;
		public int index;

		public MethodDefSignature(ClassDef... classes) {
			this.classes = classes;
			this.occurrences = 0;
			this.index = 0;
		}

		@Override
		public boolean equals(Object o) {
			return this == o || o instanceof MethodDefSignature sig && Arrays.equals(classes, sig.classes);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(classes);
		}

		@Override
		public String toString() {
			if (classes.length == 0) {
				return "";
			} else if (classes.length == 1) {
				return classes[0].toString();
			}

			var sb = new StringBuilder();

			for (int i = 0; i < classes.length; i++) {
				if (i > 0) {
					sb.append(',');
				}

				sb.append(classes[i]);
			}

			return sb.toString();
		}

		public int compareTo(MethodDefSignature other) {
			return Integer.compare(other.occurrences, occurrences);
		}
	}

	public record NamedSignature(String name, MethodDefSignature signature) {
		@Override
		public String toString() {
			return name + "(" + signature + ")";
		}
	}

	public static final class ClassDef {
		public static final ClassDef VOID = new ClassDef("void").descriptor("V");
		public static final ClassDef BOOLEAN = new ClassDef("boolean").descriptor("Z");
		public static final ClassDef CHAR = new ClassDef("char").descriptor("C");
		public static final ClassDef BYTE = new ClassDef("byte").descriptor("B");
		public static final ClassDef SHORT = new ClassDef("short").descriptor("S");
		public static final ClassDef INT = new ClassDef("int").descriptor("I");
		public static final ClassDef LONG = new ClassDef("long").descriptor("J");
		public static final ClassDef FLOAT = new ClassDef("float").descriptor("F");
		public static final ClassDef DOUBLE = new ClassDef("double").descriptor("D");
		public static final ClassDef STRING = new ClassDef("java.lang.String");
		public static final ClassDef OBJECT = new ClassDef("java.lang.Object");

		public static final ClassDef[] DEFAULT = {
				VOID,
				BOOLEAN,
				CHAR,
				BYTE,
				SHORT,
				INT,
				LONG,
				FLOAT,
				DOUBLE,
				STRING,
				OBJECT
		};

		public int index;
		public final String name;
		public final String obfName;
		public final Map<String, FieldDef> fields;
		public final Map<NamedSignature, MethodDef> methods;
		public final MutableObject<String> unmappedName;
		public boolean mapped;
		public int occurrences;

		public ClassDef type;
		public int array;
		public String obfDescriptor;

		public ClassDef(String name, String obfName, Map<String, FieldDef> fields, Map<NamedSignature, MethodDef> methods) {
			this.name = name;
			this.obfName = obfName;
			this.fields = fields;
			this.methods = methods;
			this.unmappedName = new MutableObject<>("");
			this.mapped = false;
			this.occurrences = 0;

			this.type = this;
			this.array = 0;
		}

		public ClassDef descriptor(String s) {
			this.obfDescriptor = s;
			return this;
		}

		private ClassDef array(int a) {
			var c = new ClassDef("", "", Map.of(), Map.of());
			c.type = this;
			c.mapped = mapped;
			c.array = a;
			return c;
		}

		public ClassDef(String name) {
			this(name, name, Map.of(), Map.of());
		}

		@Override
		public int hashCode() {
			return type.name.hashCode() + array * 31;
		}

		@Override
		public boolean equals(Object obj) {
			return obj == this || obj instanceof ClassDef c && type == c.type && array == c.array;
		}

		@Override
		public String toString() {
			if (array > 0) {
				return type.name + "[]".repeat(array);
			}

			return name;
		}

		public String getObfDescriptor() {
			if (obfDescriptor == null) {
				if (array > 0) {
					obfDescriptor = "[".repeat(array) + type.getObfDescriptor();
				} else {
					obfDescriptor = 'L' + obfName.replace('.', '/') + ';';
				}
			}

			return obfDescriptor;
		}

		public int compareTo(ClassDef other) {
			return Integer.compare(other.occurrences, occurrences);
		}
	}

	public record FieldDef(String name, String obfName, ClassDef type, MutableObject<String> unmappedName) {
	}

	public record MethodDef(String name, NamedSignature obfName, ClassDef returnType, MutableObject<String> unmappedName) {
		public String getObfDescriptor(boolean includeReturnType) {
			var sb = new StringBuilder();
			sb.append(obfName.name);
			sb.append('(');

			for (var s : obfName.signature.classes) {
				sb.append(s.getObfDescriptor());
			}

			sb.append(')');

			if (includeReturnType) {
				sb.append(returnType.getObfDescriptor());
			}

			return sb.toString();
		}
	}
}

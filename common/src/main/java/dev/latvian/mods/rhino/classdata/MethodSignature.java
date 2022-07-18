package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.Context;

public class MethodSignature {
	public static final MethodSignature EMPTY = new MethodSignature();
	public static final MethodSignature OBJECT = new MethodSignature(Object.class);
	public static final MethodSignature OBJECT_ARRAY = new MethodSignature(Object[].class);
	public static final MethodSignature STRING = new MethodSignature(String.class);
	public static final MethodSignature BYTE = new MethodSignature(byte.class);
	public static final MethodSignature SHORT = new MethodSignature(short.class);
	public static final MethodSignature INT = new MethodSignature(int.class);
	public static final MethodSignature LONG = new MethodSignature(long.class);
	public static final MethodSignature FLOAT = new MethodSignature(float.class);
	public static final MethodSignature DOUBLE = new MethodSignature(double.class);
	public static final MethodSignature BOOLEAN = new MethodSignature(boolean.class);
	public static final MethodSignature CHAR = new MethodSignature(char.class);

	public static MethodSignature of(Class<?>... types) {
		if (types.length == 0) {
			return EMPTY;
		} else if (types.length == 1) {
			if (types[0] == Object.class) {
				return OBJECT;
			} else if (types[0] == Object[].class) {
				return OBJECT_ARRAY;
			} else if (types[0] == String.class) {
				return STRING;
			} else if (types[0] == byte.class) {
				return BYTE;
			} else if (types[0] == short.class) {
				return SHORT;
			} else if (types[0] == int.class) {
				return INT;
			} else if (types[0] == long.class) {
				return LONG;
			} else if (types[0] == float.class) {
				return FLOAT;
			} else if (types[0] == double.class) {
				return DOUBLE;
			} else if (types[0] == boolean.class) {
				return BOOLEAN;
			} else if (types[0] == char.class) {
				return CHAR;
			}
		}

		return new MethodSignature(types);
	}

	public final Class<?>[] types;
	private String string = null;
	private int hashCode = 0;

	private MethodSignature(Class<?>... types) {
		this.types = types;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof MethodSignature o) {
			if (types.length != o.types.length) {
				return false;
			}

			for (int i = 0; i < types.length; i++) {
				if (types[i] != o.types[i]) {
					return false;
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		if (string == null) {
			StringBuilder sb = new StringBuilder();
			sb.append('(');

			for (var type : types) {
				sb.append(type.descriptorString());
			}

			sb.append(')');
			string = sb.toString();
		}

		return string;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			int h = 1;

			for (var type : types) {
				h = 31 * h + type.hashCode();
			}

			hashCode = h == 0 ? 1 : h;
		}

		return hashCode;
	}

	public boolean matches(MethodSignature actualArguments, Context cx) {
		if (this == actualArguments) {
			return true;
		}

		return equals(actualArguments);
	}
}

package dev.latvian.mods.rhino;

import java.lang.reflect.Executable;
import java.util.Arrays;

public record MethodSignature(String name, Class<?>[] args) {
	private static final Class<?>[] NO_ARGS = new Class<?>[0];

	public MethodSignature(Executable method) {
		this(method.getName(), method.getParameterCount() == 0 ? NO_ARGS : method.getParameterTypes());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MethodSignature(String name1, Class<?>[] args1)) {
			return name1.equals(name) && Arrays.equals(args, args1);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode() ^ args.length;
	}
}

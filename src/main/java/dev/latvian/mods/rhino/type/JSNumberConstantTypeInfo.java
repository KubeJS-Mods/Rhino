package dev.latvian.mods.rhino.type;

import java.util.Collection;
import java.util.Set;

// 10, -402.01
public record JSNumberConstantTypeInfo(Number number) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return number.toString();
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(number);
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return Set.of();
	}
}

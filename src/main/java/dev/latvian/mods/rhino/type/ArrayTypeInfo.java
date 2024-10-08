package dev.latvian.mods.rhino.type;

import java.util.Collection;
import java.util.Set;

public final class ArrayTypeInfo extends TypeInfoBase {
	private final TypeInfo component;
	private Class<?> asClass;

	ArrayTypeInfo(TypeInfo component) {
		this.component = component;
	}

	@Override
	public Class<?> asClass() {
		if (asClass == null) {
			asClass = component.newArray(0).getClass();
		}

		return asClass;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof ArrayTypeInfo t && component.equals(t.component);
	}

	@Override
	public int hashCode() {
		return component.hashCode();
	}

	@Override
	public String toString() {
		return component + "[]";
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		ctx.append(sb, component);
		sb.append('[');
		sb.append(']');
	}

	@Override
	public String signature() {
		return component.signature() + "[]";
	}

	@Override
	public TypeInfo componentType() {
		return component;
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		component.collectContainedComponentClasses(classes);
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return component.getContainedComponentClasses();
	}
}

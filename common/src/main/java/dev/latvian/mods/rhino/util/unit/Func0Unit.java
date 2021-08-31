package dev.latvian.mods.rhino.util.unit;

import java.util.List;

public abstract class Func0Unit extends FuncUnit implements FuncSupplier {
	@Override
	public Unit create(List<Unit> args) {
		return this;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(getFuncName());
		sb.append('(');
		sb.append(')');
	}
}

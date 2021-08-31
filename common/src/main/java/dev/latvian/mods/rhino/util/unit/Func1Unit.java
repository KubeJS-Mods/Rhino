package dev.latvian.mods.rhino.util.unit;

public abstract class Func1Unit extends FuncUnit {
	public final Unit unit;

	public Func1Unit(Unit u) {
		unit = u;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(getFuncName());
		sb.append('(');
		unit.append(sb);
		sb.append(')');
	}
}

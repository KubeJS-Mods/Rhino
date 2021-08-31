package dev.latvian.mods.rhino.util.unit;

public abstract class Func2Unit extends FuncUnit {
	public final Unit unit;
	public final Unit with;

	public Func2Unit(Unit u, Unit w) {
		unit = u;
		with = w;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(getFuncName());
		sb.append('(');
		unit.append(sb);
		sb.append(',');
		sb.append(' ');
		with.append(sb);
		sb.append(')');
	}
}

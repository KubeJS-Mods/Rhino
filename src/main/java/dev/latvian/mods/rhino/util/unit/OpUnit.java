package dev.latvian.mods.rhino.util.unit;

public abstract class OpUnit extends Unit {
	public final Unit unit;
	public final Unit with;

	public OpUnit(Unit u, Unit w) {
		unit = u;
		with = w;
	}

	public abstract String getSymbols();

	@Override
	public void append(StringBuilder sb) {
		sb.append('(');
		unit.append(sb);
		sb.append(getSymbols());
		with.append(sb);
		sb.append(')');
	}
}

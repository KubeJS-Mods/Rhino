package dev.latvian.mods.rhino.util.unit;

public abstract class SpecialOpUnit extends Unit {
	public final Unit unit;

	public SpecialOpUnit(Unit u) {
		unit = u;
	}

	public abstract char getSymbol();

	@Override
	public void append(StringBuilder sb) {
		sb.append(getSymbol());
		sb.append(unit);
	}
}
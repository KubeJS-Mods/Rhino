package dev.latvian.mods.rhino.util.unit;

public class NotUnit extends SpecialOpUnit {
	public NotUnit(Unit u) {
		super(u);
	}

	@Override
	public char getSymbol() {
		return '!';
	}

	@Override
	public float get() {
		return ~unit.getAsInt();
	}

	@Override
	public int getAsInt() {
		return ~unit.getAsInt();
	}

	@Override
	public boolean getAsBoolean() {
		return !unit.getAsBoolean();
	}

	@Override
	public Unit not() {
		return unit;
	}
}

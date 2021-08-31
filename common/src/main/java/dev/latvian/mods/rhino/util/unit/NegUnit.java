package dev.latvian.mods.rhino.util.unit;

public class NegUnit extends SpecialOpUnit {
	public NegUnit(Unit u) {
		super(u);
	}

	@Override
	public char getSymbol() {
		return '-';
	}

	@Override
	public float get() {
		float f = unit.get();
		return f == 0F ? 0F : -f;
	}

	@Override
	public int getAsInt() {
		int i = unit.getAsInt();
		return i == 0 ? i : -i;
	}

	@Override
	public Unit neg() {
		return unit;
	}
}
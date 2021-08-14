package dev.latvian.mods.rhino.util.unit;

public class AbsUnit extends Func1Unit {
	public AbsUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return Math.abs(unit.get());
	}

	@Override
	public int getAsInt() {
		return Math.abs(unit.getAsInt());
	}

	@Override
	public String toString() {
		return fString("abs", unit);
	}
}
package dev.latvian.mods.rhino.util.unit;

public class ShiftRightUnit extends Func2Unit {
	public ShiftRightUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.getAsInt() >> with.getAsInt();
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() >> with.getAsInt();
	}

	@Override
	public String toString() {
		return aString(unit, " >> ", with);
	}
}

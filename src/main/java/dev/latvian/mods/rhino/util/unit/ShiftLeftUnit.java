package dev.latvian.mods.rhino.util.unit;

public class ShiftLeftUnit extends Func2Unit {
	public ShiftLeftUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return (int) unit.get() << (int) with.get();
	}

	@Override
	public String toString() {
		return aString(unit, " << ", with);
	}
}

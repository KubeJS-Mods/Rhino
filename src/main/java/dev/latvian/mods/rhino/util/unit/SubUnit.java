package dev.latvian.mods.rhino.util.unit;

public class SubUnit extends Func2Unit {
	public SubUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.get() - with.get();
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() - with.getAsInt();
	}

	@Override
	public String toString() {
		return aString(unit, " - ", with);
	}
}
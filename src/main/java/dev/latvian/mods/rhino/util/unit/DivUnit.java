package dev.latvian.mods.rhino.util.unit;

public class DivUnit extends Func2Unit {
	public DivUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.get() / with.get();
	}

	@Override
	public String toString() {
		return aString(unit, " / ", with);
	}
}

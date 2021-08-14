package dev.latvian.mods.rhino.util.unit;

public class ModUnit extends Func2Unit {
	public ModUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.get() % with.get();
	}

	@Override
	public String toString() {
		return aString(unit, " % ", with);
	}
}
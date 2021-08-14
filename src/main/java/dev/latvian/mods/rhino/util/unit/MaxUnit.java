package dev.latvian.mods.rhino.util.unit;

public class MaxUnit extends Func2Unit {
	public MaxUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return Math.max(unit.get(), with.get());
	}

	@Override
	public String toString() {
		return fString("max", unit, with);
	}
}

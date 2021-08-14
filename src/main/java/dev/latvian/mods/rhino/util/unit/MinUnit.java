package dev.latvian.mods.rhino.util.unit;

public class MinUnit extends Func2Unit {
	public MinUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return Math.min(unit.get(), with.get());
	}

	@Override
	public int getAsInt() {
		return Math.min(unit.getAsInt(), with.getAsInt());
	}

	@Override
	public String toString() {
		return fString("min", unit, with);
	}
}

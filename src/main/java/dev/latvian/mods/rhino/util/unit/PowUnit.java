package dev.latvian.mods.rhino.util.unit;

public class PowUnit extends Func2Unit {
	public PowUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return (float) Math.pow(unit.get(), with.get());
	}

	@Override
	public String toString() {
		return aString(unit, " ** ", with);
	}
}

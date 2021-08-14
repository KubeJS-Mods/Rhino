package dev.latvian.mods.rhino.util.unit;

public class AtanUnit extends Func1Unit {
	public AtanUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.atan(unit.get());
	}

	@Override
	public String toString() {
		return fString("atan", unit);
	}
}
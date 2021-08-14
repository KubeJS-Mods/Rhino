package dev.latvian.mods.rhino.util.unit;

public class TanUnit extends Func1Unit {
	public TanUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.tan(unit.get());
	}

	@Override
	public String toString() {
		return fString("tan", unit);
	}
}
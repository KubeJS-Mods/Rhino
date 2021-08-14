package dev.latvian.mods.rhino.util.unit;

public class CosUnit extends Func1Unit {
	public CosUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.cos(unit.get());
	}

	@Override
	public String toString() {
		return fString("cos", unit);
	}
}
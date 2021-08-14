package dev.latvian.mods.rhino.util.unit;

public class SqUnit extends Func1Unit {
	public SqUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return unit.get() * unit.get();
	}

	@Override
	public String toString() {
		return fString("sq", unit);
	}
}
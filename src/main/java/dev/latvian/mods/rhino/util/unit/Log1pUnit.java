package dev.latvian.mods.rhino.util.unit;

public class Log1pUnit extends Func1Unit {
	public Log1pUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.log1p(unit.get());
	}

	@Override
	public String toString() {
		return fString("log1p", unit);
	}
}
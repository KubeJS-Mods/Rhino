package dev.latvian.mods.rhino.util.unit;

public class LogUnit extends Func1Unit {
	public LogUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.log(unit.get());
	}

	@Override
	public String toString() {
		return fString("log", unit);
	}
}
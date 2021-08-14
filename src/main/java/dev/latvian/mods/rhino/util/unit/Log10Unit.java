package dev.latvian.mods.rhino.util.unit;

public class Log10Unit extends Func1Unit {
	public Log10Unit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return (float) Math.log10(unit.get());
	}

	@Override
	public String toString() {
		return fString("log10", unit);
	}
}
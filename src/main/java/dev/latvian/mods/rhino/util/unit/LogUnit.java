package dev.latvian.mods.rhino.util.unit;

public class LogUnit extends Func1Unit {
	public LogUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "log";
	}

	@Override
	public float get() {
		return (float) Math.log(unit.get());
	}
}
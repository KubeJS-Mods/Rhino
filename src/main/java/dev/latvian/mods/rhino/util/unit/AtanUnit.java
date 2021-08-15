package dev.latvian.mods.rhino.util.unit;

public class AtanUnit extends Func1Unit {
	public AtanUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "atan";
	}

	@Override
	public float get() {
		return (float) Math.atan(unit.get());
	}
}
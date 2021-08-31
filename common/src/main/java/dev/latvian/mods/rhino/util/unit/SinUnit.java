package dev.latvian.mods.rhino.util.unit;

public class SinUnit extends Func1Unit {
	public SinUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "sin";
	}

	@Override
	public float get() {
		return (float) Math.sin(unit.get());
	}
}
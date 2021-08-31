package dev.latvian.mods.rhino.util.unit;

public class TanUnit extends Func1Unit {
	public TanUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "tan";
	}

	@Override
	public float get() {
		return (float) Math.tan(unit.get());
	}
}
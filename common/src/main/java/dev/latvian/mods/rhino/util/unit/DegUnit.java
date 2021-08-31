package dev.latvian.mods.rhino.util.unit;

public class DegUnit extends Func1Unit {
	public DegUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "deg";
	}

	@Override
	public float get() {
		return (float) Math.toDegrees(unit.get());
	}
}
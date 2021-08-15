package dev.latvian.mods.rhino.util.unit;

public class RadUnit extends Func1Unit {
	public RadUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "rad";
	}

	@Override
	public float get() {
		return (float) Math.toRadians(unit.get());
	}
}
package dev.latvian.mods.rhino.util.unit;

public class MaxUnit extends Func2Unit {
	public MaxUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getFuncName() {
		return "max";
	}

	@Override
	public float get() {
		return Math.max(unit.get(), with.get());
	}

	@Override
	public int getAsInt() {
		return Math.max(unit.getAsInt(), with.getAsInt());
	}
}

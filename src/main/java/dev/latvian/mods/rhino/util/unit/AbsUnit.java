package dev.latvian.mods.rhino.util.unit;

public class AbsUnit extends Func1Unit {
	public AbsUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "abs";
	}

	@Override
	public float get() {
		return Math.abs(unit.get());
	}

	@Override
	public int getAsInt() {
		return Math.abs(unit.getAsInt());
	}
}
package dev.latvian.mods.rhino.util.unit;

public class EqUnit extends Func2Unit {
	public EqUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.get() == with.get() ? 1F : 0F;
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() == with.getAsInt() ? 1 : 0;
	}

	@Override
	public boolean getAsBoolean() {
		return unit.getAsBoolean() == with.getAsBoolean();
	}

	@Override
	public String toString() {
		return aString(unit, " == ", with);
	}
}

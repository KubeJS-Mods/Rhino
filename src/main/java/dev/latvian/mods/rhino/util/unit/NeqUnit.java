package dev.latvian.mods.rhino.util.unit;

public class NeqUnit extends Func2Unit {
	public NeqUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public float get() {
		return unit.get() == with.get() ? 0F : 1F;
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() == with.getAsInt() ? 0 : 1;
	}

	@Override
	public boolean getAsBoolean() {
		return unit.getAsBoolean() != with.getAsBoolean();
	}

	@Override
	public String toString() {
		return aString(unit, " != ", with);
	}
}

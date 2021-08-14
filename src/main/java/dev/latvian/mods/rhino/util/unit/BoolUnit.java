package dev.latvian.mods.rhino.util.unit;

public class BoolUnit extends Func1Unit {
	public BoolUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return unit.getAsBoolean() ? 1F : 0F;
	}

	@Override
	public int getAsInt() {
		return unit.getAsBoolean() ? 1 : 0;
	}

	@Override
	public boolean getAsBoolean() {
		return unit.getAsBoolean();
	}

	@Override
	public String toString() {
		return fString("bool", unit);
	}
}

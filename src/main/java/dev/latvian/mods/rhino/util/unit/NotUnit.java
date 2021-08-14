package dev.latvian.mods.rhino.util.unit;

public class NotUnit extends Func1Unit {
	public NotUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return ~unit.getAsInt();
	}

	@Override
	public int getAsInt() {
		return ~unit.getAsInt();
	}

	@Override
	public boolean getAsBoolean() {
		return !unit.getAsBoolean();
	}

	@Override
	public String toString() {
		return "~" + unit;
	}
}

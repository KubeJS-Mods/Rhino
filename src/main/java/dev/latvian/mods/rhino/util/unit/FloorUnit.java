package dev.latvian.mods.rhino.util.unit;

public class FloorUnit extends Func1Unit {
	public static int floor(float f) {
		int i = (int) f;
		return f < (float) i ? i - 1 : i;
	}

	public FloorUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		return floor(unit.get());
	}

	@Override
	public String toString() {
		return fString("floor", unit);
	}
}
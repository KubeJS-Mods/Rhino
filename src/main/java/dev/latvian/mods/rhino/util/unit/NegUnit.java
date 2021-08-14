package dev.latvian.mods.rhino.util.unit;

public class NegUnit extends Func1Unit {
	public NegUnit(Unit u) {
		super(u);
	}

	@Override
	public float get() {
		float f = unit.get();
		return f == 0F ? 0F : -f;
	}

	@Override
	public int getAsInt() {
		int i = unit.getAsInt();
		return i == 0 ? i : -i;
	}

	@Override
	public String toString() {
		return "-" + unit;
	}

	@Override
	public Unit neg() {
		return unit;
	}
}
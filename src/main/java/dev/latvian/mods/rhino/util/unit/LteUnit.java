package dev.latvian.mods.rhino.util.unit;

public class LteUnit extends OpUnit {
	public LteUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "<=";
	}

	@Override
	public float get() {
		return unit.get() <= with.get() ? 1F : 0F;
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() <= with.getAsInt() ? 1 : 0;
	}

	@Override
	public boolean getAsBoolean() {
		return unit.get() <= with.get();
	}
}

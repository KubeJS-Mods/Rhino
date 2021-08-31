package dev.latvian.mods.rhino.util.unit;

public class NeqUnit extends OpUnit {
	public NeqUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "!=";
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
}

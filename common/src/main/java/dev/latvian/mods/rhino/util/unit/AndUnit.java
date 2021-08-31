package dev.latvian.mods.rhino.util.unit;

public class AndUnit extends OpUnit {
	public AndUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "&";
	}

	@Override
	public float get() {
		return unit.getAsInt() & with.getAsInt();
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() & with.getAsInt();
	}

	@Override
	public boolean getAsBoolean() {
		return unit.getAsBoolean() && with.getAsBoolean();
	}
}

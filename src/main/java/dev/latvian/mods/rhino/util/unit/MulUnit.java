package dev.latvian.mods.rhino.util.unit;

public class MulUnit extends OpUnit {
	public MulUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "*";
	}

	@Override
	public float get() {
		return unit.get() * with.get();
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() * with.getAsInt();
	}
}

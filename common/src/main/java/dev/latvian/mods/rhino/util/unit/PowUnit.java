package dev.latvian.mods.rhino.util.unit;

public class PowUnit extends OpUnit {
	public PowUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "**";
	}

	@Override
	public float get() {
		return (float) Math.pow(unit.get(), with.get());
	}
}

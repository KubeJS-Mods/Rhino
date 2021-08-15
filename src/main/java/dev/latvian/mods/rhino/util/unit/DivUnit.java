package dev.latvian.mods.rhino.util.unit;

public class DivUnit extends OpUnit {
	public DivUnit(Unit u, Unit w) {
		super(u, w);
	}

	@Override
	public String getSymbols() {
		return "/";
	}

	@Override
	public float get() {
		return unit.get() / with.get();
	}
}

package dev.latvian.mods.rhino.util.unit;

public class VariableUnit implements Unit {
	private final Unit unit;
	private final String key;

	public VariableUnit(Unit u, String k) {
		unit = u;
		key = k;
	}

	@Override
	public float get() {
		return unit.get();
	}

	@Override
	public String toString() {
		return "$" + key;
	}
}

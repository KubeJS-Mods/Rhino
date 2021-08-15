package dev.latvian.mods.rhino.util.unit;

public class VariableUnit extends Unit {
	public final Unit unit;
	public final String key;

	public VariableUnit(Unit u, String k) {
		unit = u;
		key = k;
	}

	@Override
	public float get() {
		return unit.get();
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append('$');
		sb.append(key);
	}
}

package dev.latvian.mods.rhino.util.unit;

public class VariableUnit extends Unit {
	public final UnitStorage unitStorage;
	public final String key;
	private Unit unit = null;
	private long version = -1L;

	public VariableUnit(UnitStorage u, String k) {
		unitStorage = u;
		key = k;
	}

	@Override
	public float get() {
		long v = unitStorage.getVariableVersion();

		if (version != v) {
			version = v;
			unit = unitStorage.getVariable(key);
		}

		return unit == null ? 0F : unit.get();
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append('$');
		sb.append(key);
	}
}

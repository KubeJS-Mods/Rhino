package dev.latvian.mods.unit;

public class VariableSubset extends VariableSet {

	private final VariableSet parent;

	VariableSubset(VariableSet parent) {
		this.parent = parent;
	}

	@Override
	public Unit get(String entry) {
		Unit v = super.get(entry);
		return v == null ? parent.get(entry) : v;
	}
}

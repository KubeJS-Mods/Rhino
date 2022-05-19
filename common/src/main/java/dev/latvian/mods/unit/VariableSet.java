package dev.latvian.mods.unit;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class VariableSet implements UnitVariables {

	private final Map<String, Unit> variables = new HashMap<>();

	public VariableSet set(String name, Unit value) {
		variables.put(name, value);
		return this;
	}

	public VariableSet set(String name, double value) {
		return set(name, FixedNumberUnit.ofFixed(value));
	}

	@Nullable
	public Unit get(String entry) {
		return variables.get(entry);
	}

	public VariableSet createSubset() {
		return new VariableSubset(this);
	}

	@Override
	public final VariableSet getVariables() {
		return this;
	}
}

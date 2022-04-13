package dev.latvian.mods.unit;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class VariableSet {

	private final Map<String, Unit> variables = new HashMap<>();

	public VariableSet set(String name, Unit value) {
		variables.put(name, value);
		return this;
	}

	public VariableSet set(String name, double value) {
		return set(name, Unit.of(value));
	}

	@Nullable
	public Unit get(String entry) {
		return variables.get(entry);
	}

	public VariableSet createSubset() {
		return new VariableSubset(this);
	}
}

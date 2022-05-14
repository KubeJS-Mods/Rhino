package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitToken;

import java.util.HashMap;
import java.util.Map;

public class VariableUnit extends Unit implements UnitToken {
	private static final Object CACHE_LOCK = new Object();
	private static final Map<String, VariableUnit> CACHE = new HashMap<>();

	public static VariableUnit of(String name) {
		synchronized (CACHE_LOCK) {
			return CACHE.computeIfAbsent(name, VariableUnit::new);
		}
	}

	public final String name;

	private VariableUnit(String n) {
		name = n;
	}

	@Override
	public double get(UnitVariables variables) {
		Unit var = variables.getVariables().get(name);

		if (var == null) {
			throw new IllegalStateException("Variable " + name + " is not defined!");
		}

		return var.get(variables);
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(name);
	}

	@Override
	public Unit interpret(UnitContext context) {
		return this;
	}
}

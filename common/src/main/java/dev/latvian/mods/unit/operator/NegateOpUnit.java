package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class NegateOpUnit extends UnaryOpUnit {
	@Override
	public double get(UnitVariables variables) {
		return -unit.get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return -unit.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return !unit.getBoolean(variables);
	}
}

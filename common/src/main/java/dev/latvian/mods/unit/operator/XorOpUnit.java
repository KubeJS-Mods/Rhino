package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class XorOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return left.getInt(variables) ^ right.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.getBoolean(variables) ^ right.getBoolean(variables);
	}
}

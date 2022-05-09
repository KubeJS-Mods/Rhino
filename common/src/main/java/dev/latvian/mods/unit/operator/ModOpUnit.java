package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class ModOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		return left.get(variables) % right.get(variables);
	}
}

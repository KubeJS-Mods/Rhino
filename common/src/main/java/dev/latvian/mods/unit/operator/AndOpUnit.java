package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class AndOpUnit extends BooleanOpUnit {
	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.getBoolean(variables) && right.getBoolean(variables);
	}
}

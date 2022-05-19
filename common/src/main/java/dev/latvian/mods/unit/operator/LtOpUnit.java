package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class LtOpUnit extends BooleanOpUnit {
	public LtOpUnit(Unit left, Unit right) {
		super(UnitSymbol.LT, left, right);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.get(variables) < right.get(variables);
	}
}

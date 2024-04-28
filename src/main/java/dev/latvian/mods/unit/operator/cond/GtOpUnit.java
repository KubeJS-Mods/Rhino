package dev.latvian.mods.unit.operator.cond;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class GtOpUnit extends CondOpUnit {
	public GtOpUnit(Unit left, Unit right) {
		super(UnitSymbol.GT, left, right);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.get(variables) > right.get(variables);
	}
}

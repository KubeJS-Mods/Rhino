package dev.latvian.mods.unit.operator.cond;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class OrOpUnit extends CondOpUnit {
	public OrOpUnit(Unit left, Unit right) {
		super(UnitSymbol.OR, left, right);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.getBoolean(variables) || right.getBoolean(variables);
	}
}

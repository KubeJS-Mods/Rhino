package dev.latvian.mods.unit.operator.cond;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class NeqOpUnit extends CondOpUnit {
	public NeqOpUnit(Unit left, Unit right) {
		super(UnitSymbol.NEQ, left, right);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left != right && Math.abs(left.get(variables) - right.get(variables)) >= 0.00001D;
	}
}

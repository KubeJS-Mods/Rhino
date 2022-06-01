package dev.latvian.mods.unit.operator.op;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.token.UnitSymbol;

public class SubOpUnit extends OpUnit {
	public SubOpUnit(Unit left, Unit right) {
		super(UnitSymbol.SUB, left, right);
	}

	@Override
	public double get(UnitVariables variables) {
		return left.get(variables) - right.get(variables);
	}
}

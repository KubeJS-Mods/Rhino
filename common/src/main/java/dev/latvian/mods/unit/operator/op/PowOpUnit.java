package dev.latvian.mods.unit.operator.op;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.token.UnitSymbol;

public class PowOpUnit extends OpUnit {
	public PowOpUnit(Unit left, Unit right) {
		super(UnitSymbol.POW, left, right);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.pow(left.get(variables), right.get(variables));
	}
}

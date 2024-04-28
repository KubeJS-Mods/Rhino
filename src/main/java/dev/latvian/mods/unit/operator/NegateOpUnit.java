package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class NegateOpUnit extends UnaryOpUnit {
	public NegateOpUnit(Unit unit) {
		super(UnitSymbol.BIT_NOT, unit);
	}

	@Override
	public double get(UnitVariables variables) {
		return -unit.get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return -unit.getInt(variables);
	}
}

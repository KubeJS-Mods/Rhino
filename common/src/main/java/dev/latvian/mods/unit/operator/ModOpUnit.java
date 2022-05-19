package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class ModOpUnit extends OpUnit {
	public ModOpUnit(Unit left, Unit right) {
		super(UnitSymbol.MOD, left, right);
	}

	@Override
	public double get(UnitVariables variables) {
		return left.get(variables) % right.get(variables);
	}
}

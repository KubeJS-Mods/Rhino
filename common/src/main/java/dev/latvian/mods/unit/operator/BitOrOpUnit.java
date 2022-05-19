package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitSymbol;

public class BitOrOpUnit extends OpUnit {
	public BitOrOpUnit(Unit left, Unit right) {
		super(UnitSymbol.BIT_OR, left, right);
	}

	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return left.getInt(variables) | right.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return left.getBoolean(variables) || right.getBoolean(variables);
	}
}

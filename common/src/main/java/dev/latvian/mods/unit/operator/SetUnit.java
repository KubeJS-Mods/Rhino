package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.VariableUnit;
import dev.latvian.mods.unit.token.UnitSymbol;

public class SetUnit extends OpUnit {
	public SetUnit(UnitSymbol symbol, Unit left, Unit right) {
		super(symbol, left, right);
	}

	@Override
	public double get(UnitVariables variables) {
		if (left instanceof VariableUnit var) {
			variables.getVariables().set(var.name, right.get(variables));
		}

		return right.get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		if (left instanceof VariableUnit var) {
			variables.getVariables().set(var.name, right.get(variables));
		}

		return right.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		if (left instanceof VariableUnit var) {
			variables.getVariables().set(var.name, right.get(variables));
		}

		return right.getBoolean(variables);
	}
}

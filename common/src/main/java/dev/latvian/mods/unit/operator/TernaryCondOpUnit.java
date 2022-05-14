package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.UnitVariables;

public class TernaryCondOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		if (right instanceof TernaryValuesOpUnit ten) {
			return (left.getBoolean(variables) ? ten.left : ten.right).get(variables);
		}

		throw new IllegalStateException("Right side is not TernaryValuesOpUnit!");
	}

	@Override
	public int getInt(UnitVariables variables) {
		if (right instanceof TernaryValuesOpUnit ten) {
			return (left.getBoolean(variables) ? ten.left : ten.right).getInt(variables);
		}

		throw new IllegalStateException("Right side is not TernaryValuesOpUnit!");
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		if (right instanceof TernaryValuesOpUnit ten) {
			return (left.getBoolean(variables) ? ten.left : ten.right).getBoolean(variables);
		}

		throw new IllegalStateException("Right side is not TernaryValuesOpUnit!");
	}
}

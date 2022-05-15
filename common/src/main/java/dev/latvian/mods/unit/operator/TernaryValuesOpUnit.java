package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class TernaryValuesOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		throw new IllegalStateException("Can't invoke getters in TernaryValuesOpUnit!");
	}

	@Override
	public int getInt(UnitVariables variables) {
		throw new IllegalStateException("Can't invoke getters in TernaryValuesOpUnit!");
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		throw new IllegalStateException("Can't invoke getters in TernaryValuesOpUnit!");
	}

	@Override
	public Unit optimize() {
		left = left.optimize();
		right = right.optimize();
		return this;
	}
}

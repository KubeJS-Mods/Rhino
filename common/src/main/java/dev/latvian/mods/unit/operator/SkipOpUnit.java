package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class SkipOpUnit extends OpUnit {
	@Override
	public double get(UnitVariables variables) {
		throw new IllegalStateException("Cannot get value of skip operator!");
	}

	@Override
	public Unit optimize() {
		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(symbol);
	}
}

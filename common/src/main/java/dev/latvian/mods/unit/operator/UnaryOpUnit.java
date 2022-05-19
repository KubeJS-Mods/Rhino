package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitSymbol;

public abstract class UnaryOpUnit extends Unit {
	public Unit unit;
	public UnitSymbol symbol;

	@Override
	public Unit optimize() {
		unit = unit.optimize();

		if (unit.isFixed()) {
			return FixedNumberUnit.ofFixed(get(EmptyVariableSet.INSTANCE));
		}

		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');
		builder.append(symbol);
		unit.toString(builder);
		builder.append(')');
	}
}
package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitSymbol;

public abstract class OpUnit extends Unit {
	public Unit left;
	public Unit right;
	public UnitSymbol symbol;

	@Override
	public Unit optimize() {
		left = left.optimize();
		right = right.optimize();

		if (left.isFixed() && right.isFixed()) {
			return FixedNumberUnit.ofFixed(get(EmptyVariableSet.INSTANCE));
		}

		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');

		if (left == null) {
			builder.append("null");
		} else {
			left.toString(builder);
		}

		builder.append(symbol);

		if (right == null) {
			builder.append("null");
		} else {
			right.toString(builder);
		}

		builder.append(')');
	}
}

package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.BooleanUnit;
import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.SymbolUnitToken;

public abstract class OpUnit extends Unit {
	public Unit left;
	public Unit right;
	public SymbolUnitToken symbol;

	@Override
	public Unit optimize() {
		left = left.optimize();
		right = right.optimize();

		if (left instanceof FixedNumberUnit && right instanceof FixedNumberUnit) {
			return FixedNumberUnit.ofFixed(get(EmptyVariableSet.INSTANCE));
		} else if (left instanceof BooleanUnit && right instanceof BooleanUnit) {
			return BooleanUnit.of(getBoolean(EmptyVariableSet.INSTANCE));
		}

		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');
		left.toString(builder);
		builder.append(symbol.symbol);
		right.toString(builder);
		builder.append(')');
	}
}

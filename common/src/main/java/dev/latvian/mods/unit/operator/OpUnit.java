package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitSymbol;

public abstract class OpUnit extends Unit {
	public final UnitSymbol symbol;
	public Unit left;
	public Unit right;

	public OpUnit(UnitSymbol symbol, Unit left, Unit right) {
		this.symbol = symbol;
		this.left = left;
		this.right = right;
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

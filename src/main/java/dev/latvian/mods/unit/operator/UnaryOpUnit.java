package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitSymbol;

public abstract class UnaryOpUnit extends Unit {
	public final UnitSymbol symbol;
	public Unit unit;

	public UnaryOpUnit(UnitSymbol symbol, Unit unit) {
		this.symbol = symbol;
		this.unit = unit;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');
		builder.append(symbol);
		unit.toString(builder);
		builder.append(')');
	}
}
package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.operator.OpUnit;

import java.util.function.Supplier;

public record OperatorFactory(UnitSymbol symbol, Supplier<OpUnit> unit) {
	public OpUnit create() {
		OpUnit u = unit.get();
		u.op = this;
		return u;
	}

	@Override
	public String toString() {
		return symbol.symbol;
	}
}

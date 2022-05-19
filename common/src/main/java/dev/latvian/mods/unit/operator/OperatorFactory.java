package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.token.UnitSymbol;

import java.util.function.Supplier;

public record OperatorFactory(UnitSymbol symbol, OpSupplier supplier) {
	@FunctionalInterface
	public interface OpSupplier extends Supplier<OpUnit> {
	}

	public OpUnit create() {
		OpUnit u = supplier.get();
		u.symbol = symbol;
		return u;
	}

	@Override
	public String toString() {
		return symbol.symbol;
	}
}

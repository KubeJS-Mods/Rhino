package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.token.UnitSymbol;

import java.util.function.Supplier;

public record UnaryOperatorFactory(UnitSymbol symbol, OpSupplier supplier) {
	@FunctionalInterface
	public interface OpSupplier extends Supplier<UnaryOpUnit> {
	}

	public UnaryOpUnit create() {
		UnaryOpUnit u = supplier.get();
		u.symbol = symbol;
		return u;
	}

	@Override
	public String toString() {
		return symbol.symbol;
	}
}

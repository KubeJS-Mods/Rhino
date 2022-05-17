package dev.latvian.mods.unit;

import dev.latvian.mods.unit.token.UnitSymbol;
import dev.latvian.mods.unit.token.UnitTokenStream;

public class SymbolUnit extends Unit {
	public final UnitSymbol symbol;

	public SymbolUnit(UnitSymbol symbol) {
		this.symbol = symbol;
	}

	@Override
	public double get(UnitVariables variables) {
		throw new IllegalStateException("Cannot get value of symbol '" + symbol + "'!");
	}

	@Override
	public Unit optimize() {
		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append(symbol.name());
	}

	@Override
	public boolean shouldNegate() {
		return symbol != UnitSymbol.RP;
	}

	@Override
	public void interpret(UnitTokenStream tokenStream) {
		if (symbol.op != null) {
			symbol.op.create().interpret(tokenStream);
		} else {
			throw new IllegalStateException("SymbolUnitToken '" + this + "' is not an operator!");
		}
	}
}

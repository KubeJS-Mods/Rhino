package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitTokenStream;

public class SkipOpUnit extends OpUnit {
	@Override
	public int getPrecedence() {
		return 0;
	}

	@Override
	public boolean shouldSkip() {
		return true;
	}

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
		builder.append(op.symbol().symbol);
	}

	@Override
	public void interpret(UnitTokenStream tokenStream) {
	}
}

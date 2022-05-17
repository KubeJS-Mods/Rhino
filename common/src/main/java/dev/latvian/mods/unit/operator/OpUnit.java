package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.OperatorFactory;
import dev.latvian.mods.unit.token.UnitTokenStream;

public abstract class OpUnit extends Unit {
	public Unit left;
	public Unit right;
	public OperatorFactory op;

	public int getPrecedence() {
		return 2;
	}

	public boolean shouldSkip() {
		return false;
	}

	public final boolean hasHigherPrecedenceThan(OpUnit operator) {
		return operator.getPrecedence() <= getPrecedence();
	}

	@Override
	public Unit optimize() {
		left = left.optimize();
		right = right.optimize();

		if (left instanceof FixedNumberUnit && right instanceof FixedNumberUnit) {
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

		builder.append(op.symbol().symbol);

		if (right == null) {
			builder.append("null");
		} else {
			right.toString(builder);
		}

		builder.append(')');
	}

	@Override
	public boolean shouldNegate() {
		return true;
	}

	@Override
	public void interpret(UnitTokenStream tokenStream) {
		right = tokenStream.resultStack.pop();
		left = tokenStream.resultStack.pop();
		tokenStream.resultStack.push(this);
	}
}

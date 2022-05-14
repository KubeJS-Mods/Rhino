package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.operator.OpUnit;

import java.util.List;

public record PostfixUnitToken(List<UnitToken> infix, boolean group) implements UnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		if (infix.size() == 1) {
			return infix.get(0).interpret(context);
		} else if (infix.size() == 3 && infix.get(1) instanceof SymbolUnitToken token && token.isOp()) {
			OpUnit unit = token.createOpUnit();

			if (unit == null) {
				throw new IllegalStateException("Failed to create operator unit!");
			}

			unit.left = infix.get(0).interpret(context);
			unit.right = infix.get(2).interpret(context);
			return unit;
		}

		throw new IllegalStateException("Im too dumb to convert infix to postfix for now!");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		if (group) {
			builder.append('(');
		}

		for (int i = 0; i < infix.size(); i++) {
			if (i > 0) {
				builder.append(' ');
			}

			builder.append(infix.get(i));
		}

		if (group) {
			builder.append(')');
		}

		return builder.toString();
	}
}

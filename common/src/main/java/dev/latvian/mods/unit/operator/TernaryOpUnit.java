package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class TernaryOpUnit extends OpUnit {
	public Unit cond;

	@Override
	public double get(UnitVariables variables) {
		return (cond.getBoolean(variables) ? left : right).get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return (cond.getBoolean(variables) ? left : right).getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return (cond.getBoolean(variables) ? left : right).getBoolean(variables);
	}

	@Override
	public Unit optimize() {
		cond = cond.optimize();
		left = left.optimize();
		right = right.optimize();
		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');

		if (cond == null) {
			builder.append("null");
		} else {
			cond.toString(builder);
		}

		builder.append('?');

		if (left == null) {
			builder.append("null");
		} else {
			left.toString(builder);
		}

		builder.append(':');

		if (right == null) {
			builder.append("null");
		} else {
			right.toString(builder);
		}

		builder.append(')');
	}
}

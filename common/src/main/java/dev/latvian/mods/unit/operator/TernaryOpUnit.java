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
		cond.toString(builder);
		builder.append('?');
		left.toString(builder);
		builder.append(':');
		right.toString(builder);
		builder.append(')');
	}
}

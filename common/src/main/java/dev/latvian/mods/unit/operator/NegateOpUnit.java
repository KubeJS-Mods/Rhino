package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.BooleanUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class NegateOpUnit extends Unit {
	public Unit unit;

	@Override
	public double get(UnitVariables variables) {
		return -unit.get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return -unit.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return !unit.getBoolean(variables);
	}

	@Override
	public Unit optimize() {
		unit = unit.optimize();

		if (unit instanceof NegateOpUnit u) {
			return u.unit;
		} else if (unit instanceof FixedNumberUnit u) {
			return FixedNumberUnit.ofFixed(-u.value);
		} else if (unit instanceof BooleanUnit u) {
			return BooleanUnit.of(!u.value);
		}

		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('-');
		unit.toString(builder);
	}
}

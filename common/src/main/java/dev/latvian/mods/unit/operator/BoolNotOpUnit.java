package dev.latvian.mods.unit.operator;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class BoolNotOpUnit extends UnaryOpUnit {
	@Override
	public double get(UnitVariables variables) {
		return getBoolean(variables) ? 1.0D : 0.0D;
	}

	@Override
	public int getInt(UnitVariables variables) {
		return getBoolean(variables) ? 1 : 0;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return !unit.getBoolean(variables);
	}

	@Override
	public Unit optimize() {
		unit = unit.optimize();
		return this;
	}
}

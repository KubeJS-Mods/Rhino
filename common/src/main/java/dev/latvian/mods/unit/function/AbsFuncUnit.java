package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class AbsFuncUnit extends FuncUnit {
	public AbsFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.abs(args[0].get(variables));
	}
}

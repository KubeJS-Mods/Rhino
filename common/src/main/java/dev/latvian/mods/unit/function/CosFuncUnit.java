package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class CosFuncUnit extends FuncUnit {
	public CosFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.cos(args[0].get(variables));
	}
}

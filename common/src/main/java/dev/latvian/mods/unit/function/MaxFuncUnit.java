package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class MaxFuncUnit extends FuncUnit {
	public MaxFuncUnit() {
		super(2);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.max(args[0].get(variables), args[1].get(variables));
	}
}

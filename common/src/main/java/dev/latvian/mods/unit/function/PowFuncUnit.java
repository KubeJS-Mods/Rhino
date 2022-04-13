package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class PowFuncUnit extends FuncUnit {
	public PowFuncUnit() {
		super(2);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.pow(args[0].get(variables), args[1].get(variables));
	}
}

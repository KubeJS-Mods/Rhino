package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class MinFuncUnit extends FuncUnit {
	public MinFuncUnit() {
		super(2);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.min(args[0].get(variables), args[1].get(variables));
	}
}

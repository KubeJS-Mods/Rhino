package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class SinFuncUnit extends FuncUnit {
	public SinFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.sin(args[0].get(variables));
	}
}

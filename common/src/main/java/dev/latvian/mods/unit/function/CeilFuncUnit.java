package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class CeilFuncUnit extends FuncUnit {
	public CeilFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.ceil(args[0].get(variables));
	}
}

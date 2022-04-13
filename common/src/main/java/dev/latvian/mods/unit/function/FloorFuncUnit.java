package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class FloorFuncUnit extends FuncUnit {
	public FloorFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.floor(args[0].get(variables));
	}
}

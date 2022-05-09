package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class DegFuncUnit extends FuncUnit {
	public DegFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.toDegrees(args[0].get(variables));
	}
}

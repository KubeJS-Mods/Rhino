package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class Log10FuncUnit extends FuncUnit {
	public Log10FuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return Math.log10(args[0].get(variables));
	}
}

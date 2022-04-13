package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class BoolFuncUnit extends FuncUnit {
	public BoolFuncUnit() {
		super(1);
	}

	@Override
	public double get(UnitVariables variables) {
		return getBoolean(variables) ? 1D : 0D;
	}

	@Override
	public float getFloat(UnitVariables variables) {
		return getBoolean(variables) ? 1F : 0F;
	}

	@Override
	public int getInt(UnitVariables variables) {
		return getBoolean(variables) ? 1 : 0;
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return args[0].getBoolean(variables);
	}
}

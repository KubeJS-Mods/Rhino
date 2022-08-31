package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class MapFuncUnit extends FuncUnit {
	public static double map(double value, double min1, double max1, double min2, double max2) {
		return LerpFuncUnit.lerp(min2, max2, (value - min1) / (max1 - min1));
	}

	public static final FunctionFactory FACTORY = FunctionFactory.of("map", 5, MapFuncUnit::new);
	public final Unit value;
	public final Unit min1;
	public final Unit max1;
	public final Unit min2;
	public final Unit max2;

	public MapFuncUnit(Unit[] args) {
		super(FACTORY);
		value = args[0];
		min1 = args[1];
		max1 = args[2];
		min2 = args[3];
		max2 = args[4];
	}

	@Override
	public double get(UnitVariables variables) {
		return map(value.get(variables), min1.get(variables), max1.get(variables), min2.get(variables), max2.get(variables));
	}


}

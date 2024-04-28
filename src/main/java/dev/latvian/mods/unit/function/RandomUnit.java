package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

import java.util.Random;

public class RandomUnit extends FuncUnit {
	public static final Random RANDOM = new Random();

	private RandomUnit() {
		super(FACTORY);
	}

	public static final FunctionFactory FACTORY = FunctionFactory.of0("random", RandomUnit::new);

	@Override
	public double get(UnitVariables variables) {
		return RANDOM.nextDouble();
	}


}
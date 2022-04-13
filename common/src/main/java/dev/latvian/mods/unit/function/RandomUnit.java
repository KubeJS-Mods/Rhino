package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

import java.util.Random;

public class RandomUnit extends FuncUnit {
	private static final RandomUnit INSTANCE = new RandomUnit();
	public static final Random RANDOM = new Random();

	public static RandomUnit getInstance() {
		return INSTANCE;
	}

	private RandomUnit() {
		super(0);
	}

	@Override
	public double get(UnitVariables variables) {
		return RANDOM.nextDouble();
	}
}
package dev.latvian.mods.rhino.util.unit;

import java.util.Random;

public class RandomUnit extends Func0Unit {
	public static final RandomUnit INSTANCE = new RandomUnit();
	public static final Random RANDOM = new Random();

	@Override
	public String getFuncName() {
		return "random";
	}

	@Override
	public float get() {
		return RANDOM.nextFloat();
	}
}
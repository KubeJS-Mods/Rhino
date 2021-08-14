package dev.latvian.mods.rhino.util.unit;

import java.util.Random;

public class RandomUnit implements Unit {
	public static final RandomUnit INSTANCE = new RandomUnit();
	public static final Random RANDOM = new Random();

	@Override
	public float get() {
		return RANDOM.nextFloat();
	}

	@Override
	public String toString() {
		return "random()";
	}
}
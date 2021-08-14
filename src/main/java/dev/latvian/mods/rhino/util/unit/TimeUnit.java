package dev.latvian.mods.rhino.util.unit;

import java.util.Random;

public class TimeUnit implements Unit {
	public static final TimeUnit INSTANCE = new TimeUnit();
	public static final Random RANDOM = new Random();

	@Override
	public float get() {
		return (float) (System.nanoTime() / 1000000L) / 1000F;
	}

	@Override
	public String toString() {
		return "time()";
	}
}
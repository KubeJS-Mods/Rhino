package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class TimeUnit extends FuncUnit {
	private static final TimeUnit INSTANCE = new TimeUnit();

	public static double time() {
		return System.nanoTime() / 1_000_000_000D;
	}

	public static TimeUnit getInstance() {
		return INSTANCE;
	}

	private TimeUnit() {
		super(0);
	}

	@Override
	public double get(UnitVariables variables) {
		return time();
	}
}
package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class RoundedTimeUnit extends FuncUnit {
	private static final RoundedTimeUnit INSTANCE = new RoundedTimeUnit();

	public static long time() {
		return Math.round(System.nanoTime() / 1_000_000_000D);
	}

	public static RoundedTimeUnit getInstance() {
		return INSTANCE;
	}

	private RoundedTimeUnit() {
		super(0);
	}

	@Override
	public double get(UnitVariables variables) {
		return time();
	}
}
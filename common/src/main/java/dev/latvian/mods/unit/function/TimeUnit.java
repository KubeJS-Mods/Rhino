package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.UnitVariables;

public class TimeUnit extends FuncUnit {
	public static final FunctionFactory FACTORY = FunctionFactory.of0("time", TimeUnit::new);

	public static double time() {
		return System.currentTimeMillis() / 1000D;
	}

	private TimeUnit() {
		super(FACTORY);
	}

	@Override
	public double get(UnitVariables variables) {
		return time();
	}
}
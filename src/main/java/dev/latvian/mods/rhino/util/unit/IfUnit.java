package dev.latvian.mods.rhino.util.unit;

public class IfUnit implements Unit {
	private final Unit statement;
	private final Unit trueUnit;
	private final Unit falseUnit;

	public IfUnit(Unit s, Unit t, Unit f) {
		statement = s;
		trueUnit = t;
		falseUnit = f;
	}

	@Override
	public float get() {
		return statement.getAsBoolean() ? trueUnit.get() : falseUnit.get();
	}

	@Override
	public int getAsInt() {
		return statement.getAsBoolean() ? trueUnit.getAsInt() : falseUnit.getAsInt();
	}

	@Override
	public boolean getAsBoolean() {
		return statement.getAsBoolean();
	}

	@Override
	public String toString() {
		return "if(" + statement + ", " + trueUnit + ", " + falseUnit + ")";
	}
}

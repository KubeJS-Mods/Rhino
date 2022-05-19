package dev.latvian.mods.unit;

public final class MutableNumberUnit extends Unit {
	public double value;

	public MutableNumberUnit(double value) {
		this.value = value;
	}

	public void set(double value) {
		this.value = value;
	}

	@Override
	public double get(UnitVariables variables) {
		return value;
	}

	@Override
	public void toString(StringBuilder builder) {
		long r = Math.round(value);

		if (Math.abs(r - value) < 0.00001D) {
			builder.append(r);
		} else {
			builder.append(value);
		}
	}
}

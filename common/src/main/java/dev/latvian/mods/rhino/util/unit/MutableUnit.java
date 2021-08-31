package dev.latvian.mods.rhino.util.unit;

public class MutableUnit extends Unit {
	private float value;

	public MutableUnit(float v) {
		value = v;
	}

	public void set(float v) {
		value = v;
	}

	@Override
	public float get() {
		return value;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(value);
	}
}

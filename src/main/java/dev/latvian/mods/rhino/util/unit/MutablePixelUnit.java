package dev.latvian.mods.rhino.util.unit;

public class MutablePixelUnit implements Unit {
	private float value;

	public MutablePixelUnit(float v) {
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
	public String toString() {
		return String.valueOf(value);
	}
}

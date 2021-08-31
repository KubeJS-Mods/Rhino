package dev.latvian.mods.rhino.util.unit;

public class TimeUnit extends Func0Unit {
	public static final TimeUnit INSTANCE = new TimeUnit();

	@Override
	public String getFuncName() {
		return "time";
	}

	@Override
	public float get() {
		return (float) (System.nanoTime() / 1000000L) / 1000F;
	}

	@Override
	public int getAsInt() {
		return (int) (System.nanoTime() / 1000000000L);
	}
}
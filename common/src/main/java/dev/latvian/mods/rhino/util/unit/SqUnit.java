package dev.latvian.mods.rhino.util.unit;

public class SqUnit extends Func1Unit {
	public SqUnit(Unit u) {
		super(u);
	}

	@Override
	public String getFuncName() {
		return "sq";
	}

	@Override
	public float get() {
		return unit.get() * unit.get();
	}

	@Override
	public int getAsInt() {
		return unit.getAsInt() * unit.getAsInt();
	}
}
package dev.latvian.mods.rhino.util.unit;

public class ConstantUnit extends FixedUnit {
	public final String name;

	public ConstantUnit(String n, float f) {
		super(f);
		name = n;
	}

	@Override
	public void append(StringBuilder sb) {
		sb.append(name);
	}
}
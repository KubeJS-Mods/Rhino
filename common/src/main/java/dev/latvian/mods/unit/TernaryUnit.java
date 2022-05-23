package dev.latvian.mods.unit;

public class TernaryUnit extends Unit {
	public final Unit cond, left, right;

	public TernaryUnit(Unit cond, Unit left, Unit right) {
		this.cond = cond;
		this.left = left;
		this.right = right;
	}

	@Override
	public double get(UnitVariables variables) {
		return cond.getBoolean(variables) ? left.get(variables) : right.get(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return cond.getBoolean(variables) ? left.getInt(variables) : right.getInt(variables);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return cond.getBoolean(variables) ? left.getBoolean(variables) : right.getBoolean(variables);
	}

	@Override
	public void toString(StringBuilder builder) {
		builder.append('(');
		cond.toString(builder);
		builder.append(" ? ");
		left.toString(builder);
		builder.append(" : ");
		right.toString(builder);
		builder.append(')');
	}
}

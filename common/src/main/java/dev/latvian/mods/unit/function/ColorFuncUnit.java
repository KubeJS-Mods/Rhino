package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;
import dev.latvian.mods.unit.token.UnitInterpretException;

public class ColorFuncUnit extends FuncUnit {
	private static int c(UnitVariables variables, Unit u) {
		return (int) (Math.min(Math.max(0D, u.get(variables) * 255D), 255D));
	}

	public static Unit colorOf(FunctionFactory factory, Unit[] units) {
		if (units.length == 1 && units[0] instanceof FixedColorUnit) {
			return units[0];
		}

		ColorFuncUnit c = new ColorFuncUnit();
		c.factory = factory;
		c.args[3] = FixedNumberUnit.ONE;

		if (units.length == 3 || units.length == 4) {
			System.arraycopy(units, 0, c.args, 0, units.length);
		} else if (units.length == 2) {
			if (units[0] instanceof FixedColorUnit u) {
				if (units[1].isFixed()) {
					return u.withAlpha(units[1].get(EmptyVariableSet.INSTANCE));
				} else {
					c.args[0] = FixedNumberUnit.of(((u.color >> 16) & 0xFF) / 255D);
					c.args[1] = FixedNumberUnit.of(((u.color >> 8) & 0xFF) / 255D);
					c.args[2] = FixedNumberUnit.of(((u.color >> 0) & 0xFF) / 255D);
					c.args[3] = units[1];
				}
			} else {
				c.args[0] = units[0];
				c.args[1] = units[0];
				c.args[2] = units[0];
				c.args[3] = units[1];
			}
		} else if (units.length == 1) {
			c.args[0] = units[0];
			c.args[1] = units[0];
			c.args[2] = units[0];
		} else {
			throw new UnitInterpretException("Invalid number of arguments for function '" + factory.name() + "'. Expected 1 to 4 but got " + units.length);
		}

		if (c.args[0].isFixed() && c.args[1].isFixed() && c.args[2].isFixed() && c.args[3].isFixed()) {
			return new FixedColorUnit(c.getInt(EmptyVariableSet.INSTANCE), true);
		}

		return c;
	}

	private ColorFuncUnit() {
		super(4);
	}

	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return (c(variables, args[0]) << 16) | (c(variables, args[1]) << 8) | c(variables, args[2]) | (c(variables, args[3]) << 24);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return args[3].getBoolean(variables);
	}
}

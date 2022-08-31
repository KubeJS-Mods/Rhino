package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.FixedColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitVariables;

public class RgbFuncUnit extends FuncUnit {
	public static final FunctionFactory FACTORY = FunctionFactory.of("rgb", 1, 4, args -> {
		if (args.length == 1 && args[0] instanceof FixedColorUnit) {
			return args[0];
		}

		RgbFuncUnit c = new RgbFuncUnit();
		c.a = FixedNumberUnit.ONE;

		if (args.length == 3 || args.length == 4) {
			c.r = args[0];
			c.g = args[1];
			c.b = args[2];

			if (args.length == 4) {
				c.a = args[3];
			}
		} else if (args.length == 2) {
			if (args[0] instanceof FixedColorUnit u) {
				if (args[1].isFixed()) {
					return u.withAlpha(args[1]);
				} else {
					c.r = FixedNumberUnit.of(((u.color >> 16) & 0xFF) / 255D);
					c.g = FixedNumberUnit.of(((u.color >> 8) & 0xFF) / 255D);
					c.b = FixedNumberUnit.of(((u.color >> 0) & 0xFF) / 255D);
					c.a = args[1];
				}
			} else {
				c.r = c.g = c.b = args[0];
				c.a = args[1];
			}
		} else if (args.length == 1) {
			c.r = c.g = c.b = args[0];
		}

		if (c.r.isFixed() && c.g.isFixed() && c.b.isFixed() && c.a.isFixed()) {
			return FixedColorUnit.of(c.getInt(EmptyVariableSet.INSTANCE), true);
		}

		return c;
	});

	private static int c(UnitVariables variables, Unit u) {
		return (int) (Math.min(Math.max(0D, u.get(variables) * 255D), 255D));
	}

	public Unit r, g, b, a;

	private RgbFuncUnit() {
		super(FACTORY);
	}

	@Override
	protected Unit[] getArguments() {
		if (a == FixedNumberUnit.ONE) {
			return new Unit[]{r, g, b};
		}

		return new Unit[]{r, g, b, a};
	}

	@Override
	public double get(UnitVariables variables) {
		return getInt(variables);
	}

	@Override
	public int getInt(UnitVariables variables) {
		return (c(variables, r) << 16) | (c(variables, g) << 8) | c(variables, b) | (c(variables, a) << 24);
	}

	@Override
	public boolean getBoolean(UnitVariables variables) {
		return a.getBoolean(variables);
	}
}

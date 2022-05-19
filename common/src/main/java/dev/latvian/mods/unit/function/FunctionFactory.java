package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitInterpretException;

public record FunctionFactory(String name, FuncSupplier supplier) {
	@FunctionalInterface
	public interface FuncSupplier {
		Unit create(FunctionFactory factory, Unit[] args);
	}

	@FunctionalInterface
	public interface SimpleFuncSupplier extends FuncSupplier {
		Unit create();

		@Override
		default Unit create(FunctionFactory factory, Unit[] args) {
			var unit = create();

			if (unit instanceof FuncUnit f) {
				f.factory = factory;

				if (f.args.length != args.length) {
					throw new UnitInterpretException("Invalid number of arguments for function '" + factory.name + "'. Expected " + f.args.length + " but got " + args.length);
				}

				System.arraycopy(args, 0, f.args, 0, args.length);
			}

			return unit;
		}
	}
}

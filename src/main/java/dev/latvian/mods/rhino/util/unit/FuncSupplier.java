package dev.latvian.mods.rhino.util.unit;

import java.util.List;

@FunctionalInterface
public interface FuncSupplier {
	Unit create(List<Unit> args);

	@FunctionalInterface
	interface Func1 extends FuncSupplier {
		Unit create1(Unit with);

		@Override
		default Unit create(List<Unit> args) {
			return create1(args.get(0));
		}
	}

	@FunctionalInterface
	interface Func2 extends FuncSupplier {
		Unit create2(Unit unit, Unit with);

		@Override
		default Unit create(List<Unit> args) {
			return create2(args.get(0), args.get(1));
		}
	}
}

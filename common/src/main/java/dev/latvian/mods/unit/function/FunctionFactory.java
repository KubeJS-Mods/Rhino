package dev.latvian.mods.unit.function;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.token.UnitInterpretException;

import java.util.function.Supplier;

public record FunctionFactory(String name, int minArgs, int maxArgs, FuncSupplier supplier) {
	@FunctionalInterface
	public interface FuncSupplier {
		Unit create(Unit[] args);
	}

	public static FunctionFactory of(String name, int minArgs, int maxArgs, FuncSupplier supplier) {
		return new FunctionFactory(name, minArgs, maxArgs, supplier);
	}

	public static FunctionFactory of(String name, int args, FuncSupplier supplier) {
		return of(name, args, args, supplier);
	}

	public static final class Arg0 implements FuncSupplier {
		private final Supplier<Unit> unit;
		private Unit cachedUnit;

		public Arg0(Supplier<Unit> unit) {
			this.unit = unit;
		}

		@Override
		public Unit create(Unit[] args) {
			if (cachedUnit == null) {
				cachedUnit = unit.get();
			}

			return cachedUnit;
		}
	}

	public static FunctionFactory of0(String name, Supplier<Unit> supplier) {
		return of(name, 0, new Arg0(supplier));
	}

	@FunctionalInterface
	public interface Arg1 extends FuncSupplier {
		Unit createArg(Unit a);

		@Override
		default Unit create(Unit[] args) {
			return createArg(args[0]);
		}
	}

	public static FunctionFactory of1(String name, Arg1 supplier) {
		return of(name, 1, supplier);
	}

	@FunctionalInterface
	public interface Arg2 extends FuncSupplier {
		Unit createArg(Unit a, Unit b);

		@Override
		default Unit create(Unit[] args) {
			return createArg(args[0], args[1]);
		}
	}

	public static FunctionFactory of2(String name, Arg2 supplier) {
		return of(name, 2, supplier);
	}

	@FunctionalInterface
	public interface Arg3 extends FuncSupplier {
		Unit createArg(Unit a, Unit b, Unit c);

		@Override
		default Unit create(Unit[] args) {
			return createArg(args[0], args[1], args[2]);
		}
	}

	public static FunctionFactory of3(String name, Arg3 supplier) {
		return of(name, 3, supplier);
	}

	public Unit create(Unit[] args) {
		if (args.length < minArgs || args.length > maxArgs) {
			throw new UnitInterpretException("Invalid number of arguments for function '" + name + "'. Expected " + (minArgs == maxArgs ? String.valueOf(minArgs) : (minArgs + "-" + maxArgs)) + " but got " + args.length);
		}

		return supplier.create(args);
	}
}

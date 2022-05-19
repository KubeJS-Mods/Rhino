package dev.latvian.mods.unit;

import dev.latvian.mods.unit.function.AbsFuncUnit;
import dev.latvian.mods.unit.function.Atan2FuncUnit;
import dev.latvian.mods.unit.function.AtanFuncUnit;
import dev.latvian.mods.unit.function.BoolFuncUnit;
import dev.latvian.mods.unit.function.CeilFuncUnit;
import dev.latvian.mods.unit.function.CosFuncUnit;
import dev.latvian.mods.unit.function.DegFuncUnit;
import dev.latvian.mods.unit.function.FloorFuncUnit;
import dev.latvian.mods.unit.function.FuncUnit;
import dev.latvian.mods.unit.function.FunctionFactory;
import dev.latvian.mods.unit.function.IfFuncUnit;
import dev.latvian.mods.unit.function.Log10FuncUnit;
import dev.latvian.mods.unit.function.Log1pFuncUnit;
import dev.latvian.mods.unit.function.LogFuncUnit;
import dev.latvian.mods.unit.function.MaxFuncUnit;
import dev.latvian.mods.unit.function.MinFuncUnit;
import dev.latvian.mods.unit.function.PowFuncUnit;
import dev.latvian.mods.unit.function.RadFuncUnit;
import dev.latvian.mods.unit.function.RandomUnit;
import dev.latvian.mods.unit.function.RoundedTimeUnit;
import dev.latvian.mods.unit.function.SinFuncUnit;
import dev.latvian.mods.unit.function.SqFuncUnit;
import dev.latvian.mods.unit.function.SqrtFuncUnit;
import dev.latvian.mods.unit.function.TanFuncUnit;
import dev.latvian.mods.unit.function.TimeUnit;
import dev.latvian.mods.unit.token.UnitTokenStream;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class UnitContext {
	public static final UnitContext DEFAULT = new UnitContext();

	static {
		DEFAULT.addFunction("time", TimeUnit::getInstance);
		DEFAULT.addFunction("roundedTime", RoundedTimeUnit::getInstance);
		DEFAULT.addFunction("random", RandomUnit::getInstance);
		DEFAULT.addFunction("if", IfFuncUnit::new);
		DEFAULT.addFunction("min", MinFuncUnit::new);
		DEFAULT.addFunction("max", MaxFuncUnit::new);
		DEFAULT.addFunction("pow", PowFuncUnit::new);
		DEFAULT.addFunction("abs", AbsFuncUnit::new);
		DEFAULT.addFunction("sin", SinFuncUnit::new);
		DEFAULT.addFunction("cos", CosFuncUnit::new);
		DEFAULT.addFunction("tan", TanFuncUnit::new);
		DEFAULT.addFunction("deg", DegFuncUnit::new);
		DEFAULT.addFunction("rad", RadFuncUnit::new);
		DEFAULT.addFunction("atan", AtanFuncUnit::new);
		DEFAULT.addFunction("atan2", Atan2FuncUnit::new);
		DEFAULT.addFunction("log", LogFuncUnit::new);
		DEFAULT.addFunction("log10", Log10FuncUnit::new);
		DEFAULT.addFunction("log1p", Log1pFuncUnit::new);
		DEFAULT.addFunction("sqrt", SqrtFuncUnit::new);
		DEFAULT.addFunction("sq", SqFuncUnit::new);
		DEFAULT.addFunction("floor", FloorFuncUnit::new);
		DEFAULT.addFunction("ceil", CeilFuncUnit::new);
		DEFAULT.addFunction("bool", BoolFuncUnit::new);
		// addFunc("color", a -> new ColorUnit(a.get(0), a.get(1), a.get(2), a.size() >= 4 ? a.get(3) : null));
	}

	private final Map<String, FunctionFactory> functions = new HashMap<>();
	private final Map<String, Unit> cache = new HashMap<>();
	private int debug = -1;

	public void addFunction(String name, Supplier<FuncUnit> func) {
		FunctionFactory factory = new FunctionFactory(name.toLowerCase(), func);
		functions.put(factory.name(), factory);
	}

	@Nullable
	public FuncUnit getFunction(String name) {
		FunctionFactory func = functions.get(name.toLowerCase());

		if (func == null) {
			return null;
		}

		FuncUnit unit = func.factory().get();

		if (unit != null) {
			unit.factory = func;
		}

		return unit;
	}

	public UnitContext sub() {
		UnitContext ctx = new UnitContext();
		ctx.functions.putAll(functions);
		ctx.debug = debug;
		return ctx;
	}

	public UnitTokenStream createStream(String input) {
		return new UnitTokenStream(this, input);
	}

	public Unit parse(String input) {
		Unit u = cache.get(input);

		if (u == null) {
			u = createStream(input).getUnit();
			u = u.optimize();
			cache.put(input, u);
		}

		return u;
	}

	public boolean isDebug() {
		return debug >= 0;
	}

	public void pushDebug() {
		debug++;
	}

	public void popDebug() {
		debug--;
	}

	public void debugInfo(String s) {
		if (debug >= 0) {
			if (debug >= 2) {
				System.out.println("  ".repeat(debug - 1) + s);
			} else {
				System.out.println(s);
			}
		}
	}

	public void debugInfo(String s, Collection<?> values) {
		debugInfo(s + ": " + values.stream().map(Object::toString).collect(Collectors.joining("  ")));
	}
}

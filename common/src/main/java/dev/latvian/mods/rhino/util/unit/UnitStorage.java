package dev.latvian.mods.rhino.util.unit;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitStorage {
	private final Map<String, Unit> variables;
	private final Map<String, ConstantUnit> constants;
	private final Map<String, OpSupplier> operations;
	private final Map<String, FuncSupplier> functions;
	private long variableVersion = 0L;

	public UnitStorage() {
		variables = new HashMap<>();
		constants = new HashMap<>();
		operations = new HashMap<>();
		functions = new HashMap<>();

		addConstant("PI", (float) Math.PI);
		addConstant("E", (float) Math.E);
		addConstant("true", 1F);
		addConstant("false", 0F);

		addOp("+", Unit::add);
		addOp("-", Unit::sub);
		addOp("*", Unit::mul);
		addOp("/", Unit::div);
		addOp("%", Unit::mod);
		addOp("**", Unit::pow);
		addOp("<<", Unit::shiftLeft);
		addOp(">>", Unit::shiftRight);
		addOp("&", Unit::and);
		addOp("&&", Unit::and);
		addOp("|", Unit::or);
		addOp("||", Unit::or);
		addOp("^", Unit::xor);
		addOp("==", Unit::eq);
		addOp("!=", Unit::neq);
		addOp("~=", Unit::neq);
		addOp(">", Unit::gt);
		addOp("<", Unit::lt);
		addOp(">=", Unit::gte);
		addOp("<=", Unit::lte);

		addFunc("random", RandomUnit.INSTANCE);
		addFunc("time", TimeUnit.INSTANCE);
		addFunc2("min", Unit::min);
		addFunc2("max", Unit::max);
		addFunc2("pow", Unit::pow);
		addFunc1("abs", Unit::abs);
		addFunc1("sin", Unit::sin);
		addFunc1("cos", Unit::cos);
		addFunc1("tan", Unit::tan);
		addFunc1("deg", Unit::deg);
		addFunc1("rad", Unit::rad);
		addFunc1("atan", Unit::atan);
		addFunc2("atan2", Unit::atan2);
		addFunc1("log", Unit::log);
		addFunc1("log10", Unit::log10);
		addFunc1("log1p", Unit::log1p);
		addFunc1("sqrt", Unit::sqrt);
		addFunc1("sq", Unit::sq);
		addFunc1("floor", Unit::floor);
		addFunc1("ceil", Unit::ceil);
		addFunc1("bool", Unit::toBool);
		addFunc("if", a -> new IfUnit(a.get(0), a.get(1), a.get(2)));
		addFunc("color", a -> new ColorUnit(a.get(0), a.get(1), a.get(2), a.size() >= 4 ? a.get(3) : null));
	}

	public void clearVariables() {
		variables.clear();
	}

	public void setVariable(String key, Unit unit) {
		variables.put(key, unit);
		variableVersion++;
	}

	@Nullable
	public Unit getVariable(String key) {
		return variables.get(key);
	}

	public long getVariableVersion() {
		return variableVersion;
	}

	public void addConstant(String name, float val) {
		constants.put(name, new ConstantUnit(name, val));
	}

	public void addOp(String name, OpSupplier op) {
		operations.put(name, op);
	}

	public void addFunc(String name, FuncSupplier func) {
		functions.put(name, func);
	}

	public void addFunc1(String name, FuncSupplier.Func1 func) {
		addFunc(name, func);
	}

	public void addFunc2(String name, FuncSupplier.Func2 func) {
		addFunc(name, func);
	}

	@Nullable
	public ConstantUnit getConstant(String name) {
		return constants.get(name);
	}

	@Nullable
	public Unit createOp(String name, Unit unit, Unit with) {
		OpSupplier op = operations.get(name);
		return op == null ? null : op.create(unit, with);
	}

	@Nullable
	public Unit createFunc(String name, List<Unit> args) {
		FuncSupplier func = functions.get(name);
		return func == null ? null : func.create(args);
	}

	public Unit parse(String string) {
		return new UnitParser(string, this).parse();
	}
}

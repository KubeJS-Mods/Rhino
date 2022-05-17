package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.SymbolUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.AddOpUnit;
import dev.latvian.mods.unit.operator.AndOpUnit;
import dev.latvian.mods.unit.operator.BitAndOpUnit;
import dev.latvian.mods.unit.operator.BitNotOpUnit;
import dev.latvian.mods.unit.operator.BitOrOpUnit;
import dev.latvian.mods.unit.operator.BoolNotOpUnit;
import dev.latvian.mods.unit.operator.DivOpUnit;
import dev.latvian.mods.unit.operator.EqOpUnit;
import dev.latvian.mods.unit.operator.GtOpUnit;
import dev.latvian.mods.unit.operator.GteOpUnit;
import dev.latvian.mods.unit.operator.LshOpUnit;
import dev.latvian.mods.unit.operator.LtOpUnit;
import dev.latvian.mods.unit.operator.LteOpUnit;
import dev.latvian.mods.unit.operator.ModOpUnit;
import dev.latvian.mods.unit.operator.MulOpUnit;
import dev.latvian.mods.unit.operator.NegateOpUnit;
import dev.latvian.mods.unit.operator.NeqOpUnit;
import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.operator.OrOpUnit;
import dev.latvian.mods.unit.operator.PowOpUnit;
import dev.latvian.mods.unit.operator.RshOpUnit;
import dev.latvian.mods.unit.operator.SkipOpUnit;
import dev.latvian.mods.unit.operator.SubOpUnit;
import dev.latvian.mods.unit.operator.TernaryOpUnit;
import dev.latvian.mods.unit.operator.XorOpUnit;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum UnitSymbol {
	// Misc
	COMMA(","),
	LP("("),
	RP(")"),
	HASH("#"),
	HOOK("?", TernaryOpUnit::new),
	COLON(":", SkipOpUnit::new),
	NEGATE("-", NegateOpUnit::new),
	// Operators
	ADD("+", AddOpUnit::new),
	SUB("-", SubOpUnit::new),
	MUL("*", MulOpUnit::new),
	DIV("/", DivOpUnit::new),
	MOD("%", ModOpUnit::new),
	POW("**", PowOpUnit::new),
	// Int Operators
	LSH("<<", LshOpUnit::new),
	RSH(">>", RshOpUnit::new),
	BIT_AND("&", BitAndOpUnit::new),
	BIT_OR("|", BitOrOpUnit::new),
	XOR("^", XorOpUnit::new),
	BIT_NOT("~", BitNotOpUnit::new),
	// Conditions
	EQ("==", EqOpUnit::new),
	NEQ("!=", NeqOpUnit::new),
	LT("<", LtOpUnit::new),
	GT(">", GtOpUnit::new),
	LTE("<=", LteOpUnit::new),
	GTE(">=", GteOpUnit::new),
	AND("&&", AndOpUnit::new),
	OR("||", OrOpUnit::new),
	BOOL_NOT("!", BoolNotOpUnit::new),

	;

	public final String symbol;
	public final OperatorFactory op;
	public final SymbolUnit unit;

	UnitSymbol(String s, Supplier<OpUnit> opUnit) {
		symbol = s;
		op = opUnit == null ? null : new OperatorFactory(this, opUnit);
		unit = new SymbolUnit(this);
	}

	UnitSymbol(String s) {
		this(s, null);
	}

	@Override
	public String toString() {
		return symbol;
	}

	@Nullable
	@SuppressWarnings("ConditionalExpressionWithIdenticalBranches")
	public static UnitSymbol read(char first, CharStream stream) {
		return switch (first) {
			case ',' -> COMMA;
			case '(' -> LP;
			case ')' -> RP;
			case '#' -> HASH;
			case '?' -> HOOK;
			case ':' -> COLON;
			case '+' -> ADD;
			case '-' -> SUB;
			case '*' -> stream.nextIf('*') ? POW : MUL;
			case '/' -> DIV;
			case '%' -> MOD;
			case '^' -> XOR;
			case '~' -> BIT_NOT;
			case '&' -> stream.nextIf('&') ? AND : BIT_AND;
			case '|' -> stream.nextIf('|') ? OR : BIT_OR;
			case '!' -> stream.nextIf('=') ? NEQ : BOOL_NOT;
			case '<' -> stream.nextIf('=') ? LTE : stream.nextIf('<') ? LSH : LT;
			case '>' -> stream.nextIf('=') ? GTE : stream.nextIf('>') ? RSH : GT;
			case '=' -> stream.nextIf('=') ? EQ : EQ; // Allow both == and =
			default -> null;
		};
	}

	public boolean is(Unit unit) {
		return unit instanceof SymbolUnit s && s.symbol == this;
	}
}

package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.operator.AddOpUnit;
import dev.latvian.mods.unit.operator.AndOpUnit;
import dev.latvian.mods.unit.operator.BitAndOpUnit;
import dev.latvian.mods.unit.operator.BitOrOpUnit;
import dev.latvian.mods.unit.operator.DivOpUnit;
import dev.latvian.mods.unit.operator.EqOpUnit;
import dev.latvian.mods.unit.operator.GtOpUnit;
import dev.latvian.mods.unit.operator.GteOpUnit;
import dev.latvian.mods.unit.operator.LshOpUnit;
import dev.latvian.mods.unit.operator.LtOpUnit;
import dev.latvian.mods.unit.operator.LteOpUnit;
import dev.latvian.mods.unit.operator.ModOpUnit;
import dev.latvian.mods.unit.operator.MulOpUnit;
import dev.latvian.mods.unit.operator.NeqOpUnit;
import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.operator.OrOpUnit;
import dev.latvian.mods.unit.operator.PowOpUnit;
import dev.latvian.mods.unit.operator.RshOpUnit;
import dev.latvian.mods.unit.operator.SubOpUnit;
import dev.latvian.mods.unit.operator.TernaryOpUnit;
import dev.latvian.mods.unit.operator.XorOpUnit;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum SymbolUnitToken implements UnitToken {
	// Misc
	COMMA(","),
	LP("("),
	RP(")"),
	HASH("#"),
	HOOK("?", TernaryOpUnit::new, 1),
	COLON(":"),
	NEGATE("-"),
	BOOL_NOT("!"),
	BIT_NOT("~"),
	SET("="),
	// Operators
	ADD("+", AddOpUnit::new, 4),
	SUB("-", SubOpUnit::new, 4),
	MUL("*", MulOpUnit::new, 3),
	DIV("/", DivOpUnit::new, 3),
	MOD("%", ModOpUnit::new, 3),
	POW("**", PowOpUnit::new, 2),
	// Int Operators
	LSH("<<", LshOpUnit::new, 4),
	RSH(">>", RshOpUnit::new, 4),
	BIT_AND("&", BitAndOpUnit::new, 4),
	BIT_OR("|", BitOrOpUnit::new, 4),
	XOR("^", XorOpUnit::new, 4),
	// Conditions
	EQ("==", EqOpUnit::new, 5),
	NEQ("!=", NeqOpUnit::new, 5),
	LT("<", LtOpUnit::new, 5),
	GT(">", GtOpUnit::new, 5),
	LTE("<=", LteOpUnit::new, 5),
	GTE(">=", GteOpUnit::new, 5),
	AND("&&", AndOpUnit::new, 5),
	OR("||", OrOpUnit::new, 5),

	;

	public final String symbol;
	private final Supplier<OpUnit> operatorUnit;
	public final int precedence;

	SymbolUnitToken(String s, Supplier<OpUnit> op, int p) {
		symbol = s;
		operatorUnit = op;
		precedence = p;
	}

	SymbolUnitToken(String s) {
		this(s, null, Integer.MAX_VALUE);
	}

	@Override
	public String toString() {
		return symbol;
	}

	@Nullable
	public static SymbolUnitToken read(char first, CharStream stream) {
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
			case '=' -> stream.nextIf('=') ? EQ : SET;
			default -> null;
		};
	}

	@Override
	public boolean shouldNegate() {
		return this != RP;
	}

	public boolean isOp() {
		return operatorUnit != null;
	}

	@Nullable
	public OpUnit createOpUnit() {
		if (operatorUnit == null) {
			return null;
		}

		OpUnit unit = operatorUnit.get();

		if (unit != null) {
			unit.symbol = this;
		}

		return unit;
	}
}

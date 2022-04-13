package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.AddOpUnit;
import dev.latvian.mods.unit.operator.AndOpUnit;
import dev.latvian.mods.unit.operator.BitAndOpUnit;
import dev.latvian.mods.unit.operator.BitOrOpUnit;
import dev.latvian.mods.unit.operator.DivOpUnit;
import dev.latvian.mods.unit.operator.EqOpUnit;
import dev.latvian.mods.unit.operator.GtOpUnit;
import dev.latvian.mods.unit.operator.GteOpUnit;
import dev.latvian.mods.unit.operator.LtOpUnit;
import dev.latvian.mods.unit.operator.LteOpUnit;
import dev.latvian.mods.unit.operator.ModOpUnit;
import dev.latvian.mods.unit.operator.MulOpUnit;
import dev.latvian.mods.unit.operator.NeqOpUnit;
import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.operator.OrOpUnit;
import dev.latvian.mods.unit.operator.PowOpUnit;
import dev.latvian.mods.unit.operator.SubOpUnit;
import dev.latvian.mods.unit.operator.XorOpUnit;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public enum SymbolUnitToken implements UnitToken {
	COMMA(","),
	LP("("),
	RP(")"),
	HOOK("?"),
	COLON(":"),
	NEGATE("-"),
	ADD("+", AddOpUnit::new, 2),
	SUB("-", SubOpUnit::new, 2),
	MUL("*", MulOpUnit::new, 3),
	POW("**", PowOpUnit::new, 4),
	DIV("/", DivOpUnit::new, 3),
	MOD("%", ModOpUnit::new, 2),
	SET("="),
	EQ("==", EqOpUnit::new, 1),
	NEQ("!=", NeqOpUnit::new, 1),
	LT("<", LtOpUnit::new, 1),
	GT(">", GtOpUnit::new, 1),
	LTE("<=", LteOpUnit::new, 1),
	GTE(">=", GteOpUnit::new, 1),
	LSH("<<"),
	RSH(">>"),
	AND("&&", AndOpUnit::new, 1),
	OR("||", OrOpUnit::new, 1),
	BIT_AND("&", BitAndOpUnit::new, 1),
	BIT_OR("|", BitOrOpUnit::new, 1),
	BIT_NOT("!"),
	XOR("^", XorOpUnit::new, 1),
	;

	public final String symbol;
	public final Supplier<OpUnit> operatorUnit;
	public final int precedence;

	SymbolUnitToken(String s, Supplier<OpUnit> op, int p) {
		symbol = s;
		operatorUnit = op;
		precedence = p;
	}

	SymbolUnitToken(String s) {
		this(s, null, 0);
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
			case '?' -> HOOK;
			case ':' -> COLON;
			case '+' -> ADD;
			case '-' -> SUB;
			case '*' -> stream.nextIf('*') ? POW : MUL;
			case '/' -> DIV;
			case '%' -> MOD;
			case '^' -> XOR;
			case '&' -> stream.nextIf('&') ? AND : BIT_AND;
			case '|' -> stream.nextIf('|') ? OR : BIT_OR;
			case '!' -> stream.nextIf('=') ? NEQ : BIT_NOT;
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

	@Override
	public Unit interpret(UnitTokenStream stream) {
		if (this == LP) {
			// do its thing
		}

		throw new IllegalStateException("Symbol '" + symbol + "' can't be interpreted!");
	}
}

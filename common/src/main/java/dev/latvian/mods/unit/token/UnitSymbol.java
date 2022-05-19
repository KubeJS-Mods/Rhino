package dev.latvian.mods.unit.token;

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
import dev.latvian.mods.unit.operator.OperatorFactory;
import dev.latvian.mods.unit.operator.OrOpUnit;
import dev.latvian.mods.unit.operator.PowOpUnit;
import dev.latvian.mods.unit.operator.RshOpUnit;
import dev.latvian.mods.unit.operator.SubOpUnit;
import dev.latvian.mods.unit.operator.UnaryOperatorFactory;
import dev.latvian.mods.unit.operator.XorOpUnit;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public enum UnitSymbol implements UnitToken {
	// Misc
	COMMA(","),
	LP("("),
	RP(")"),
	HASH("#"),
	HOOK("?"),
	COLON(":"),
	NEGATE("-", NegateOpUnit::new),
	// Operators
	ADD("+", 2, AddOpUnit::new),
	SUB("-", 2, SubOpUnit::new),
	MUL("*", 3, MulOpUnit::new),
	DIV("/", 3, DivOpUnit::new),
	MOD("%", 3, ModOpUnit::new),
	POW("**", 4, PowOpUnit::new),
	// Int Operators
	LSH("<<", 2, LshOpUnit::new),
	RSH(">>", 2, RshOpUnit::new),
	BIT_AND("&", 2, BitAndOpUnit::new),
	BIT_OR("|", 2, BitOrOpUnit::new),
	XOR("^", 2, XorOpUnit::new),
	BIT_NOT("~", BitNotOpUnit::new),
	// Conditions
	EQ("==", 1, EqOpUnit::new),
	NEQ("!=", 1, NeqOpUnit::new),
	LT("<", 1, LtOpUnit::new),
	GT(">", 1, GtOpUnit::new),
	LTE("<=", 1, LteOpUnit::new),
	GTE(">=", 1, GteOpUnit::new),
	AND("&&", 1, AndOpUnit::new),
	OR("||", 1, OrOpUnit::new),
	BOOL_NOT("!", BoolNotOpUnit::new),

	;

	public final String symbol;
	public final int precedence;
	public final OperatorFactory op;
	public final UnaryOperatorFactory unaryOp;

	UnitSymbol(String s) {
		symbol = s;
		precedence = 0;
		op = null;
		unaryOp = null;
	}

	UnitSymbol(String s, int p, OperatorFactory.OpSupplier opUnit) {
		symbol = s;
		precedence = p;
		op = new OperatorFactory(this, opUnit);
		unaryOp = null;
	}

	UnitSymbol(String s, UnaryOperatorFactory.OpSupplier unaryOpUnit) {
		symbol = s;
		precedence = 0;
		op = null;
		unaryOp = new UnaryOperatorFactory(this, unaryOpUnit);
	}

	@Override
	public String toString() {
		return symbol;
	}

	@Override
	public boolean shouldNegate() {
		return this != RP;
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

	public boolean is(UnitToken next) {
		return next == this;
	}

	@Override
	public void unstack(UnitTokenStream stream, Stack<UnitToken> stack) {
		if (op != null) {
			if (stack.size() < 2) {
				throw stream.parsingError("Not enough elements in stack!");
			}

			var right = stack.pop();
			var left = stack.pop();
			stack.push(new OpResultUnitToken(this, left, right));
		} else {
			throw stream.parsingError("Unexpected symbol '" + this + "'!");
		}
	}

	public final boolean hasHigherPrecedenceThan(UnitSymbol operator) {
		return operator.precedence <= precedence;
	}
}

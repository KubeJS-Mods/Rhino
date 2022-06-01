package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.operator.OperatorFactory;
import dev.latvian.mods.unit.operator.UnaryOperatorFactory;
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
	SEMICOLON(";"),
	POSITIVE("+", Unit::positive),
	NEGATE("-", Unit::negate),
	// Operators
	ADD("+", 2, Unit::add),
	SUB("-", 2, Unit::sub),
	MUL("*", 3, Unit::mul),
	DIV("/", 3, Unit::div),
	MOD("%", 3, Unit::mod),
	POW("**", 4, Unit::pow),
	// Int Operators
	LSH("<<", 2, Unit::lsh),
	RSH(">>", 2, Unit::rsh),
	BIT_AND("&", 2, Unit::bitAnd),
	BIT_OR("|", 2, Unit::bitOr),
	XOR("^", 2, Unit::xor),
	BIT_NOT("~", Unit::bitNot),
	// Conditions
	EQ("==", 1, Unit::eq),
	NEQ("!=", 1, Unit::neq),
	LT("<", 1, Unit::lt),
	GT(">", 1, Unit::gt),
	LTE("<=", 1, Unit::lte),
	GTE(">=", 1, Unit::gte),
	AND("&&", 1, Unit::and),
	OR("||", 1, Unit::or),
	BOOL_NOT("!", Unit::boolNot),
	// Mutators
	SET("=", 0, Unit::set),
	ADD_SET("+=", 0, Unit::addSet),
	SUB_SET("-=", 0, Unit::subSet),
	MUL_SET("*=", 0, Unit::mulSet),
	DIV_SET("/=", 0, Unit::divSet),
	MOD_SET("%=", 0, Unit::modSet),

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

	UnitSymbol(String s, int p, OperatorFactory opUnit) {
		symbol = s;
		precedence = p;
		op = opUnit;
		unaryOp = null;
	}

	UnitSymbol(String s, UnaryOperatorFactory unaryOpUnit) {
		symbol = s;
		precedence = 0;
		op = null;
		unaryOp = unaryOpUnit;
	}

	@Override
	public String toString() {
		return symbol;
	}

	@Override
	public boolean nextUnaryOperator() {
		return this != RP;
	}

	@Nullable
	public UnitSymbol getUnarySymbol() {
		return switch (this) {
			case ADD -> POSITIVE;
			case SUB -> NEGATE;
			default -> null;
		};
	}

	@Nullable
	public static UnitSymbol read(char first, CharStream stream) {
		return switch (first) {
			case ',' -> COMMA;
			case '(' -> LP;
			case ')' -> RP;
			case '#' -> HASH;
			case '?' -> HOOK;
			case ':' -> COLON;
			case ';' -> SEMICOLON;
			case '+' -> stream.nextIf('=') ? ADD_SET : ADD;
			case '-' -> stream.nextIf('=') ? SUB_SET : SUB;
			case '*' -> stream.nextIf('*') ? POW : stream.nextIf('=') ? MUL_SET : MUL;
			case '/' -> stream.nextIf('=') ? DIV_SET : DIV;
			case '%' -> stream.nextIf('=') ? MOD_SET : MOD;
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

	public boolean is(UnitToken next) {
		return next == this;
	}

	@Override
	public void unstack(Stack<UnitToken> stack) {
		if (op != null) {
			if (stack.size() < 2) {
				throw new UnitInterpretException("Not enough elements in stack!");
			}

			var right = stack.pop();
			var left = stack.pop();
			stack.push(new OpResultUnitToken(this, left, right));
		} else {
			throw new UnitInterpretException("Unexpected symbol '" + this + "'!");
		}
	}

	public final boolean hasHigherPrecedenceThan(UnitSymbol operator) {
		return operator.precedence <= precedence;
	}
}

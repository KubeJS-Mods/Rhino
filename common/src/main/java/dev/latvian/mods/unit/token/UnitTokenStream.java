package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.ColorUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;

import java.util.ArrayList;

public final class UnitTokenStream {
	private static boolean isHex(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
	}

	public final UnitContext context;

	public final String input;
	public final CharStream charStream;
	public final ArrayList<UnitToken> infix;
	public final ArrayList<Integer> inputStringPos;
	private int infixPos;
	public final Unit unit;

	public UnitTokenStream(UnitContext context, String input) {
		this.context = context;
		this.input = input;
		this.charStream = new CharStream(input.toCharArray());
		this.infix = new ArrayList<>();
		this.inputStringPos = new ArrayList<>();
		this.infixPos = -1;

		var current = new StringBuilder();

		while (true) {
			var c = charStream.next();

			if (c == 0) {
				break;
			}

			int cpos = charStream.position;
			var symbol = UnitSymbol.read(c, charStream);

			if (symbol == UnitSymbol.HASH) {
				if (isHex(charStream.peek(1)) && isHex(charStream.peek(2)) && isHex(charStream.peek(3)) && isHex(charStream.peek(4)) && isHex(charStream.peek(5)) && isHex(charStream.peek(6))) {
					var alpha = isHex(charStream.peek(7)) && isHex(charStream.peek(8));

					current.append('#');

					for (var i = 0; i < (alpha ? 8 : 6); i++) {
						current.append(charStream.next());
					}

					var color = Long.decode(current.toString()).intValue();
					current.setLength(0);

					inputStringPos.add(cpos);
					infix.add(new ColorUnit(color, alpha));
				} else {
					throw new IllegalStateException("Invalid color code @ " + charStream.position);
				}
			} else {
				if (symbol != null && current.length() > 0) {
					inputStringPos.add(cpos);
					infix.add(new StringUnitToken(current.toString()));
					current.setLength(0);
				}

				if (symbol == UnitSymbol.SUB && (infix.isEmpty() || infix.get(infix.size() - 1).shouldNegate())) {
					inputStringPos.add(cpos);
					infix.add(UnitSymbol.NEGATE);
				} else if (symbol != null) {
					inputStringPos.add(cpos);
					infix.add(symbol);
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			inputStringPos.add(charStream.position - current.length());
			infix.add(new StringUnitToken(current.toString()));
			current.setLength(0);
		}

		if (infix.size() == 1) {
			unit = infix.get(0).interpret(this);
			return;
		}

		if (context.isDebug()) {
			context.debugInfo("Infix", infix);
		}

		var unitToken = readFully();
		var rawUnit = unitToken.interpret(this);
		this.unit = rawUnit.optimize();
	}

	public Unit getUnit() {
		return unit;
	}

	@Override
	public String toString() {
		return infix.toString();
	}

	public UnitToken nextToken() {
		if (++infixPos >= infix.size()) {
			throw parsingError("EOL!");
		}

		return infix.get(infixPos);
	}

	public UnitToken peekToken() {
		if (infixPos + 1 >= infix.size()) {
			return null;
		}

		return infix.get(infixPos + 1);
	}

	public boolean ifNextToken(UnitToken token) {
		if (token.equals(peekToken())) {
			nextToken();
			return true;
		}

		return false;
	}

	public UnitToken readFully() {
		PostfixUnitToken postfix = new PostfixUnitToken(new ArrayList<>());

		if (ifNextToken(UnitSymbol.LP)) {
			postfix.infix().add(readFully());

			if (!ifNextToken(UnitSymbol.RP)) {
				throw parsingError("Expected ')', got '" + peekToken() + "'!");
			}
		} else {
			postfix.infix().add(readSingleToken());
		}

		while (peekToken() instanceof UnitSymbol symbol && symbol.op != null) {
			postfix.infix().add(nextToken());

			if (peekToken() == UnitSymbol.LP) {
				postfix.infix().add(readFully());
			} else {
				postfix.infix().add(readSingleToken());
			}
		}

		return postfix.infix().size() == 1 ? postfix.infix().get(0) : postfix;
	}

	public UnitToken readSingleToken() {
		UnitToken token = nextToken();

		if (token instanceof UnitSymbol symbol && symbol.unaryOp != null) {
			if (peekToken() == UnitSymbol.LP) {
				return new UnaryOpUnitToken(symbol, readFully());
			} else {
				return new UnaryOpUnitToken(symbol, readSingleToken());
			}
		}

		if (token instanceof StringUnitToken str) {
			if (ifNextToken(UnitSymbol.LP)) {
				FunctionUnitToken func = new FunctionUnitToken(str.name(), new ArrayList<>());

				while (true) {
					if (ifNextToken(UnitSymbol.RP)) {
						break;
					} else if (!ifNextToken(UnitSymbol.COMMA)) {
						func.args().add(readFully());
					}
				}

				return func;
			}

			return str;
		}

		throw parsingError("Unexpected token: " + token);
	}

	public IllegalStateException parsingError(String error) {
		return new IllegalStateException("Error parsing '" + input + "' @ " + (infixPos < 0 || infixPos >= inputStringPos.size() ? -1 : inputStringPos.get(infixPos)) + ": " + error);
	}
}

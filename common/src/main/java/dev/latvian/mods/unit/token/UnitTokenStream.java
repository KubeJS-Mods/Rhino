package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.ColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class UnitTokenStream {
	private static boolean isString(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$' || c == '.';
	}

	private static boolean isHex(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
	}

	public final UnitContext context;

	public final String input;
	public final CharStream charStream;
	public final LinkedList<UnitToken> tokens;
	public final InterpretableUnitToken root;

	public UnitTokenStream(UnitContext context, String input, CharStream charStream) {
		this.context = context;
		this.input = input;
		this.charStream = charStream;
		this.tokens = new LinkedList<>();

		var current = new StringBuilder();

		while (true) {
			var c = charStream.next();

			if (c == 0) {
				break;
			}

			var symbol = SymbolUnitToken.read(c, charStream);

			if (symbol == SymbolUnitToken.HASH) {
				if (isHex(charStream.peek(1)) && isHex(charStream.peek(2)) && isHex(charStream.peek(3)) && isHex(charStream.peek(4)) && isHex(charStream.peek(5)) && isHex(charStream.peek(6))) {
					var alpha = isHex(charStream.peek(7)) && isHex(charStream.peek(8));

					current.append('#');

					for (var i = 0; i < (alpha ? 8 : 6); i++) {
						current.append(charStream.next());
					}

					var color = Long.decode(current.toString()).intValue();
					current.setLength(0);

					tokens.add(new ColorUnit(color, alpha));
				} else {
					throw new IllegalStateException("Invalid color code @ " + charStream.position);
				}
			} else {
				if (symbol != null && current.length() > 0) {
					tokens.add(createTokenFromString(current.toString()));
					current.setLength(0);
				}

				if (symbol == SymbolUnitToken.SUB && (tokens.isEmpty() || tokens.getLast().shouldNegate())) {
					tokens.add(SymbolUnitToken.NEGATE);
				} else if (symbol != null) {
					tokens.add(symbol);
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			tokens.add(createTokenFromString(current.toString()));
			current.setLength(0);
		}

		this.root = readFully();
	}

	public Unit getUnit() {
		return root.interpret(context);
	}

	private static InterpretableUnitToken createTokenFromString(String input) {
		var constant = FixedNumberUnit.CONSTANTS.get(input);

		if (constant != null) {
			return constant;
		}

		try {
			return FixedNumberUnit.ofFixed(Double.parseDouble(input));
		} catch (Exception ex) {
			return new NameUnitToken(input);
		}
	}

	private InterpretableUnitToken readInterpretableToken() {
		var token = nextToken();

		InterpretableUnitToken result = null;

		if (token instanceof NameUnitToken nameUnit && nextTokenIf(SymbolUnitToken.LP)) {
			var func = new FunctionUnitToken(nameUnit.name(), new ArrayList<>());

			while (true) {
				if (nextTokenIf(SymbolUnitToken.RP)) {
					break;
				} else {
					func.args().add(readFully());

					if (!nextTokenIf(SymbolUnitToken.COMMA) && peekToken() != SymbolUnitToken.RP) {
						throw new IllegalStateException("Unexpected token " + peekToken() + "!");
					}
				}
			}

			result = func;
		} else if (token instanceof InterpretableUnitToken it) {
			result = it;
		}

		if (result == null) {
			throw new IllegalStateException("Token " + token + " not interpretable!");
		}

		return result;
	}

	private InterpretableUnitToken readExpr() {
		boolean negate = false;
		boolean bitNot = false;

		while (peekToken() == SymbolUnitToken.NEGATE || peekToken() == SymbolUnitToken.BIT_NOT) {
			if (nextTokenIf(SymbolUnitToken.NEGATE)) {
				negate = !negate;
			}

			if (nextTokenIf(SymbolUnitToken.BIT_NOT)) {
				bitNot = !bitNot;
			}
		}

		var postfix = new PostfixUnitToken(new ArrayList<>(), nextTokenIf(SymbolUnitToken.LP));

		var firstToken = readInterpretableToken();

		if (!postfix.group()) {
			firstToken = firstToken.negateAndBitNot(negate, bitNot);
		}

		postfix.infix().add(firstToken);

		if (postfix.group() && nextTokenIf(SymbolUnitToken.RP)) {
			return firstToken;
		}

		while (peekToken() instanceof SymbolUnitToken symbol && symbol.isOp()) {
			postfix.infix().add(nextToken());

			var next = readFully();

			if (next instanceof PostfixUnitToken npostfix && !npostfix.group()) {
				postfix.infix().addAll(npostfix.infix());
			} else {
				postfix.infix().add(next);
			}
		}

		if (postfix.group() && !nextTokenIf(SymbolUnitToken.RP)) {
			throw new IllegalStateException("Expected ')'!");
		}

		return postfix.infix().size() == 1 ? firstToken : postfix.group() ? postfix.negateAndBitNot(negate, bitNot) : postfix;
	}

	private InterpretableUnitToken readFully() {
		var expr = readExpr();

		if (nextTokenIf(SymbolUnitToken.HOOK)) {
			var ifTrue = readFully();

			if (nextTokenIf(SymbolUnitToken.COLON)) {
				var ifFalse = readFully();
				return new TernaryOperatorUnitToken(expr, ifTrue, ifFalse);
			} else {
				throw new IllegalStateException("Expected ':'!");
			}
		}

		return expr;
	}

	@Nullable
	public UnitToken nextToken() {
		return tokens.isEmpty() ? null : tokens.removeFirst();
	}

	@Nullable
	public UnitToken peekToken() {
		return tokens.isEmpty() ? null : tokens.getFirst();
	}

	public boolean nextTokenIf(UnitToken match) {
		if (match.equals(peekToken())) {
			nextToken();
			return true;
		}

		return false;
	}

	public List<String> toTokenStrings() {
		return tokens.stream().map(UnitToken::toString).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return input + toTokenStrings();
	}
}

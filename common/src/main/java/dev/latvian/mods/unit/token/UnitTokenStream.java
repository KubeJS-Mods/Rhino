package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.ColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.VariableUnit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
	public final List<UnitToken> tokens;
	public final UnitToken root;
	private int position;

	public UnitTokenStream(UnitContext context, String input, CharStream charStream) {
		this.context = context;
		this.input = input;
		this.charStream = charStream;
		this.tokens = new ArrayList<>();
		this.position = -1;

		var current = new StringBuilder();

		while (true) {
			var c = charStream.next();

			if (c == 0) {
				break;
			} else if (c == '#') {
				if (isHex(charStream.peek(1)) && isHex(charStream.peek(2)) && isHex(charStream.peek(3)) && isHex(charStream.peek(4)) && isHex(charStream.peek(5)) && isHex(charStream.peek(6))) {
					var alpha = isHex(charStream.peek(7)) && isHex(charStream.peek(8));

					current.append('#');

					for (var i = 0; i < (alpha ? 8 : 6); i++) {
						current.append(charStream.next());
					}

					var color = Long.decode(current.toString()).intValue();
					current.setLength(0);

					add(new ColorUnit(color, alpha));
				} else {
					throw new IllegalStateException("Invalid color code @ " + charStream.position);
				}
			} else {
				var symbol = SymbolUnitToken.read(c, charStream);

				if (symbol != null && current.length() > 0) {
					add(createTokenFromString(current.toString()));
					current.setLength(0);
				}

				if (symbol == SymbolUnitToken.SUB && shouldNegate()) {
					add(SymbolUnitToken.NEGATE);
				} else if (symbol != null) {
					add(symbol);
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			add(createTokenFromString(current.toString()));
			current.setLength(0);
		}

		this.root = readToken();
	}

	public Unit getUnit() {
		return root.interpret(context);
	}

	private void add(UnitToken token) {
		tokens.add(token);
	}

	private void replaceLast(UnitToken token) {
		tokens.remove(tokens.size() - 1);
		add(token);
	}

	private static UnitToken createTokenFromString(String input) {
		var constant = FixedNumberUnit.CONSTANTS.get(input);

		if (constant != null) {
			return constant;
		}

		try {
			return FixedNumberUnit.ofFixed(Double.parseDouble(input));
		} catch (Exception ex) {
			return VariableUnit.of(input);
		}
	}

	@Nullable
	public UnitToken last() {
		return tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
	}

	public boolean shouldNegate() {
		var last = last();
		return last == null || last.shouldNegate();
	}

	private UnitToken readToken0() {
		var token = nextToken();

		if (token instanceof VariableUnit varUnit) {
			if (nextTokenIf(SymbolUnitToken.LP)) {
				var func = new FunctionUnitToken(varUnit.name, new ArrayList<>());

				while (true) {
					var arg = readToken();

					if (arg == SymbolUnitToken.RP) {
						break;
					} else {
						func.args().add(arg);
					}
				}

				return func;
			}
		}

		return token;
	}

	private UnitToken readToken1() {
		var token = readToken0();

		if (token == SymbolUnitToken.LP) {
			var postfix = new PostfixUnitToken(new ArrayList<>(), true);
			var t = readToken();

			if (t instanceof PostfixUnitToken p) {
				postfix.infix().addAll(p.infix());
			} else {
				postfix.infix().add(t);
			}

			if (nextTokenIf(SymbolUnitToken.RP)) {
				return postfix;
			} else {
				throw new IllegalStateException("Expected )!");
			}
		} else if (peekToken() instanceof SymbolUnitToken symbol && symbol.isOp()) {
			var postfix = new PostfixUnitToken(new ArrayList<>(), false);
			postfix.infix().add(token);

			while (peekToken() instanceof SymbolUnitToken symbol1 && symbol1.isOp()) {
				postfix.infix().add(nextToken());
				postfix.infix().add(readToken());
			}

			return postfix;
		}

		return token;
	}

	private UnitToken readToken() {
		var token = readToken1();

		if (token == null) {
			throw new IllegalStateException("EOL!");
		} else if (token == SymbolUnitToken.NEGATE) {
			return readToken().negate();
		} else if (token == SymbolUnitToken.BIT_NOT) {
			return readToken().bitNot();
		}

		return token;
	}

	@Nullable
	public UnitToken nextToken() {
		return ++position >= tokens.size() ? null : tokens.get(position);
	}

	public boolean nextTokenIf(UnitToken match) {
		if (match.equals(peekToken())) {
			nextToken();
			return true;
		}

		return false;
	}

	@Nullable
	public UnitToken peekToken(int ahead) {
		if (position + ahead >= tokens.size()) {
			return null;
		}

		return tokens.get(position + ahead);
	}

	@Nullable
	public UnitToken peekToken() {
		return peekToken(1);
	}

	public List<String> toTokenStrings() {
		return tokens.stream().map(UnitToken::toString).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return input + toTokenStrings();
	}
}

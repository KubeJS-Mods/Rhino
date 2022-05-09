package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.ColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.OpGroupUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
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

	private static UnitToken createToken(String input) {
		FixedNumberUnit constant = FixedNumberUnit.CONSTANTS.get(input);

		if (constant != null) {
			return constant;
		}

		try {
			return FixedNumberUnit.ofFixed(Double.parseDouble(input));
		} catch (Exception ex) {
			return new StringUnitToken(input);
		}
	}

	public final UnitContext context;
	public final String input;
	public final CharStream charStream;
	public final List<PositionedUnitToken> tokens;
	public int position;

	public UnitTokenStream(UnitContext context, String input, CharStream charStream) {
		this.context = context;
		this.input = input;
		this.charStream = charStream;
		this.tokens = new ArrayList<>();
		this.position = -1;

		StringBuilder current = new StringBuilder();

		while (true) {
			char c = charStream.next();
			int pos = charStream.position;

			if (c == 0) {
				break;
			} else if (c == '#') {
				if (isHex(charStream.peek(1)) && isHex(charStream.peek(2)) && isHex(charStream.peek(3)) && isHex(charStream.peek(4)) && isHex(charStream.peek(5)) && isHex(charStream.peek(6))) {
					boolean alpha = isHex(charStream.peek(7)) && isHex(charStream.peek(8));

					current.append('#');

					for (int i = 0; i < (alpha ? 8 : 6); i++) {
						current.append(charStream.next());
					}

					int color = Long.decode(current.toString()).intValue();
					current.setLength(0);

					add(new ColorUnit(color, alpha), pos);
				} else {
					throw new IllegalStateException("Invalid color code @ " + charStream.position);
				}
			} else {
				SymbolUnitToken symbol = SymbolUnitToken.read(c, charStream);

				if (symbol == SymbolUnitToken.SUB && shouldNegate()) {
					if (current.length() > 0) {
						addString(current.toString(), pos);
						current.setLength(0);
					}

					add(SymbolUnitToken.NEGATE, pos);
				} else if (symbol != null) {
					if (current.length() > 0) {
						addString(current.toString(), pos);
						current.setLength(0);
					}

					add(symbol, pos);
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			addString(current.toString(), charStream.position);
			current.setLength(0);
		}
	}

	public Unit nextUnit() {
		UnitToken token = nextToken();

		if (token == null) {
			throw new IllegalStateException("EOL!");
		}

		return OpGroupUnit.interpret(token.interpret(this), this, null);
	}

	private void add(UnitToken token, int pos) {
		tokens.add(new PositionedUnitToken(token, pos));
	}

	private void replaceLast(UnitToken token) {
		tokens.set(tokens.size() - 1, new PositionedUnitToken(token, tokens.get(tokens.size() - 1).position()));
	}

	private void addString(String input, int pos) {
		UnitToken token = createToken(input);

		if (token instanceof FixedNumberUnit num && last() == SymbolUnitToken.NEGATE) {
			replaceLast(FixedNumberUnit.ofFixed(-num.value));
		} else {
			add(token, pos);
		}
	}

	@Nullable
	public UnitToken last() {
		return tokens.isEmpty() ? null : tokens.get(tokens.size() - 1).token();
	}

	public boolean shouldNegate() {
		UnitToken last = last();
		return last == null || last.shouldNegate();
	}

	@Nullable
	public UnitToken nextToken() {
		if (++position >= tokens.size()) {
			return null;
		}

		return tokens.get(position).token();
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

		return tokens.get(position + ahead).token();
	}

	@Nullable
	public UnitToken peekToken() {
		return peekToken(1);
	}

	public List<String> toTokenStrings() {
		return tokens.stream().map(t -> t.token().toString()).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return input + toTokenStrings();
	}
}

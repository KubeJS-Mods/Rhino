package dev.latvian.mods.rhino.util.unit;

import java.util.ArrayList;
import java.util.List;

class UnitParser {
	private final String string;
	private final char[] chars;
	private int pos;
	private final UnitStorage storage;

	public UnitParser(String s, UnitStorage st) {
		string = s;
		chars = string.trim().toCharArray();
		pos = 0;
		storage = st;
	}

	private boolean isEOL() {
		return pos >= chars.length;
	}

	private void move() {
		if (isEOL()) {
			throw new UnitParserException(string, pos, "End of line!");
		}

		pos++;
	}

	private char peek() {
		if (isEOL()) {
			throw new UnitParserException(string, pos, "End of line!");
		}

		return chars[pos];
	}

	private char read() {
		char c = peek();
		move();
		return c;
	}

	private void readClose(int startPos) {
		skipS();

		if (read() != ')') {
			throw new UnitParserException(string, startPos, "Expected ) at " + pos);
		}
	}

	private boolean isW(char c) {
		return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
	}

	private String readW() {
		int p0 = pos;

		while (!isEOL() && isW(peek())) {
			move();
		}

		return new String(chars, p0, pos - p0);
	}

	private boolean isN(char c) {
		return c >= '0' && c <= '9' || c == '.';
	}

	private String readN() {
		int p0 = pos;

		while (!isEOL() && isN(peek())) {
			move();
		}

		return new String(chars, p0, pos - p0);
	}

	private boolean isSym(char c) {
		return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '>' || c == '<' || c == '!' || c == '=' || c == '~' || c == '^';
	}

	private String readSym() {
		int p0 = pos;

		while (!isEOL() && isSym(peek())) {
			move();
		}

		return new String(chars, p0, pos - p0);
	}

	private boolean isS(char c) {
		return c <= ' ';
	}

	private void skipS() {
		while (!isEOL() && isS(peek())) {
			move();
		}
	}

	public Unit parse() {
		int startPos = pos;

		try {
			return readUnit();
		} catch (StackOverflowError ex) {
			ex.printStackTrace();
			throw new UnitParserException(string, startPos, "Stack overflow!");
		}
	}

	public Unit readUnit() {
		skipS();
		int startPos = pos;
		char start = peek();

		if (start == '$') {
			move();
			return new VariableUnit(storage, readW());
		} else if (start == '-') {
			move();
			return readUnit().neg();
		} else if (start == '~' || start == '!') {
			move();
			return readUnit().not();
		} else if (start == '#') {
			move();
			String hex = readW();
			int i = Long.decode("#" + hex).intValue();
			int r = (i >> 16) & 0xFF;
			int g = (i >> 8) & 0xFF;
			int b = i & 0xFF;
			int a = (i >> 24) & 0xFF;
			return new ColorUnit(FixedUnit.of(r), FixedUnit.of(g), FixedUnit.of(b), hex.length() == 6 ? null : FixedUnit.of(a));
		} else if (start >= '0' && start <= '9') {
			return FixedUnit.of(Float.parseFloat(readN()));
		} else if (start == '(') {
			move();
			Unit unit = readUnit();
			skipS();
			String c = readSym();
			Unit with = readUnit();
			readClose(startPos);

			Unit u = storage.createOp(c, unit, with);

			if (u == null) {
				throw new UnitParserException(string, startPos, "Unknown operation " + c);
			}

			return u;
		} else if (isW(start)) {
			String func = readW();

			ConstantUnit constant = storage.getConstant(func);

			if (constant != null) {
				return constant;
			}

			skipS();

			if (read() != '(') {
				throw new UnitParserException(string, startPos, "Unknown constant '" + func + "' at " + pos);
			}

			List<Unit> args = new ArrayList<>(2);

			skipS();
			if (peek() != ')') {
				args.add(readUnit());

				argsRead:
				while (true) {
					skipS();
					char c = read();

					switch (c) {
						case ',':
							args.add(readUnit());
							break;
						case ')':
							break argsRead;
						default:
							throw new UnitParserException(string, startPos, "Unexpected character " + c + ", expected ) or ,!");
					}
				}
			} else {
				readClose(startPos);
			}

			Unit function = storage.createFunc(func, args);

			if (function != null) {
				return function;
			}

			throw new UnitParserException(string, startPos, "Unknown function " + func);
		}

		throw new UnitParserException(string, startPos, "Unknown syntax");
	}
}

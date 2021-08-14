package dev.latvian.mods.rhino.util.unit;

import java.util.ArrayList;
import java.util.List;

class UnitParser {
	private final String string;
	private final char[] chars;
	private int pos;
	private final UnitVariables variables;

	public UnitParser(String s, UnitVariables v) {
		string = s;
		chars = string.trim().toCharArray();
		pos = 0;
		variables = v;
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

	private void readOpen(int startPos) {
		skipS();

		if (read() != '(') {
			throw new UnitParserException(string, startPos, "Expected ( at " + pos);
		}
	}

	private void readComma(int startPos) {
		skipS();

		if (read() != ',') {
			throw new UnitParserException(string, startPos, "Expected , at " + pos);
		}
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
		return c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '>' || c == '<';
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

	private Unit readUnit() {
		skipS();
		int startPos = pos;
		char start = peek();

		if (start == '$') {
			move();
			String key = readW();
			Unit unit = variables.get(key);

			if (unit == null) {
				throw new UnitParserException(string, startPos, "Variable $" + key + " not set!");
			}

			return new VariableUnit(unit, key);
		} else if (start == '-') {
			move();
			return readUnit().neg();
		} else if (start >= '0' && start <= '9') {
			return Unit.fixed(Float.parseFloat(readN()));
		} else if (start == '(') {
			move();
			Unit unit = readUnit();
			skipS();
			String c = readSym();
			Unit with = readUnit();
			readClose(startPos);

			switch (c) {
				case "+":
					return unit.add(with);
				case "-":
					return unit.sub(with);
				case "*":
					return unit.mul(with);
				case "/":
					return unit.div(with);
				case "%":
					return unit.mod(with);
				case "**":
					return unit.pow(with);
				case "<<":
					return unit.shiftLeft(with);
				case ">>":
					return unit.shiftRight(with);
				default:
					throw new UnitParserException(string, startPos, "Unknown operation " + c);
			}
		} else if (isW(start)) {
			String func = readW();

			switch (func) {
				case "PI":
					return Unit.PI;
				case "E":
					return Unit.E;
			}

			readOpen(startPos);
			List<Unit> args = new ArrayList<>(1);

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

			switch (func) {
				case "random":
					return RandomUnit.INSTANCE;
				case "time":
					return TimeUnit.INSTANCE;
				case "min":
					return args.get(0).min(args.get(1));
				case "max":
					return args.get(0).max(args.get(1));
				case "pow":
					return args.get(0).pow(args.get(1));
				case "atan2":
					return args.get(0).atan2(args.get(1));
				case "abs":
					return args.get(0).abs();
				case "sin":
					return args.get(0).sin();
				case "cos":
					return args.get(0).cos();
				case "tan":
					return args.get(0).tan();
				case "deg":
					return args.get(0).deg();
				case "rad":
					return args.get(0).rad();
				case "atan":
					return args.get(0).atan();
				case "log":
					return args.get(0).log();
				case "log10":
					return args.get(0).log10();
				case "log1p":
					return args.get(0).log1p();
				case "sqrt":
					return args.get(0).sqrt();
				case "sq":
					return args.get(0).sq();
				case "floor":
					return args.get(0).floor();
				case "ceil":
					return args.get(0).ceil();
				default:
					throw new UnitParserException(string, startPos, "Unknown function " + func);
			}
		}

		throw new UnitParserException(string, startPos, "Unknown syntax");
	}
}

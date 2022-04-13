package dev.latvian.mods.unit.token;

public class CharStream {
	public final char[] chars;
	public int position;
	public boolean skipWhitespace;

	public CharStream(char[] c) {
		chars = c;
		position = -1;
		skipWhitespace = true;
	}

	public char next() {
		if (++position >= chars.length) {
			return 0;
		}

		while (skipWhitespace && chars[position] <= ' ') {
			position++;

			if (position >= chars.length) {
				return 0;
			}
		}

		return chars[position];
	}

	public boolean nextIf(char match) {
		if (peek() == match) {
			next();
			return true;
		}

		return false;
	}

	public char peek(int ahead) {
		if (position + ahead >= chars.length) {
			return 0;
		}

		while (skipWhitespace && chars[position + ahead] <= ' ') {
			ahead++;

			if (position + ahead >= chars.length) {
				return 0;
			}
		}

		return chars[position + ahead];
	}

	public char peek() {
		return peek(1);
	}
}

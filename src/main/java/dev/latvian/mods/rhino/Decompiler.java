/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.FunctionNode;

/**
 * The following class save decompilation information about the source.
 * Source information is returned from the parser as a String
 * associated with function nodes and with the toplevel script.  When
 * saved in the constant pool of a class, this string will be UTF-8
 * encoded, and token values will occupy a single byte.
 * <p>
 * Source is saved (mostly) as token numbers.  The tokens saved pretty
 * much correspond to the token stream of a 'canonical' representation
 * of the input program, as directed by the parser.  (There were a few
 * cases where tokens could have been left out where decompiler could
 * easily reconstruct them, but I left them in for clarity).  (I also
 * looked adding source collection to TokenStream instead, where I
 * could have limited the changes to a few lines in getToken... but
 * this wouldn't have saved any space in the resulting source
 * representation, and would have meant that I'd have to duplicate
 * parser logic in the decompiler to disambiguate situations where
 * newlines are important.)  The function decompile expands the
 * tokens back into their string representations, using simple
 * lookahead to correct spacing and indentation.
 * <p>
 * Assignments are saved as two-token pairs (Token.ASSIGN, op). Number tokens
 * are stored inline, as a NUMBER token, a character representing the type, and
 * either 1 or 4 characters representing the bit-encoding of the number.  String
 * types NAME, STRING and OBJECT are currently stored as a token type,
 * followed by a character giving the length of the string (assumed to
 * be less than 2^16), followed by the characters of the string
 * inlined into the source string.  Changing this to some reference to
 * to the string in the compiled class' constant pool would probably
 * save a lot of space... but would require some method of deriving
 * the final constant pool entry from information available at parse
 * time.
 */
public class Decompiler {
	// Marker to denote the last RC of function so it can be distinguished from
	// the last RC of object literals in case of function expressions
	private static final int FUNCTION_END = Token.LAST_TOKEN + 1;

	String getEncodedSource() {
		return sourceToString(0);
	}

	int getCurrentOffset() {
		return sourceTop;
	}

	int markFunctionStart(int functionType) {
		int savedOffset = getCurrentOffset();
		if (functionType != FunctionNode.ARROW_FUNCTION) {
			addToken(Token.FUNCTION);
			append((char) functionType);
		}
		return savedOffset;
	}

	int markFunctionEnd(int functionStart) {
		int offset = getCurrentOffset();
		append((char) FUNCTION_END);
		return offset;
	}

	void addToken(int token) {
		if (!(0 <= token && token <= Token.LAST_TOKEN)) {
			throw new IllegalArgumentException();
		}

		append((char) token);
	}

	void addEOL(int token) {
		if (!(0 <= token && token <= Token.LAST_TOKEN)) {
			throw new IllegalArgumentException();
		}

		append((char) token);
		append((char) Token.EOL);
	}

	void addName(String str) {
		addToken(Token.NAME);
		appendString(str);
	}

	void addString(String str) {
		addToken(Token.STRING);
		appendString(str);
	}

	void addTemplateLiteral(String str) {
		addToken(Token.TEMPLATE_CHARS);
		appendString(str);
	}

	void addRegexp(String regexp, String flags) {
		addToken(Token.REGEXP);
		appendString('/' + regexp + '/' + flags);
	}

	void addNumber(double n) {
		addToken(Token.NUMBER);

		/* encode the number in the source stream.
		 * Save as NUMBER type (char | char char char char)
		 * where type is
		 * 'D' - double, 'S' - short, 'J' - long.

		 * We need to retain float vs. integer type info to keep the
		 * behavior of liveconnect type-guessing the same after
		 * decompilation.  (Liveconnect tries to present 1.0 to Java
		 * as a float/double)
		 * OPT: This is no longer true. We could compress the format.

		 * This may not be the most space-efficient encoding;
		 * the chars created below may take up to 3 bytes in
		 * constant pool UTF-8 encoding, so a Double could take
		 * up to 12 bytes.
		 */

		long lbits = (long) n;
		if (lbits != n) {
			// if it's floating point, save as a Double bit pattern.
			// (12/15/97 our scanner only returns Double for f.p.)
			lbits = Double.doubleToLongBits(n);
			append('D');
			append((char) (lbits >> 48));
			append((char) (lbits >> 32));
			append((char) (lbits >> 16));
			append((char) lbits);
		} else {
			// we can ignore negative values, bc they're already prefixed
			// by NEG
			if (lbits < 0) {
				Kit.codeBug();
			}

			// will it fit in a char?
			// this gives a short encoding for integer values up to 2^16.
			if (lbits <= Character.MAX_VALUE) {
				append('S');
				append((char) lbits);
			} else { // Integral, but won't fit in a char. Store as a long.
				append('J');
				append((char) (lbits >> 48));
				append((char) (lbits >> 32));
				append((char) (lbits >> 16));
				append((char) lbits);
			}
		}
	}

	private void appendString(String str) {
		int L = str.length();
		int lengthEncodingSize = 1;
		if (L >= 0x8000) {
			lengthEncodingSize = 2;
		}
		int nextTop = sourceTop + lengthEncodingSize + L;
		if (nextTop > sourceBuffer.length) {
			increaseSourceCapacity(nextTop);
		}
		if (L >= 0x8000) {
			// Use 2 chars to encode strings exceeding 32K, were the highest
			// bit in the first char indicates presence of the next byte
			sourceBuffer[sourceTop] = (char) (0x8000 | (L >>> 16));
			++sourceTop;
		}
		sourceBuffer[sourceTop] = (char) L;
		++sourceTop;
		str.getChars(0, L, sourceBuffer, sourceTop);
		sourceTop = nextTop;
	}

	private void append(char c) {
		if (sourceTop == sourceBuffer.length) {
			increaseSourceCapacity(sourceTop + 1);
		}
		sourceBuffer[sourceTop] = c;
		++sourceTop;
	}

	private void increaseSourceCapacity(int minimalCapacity) {
		// Call this only when capacity increase is must
		if (minimalCapacity <= sourceBuffer.length) {
			Kit.codeBug();
		}
		int newCapacity = sourceBuffer.length * 2;
		if (newCapacity < minimalCapacity) {
			newCapacity = minimalCapacity;
		}
		char[] tmp = new char[newCapacity];
		System.arraycopy(sourceBuffer, 0, tmp, 0, sourceTop);
		sourceBuffer = tmp;
	}

	private String sourceToString(int offset) {
		if (offset < 0 || sourceTop < offset) {
			Kit.codeBug();
		}
		return new String(sourceBuffer, offset, sourceTop - offset);
	}

	private char[] sourceBuffer = new char[128];

	// Per script/function source buffer top: parent source does not include a
	// nested functions source and uses function index as a reference instead.
	private int sourceTop;
}

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.io.IOException;

/**
 * This class implements the JavaScript scanner.
 * <p>
 * It is based on the C source files jsscan.c and jsscan.h
 * in the jsref package.
 *
 * @author Mike McCabe
 * @author Brendan Eich
 * @see Parser
 */

class TokenStream {
	/*
	 * For chars - because we need something out-of-range
	 * to check.  (And checking EOF by exception is annoying.)
	 * Note distinction from EOF token type!
	 */
	private final static int EOF_CHAR = -1;

	private final static char BYTE_ORDER_MARK = '\uFEFF';

	TokenStream(Parser parser, String sourceString, int lineno) {
		this.parser = parser;
		this.lineno = lineno;
		if (sourceString == null) {
			Kit.codeBug();
		}
		this.sourceString = sourceString;
		this.sourceEnd = sourceString.length();
		this.sourceCursor = this.cursor = 0;
	}

	static boolean isKeyword(String s, boolean isStrict) {
		return Token.EOF != stringToKeyword(s, isStrict);
	}

	private static int stringToKeyword(String name, boolean isStrict) {
		return stringToKeywordForES(name, isStrict);
	}

	/**
	 * ECMAScript 6.
	 */
	private static int stringToKeywordForES(String name, boolean isStrict) {
		return switch (name) {
			case "break" -> Token.BREAK;
			case "case" -> Token.CASE;
			case "catch" -> Token.CATCH;
			case "const" -> Token.CONST;
			case "continue" -> Token.CONTINUE;
			case "default" -> Token.DEFAULT;
			case "delprop" -> Token.DELPROP;
			case "do" -> Token.DO;
			case "else" -> Token.ELSE;
			case "finally" -> Token.FINALLY;
			case "for" -> Token.FOR;
			case "function" -> Token.FUNCTION;
			case "if" -> Token.IF;
			case "in" -> Token.IN;
			case "instanceof" -> Token.INSTANCEOF;
			case "new" -> Token.NEW;
			case "return" -> Token.RETURN;
			case "switch" -> Token.SWITCH;
			case "this" -> Token.THIS;
			case "throw" -> Token.THROW;
			case "try" -> Token.TRY;
			case "typeof" -> Token.TYPEOF;
			case "var" -> Token.VAR;
			case "void" -> Token.VOID;
			case "while" -> Token.WHILE;
			case "with" -> Token.WITH;
			case "yield" -> Token.YIELD;
			case "false" -> Token.FALSE;
			case "null" -> Token.NULL;
			case "true" -> Token.TRUE;
			case "let" -> Token.LET;
			case "class", "export", "static", "public", "protected", "private", "package", "interface", "implements", "enum", "await", "super", "import", "extends" -> Token.RESERVED;
			default -> Token.EOF;
		}; // & 0xFF;
	}

	final String getSourceString() {
		return sourceString;
	}

	final int getLineno() {
		return lineno;
	}

	final String getString() {
		return string;
	}

	final char getQuoteChar() {
		return (char) quoteChar;
	}

	final double getNumber() {
		return number;
	}

	final boolean isNumberBinary() {
		return isBinary;
	}

	final boolean isNumberOldOctal() {
		return isOldOctal;
	}

	final boolean isNumberOctal() {
		return isOctal;
	}

	final boolean isNumberHex() {
		return isHex;
	}

	final boolean eof() {
		return hitEOF;
	}

	final int getToken() throws IOException {
		int c;

		retry:
		for (; ; ) {
			// Eat whitespace, possibly sensitive to newlines.
			for (; ; ) {
				c = getChar();
				if (c == EOF_CHAR) {
					tokenBeg = cursor - 1;
					tokenEnd = cursor;
					return Token.EOF;
				} else if (c == '\n') {
					dirtyLine = false;
					tokenBeg = cursor - 1;
					tokenEnd = cursor;
					return Token.EOL;
				} else if (!isJSSpace(c)) {
					if (c != '-') {
						dirtyLine = true;
					}
					break;
				}
			}

			// Assume the token will be 1 char - fixed up below.
			tokenBeg = cursor - 1;
			tokenEnd = cursor;

			/*
			if (c == '@') {
				return Token.XMLATTR;
			}
			*/

			// identifier/keyword/instanceof?
			// watch out for starting with a <backslash>
			boolean identifierStart;
			boolean isUnicodeEscapeStart = false;
			if (c == '\\') {
				c = getChar();
				if (c == 'u') {
					identifierStart = true;
					isUnicodeEscapeStart = true;
					stringBufferTop = 0;
				} else {
					identifierStart = false;
					ungetChar(c);
					c = '\\';
				}
			} else {
				identifierStart = Character.isJavaIdentifierStart((char) c);
				if (identifierStart) {
					stringBufferTop = 0;
					addToString(c);
				}
			}

			if (identifierStart) {
				boolean containsEscape = isUnicodeEscapeStart;
				for (; ; ) {
					if (isUnicodeEscapeStart) {
						// strictly speaking we should probably push-back
						// all the bad characters if the <backslash>uXXXX
						// sequence is malformed. But since there isn't a
						// correct context(is there?) for a bad Unicode
						// escape sequence in an identifier, we can report
						// an error here.
						int escapeVal = 0;
						for (int i = 0; i != 4; ++i) {
							c = getChar();
							escapeVal = Kit.xDigitToInt(c, escapeVal);
							// Next check takes care about c < 0 and bad escape
							if (escapeVal < 0) {
								break;
							}
						}
						if (escapeVal < 0) {
							parser.addError("msg.invalid.escape");
							return Token.ERROR;
						}
						addToString(escapeVal);
						isUnicodeEscapeStart = false;
					} else {
						c = getChar();
						if (c == '\\') {
							c = getChar();
							if (c == 'u') {
								isUnicodeEscapeStart = true;
								containsEscape = true;
							} else {
								parser.addError("msg.illegal.character", c);
								return Token.ERROR;
							}
						} else {
							if (c == EOF_CHAR || c == BYTE_ORDER_MARK || !Character.isJavaIdentifierPart((char) c)) {
								break;
							}
							addToString(c);
						}
					}
				}
				ungetChar(c);

				String str = getStringFromBuffer();
				if (!containsEscape) {
					// OPT we shouldn't have to make a string (object!) to
					// check if it's a keyword.

					// Return the corresponding token if it's a keyword
					int result = stringToKeyword(str, parser.inUseStrictDirective());
					if (result != Token.EOF) {
						// Save the string in case we need to use in
						// object literal definitions.
						this.string = (String) allStrings.intern(str);
						return result;
					}
				} else if (isKeyword(str, parser.inUseStrictDirective())) {
					// If a string contains unicodes, and converted to a keyword,
					// we convert the last character back to unicode
					str = convertLastCharToHex(str);
				}
				this.string = (String) allStrings.intern(str);
				return Token.NAME;
			}

			// is it a number?
			if (isDigit(c) || (c == '.' && isDigit(peekChar()))) {
				stringBufferTop = 0;
				int base = 10;
				isHex = isOldOctal = isOctal = isBinary = false;

				if (c == '0') {
					c = getChar();
					if (c == 'x' || c == 'X') {
						base = 16;
						isHex = true;
						c = getChar();
					} else if ((c == 'o' || c == 'O')) {
						base = 8;
						isOctal = true;
						c = getChar();
					} else if ((c == 'b' || c == 'B')) {
						base = 2;
						isBinary = true;
						c = getChar();
					} else if (isDigit(c)) {
						base = 8;
						isOldOctal = true;
					} else {
						addToString('0');
					}
				}

				boolean isEmpty = true;
				if (base == 16) {
					while (0 <= Kit.xDigitToInt(c, 0)) {
						addToString(c);
						c = getChar();
						isEmpty = false;
					}
				} else {
					while ('0' <= c && c <= '9') {
						if (base == 8 && c >= '8') {
							if (isOldOctal) {
								/*
								 * We permit 08 and 09 as decimal numbers, which
								 * makes our behavior a superset of the ECMA
								 * numeric grammar.  We might not always be so
								 * permissive, so we warn about it.
								 */
								parser.addWarning("msg.bad.octal.literal", c == '8' ? "8" : "9");
								base = 10;
							} else {
								parser.addError("msg.caught.nfe");
								return Token.ERROR;
							}
						} else if (base == 2 && c >= '2') {
							parser.addError("msg.caught.nfe");
							return Token.ERROR;
						}
						addToString(c);
						c = getChar();
						isEmpty = false;
					}
				}
				if (isEmpty && (isBinary || isOctal || isHex)) {
					parser.addError("msg.caught.nfe");
					return Token.ERROR;
				}

				boolean isInteger = true;

				if (base == 10 && (c == '.' || c == 'e' || c == 'E')) {
					isInteger = false;
					if (c == '.') {
						do {
							addToString(c);
							c = getChar();
						} while (isDigit(c));
					}
					if (c == 'e' || c == 'E') {
						addToString(c);
						c = getChar();
						if (c == '+' || c == '-') {
							addToString(c);
							c = getChar();
						}
						if (!isDigit(c)) {
							parser.addError("msg.missing.exponent");
							return Token.ERROR;
						}
						do {
							addToString(c);
							c = getChar();
						} while (isDigit(c));
					}
				}
				ungetChar(c);
				String numString = getStringFromBuffer();
				this.string = numString;

				double dval;
				if (base == 10 && !isInteger) {
					try {
						// Use Java conversion to number from string...
						dval = Double.parseDouble(numString);
					} catch (NumberFormatException ex) {
						parser.addError("msg.caught.nfe");
						return Token.ERROR;
					}
				} else {
					dval = ScriptRuntime.stringPrefixToNumber(numString, 0, base);
				}

				this.number = dval;
				return Token.NUMBER;
			}

			// is it a string?
			if (c == '"' || c == '\'') {
				// We attempt to accumulate a string the fast way, by
				// building it directly out of the reader.  But if there
				// are any escaped characters in the string, we revert to
				// building it out of a StringBuffer.

				quoteChar = c;
				stringBufferTop = 0;

				c = getChar(false);
				strLoop:
				while (c != quoteChar) {
					if (c == '\n' || c == EOF_CHAR) {
						ungetChar(c);
						tokenEnd = cursor;
						parser.addError("msg.unterminated.string.lit");
						return Token.ERROR;
					}

					if (c == '\\') {
						// We've hit an escaped character
						int escapeVal;

						c = getChar();
						switch (c) {
							case 'b':
								c = '\b';
								break;
							case 'f':
								c = '\f';
								break;
							case 'n':
								c = '\n';
								break;
							case 'r':
								c = '\r';
								break;
							case 't':
								c = '\t';
								break;

							// \v a late addition to the ECMA spec,
							// it is not in Java, so use 0xb
							case 'v':
								c = 0xb;
								break;

							case 'u':
								// Get 4 hex digits; if the u escape is not
								// followed by 4 hex digits, use 'u' + the
								// literal character sequence that follows.
								int escapeStart = stringBufferTop;
								addToString('u');
								escapeVal = 0;
								for (int i = 0; i != 4; ++i) {
									c = getChar();
									escapeVal = Kit.xDigitToInt(c, escapeVal);
									if (escapeVal < 0) {
										continue strLoop;
									}
									addToString(c);
								}
								// prepare for replace of stored 'u' sequence
								// by escape value
								stringBufferTop = escapeStart;
								c = escapeVal;
								break;
							case 'x':
								// Get 2 hex digits, defaulting to 'x'+literal
								// sequence, as above.
								c = getChar();
								escapeVal = Kit.xDigitToInt(c, 0);
								if (escapeVal < 0) {
									addToString('x');
									continue strLoop;
								}
								int c1 = c;
								c = getChar();
								escapeVal = Kit.xDigitToInt(c, escapeVal);
								if (escapeVal < 0) {
									addToString('x');
									addToString(c1);
									continue strLoop;
								}
								// got 2 hex digits
								c = escapeVal;
								break;

							case '\n':
								// Remove line terminator after escape to follow
								// SpiderMonkey and C/C++
								c = getChar();
								continue strLoop;

							default:
								if ('0' <= c && c < '8') {
									int val = c - '0';
									c = getChar();
									if ('0' <= c && c < '8') {
										val = 8 * val + c - '0';
										c = getChar();
										if ('0' <= c && c < '8' && val <= 037) {
											// c is 3rd char of octal sequence only
											// if the resulting val <= 0377
											val = 8 * val + c - '0';
											c = getChar();
										}
									}
									ungetChar(c);
									c = val;
								}
						}
					}
					addToString(c);
					c = getChar(false);
				}

				String str = getStringFromBuffer();
				this.string = (String) allStrings.intern(str);
				return Token.STRING;
			}

			switch (c) {
				case ';':
					return Token.SEMI;
				case '[':
					return Token.LB;
				case ']':
					return Token.RB;
				case '{':
					return Token.LC;
				case '}':
					return Token.RC;
				case '(':
					return Token.LP;
				case ')':
					return Token.RP;
				case ',':
					return Token.COMMA;
				case '?':
					if (matchChar('?')) {
						return Token.NULLISH_COALESCING;
					} else if (matchChar('.')) {
						return Token.OPTIONAL_CHAINING;
					} else {
						return Token.HOOK;
					}
				case ':':
					return Token.COLON;
				case '.':
					return Token.DOT;
				case '|':
					if (matchChar('|')) {
						return Token.OR;
					} else if (matchChar('=')) {
						return Token.ASSIGN_BITOR;
					} else {
						return Token.BITOR;
					}

				case '^':
					if (matchChar('=')) {
						return Token.ASSIGN_BITXOR;
					}
					return Token.BITXOR;

				case '&':
					if (matchChar('&')) {
						return Token.AND;
					} else if (matchChar('=')) {
						return Token.ASSIGN_BITAND;
					} else {
						return Token.BITAND;
					}

				case '=':
					if (matchChar('=')) {
						if (matchChar('=')) {
							return Token.SHEQ;
						}
						return Token.EQ;
					} else if (matchChar('>')) {
						return Token.ARROW;
					} else {
						return Token.ASSIGN;
					}

				case '!':
					if (matchChar('=')) {
						if (matchChar('=')) {
							return Token.SHNE;
						}
						return Token.NE;
					}
					return Token.NOT;

				case '<':
					/* NB:treat HTML begin-comment as comment-till-eol */
					if (matchChar('!')) {
						if (matchChar('-')) {
							if (matchChar('-')) {
								tokenBeg = cursor - 4;
								skipLine();
								commentType = Token.CommentType.HTML;
								return Token.COMMENT;
							}
							ungetCharIgnoreLineEnd('-');
						}
						ungetCharIgnoreLineEnd('!');
					}
					if (matchChar('<')) {
						if (matchChar('=')) {
							return Token.ASSIGN_LSH;
						}
						return Token.LSH;
					}
					if (matchChar('=')) {
						return Token.LE;
					}
					return Token.LT;

				case '>':
					if (matchChar('>')) {
						if (matchChar('>')) {
							if (matchChar('=')) {
								return Token.ASSIGN_URSH;
							}
							return Token.URSH;
						}
						if (matchChar('=')) {
							return Token.ASSIGN_RSH;
						}
						return Token.RSH;
					}
					if (matchChar('=')) {
						return Token.GE;
					}
					return Token.GT;

				case '*':
					if (matchChar('=')) {
						return Token.ASSIGN_MUL;
					} else if (matchChar('*')) {
						return Token.POW;
					} else {
						return Token.MUL;
					}

				case '/':
					markCommentStart();
					// is it a // comment?
					if (matchChar('/')) {
						tokenBeg = cursor - 2;
						skipLine();
						commentType = Token.CommentType.LINE;
						return Token.COMMENT;
					}
					// is it a /* or /** comment?
					if (matchChar('*')) {
						boolean lookForSlash = false;
						tokenBeg = cursor - 2;
						if (matchChar('*')) {
							lookForSlash = true;
							commentType = Token.CommentType.JSDOC;
						} else {
							commentType = Token.CommentType.BLOCK_COMMENT;
						}
						for (; ; ) {
							c = getChar();
							if (c == EOF_CHAR) {
								tokenEnd = cursor - 1;
								parser.addError("msg.unterminated.comment");
								return Token.COMMENT;
							} else if (c == '*') {
								lookForSlash = true;
							} else if (c == '/') {
								if (lookForSlash) {
									tokenEnd = cursor;
									return Token.COMMENT;
								}
							} else {
								lookForSlash = false;
								tokenEnd = cursor;
							}
						}
					}

					if (matchChar('=')) {
						return Token.ASSIGN_DIV;
					}
					return Token.DIV;

				case '%':
					if (matchChar('=')) {
						return Token.ASSIGN_MOD;
					}
					return Token.MOD;

				case '~':
					return Token.BITNOT;

				case '+':
					if (matchChar('=')) {
						return Token.ASSIGN_ADD;
					} else if (matchChar('+')) {
						return Token.INC;
					} else {
						return Token.ADD;
					}

				case '-':
					if (matchChar('=')) {
						c = Token.ASSIGN_SUB;
					} else if (matchChar('-')) {
						if (!dirtyLine) {
							// treat HTML end-comment after possible whitespace
							// after line start as comment-until-eol
							if (matchChar('>')) {
								markCommentStart("--");
								skipLine();
								commentType = Token.CommentType.HTML;
								return Token.COMMENT;
							}
						}
						c = Token.DEC;
					} else {
						c = Token.SUB;
					}
					dirtyLine = true;
					return c;
				case '`':
					return Token.TEMPLATE_LITERAL;

				default:
					parser.addError("msg.illegal.character", c);
					return Token.ERROR;
			}
		}
	}

	private static boolean isAlpha(int c) {
		// Use 'Z' < 'a'
		if (c <= 'Z') {
			return 'A' <= c;
		}
		return 'a' <= c && c <= 'z';
	}

	static boolean isDigit(int c) {
		return '0' <= c && c <= '9';
	}

	/* As defined in ECMA.  jsscan.c uses C isspace() (which allows
	 * \v, I think.)  note that code in getChar() implicitly accepts
	 * '\r' == \u000D as well.
	 */
	static boolean isJSSpace(int c) {
		if (c <= 127) {
			return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB;
		}
		return c == 0xA0 || c == BYTE_ORDER_MARK || Character.getType((char) c) == Character.SPACE_SEPARATOR;
	}

	private static boolean isJSFormatChar(int c) {
		return c > 127 && Character.getType((char) c) == Character.FORMAT;
	}

	/**
	 * Parser calls the method when it gets / or /= in literal context.
	 */
	void readRegExp(int startToken) throws IOException {
		int start = tokenBeg;
		stringBufferTop = 0;
		if (startToken == Token.ASSIGN_DIV) {
			// Miss-scanned /=
			addToString('=');
		} else {
			if (startToken != Token.DIV) {
				Kit.codeBug();
			}
			if (peekChar() == '*') {
				tokenEnd = cursor - 1;
				this.string = new String(stringBuffer, 0, stringBufferTop);
				parser.reportError("msg.unterminated.re.lit");
				return;
			}
		}

		boolean inCharSet = false; // true if inside a '['..']' pair
		int c;
		while ((c = getChar()) != '/' || inCharSet) {
			if (c == '\n' || c == EOF_CHAR) {
				ungetChar(c);
				tokenEnd = cursor - 1;
				this.string = new String(stringBuffer, 0, stringBufferTop);
				parser.reportError("msg.unterminated.re.lit");
				return;
			}
			if (c == '\\') {
				addToString(c);
				c = getChar();
				if (c == '\n' || c == EOF_CHAR) {
					ungetChar(c);
					tokenEnd = cursor - 1;
					this.string = new String(stringBuffer, 0, stringBufferTop);
					parser.reportError("msg.unterminated.re.lit");
					return;
				}
			} else if (c == '[') {
				inCharSet = true;
			} else if (c == ']') {
				inCharSet = false;
			}
			addToString(c);
		}
		int reEnd = stringBufferTop;

		while (true) {
			if (matchChar('g')) {
				addToString('g');
			} else if (matchChar('i')) {
				addToString('i');
			} else if (matchChar('m')) {
				addToString('m');
			} else if (matchChar('y'))  // FireFox 3
			{
				addToString('y');
			} else {
				break;
			}
		}
		tokenEnd = start + stringBufferTop + 2;  // include slashes

		if (isAlpha(peekChar())) {
			parser.reportError("msg.invalid.re.flag");
		}

		this.string = new String(stringBuffer, 0, reEnd);
		this.regExpFlags = new String(stringBuffer, reEnd, stringBufferTop - reEnd);
	}

	String readAndClearRegExpFlags() {
		String flags = this.regExpFlags;
		this.regExpFlags = null;
		return flags;
	}

	private final StringBuilder rawString = new StringBuilder();

	String getRawString() {
		if (rawString.length() == 0) {
			return "";
		}
		return rawString.toString();
	}

	private int getTemplateLiteralChar() throws IOException {
		boolean unget = ungetCursor != 0;
		int oldLineEnd = lineEndChar;
		// getChar() skips past '\r\n' sequences, but we need a faithful
		// representation of the complete input for template literals
		int c = getCharIgnoreLineEnd(false);
		if (c == '\n') {
			c = lineEndChar;
		}
		// update lineno after passing line boundaries (cf. getChar())
		// - unless this is a 'unget' character
		// - unless this is a '\r\n' sequence
		if (oldLineEnd >= 0 && !unget && !(oldLineEnd == '\r' && c == '\n')) {
			lineEndChar = -1;
			lineStart = sourceCursor - 1;
			lineno++;
		}
		rawString.append((char) c);
		return c;
	}

	private void ungetTemplateLiteralChar(int c) {
		ungetCharIgnoreLineEnd(c);
		rawString.setLength(rawString.length() - 1);
	}

	private boolean matchTemplateLiteralChar(int test) throws IOException {
		int c = getTemplateLiteralChar();
		if (c == test) {
			return true;
		}
		ungetTemplateLiteralChar(c);
		return false;
	}

	private int peekTemplateLiteralChar() throws IOException {
		int c = getTemplateLiteralChar();
		ungetTemplateLiteralChar(c);
		return c;
	}

	int readTemplateLiteral() throws IOException {
		rawString.setLength(0);
		stringBufferTop = 0;
		while (true) {
			int c = getTemplateLiteralChar();
			switch (c) {
				case EOF_CHAR:
					this.string = getStringFromBuffer();
					tokenEnd = cursor - 1; // restore tokenEnd
					parser.reportError("msg.unexpected.eof");
					return Token.ERROR;
				case '`':
					rawString.setLength(rawString.length() - 1); // don't include "`"
					this.string = getStringFromBuffer();
					return Token.TEMPLATE_LITERAL;
				case '$':
					if (matchTemplateLiteralChar('{')) {
						rawString.setLength(rawString.length() - 2); // don't include "${"
						this.string = getStringFromBuffer();
						this.tokenEnd = cursor - 1; // don't include "{"
						return Token.TEMPLATE_LITERAL_SUBST;
					} else {
						addToString(c);
						break;
					}
				case '\\':
					// LineContinuation ::
					//   \ LineTerminatorSequence
					// EscapeSequence ::
					//   CharacterEscapeSequence
					//   0 [LA not DecimalDigit]
					//   HexEscapeSequence
					//   UnicodeEscapeSequence
					// CharacterEscapeSequence ::
					//   SingleEscapeCharacter
					//   NonEscapeCharacter
					// SingleEscapeCharacter ::
					//   ' "  \  b f n r t v
					// NonEscapeCharacter ::
					//   SourceCharacter but not one of EscapeCharacter or LineTerminator
					// EscapeCharacter ::
					//   SingleEscapeCharacter
					//   DecimalDigit
					//   x
					//   u
					c = getTemplateLiteralChar();
					switch (c) {
						case '\r':
							// skip past \r\n sequence
							matchTemplateLiteralChar('\n');
							continue;
						case '\n':
						case '\u2028':
						case '\u2029':
							continue;
						case '\'':
						case '"':
						case '\\':
							// use as-is
							break;
						case 'b':
							c = '\b';
							break;
						case 'f':
							c = '\f';
							break;
						case 'n':
							c = '\n';
							break;
						case 'r':
							c = '\r';
							break;
						case 't':
							c = '\t';
							break;
						case 'v':
							c = 0xb;
							break;
						case 'x': {
							int escapeVal = 0;
							escapeVal = Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
							escapeVal = Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
							if (escapeVal < 0) {
								parser.reportError("msg.syntax");
								return Token.ERROR;
							}
							c = escapeVal;
							break;
						}
						case 'u': {
							int escapeVal = 0;
							c = getTemplateLiteralChar();
							if (c == '{') {
								c = getTemplateLiteralChar();
								do {
									escapeVal = Kit.xDigitToInt(c, escapeVal);
									if (escapeVal < 0 || escapeVal > 0x10FFFF) {
										parser.reportError("msg.syntax");
										return Token.ERROR;
									}
								} while ((c = getTemplateLiteralChar()) != '}');
								if (escapeVal > 0xFFFF) {
									addToString(Character.highSurrogate(escapeVal));
									addToString(Character.lowSurrogate(escapeVal));
									continue;
								}
								c = escapeVal;
								break;
							}
							escapeVal = Kit.xDigitToInt(c, escapeVal);
							escapeVal = Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
							escapeVal = Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
							escapeVal = Kit.xDigitToInt(getTemplateLiteralChar(), escapeVal);
							if (escapeVal < 0) {
								parser.reportError("msg.syntax");
								return Token.ERROR;
							}
							c = escapeVal;
							break;
						}
						case '0': {
							int d = peekTemplateLiteralChar();
							if (d >= '0' && d <= '9') {
								parser.reportError("msg.syntax");
								return Token.ERROR;
							}
							c = 0x00;
							break;
						}
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							parser.reportError("msg.syntax");
							return Token.ERROR;
						default:
							// use as-is
							break;
					}
					addToString(c);
					break;
				default:
					addToString(c);
					break;
			}
		}
	}

	private String getStringFromBuffer() {
		tokenEnd = cursor;
		return new String(stringBuffer, 0, stringBufferTop);
	}

	private void addToString(int c) {
		int N = stringBufferTop;
		if (N == stringBuffer.length) {
			char[] tmp = new char[stringBuffer.length * 2];
			System.arraycopy(stringBuffer, 0, tmp, 0, N);
			stringBuffer = tmp;
		}
		stringBuffer[N] = (char) c;
		stringBufferTop = N + 1;
	}

	private boolean canUngetChar() {
		return ungetCursor == 0 || ungetBuffer[ungetCursor - 1] != '\n';
	}

	private void ungetChar(int c) {
		// can not unread past across line boundary
		if (ungetCursor != 0 && ungetBuffer[ungetCursor - 1] == '\n') {
			Kit.codeBug();
		}
		ungetBuffer[ungetCursor++] = c;
		cursor--;
	}

	private boolean matchChar(int test) throws IOException {
		int c = getCharIgnoreLineEnd();
		if (c == test) {
			tokenEnd = cursor;
			return true;
		}
		ungetCharIgnoreLineEnd(c);
		return false;
	}

	private int peekChar() throws IOException {
		int c = getChar();
		ungetChar(c);
		return c;
	}

	private int getChar() throws IOException {
		return getChar(true);
	}

	private int getChar(boolean skipFormattingChars) throws IOException {
		if (ungetCursor != 0) {
			cursor++;
			return ungetBuffer[--ungetCursor];
		}

		for (; ; ) {
			int c;
			if (sourceCursor == sourceEnd) {
				hitEOF = true;
				return EOF_CHAR;
			}
			cursor++;
			c = sourceString.charAt(sourceCursor++);

			if (lineEndChar >= 0) {
				if (lineEndChar == '\r' && c == '\n') {
					lineEndChar = '\n';
					continue;
				}
				lineEndChar = -1;
				lineStart = sourceCursor - 1;
				lineno++;
			}

			if (c <= 127) {
				if (c == '\n' || c == '\r') {
					lineEndChar = c;
					c = '\n';
				}
			} else {
				if (c == BYTE_ORDER_MARK) {
					return c; // BOM is considered whitespace
				}
				if (skipFormattingChars && isJSFormatChar(c)) {
					continue;
				}
				if (ScriptRuntime.isJSLineTerminator(c)) {
					lineEndChar = c;
					c = '\n';
				}
			}
			return c;
		}
	}

	private int getCharIgnoreLineEnd() throws IOException {
		return getCharIgnoreLineEnd(true);
	}

	private int getCharIgnoreLineEnd(boolean skipFormattingChars) throws IOException {
		if (ungetCursor != 0) {
			cursor++;
			return ungetBuffer[--ungetCursor];
		}

		for (; ; ) {
			int c;
			if (sourceCursor == sourceEnd) {
				hitEOF = true;
				return EOF_CHAR;
			}
			cursor++;
			c = sourceString.charAt(sourceCursor++);

			if (c <= 127) {
				if (c == '\n' || c == '\r') {
					lineEndChar = c;
					c = '\n';
				}
			} else {
				if (c == BYTE_ORDER_MARK) {
					return c; // BOM is considered whitespace
				}
				if (skipFormattingChars && isJSFormatChar(c)) {
					continue;
				}
				if (ScriptRuntime.isJSLineTerminator(c)) {
					lineEndChar = c;
					c = '\n';
				}
			}
			return c;
		}
	}

	private void ungetCharIgnoreLineEnd(int c) {
		ungetBuffer[ungetCursor++] = c;
		cursor--;
	}

	private void skipLine() throws IOException {
		// skip to end of line
		int c;
		while ((c = getChar()) != EOF_CHAR && c != '\n') {
		}
		ungetChar(c);
		tokenEnd = cursor;
	}

	/**
	 * Returns the offset into the current line.
	 */
	final int getOffset() {
		int n = sourceCursor - lineStart;
		if (lineEndChar >= 0) {
			--n;
		}
		return n;
	}

	private int charAt(int index) {
		return index < 0 || index >= sourceEnd ? EOF_CHAR : sourceString.charAt(index);
	}

	private String substring(int beginIndex, int endIndex) {
		return sourceString.substring(beginIndex, endIndex);
	}

	final String getLine() {
		int lineEnd = sourceCursor;
		if (lineEndChar >= 0) {
			// move cursor before newline sequence
			lineEnd -= 1;
			if (lineEndChar == '\n' && charAt(lineEnd - 1) == '\r') {
				lineEnd -= 1;
			}
		} else {
			// Read until the end of line
			int lineLength = lineEnd - lineStart;
			for (; ; ++lineLength) {
				int c = charAt(lineStart + lineLength);
				if (c == EOF_CHAR || ScriptRuntime.isJSLineTerminator(c)) {
					break;
				}
			}
			lineEnd = lineStart + lineLength;
		}
		return substring(lineStart, lineEnd);
	}

	final String getLine(int position, int[] linep) {
		assert position >= 0 && position <= cursor;
		assert linep.length == 2;
		int delta = (cursor + ungetCursor) - position;
		int cur = sourceCursor;
		if (delta > cur) {
			// requested line outside of source buffer
			return null;
		}
		// read back until position
		int end = 0, lines = 0;
		for (; delta > 0; --delta, --cur) {
			assert cur > 0;
			int c = charAt(cur - 1);
			if (ScriptRuntime.isJSLineTerminator(c)) {
				if (c == '\n' && charAt(cur - 2) == '\r') {
					// \r\n sequence
					delta -= 1;
					cur -= 1;
				}
				lines += 1;
				end = cur - 1;
			}
		}
		// read back until line start
		int start = 0, offset = 0;
		for (; cur > 0; --cur, ++offset) {
			int c = charAt(cur - 1);
			if (ScriptRuntime.isJSLineTerminator(c)) {
				start = cur;
				break;
			}
		}
		linep[0] = lineno - lines + (lineEndChar >= 0 ? 1 : 0);
		linep[1] = offset;
		if (lines == 0) {
			return getLine();
		}
		return substring(start, end);
	}

	/**
	 * Return the current position of the scanner cursor.
	 */
	public int getCursor() {
		return cursor;
	}

	/**
	 * Return the absolute source offset of the last scanned token.
	 */
	public int getTokenBeg() {
		return tokenBeg;
	}

	/**
	 * Return the absolute source end-offset of the last scanned token.
	 */
	public int getTokenEnd() {
		return tokenEnd;
	}

	/**
	 * Return tokenEnd - tokenBeg
	 */
	public int getTokenLength() {
		return tokenEnd - tokenBeg;
	}

	/**
	 * Return the type of the last scanned comment.
	 *
	 * @return type of last scanned comment, or 0 if none have been scanned.
	 */
	public Token.CommentType getCommentType() {
		return commentType;
	}

	private void markCommentStart() {
		markCommentStart("");
	}

	private void markCommentStart(String prefix) {
	}

	private static String convertLastCharToHex(String str) {
		int lastIndex = str.length() - 1;
		StringBuilder buf = new StringBuilder(str.substring(0, lastIndex));
		buf.append("\\u");
		String hexCode = Integer.toHexString(str.charAt(lastIndex));
		for (int i = 0; i < 4 - hexCode.length(); ++i) {
			buf.append('0');
		}
		buf.append(hexCode);
		return buf.toString();
	}

	// stuff other than whitespace since start of line
	private boolean dirtyLine;

	String regExpFlags;

	// Set this to an initial non-null value so that the Parser has
	// something to retrieve even if an error has occurred and no
	// string is found.  Fosters one class of error, but saves lots of
	// code.
	private String string = "";
	private double number;
	private boolean isBinary;
	private boolean isOldOctal;
	private boolean isOctal;
	private boolean isHex;

	// delimiter for last string literal scanned
	private int quoteChar;

	private char[] stringBuffer = new char[128];
	private int stringBufferTop;
	private final ObjToIntMap allStrings = new ObjToIntMap(50);

	// Room to backtrace from to < on failed match of the last - in <!--
	private final int[] ungetBuffer = new int[3];
	private int ungetCursor;

	private boolean hitEOF = false;

	private int lineStart = 0;
	private int lineEndChar = -1;
	int lineno;

	private final String sourceString;
	private final int sourceEnd;

	// sourceCursor is an index into a small buffer that keeps a
	// sliding window of the source stream.
	int sourceCursor;

	// cursor is a monotonically increasing index into the original
	// source stream, tracking exactly how far scanning has progressed.
	// Its value is the index of the next character to be scanned.
	int cursor;

	// Record start and end positions of last scanned token.
	int tokenBeg;
	int tokenEnd;

	// Type of last comment scanned.
	Token.CommentType commentType;

	private final Parser parser;
}

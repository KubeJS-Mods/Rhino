/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

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

public interface Token {
	enum CommentType {
		LINE, BLOCK_COMMENT, JSDOC, HTML
	}

	/**
	 * Token types.  These values correspond to JSTokenType values in
	 * jsscan.c.
	 */

	// start enum
	int ERROR = -1; // well-known as the only code < EOF
	int EOF = 0;  // end of file token - (not EOF_CHAR)
	int EOL = 1;  // end of line

	// Interpreter reuses the following as bytecodes
	int FIRST_BYTECODE_TOKEN = 2;

	int ENTERWITH = 2;
	int LEAVEWITH = 3;
	int RETURN = 4;
	int GOTO = 5;
	int IFEQ = 6;
	int IFNE = 7;
	int SETNAME = 8;
	int BITOR = 9;
	int BITXOR = 10;
	int BITAND = 11;
	int EQ = 12;
	int NE = 13;
	int LT = 14;
	int LE = 15;
	int GT = 16;
	int GE = 17;
	int LSH = 18;
	int RSH = 19;
	int URSH = 20;
	int ADD = 21;
	int SUB = 22;
	int MUL = 23;
	int DIV = 24;
	int MOD = 25;
	int NOT = 26;
	int BITNOT = 27;
	int POS = 28;
	int NEG = 29;
	int NEW = 30;
	int DELPROP = 31;
	int TYPEOF = 32;
	int GETPROP = 33;
	int GETPROPNOWARN = 34;
	int SETPROP = 35;
	int GETELEM = 36;
	int SETELEM = 37;
	int CALL = 38;
	int NAME = 39;
	int NUMBER = 40;
	int STRING = 41;
	int NULL = 42;
	int THIS = 43;
	int FALSE = 44;
	int TRUE = 45;
	int SHEQ = 46;   // shallow equality (===)
	int SHNE = 47;   // shallow inequality (!==)
	int REGEXP = 48;
	int BINDNAME = 49;
	int THROW = 50;
	int RETHROW = 51; // rethrow caught exception: catch (e if ) use it
	int IN = 52;
	int INSTANCEOF = 53;
	int LOCAL_LOAD = 54;
	int GETVAR = 55;
	int SETVAR = 56;
	int CATCH_SCOPE = 57;
	int ENUM_INIT_KEYS = 58;
	int ENUM_INIT_VALUES = 59;
	int ENUM_INIT_ARRAY = 60;
	int ENUM_INIT_VALUES_IN_ORDER = 61;
	int ENUM_NEXT = 62;
	int ENUM_ID = 63;
	int THISFN = 64;
	int RETURN_RESULT = 65; // to return previously stored return result
	int ARRAYLIT = 66; // array literal
	int OBJECTLIT = 67; // object literal
	int GET_REF = 68; // *reference
	int SET_REF = 69; // *reference    = something
	int DEL_REF = 70; // delete reference
	int REF_CALL = 71; // f(args)    = something or f(args)++
	int REF_SPECIAL = 72; // reference for special properties like __proto
	int YIELD = 73;  // JS 1.7 yield pseudo keyword
	int STRICT_SETNAME = 74;
	int NULLISH_COALESCING = 75; // nullish coalescing operator (??)
	int POW = 76; // power (**)
	int OPTIONAL_CHAINING = 77; // optional chaining operator (?.)
	int GETOPTIONAL = 78;

	// End of interpreter bytecodes
	int LAST_BYTECODE_TOKEN = 81;

	int TRY = 82;
	int SEMI = 83;  // semicolon
	int LB = 84;  // left and right brackets
	int RB = 85;
	int LC = 86;  // left and right curlies (braces)
	int RC = 87;
	int LP = 88;  // left and right parentheses
	int RP = 89;
	int COMMA = 90;  // comma operator

	int ASSIGN = 91;  // simple assignment  (=)
	int ASSIGN_BITOR = 92;  // |=
	int ASSIGN_BITXOR = 93;  // ^=
	int ASSIGN_BITAND = 94;  // |=
	int ASSIGN_LSH = 95;  // <<=
	int ASSIGN_RSH = 96;  // >>=
	int ASSIGN_URSH = 97;  // >>>=
	int ASSIGN_ADD = 98;  // +=
	int ASSIGN_SUB = 99;  // -=
	int ASSIGN_MUL = 100;  // *=
	int ASSIGN_DIV = 101;  // /=
	int ASSIGN_MOD = 102;  // %=

	int FIRST_ASSIGN = ASSIGN;
	int LAST_ASSIGN = ASSIGN_MOD;

	int HOOK = 103; // conditional (?:)
	int COLON = 104;
	int OR = 105; // logical or (||)
	int AND = 106; // logical and (&&)
	int INC = 107; // increment/decrement (++ --)
	int DEC = 108;
	int DOT = 109; // member operator (.)
	int FUNCTION = 110; // function keyword
	int EXPORT = 111; // export keyword
	int IMPORT = 112; // import keyword
	int IF = 113; // if keyword
	int ELSE = 114; // else keyword
	int SWITCH = 115; // switch keyword
	int CASE = 116; // case keyword
	int DEFAULT = 117; // default keyword
	int WHILE = 118; // while keyword
	int DO = 119; // do keyword
	int FOR = 120; // for keyword
	int BREAK = 121; // break keyword
	int CONTINUE = 122; // continue keyword
	int VAR = 123; // var keyword
	int WITH = 124; // with keyword
	int CATCH = 125; // catch keyword
	int FINALLY = 126; // finally keyword
	int VOID = 127; // void keyword
	int RESERVED = 128; // reserved keywords

	int EMPTY = 129;

	// types used for the parse tree - these never get returned  by the scanner.
	int BLOCK = 130; // statement block
	int LABEL = 131; // label
	int TARGET = 132;
	int LOOP = 133;
	int EXPR_VOID = 134; // expression statement in functions
	int EXPR_RESULT = 135; // expression statement in scripts
	int JSR = 136;
	int SCRIPT = 137; // top-level node for entire script
	int TYPEOFNAME = 138; // for typeof(simple-name)
	int USE_STACK = 139;
	int SETPROP_OP = 140; // x.y op= something
	int SETELEM_OP = 141; // x[y] op= something
	int LOCAL_BLOCK = 142;
	int SET_REF_OP = 143; // *reference op= something

	// Optimizer-only-tokens
	int TO_OBJECT = 150;
	int TO_DOUBLE = 151;

	int GET = 152;  // JS 1.5 get pseudo keyword
	int SET = 153;  // JS 1.5 set pseudo keyword
	int LET = 154;  // JS 1.7 let pseudo keyword
	int CONST = 155;
	int SETCONST = 156;
	int SETCONSTVAR = 157;
	int ARRAYCOMP = 158;  // array comprehension
	int LETEXPR = 159;
	int WITHEXPR = 160;
	// int DEBUGGER = 161;
	int COMMENT = 162;
	int GENEXPR = 163;
	int METHOD = 164;  // ES6 MethodDefinition
	int ARROW = 165;  // ES6 ArrowFunction
	int YIELD_STAR = 166;  // ES6 "yield *", a specialization of yield
	int TEMPLATE_LITERAL = 167;  // template literal
	int TEMPLATE_CHARS = 168;  // template literal - literal section
	int TEMPLATE_LITERAL_SUBST = 169;  // template literal - substitution
	int TAGGED_TEMPLATE_LITERAL = 170;  // template literal - tagged/handler

	int LAST_TOKEN = TAGGED_TEMPLATE_LITERAL;


	/**
	 * Returns a name for the token.  If Rhino is compiled with certain
	 * hardcoded debugging flags in this file, it calls {@code #typeToName};
	 * otherwise it returns a string whose value is the token number.
	 */
	static String name(int token) {
		return String.valueOf(token);
	}

	/**
	 * Always returns a human-readable string for the token name.
	 * For instance, {@link #FINALLY} has the name "FINALLY".
	 *
	 * @param token the token code
	 * @return the actual name for the token code
	 */
	static String typeToName(int token) {
		return switch (token) {
			case ERROR -> "ERROR";
			case EOF -> "EOF";
			case EOL -> "EOL";
			case ENTERWITH -> "ENTERWITH";
			case LEAVEWITH -> "LEAVEWITH";
			case RETURN -> "RETURN";
			case GOTO -> "GOTO";
			case IFEQ -> "IFEQ";
			case IFNE -> "IFNE";
			case SETNAME -> "SETNAME";
			case BITOR -> "BITOR";
			case BITXOR -> "BITXOR";
			case BITAND -> "BITAND";
			case EQ -> "EQ";
			case NE -> "NE";
			case LT -> "LT";
			case LE -> "LE";
			case GT -> "GT";
			case GE -> "GE";
			case LSH -> "LSH";
			case RSH -> "RSH";
			case URSH -> "URSH";
			case ADD -> "ADD";
			case SUB -> "SUB";
			case MUL -> "MUL";
			case DIV -> "DIV";
			case MOD -> "MOD";
			case NOT -> "NOT";
			case BITNOT -> "BITNOT";
			case POS -> "POS";
			case NEG -> "NEG";
			case NEW -> "NEW";
			case DELPROP -> "DELPROP";
			case TYPEOF -> "TYPEOF";
			case GETPROP -> "GETPROP";
			case GETPROPNOWARN -> "GETPROPNOWARN";
			case SETPROP -> "SETPROP";
			case GETELEM -> "GETELEM";
			case SETELEM -> "SETELEM";
			case CALL -> "CALL";
			case NAME -> "NAME";
			case NUMBER -> "NUMBER";
			case STRING -> "STRING";
			case NULL -> "NULL";
			case THIS -> "THIS";
			case FALSE -> "FALSE";
			case TRUE -> "TRUE";
			case SHEQ -> "SHEQ";
			case SHNE -> "SHNE";
			case REGEXP -> "REGEXP";
			case BINDNAME -> "BINDNAME";
			case THROW -> "THROW";
			case RETHROW -> "RETHROW";
			case IN -> "IN";
			case INSTANCEOF -> "INSTANCEOF";
			case LOCAL_LOAD -> "LOCAL_LOAD";
			case GETVAR -> "GETVAR";
			case SETVAR -> "SETVAR";
			case CATCH_SCOPE -> "CATCH_SCOPE";
			case ENUM_INIT_KEYS -> "ENUM_INIT_KEYS";
			case ENUM_INIT_VALUES -> "ENUM_INIT_VALUES";
			case ENUM_INIT_ARRAY -> "ENUM_INIT_ARRAY";
			case ENUM_INIT_VALUES_IN_ORDER -> "ENUM_INIT_VALUES_IN_ORDER";
			case ENUM_NEXT -> "ENUM_NEXT";
			case ENUM_ID -> "ENUM_ID";
			case THISFN -> "THISFN";
			case RETURN_RESULT -> "RETURN_RESULT";
			case ARRAYLIT -> "ARRAYLIT";
			case OBJECTLIT -> "OBJECTLIT";
			case GET_REF -> "GET_REF";
			case SET_REF -> "SET_REF";
			case DEL_REF -> "DEL_REF";
			case REF_CALL -> "REF_CALL";
			case REF_SPECIAL -> "REF_SPECIAL";
			case TRY -> "TRY";
			case SEMI -> "SEMI";
			case LB -> "LB";
			case RB -> "RB";
			case LC -> "LC";
			case RC -> "RC";
			case LP -> "LP";
			case RP -> "RP";
			case COMMA -> "COMMA";
			case ASSIGN -> "ASSIGN";
			case ASSIGN_BITOR -> "ASSIGN_BITOR";
			case ASSIGN_BITXOR -> "ASSIGN_BITXOR";
			case ASSIGN_BITAND -> "ASSIGN_BITAND";
			case ASSIGN_LSH -> "ASSIGN_LSH";
			case ASSIGN_RSH -> "ASSIGN_RSH";
			case ASSIGN_URSH -> "ASSIGN_URSH";
			case ASSIGN_ADD -> "ASSIGN_ADD";
			case ASSIGN_SUB -> "ASSIGN_SUB";
			case ASSIGN_MUL -> "ASSIGN_MUL";
			case ASSIGN_DIV -> "ASSIGN_DIV";
			case ASSIGN_MOD -> "ASSIGN_MOD";
			case HOOK -> "HOOK";
			case COLON -> "COLON";
			case OR -> "OR";
			case AND -> "AND";
			case INC -> "INC";
			case DEC -> "DEC";
			case DOT -> "DOT";
			case FUNCTION -> "FUNCTION";
			case EXPORT -> "EXPORT";
			case IMPORT -> "IMPORT";
			case IF -> "IF";
			case ELSE -> "ELSE";
			case SWITCH -> "SWITCH";
			case CASE -> "CASE";
			case DEFAULT -> "DEFAULT";
			case WHILE -> "WHILE";
			case DO -> "DO";
			case FOR -> "FOR";
			case BREAK -> "BREAK";
			case CONTINUE -> "CONTINUE";
			case VAR -> "VAR";
			case WITH -> "WITH";
			case CATCH -> "CATCH";
			case FINALLY -> "FINALLY";
			case VOID -> "VOID";
			case RESERVED -> "RESERVED";
			case EMPTY -> "EMPTY";
			case BLOCK -> "BLOCK";
			case LABEL -> "LABEL";
			case TARGET -> "TARGET";
			case LOOP -> "LOOP";
			case EXPR_VOID -> "EXPR_VOID";
			case EXPR_RESULT -> "EXPR_RESULT";
			case JSR -> "JSR";
			case SCRIPT -> "SCRIPT";
			case TYPEOFNAME -> "TYPEOFNAME";
			case USE_STACK -> "USE_STACK";
			case SETPROP_OP -> "SETPROP_OP";
			case SETELEM_OP -> "SETELEM_OP";
			case LOCAL_BLOCK -> "LOCAL_BLOCK";
			case SET_REF_OP -> "SET_REF_OP";
			case TO_OBJECT -> "TO_OBJECT";
			case TO_DOUBLE -> "TO_DOUBLE";
			case GET -> "GET";
			case SET -> "SET";
			case LET -> "LET";
			case YIELD -> "YIELD";
			case CONST -> "CONST";
			case SETCONST -> "SETCONST";
			case ARRAYCOMP -> "ARRAYCOMP";
			case WITHEXPR -> "WITHEXPR";
			case LETEXPR -> "LETEXPR";
			case COMMENT -> "COMMENT";
			case GENEXPR -> "GENEXPR";
			case METHOD -> "METHOD";
			case ARROW -> "ARROW";
			case YIELD_STAR -> "YIELD_STAR";
			case TEMPLATE_LITERAL -> "TEMPLATE_LITERAL";
			case TEMPLATE_CHARS -> "TEMPLATE_CHARS";
			case TEMPLATE_LITERAL_SUBST -> "TEMPLATE_LITERAL_SUBST";
			case TAGGED_TEMPLATE_LITERAL -> "TAGGED_TEMPLATE_LITERAL";
			case NULLISH_COALESCING -> "NULLISH_COALESCING";
			case POW -> "POW";
			case OPTIONAL_CHAINING -> "OPTIONAL_CHAINING";
			case GETOPTIONAL -> "GETOPTIONAL";
			default -> throw new IllegalStateException(String.valueOf(token));
		};
	}

	/**
	 * Return true if the passed code is a valid Token constant.
	 *
	 * @param code a potential token code
	 * @return true if it's a known token
	 */
	static boolean isValidToken(int code) {
		return code >= ERROR && code <= LAST_TOKEN;
	}
}

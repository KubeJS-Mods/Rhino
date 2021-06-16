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

	// debug flags
	boolean printTrees = false;
	boolean printICode = false;
	boolean printNames = printTrees || printICode;

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

	// End of interpreter bytecodes
	int LAST_BYTECODE_TOKEN = STRICT_SETNAME;

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
	int DEBUGGER = 161;
	int COMMENT = 162;
	int GENEXPR = 163;
	int METHOD = 164;  // ES6 MethodDefinition
	int ARROW = 165;  // ES6 ArrowFunction
	int YIELD_STAR = 166;  // ES6 "yield *", a specialization of yield
	int TEMPLATE_LITERAL = 167;  // template literal
	int TEMPLATE_CHARS = 168;  // template literal - literal section
	int TEMPLATE_LITERAL_SUBST = 169;  // template literal - substitution
	int TAGGED_TEMPLATE_LITERAL = 170;  // template literal - tagged/handler
	int LAST_TOKEN = 170;


	/**
	 * Returns a name for the token.  If Rhino is compiled with certain
	 * hardcoded debugging flags in this file, it calls {@code #typeToName};
	 * otherwise it returns a string whose value is the token number.
	 */
	static String name(int token) {
		if (!printNames) {
			return String.valueOf(token);
		}
		return typeToName(token);
	}

	/**
	 * Always returns a human-readable string for the token name.
	 * For instance, {@link #FINALLY} has the name "FINALLY".
	 *
	 * @param token the token code
	 * @return the actual name for the token code
	 */
	static String typeToName(int token) {
		switch (token) {
			case ERROR:
				return "ERROR";
			case EOF:
				return "EOF";
			case EOL:
				return "EOL";
			case ENTERWITH:
				return "ENTERWITH";
			case LEAVEWITH:
				return "LEAVEWITH";
			case RETURN:
				return "RETURN";
			case GOTO:
				return "GOTO";
			case IFEQ:
				return "IFEQ";
			case IFNE:
				return "IFNE";
			case SETNAME:
				return "SETNAME";
			case BITOR:
				return "BITOR";
			case BITXOR:
				return "BITXOR";
			case BITAND:
				return "BITAND";
			case EQ:
				return "EQ";
			case NE:
				return "NE";
			case LT:
				return "LT";
			case LE:
				return "LE";
			case GT:
				return "GT";
			case GE:
				return "GE";
			case LSH:
				return "LSH";
			case RSH:
				return "RSH";
			case URSH:
				return "URSH";
			case ADD:
				return "ADD";
			case SUB:
				return "SUB";
			case MUL:
				return "MUL";
			case DIV:
				return "DIV";
			case MOD:
				return "MOD";
			case NOT:
				return "NOT";
			case BITNOT:
				return "BITNOT";
			case POS:
				return "POS";
			case NEG:
				return "NEG";
			case NEW:
				return "NEW";
			case DELPROP:
				return "DELPROP";
			case TYPEOF:
				return "TYPEOF";
			case GETPROP:
				return "GETPROP";
			case GETPROPNOWARN:
				return "GETPROPNOWARN";
			case SETPROP:
				return "SETPROP";
			case GETELEM:
				return "GETELEM";
			case SETELEM:
				return "SETELEM";
			case CALL:
				return "CALL";
			case NAME:
				return "NAME";
			case NUMBER:
				return "NUMBER";
			case STRING:
				return "STRING";
			case NULL:
				return "NULL";
			case THIS:
				return "THIS";
			case FALSE:
				return "FALSE";
			case TRUE:
				return "TRUE";
			case SHEQ:
				return "SHEQ";
			case SHNE:
				return "SHNE";
			case REGEXP:
				return "REGEXP";
			case BINDNAME:
				return "BINDNAME";
			case THROW:
				return "THROW";
			case RETHROW:
				return "RETHROW";
			case IN:
				return "IN";
			case INSTANCEOF:
				return "INSTANCEOF";
			case LOCAL_LOAD:
				return "LOCAL_LOAD";
			case GETVAR:
				return "GETVAR";
			case SETVAR:
				return "SETVAR";
			case CATCH_SCOPE:
				return "CATCH_SCOPE";
			case ENUM_INIT_KEYS:
				return "ENUM_INIT_KEYS";
			case ENUM_INIT_VALUES:
				return "ENUM_INIT_VALUES";
			case ENUM_INIT_ARRAY:
				return "ENUM_INIT_ARRAY";
			case ENUM_INIT_VALUES_IN_ORDER:
				return "ENUM_INIT_VALUES_IN_ORDER";
			case ENUM_NEXT:
				return "ENUM_NEXT";
			case ENUM_ID:
				return "ENUM_ID";
			case THISFN:
				return "THISFN";
			case RETURN_RESULT:
				return "RETURN_RESULT";
			case ARRAYLIT:
				return "ARRAYLIT";
			case OBJECTLIT:
				return "OBJECTLIT";
			case GET_REF:
				return "GET_REF";
			case SET_REF:
				return "SET_REF";
			case DEL_REF:
				return "DEL_REF";
			case REF_CALL:
				return "REF_CALL";
			case REF_SPECIAL:
				return "REF_SPECIAL";
			case TRY:
				return "TRY";
			case SEMI:
				return "SEMI";
			case LB:
				return "LB";
			case RB:
				return "RB";
			case LC:
				return "LC";
			case RC:
				return "RC";
			case LP:
				return "LP";
			case RP:
				return "RP";
			case COMMA:
				return "COMMA";
			case ASSIGN:
				return "ASSIGN";
			case ASSIGN_BITOR:
				return "ASSIGN_BITOR";
			case ASSIGN_BITXOR:
				return "ASSIGN_BITXOR";
			case ASSIGN_BITAND:
				return "ASSIGN_BITAND";
			case ASSIGN_LSH:
				return "ASSIGN_LSH";
			case ASSIGN_RSH:
				return "ASSIGN_RSH";
			case ASSIGN_URSH:
				return "ASSIGN_URSH";
			case ASSIGN_ADD:
				return "ASSIGN_ADD";
			case ASSIGN_SUB:
				return "ASSIGN_SUB";
			case ASSIGN_MUL:
				return "ASSIGN_MUL";
			case ASSIGN_DIV:
				return "ASSIGN_DIV";
			case ASSIGN_MOD:
				return "ASSIGN_MOD";
			case HOOK:
				return "HOOK";
			case COLON:
				return "COLON";
			case OR:
				return "OR";
			case AND:
				return "AND";
			case INC:
				return "INC";
			case DEC:
				return "DEC";
			case DOT:
				return "DOT";
			case FUNCTION:
				return "FUNCTION";
			case EXPORT:
				return "EXPORT";
			case IMPORT:
				return "IMPORT";
			case IF:
				return "IF";
			case ELSE:
				return "ELSE";
			case SWITCH:
				return "SWITCH";
			case CASE:
				return "CASE";
			case DEFAULT:
				return "DEFAULT";
			case WHILE:
				return "WHILE";
			case DO:
				return "DO";
			case FOR:
				return "FOR";
			case BREAK:
				return "BREAK";
			case CONTINUE:
				return "CONTINUE";
			case VAR:
				return "VAR";
			case WITH:
				return "WITH";
			case CATCH:
				return "CATCH";
			case FINALLY:
				return "FINALLY";
			case VOID:
				return "VOID";
			case RESERVED:
				return "RESERVED";
			case EMPTY:
				return "EMPTY";
			case BLOCK:
				return "BLOCK";
			case LABEL:
				return "LABEL";
			case TARGET:
				return "TARGET";
			case LOOP:
				return "LOOP";
			case EXPR_VOID:
				return "EXPR_VOID";
			case EXPR_RESULT:
				return "EXPR_RESULT";
			case JSR:
				return "JSR";
			case SCRIPT:
				return "SCRIPT";
			case TYPEOFNAME:
				return "TYPEOFNAME";
			case USE_STACK:
				return "USE_STACK";
			case SETPROP_OP:
				return "SETPROP_OP";
			case SETELEM_OP:
				return "SETELEM_OP";
			case LOCAL_BLOCK:
				return "LOCAL_BLOCK";
			case SET_REF_OP:
				return "SET_REF_OP";
			case TO_OBJECT:
				return "TO_OBJECT";
			case TO_DOUBLE:
				return "TO_DOUBLE";
			case GET:
				return "GET";
			case SET:
				return "SET";
			case LET:
				return "LET";
			case YIELD:
				return "YIELD";
			case CONST:
				return "CONST";
			case SETCONST:
				return "SETCONST";
			case ARRAYCOMP:
				return "ARRAYCOMP";
			case WITHEXPR:
				return "WITHEXPR";
			case LETEXPR:
				return "LETEXPR";
			case DEBUGGER:
				return "DEBUGGER";
			case COMMENT:
				return "COMMENT";
			case GENEXPR:
				return "GENEXPR";
			case METHOD:
				return "METHOD";
			case ARROW:
				return "ARROW";
			case YIELD_STAR:
				return "YIELD_STAR";
			case TEMPLATE_LITERAL:
				return "TEMPLATE_LITERAL";
			case TEMPLATE_CHARS:
				return "TEMPLATE_CHARS";
			case TEMPLATE_LITERAL_SUBST:
				return "TEMPLATE_LITERAL_SUBST";
			case TAGGED_TEMPLATE_LITERAL:
				return "TAGGED_TEMPLATE_LITERAL";
		}

		// Token without name
		throw new IllegalStateException(String.valueOf(token));
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

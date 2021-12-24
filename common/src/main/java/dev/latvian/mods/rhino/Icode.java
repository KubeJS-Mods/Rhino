/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * Additional interpreter-specific codes
 */
abstract class Icode {

	static final int

			// delete operator used on a name
			Icode_DELNAME = 0,

	// Stack: ... value1 -> ... value1 value1
	Icode_DUP = -1,

	// Stack: ... value2 value1 -> ... value2 value1 value2 value1
	Icode_DUP2 = -2,

	// Stack: ... value2 value1 -> ... value1 value2
	Icode_SWAP = -3,

	// Stack: ... value1 -> ...
	Icode_POP = -4,

	// Store stack top into return register and then pop it
	Icode_POP_RESULT = -5,

	// To jump conditionally and pop additional stack value
	Icode_IFEQ_POP = -6,

	// various types of ++/--
	Icode_VAR_INC_DEC = -7, Icode_NAME_INC_DEC = -8, Icode_PROP_INC_DEC = -9, Icode_ELEM_INC_DEC = -10, Icode_REF_INC_DEC = -11,

	// load/save scope from/to local
	Icode_SCOPE_LOAD = -12, Icode_SCOPE_SAVE = -13,

	Icode_TYPEOFNAME = -14,

	// helper for function calls
	Icode_NAME_AND_THIS = -15, Icode_PROP_AND_THIS = -16, Icode_ELEM_AND_THIS = -17, Icode_VALUE_AND_THIS = -18,

	// Create closure object for nested functions
	Icode_CLOSURE_EXPR = -19, Icode_CLOSURE_STMT = -20,

	// Special calls
	Icode_CALLSPECIAL = -21,

	// To return undefined value
	Icode_RETUNDEF = -22,

	// Exception handling implementation
	Icode_GOSUB = -23, Icode_STARTSUB = -24, Icode_RETSUB = -25,

	// To indicating a line number change in icodes.
	Icode_LINE = -26,

	// To store shorts and ints inline
	Icode_SHORTNUMBER = -27, Icode_INTNUMBER = -28,

	// To create and populate array to hold values for [] and {} literals
	Icode_LITERAL_NEW = -29, Icode_LITERAL_SET = -30,

	// Array literal with skipped index like [1,,2]
	Icode_SPARE_ARRAYLIT = -31,

	// Load index register to prepare for the following index operation
	Icode_REG_IND_C0 = -32, Icode_REG_IND_C1 = -33, Icode_REG_IND_C2 = -34, Icode_REG_IND_C3 = -35, Icode_REG_IND_C4 = -36, Icode_REG_IND_C5 = -37, Icode_REG_IND1 = -38, Icode_REG_IND2 = -39, Icode_REG_IND4 = -40,

	// Load string register to prepare for the following string operation
	Icode_REG_STR_C0 = -41, Icode_REG_STR_C1 = -42, Icode_REG_STR_C2 = -43, Icode_REG_STR_C3 = -44, Icode_REG_STR1 = -45, Icode_REG_STR2 = -46, Icode_REG_STR4 = -47,

	// Version of getvar/setvar that read var index directly from bytecode
	Icode_GETVAR1 = -48, Icode_SETVAR1 = -49,

	// Load undefined
	Icode_UNDEF = -50, Icode_ZERO = -51, Icode_ONE = -52,

	// entrance and exit from .()
	Icode_ENTERDQ = -53, Icode_LEAVEDQ = -54,

	Icode_TAIL_CALL = -55,

	// Clear local to allow GC its context
	Icode_LOCAL_CLEAR = -56,

	// Literal get/set
	Icode_LITERAL_GETTER = -57, Icode_LITERAL_SETTER = -58,

	// const
	Icode_SETCONST = -59, Icode_SETCONSTVAR = -60, Icode_SETCONSTVAR1 = -61,

	// Generator opcodes (along with Token.YIELD)
	Icode_GENERATOR = -62, Icode_GENERATOR_END = -63,

	// Icode_DEBUGGER = -64,

	Icode_GENERATOR_RETURN = -65, Icode_YIELD_STAR = -66,

	// Call to GetTemplateLiteralCallSite
	Icode_TEMPLATE_LITERAL_CALLSITE = -67,

	// Last icode
	MIN_ICODE = -67;

	static String bytecodeName(int bytecode) {
		if (!validBytecode(bytecode)) {
			throw new IllegalArgumentException(String.valueOf(bytecode));
		}

		if (!Token.printICode) {
			return String.valueOf(bytecode);
		}

		if (validTokenCode(bytecode)) {
			return Token.name(bytecode);
		}

		return switch (bytecode) {
			case Icode_DUP -> "DUP";
			case Icode_DUP2 -> "DUP2";
			case Icode_SWAP -> "SWAP";
			case Icode_POP -> "POP";
			case Icode_POP_RESULT -> "POP_RESULT";
			case Icode_IFEQ_POP -> "IFEQ_POP";
			case Icode_VAR_INC_DEC -> "VAR_INC_DEC";
			case Icode_NAME_INC_DEC -> "NAME_INC_DEC";
			case Icode_PROP_INC_DEC -> "PROP_INC_DEC";
			case Icode_ELEM_INC_DEC -> "ELEM_INC_DEC";
			case Icode_REF_INC_DEC -> "REF_INC_DEC";
			case Icode_SCOPE_LOAD -> "SCOPE_LOAD";
			case Icode_SCOPE_SAVE -> "SCOPE_SAVE";
			case Icode_TYPEOFNAME -> "TYPEOFNAME";
			case Icode_NAME_AND_THIS -> "NAME_AND_THIS";
			case Icode_PROP_AND_THIS -> "PROP_AND_THIS";
			case Icode_ELEM_AND_THIS -> "ELEM_AND_THIS";
			case Icode_VALUE_AND_THIS -> "VALUE_AND_THIS";
			case Icode_CLOSURE_EXPR -> "CLOSURE_EXPR";
			case Icode_CLOSURE_STMT -> "CLOSURE_STMT";
			case Icode_CALLSPECIAL -> "CALLSPECIAL";
			case Icode_RETUNDEF -> "RETUNDEF";
			case Icode_GOSUB -> "GOSUB";
			case Icode_STARTSUB -> "STARTSUB";
			case Icode_RETSUB -> "RETSUB";
			case Icode_LINE -> "LINE";
			case Icode_SHORTNUMBER -> "SHORTNUMBER";
			case Icode_INTNUMBER -> "INTNUMBER";
			case Icode_LITERAL_NEW -> "LITERAL_NEW";
			case Icode_LITERAL_SET -> "LITERAL_SET";
			case Icode_SPARE_ARRAYLIT -> "SPARE_ARRAYLIT";
			case Icode_REG_IND_C0 -> "REG_IND_C0";
			case Icode_REG_IND_C1 -> "REG_IND_C1";
			case Icode_REG_IND_C2 -> "REG_IND_C2";
			case Icode_REG_IND_C3 -> "REG_IND_C3";
			case Icode_REG_IND_C4 -> "REG_IND_C4";
			case Icode_REG_IND_C5 -> "REG_IND_C5";
			case Icode_REG_IND1 -> "LOAD_IND1";
			case Icode_REG_IND2 -> "LOAD_IND2";
			case Icode_REG_IND4 -> "LOAD_IND4";
			case Icode_REG_STR_C0 -> "REG_STR_C0";
			case Icode_REG_STR_C1 -> "REG_STR_C1";
			case Icode_REG_STR_C2 -> "REG_STR_C2";
			case Icode_REG_STR_C3 -> "REG_STR_C3";
			case Icode_REG_STR1 -> "LOAD_STR1";
			case Icode_REG_STR2 -> "LOAD_STR2";
			case Icode_REG_STR4 -> "LOAD_STR4";
			case Icode_GETVAR1 -> "GETVAR1";
			case Icode_SETVAR1 -> "SETVAR1";
			case Icode_UNDEF -> "UNDEF";
			case Icode_ZERO -> "ZERO";
			case Icode_ONE -> "ONE";
			case Icode_ENTERDQ -> "ENTERDQ";
			case Icode_LEAVEDQ -> "LEAVEDQ";
			case Icode_TAIL_CALL -> "TAIL_CALL";
			case Icode_LOCAL_CLEAR -> "LOCAL_CLEAR";
			case Icode_LITERAL_GETTER -> "LITERAL_GETTER";
			case Icode_LITERAL_SETTER -> "LITERAL_SETTER";
			case Icode_SETCONST -> "SETCONST";
			case Icode_SETCONSTVAR -> "SETCONSTVAR";
			case Icode_SETCONSTVAR1 -> "SETCONSTVAR1";
			case Icode_GENERATOR -> "GENERATOR";
			case Icode_GENERATOR_END -> "GENERATOR_END";
			case Icode_GENERATOR_RETURN -> "GENERATOR_RETURN";
			case Icode_YIELD_STAR -> "YIELD_STAR";
			case Icode_TEMPLATE_LITERAL_CALLSITE -> "TEMPLATE_LITERAL_CALLSITE";
			default -> throw new IllegalStateException(String.valueOf(bytecode));
		};
	}

	static boolean validIcode(int icode) {
		return MIN_ICODE <= icode && icode <= 0;
	}

	static boolean validTokenCode(int token) {
		return Token.FIRST_BYTECODE_TOKEN <= token && token <= Token.LAST_BYTECODE_TOKEN;
	}

	static boolean validBytecode(int bytecode) {
		return validIcode(bytecode) || validTokenCode(bytecode);
	}
}

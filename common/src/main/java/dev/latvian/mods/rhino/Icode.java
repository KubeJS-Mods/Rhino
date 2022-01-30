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
			Icode_DELNAME = 0;

	static final int// Stack: ... value1 -> ... value1 value1
			Icode_DUP = -1;

	static final int// Stack: ... value2 value1 -> ... value2 value1 value2 value1
			Icode_DUP2 = -2;

	static final int// Stack: ... value2 value1 -> ... value1 value2
			Icode_SWAP = -3;

	static final int// Stack: ... value1 -> ...
			Icode_POP = -4;

	static final int// Store stack top into return register and then pop it
			Icode_POP_RESULT = -5;

	static final int// To jump conditionally and pop additional stack value
			Icode_IFEQ_POP = -6;

	static final int// various types of ++/--
			Icode_VAR_INC_DEC = -7;
	static final int Icode_NAME_INC_DEC = -8;
	static final int Icode_PROP_INC_DEC = -9;
	static final int Icode_ELEM_INC_DEC = -10;
	static final int Icode_REF_INC_DEC = -11;

	static final int// load/save scope from/to local
			Icode_SCOPE_LOAD = -12;
	static final int Icode_SCOPE_SAVE = -13;

	static final int Icode_TYPEOFNAME = -14;

	static final int// helper for function calls
			Icode_NAME_AND_THIS = -15;
	static final int Icode_PROP_AND_THIS = -16;
	static final int Icode_ELEM_AND_THIS = -17;
	static final int Icode_VALUE_AND_THIS = -18;

	static final int// Create closure object for nested functions
			Icode_CLOSURE_EXPR = -19;
	static final int Icode_CLOSURE_STMT = -20;

	static final int// Special calls
			Icode_CALLSPECIAL = -21;

	static final int// To return undefined value
			Icode_RETUNDEF = -22;

	static final int// Exception handling implementation
			Icode_GOSUB = -23;
	static final int Icode_STARTSUB = -24;
	static final int Icode_RETSUB = -25;

	static final int// To indicating a line number change in icodes.
			Icode_LINE = -26;

	static final int// To store shorts and ints inline
			Icode_SHORTNUMBER = -27;
	static final int Icode_INTNUMBER = -28;

	static final int// To create and populate array to hold values for [] and {} literals
			Icode_LITERAL_NEW = -29;
	static final int Icode_LITERAL_SET = -30;

	static final int// Array literal with skipped index like [1,,2]
			Icode_SPARE_ARRAYLIT = -31;

	static final int// Load index register to prepare for the following index operation
			Icode_REG_IND_C0 = -32;
	static final int Icode_REG_IND_C1 = -33;
	static final int Icode_REG_IND_C2 = -34;
	static final int Icode_REG_IND_C3 = -35;
	static final int Icode_REG_IND_C4 = -36;
	static final int Icode_REG_IND_C5 = -37;
	static final int Icode_REG_IND1 = -38;
	static final int Icode_REG_IND2 = -39;
	static final int Icode_REG_IND4 = -40;

	static final int// Load string register to prepare for the following string operation
			Icode_REG_STR_C0 = -41;
	static final int Icode_REG_STR_C1 = -42;
	static final int Icode_REG_STR_C2 = -43;
	static final int Icode_REG_STR_C3 = -44;
	static final int Icode_REG_STR1 = -45;
	static final int Icode_REG_STR2 = -46;
	static final int Icode_REG_STR4 = -47;

	static final int// Version of getvar/setvar that read var index directly from bytecode
			Icode_GETVAR1 = -48;
	static final int Icode_SETVAR1 = -49;

	static final int// Load undefined
			Icode_UNDEF = -50;
	static final int Icode_ZERO = -51;
	static final int Icode_ONE = -52;

	static final int// entrance and exit from .()
			Icode_ENTERDQ = -53;
	static final int Icode_LEAVEDQ = -54;

	static final int Icode_TAIL_CALL = -55;

	static final int// Clear local to allow GC its context
			Icode_LOCAL_CLEAR = -56;

	static final int// Literal get/set
			Icode_LITERAL_GETTER = -57;
	static final int Icode_LITERAL_SETTER = -58;

	static final int// const
			Icode_SETCONST = -59;
	static final int Icode_SETCONSTVAR = -60;
	static final int Icode_SETCONSTVAR1 = -61;

	static final int// Generator opcodes (along with Token.YIELD)
			Icode_GENERATOR = -62;
	static final int Icode_GENERATOR_END = -63;

	// Icode_DEBUGGER = -64,

	static final int Icode_GENERATOR_RETURN = -65;
	static final int Icode_YIELD_STAR = -66;

	static final int// Call to GetTemplateLiteralCallSite
			Icode_TEMPLATE_LITERAL_CALLSITE = -67;

	static final int// Last icode
			MIN_ICODE = -67;

	static boolean validIcode(int icode) {
		return MIN_ICODE <= icode && icode <= 0;
	}

	static boolean validTokenCode(int token) {
		return Token.FIRST_BYTECODE_TOKEN <= token && token <= Token.LAST_BYTECODE_TOKEN;
	}
}

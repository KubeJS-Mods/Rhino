/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import java.util.Arrays;

final class InterpreterData {
	static final int INITIAL_MAX_ICODE_LENGTH = 1024;
	static final int INITIAL_STRINGTABLE_SIZE = 64;
	static final int INITIAL_NUMBERTABLE_SIZE = 64;
	String itsName;
	String itsSourceFile;
	boolean itsNeedsActivation;
	int itsFunctionType;
	String[] itsStringTable;
	double[] itsDoubleTable;
	InterpreterData[] itsNestedFunctions;
	Object[] itsRegExpLiterals;
	Object[] itsTemplateLiterals;
	byte[] itsICode;
	int[] itsExceptionTable;
	int itsMaxVars;
	int itsMaxLocals;
	int itsMaxStack;
	int itsMaxFrameArray;
	// see comments in NativeFunction for definition of argNames and argCount
	String[] argNames;
	boolean[] argIsConst;
	int argCount;
	int itsMaxCalleeArgs;
	boolean isStrict;
	boolean topLevel;
	boolean isES6Generator;
	Object[] literalIds;
	UintMap longJumps;
	int firstLinePC = -1; // PC for the first LINE icode
	InterpreterData parentData;
	boolean evalScriptFlag; // true if script corresponds to eval() code
	/**
	 * true if the function has been declared like "var foo = function() {...}"
	 */
	boolean declaredAsVar;
	/**
	 * true if the function has been declared like "!function() {}".
	 */
	boolean declaredAsFunctionExpression;
	private int icodeHashCode = 0;

	InterpreterData(String sourceFile, boolean isStrict) {
		this.itsSourceFile = sourceFile;
		this.isStrict = isStrict;
		init();
	}

	InterpreterData(InterpreterData parent) {
		this.parentData = parent;
		this.itsSourceFile = parent.itsSourceFile;
		this.isStrict = parent.isStrict;
		init();
	}

	private void init() {
		itsICode = new byte[INITIAL_MAX_ICODE_LENGTH];
		itsStringTable = new String[INITIAL_STRINGTABLE_SIZE];
	}

	public String getFunctionName() {
		return itsName;
	}

	public int getParamAndVarCount() {
		return argNames.length;
	}

	public boolean getParamOrVarConst(int index) {
		return argIsConst[index];
	}

	public int getFunctionCount() {
		return (itsNestedFunctions == null) ? 0 : itsNestedFunctions.length;
	}

	public InterpreterData getFunction(int index) {
		return itsNestedFunctions[index];
	}

	public InterpreterData getParent() {
		return parentData;
	}

	public int icodeHashCode() {
		int h = icodeHashCode;
		if (h == 0) {
			icodeHashCode = h = Arrays.hashCode(itsICode);
		}
		return h;
	}
}

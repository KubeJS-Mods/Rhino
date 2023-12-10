/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.ScriptNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Interpreter extends Icode implements Evaluator {
	static final int EXCEPTION_TRY_START_SLOT = 0;
	static final int EXCEPTION_TRY_END_SLOT = 1;
	static final int EXCEPTION_HANDLER_SLOT = 2;
	static final int EXCEPTION_TYPE_SLOT = 3;
	static final int EXCEPTION_LOCAL_SLOT = 4;
	static final int EXCEPTION_SCOPE_SLOT = 5;
	// SLOT_SIZE: space for try start/end, handler, start, handler type,
	//            exception local and scope local
	static final int EXCEPTION_SLOT_SIZE = 6;

	static {
		// Checks for byte code consistencies, good compiler can eliminate them

		if (Token.LAST_BYTECODE_TOKEN > 127) {
			String str = "Violation of Token.LAST_BYTECODE_TOKEN <= 127";
			System.err.println(str);
			throw new IllegalStateException(str);
		}
		if (MIN_ICODE < -128) {
			String str = "Violation of Interpreter.MIN_ICODE >= -128";
			System.err.println(str);
			throw new IllegalStateException(str);
		}
	}

	/**
	 * Class to hold data corresponding to one interpreted call stack frame.
	 */
	private static class CallFrame implements Cloneable {
		// fields marked "final" in a comment are effectively final except when they're modified immediately after cloning.

		private static Boolean equals(CallFrame f1, CallFrame f2, EqualObjectGraphs equal, Context cx) {
			// Iterative instead of recursive, as interpreter stack depth can
			// be larger than JVM stack depth.
			for (; ; ) {
				if (f1 == f2) {
					return Boolean.TRUE;
				} else if (f1 == null || f2 == null) {
					return Boolean.FALSE;
				} else if (!f1.fieldsEqual(f2, equal, cx)) {
					return Boolean.FALSE;
				} else {
					f1 = f1.parentFrame;
					f2 = f2.parentFrame;
				}
			}
		}

		final Context localContext;
		final InterpretedFunction fnOrScript;
		final InterpreterData idata;
		final CallFrame varSource; // defaults to this unless continuation frame
		final int localShift;

		// Stack structure
		// stack[0 <= i < localShift]: arguments and local variables
		// stack[localShift <= i <= emptyStackTop]: used for local temporaries
		// stack[emptyStackTop < i < stack.length]: stack data
		// sDbl[i]: if stack[i] is UniqueTag.DOUBLE_MARK, sDbl[i] holds the number value
		final int emptyStackTop;
		final boolean useActivation;
		final Scriptable thisObj;
		/*final*/ CallFrame parentFrame;
		// amount of stack frames before this one on the interpretation stack
		/*final*/ int frameIndex;
		// If true indicates read-only frame that is a part of continuation
		boolean frozen;
		/*final*/ Object[] stack;
		/*final*/ int[] stackAttributes;
		/*final*/ double[] sDbl;

		// The values that change during interpretation
		boolean isContinuationsTopFrame;
		Object result;
		double resultDbl;
		int pc;
		int pcPrevBranch;
		int pcSourceLineStart;
		Scriptable scope;
		int savedStackTop;
		int savedCallOp;
		Object throwable;

		CallFrame(Context cx, Scriptable thisObj, InterpretedFunction fnOrScript, CallFrame parentFrame) {
			localContext = cx;
			idata = fnOrScript.idata;

			useActivation = idata.itsNeedsActivation;

			emptyStackTop = idata.itsMaxVars + idata.itsMaxLocals - 1;
			this.fnOrScript = fnOrScript;
			varSource = this;
			localShift = idata.itsMaxVars;
			this.thisObj = thisObj;

			this.parentFrame = parentFrame;
			frameIndex = (parentFrame == null) ? 0 : parentFrame.frameIndex + 1;
			if (frameIndex > cx.getMaximumInterpreterStackDepth()) {
				throw Context.reportRuntimeError("Exceeded maximum stack depth", cx);
			}

			// Initialize initial values of variables that change during
			// interpretation.
			result = Undefined.instance;
			pcSourceLineStart = idata.firstLinePC;

			savedStackTop = emptyStackTop;
		}

		void initializeArgs(Context cx, Scriptable callerScope, Object[] args, double[] argsDbl, int argShift, int argCount) {
			if (useActivation) {
				// Copy args to new array to pass to enterActivationFunction
				// or debuggerFrame.onEnter
				if (argsDbl != null) {
					args = getArgsArray(args, argsDbl, argShift, argCount);
				}
				argShift = 0;
				argsDbl = null;
			}

			if (idata.itsFunctionType != 0) {
				scope = fnOrScript.getParentScope();

				if (useActivation) {
					if (idata.itsFunctionType == FunctionNode.ARROW_FUNCTION) {
						scope = ScriptRuntime.createArrowFunctionActivation(cx, scope, fnOrScript, args, idata.isStrict);
					} else {
						scope = ScriptRuntime.createFunctionActivation(cx, scope, fnOrScript, args, idata.isStrict);
					}
				}
			} else {
				scope = callerScope;
				ScriptRuntime.initScript(cx, scope, fnOrScript, thisObj, fnOrScript.idata.evalScriptFlag);
			}

			if (idata.itsNestedFunctions != null) {
				if (idata.itsFunctionType != 0 && !idata.itsNeedsActivation) {
					Kit.codeBug();
				}
				for (int i = 0; i < idata.itsNestedFunctions.length; i++) {
					InterpreterData fdata = idata.itsNestedFunctions[i];
					if (fdata.itsFunctionType == FunctionNode.FUNCTION_STATEMENT) {
						initFunction(cx, scope, fnOrScript, i);
					}
				}
			}

			final int maxFrameArray = idata.itsMaxFrameArray;
			// TODO: move this check into InterpreterData construction
			if (maxFrameArray != emptyStackTop + idata.itsMaxStack + 1) {
				Kit.codeBug();
			}

			// Initialize args, vars, locals and stack

			stack = new Object[maxFrameArray];
			stackAttributes = new int[maxFrameArray];
			sDbl = new double[maxFrameArray];

			int varCount = idata.getParamAndVarCount();
			for (int i = 0; i < varCount; i++) {
				if (idata.getParamOrVarConst(i)) {
					stackAttributes[i] = ScriptableObject.CONST;
				}
			}
			int definedArgs = idata.argCount;
			if (definedArgs > argCount) {
				definedArgs = argCount;
			}

			// Fill the frame structure

			System.arraycopy(args, argShift, stack, 0, definedArgs);
			if (argsDbl != null) {
				System.arraycopy(argsDbl, argShift, sDbl, 0, definedArgs);
			}
			for (int i = definedArgs; i != idata.itsMaxVars; ++i) {
				stack[i] = Undefined.instance;
			}
		}

		CallFrame cloneFrozen() {
			if (!frozen) {
				Kit.codeBug();
			}

			CallFrame copy;
			try {
				copy = (CallFrame) clone();
			} catch (CloneNotSupportedException ex) {
				throw new IllegalStateException();
			}

			// clone stack but keep varSource to point to values
			// from this frame to share variables.

			copy.stack = stack.clone();
			copy.stackAttributes = stackAttributes.clone();
			copy.sDbl = sDbl.clone();

			copy.frozen = false;
			return copy;
		}

		@Override
		public boolean equals(Object other) {
			// Overridden for semantic equality comparison. These objects
			// are typically exposed as NativeContinuation.implementation,
			// comparing them allows establishing whether the continuations
			// are semantically equal.
			if (other instanceof CallFrame otherCallFrame) {
				// If the call is not within a Context with a top call, we force
				// one. It is required as some objects within fully initialized
				// global scopes (notably, XMLLibImpl) need to have a top scope
				// in order to evaluate their attributes.
				//final Context cx = Context.enter();
				//try {
				if (localContext.hasTopCallScope()) {
					return equalsInTopScope(otherCallFrame);
				}
				final Scriptable top = ScriptableObject.getTopLevelScope(scope);
				return (Boolean) localContext.doTopCall(top, (c, scope, thisObj, args) -> equalsInTopScope(otherCallFrame), top, ScriptRuntime.EMPTY_OBJECTS, isStrictTopFrame());
				//} finally {
				//	Context.exit();
				//}
			}
			return false;
		}

		@Override
		public int hashCode() {
			// Overridden for consistency with equals.
			// Trying to strike a balance between speed of calculation and
			// distribution. Not hashing stack variables as those could have
			// unbounded computational cost and limit it to topmost 8 frames.
			int depth = 0;
			CallFrame f = this;
			int h = 0;
			do {
				h = 31 * (31 * h + f.pc) + f.idata.icodeHashCode();
				f = f.parentFrame;
			} while (f != null && depth++ < 8);
			return h;
		}

		private Boolean equalsInTopScope(CallFrame other) {
			return EqualObjectGraphs.withThreadLocal(eq -> equals(this, other, eq, localContext));
		}

		private boolean isStrictTopFrame() {
			CallFrame f = this;
			for (; ; ) {
				final CallFrame p = f.parentFrame;
				if (p == null) {
					return f.idata.isStrict;
				}
				f = p;
			}
		}

		private boolean fieldsEqual(CallFrame other, EqualObjectGraphs equal, Context cx) {
			return frameIndex == other.frameIndex && pc == other.pc && compareIdata(idata, other.idata) && equal.equalGraphs(cx, varSource.stack, other.varSource.stack) && Arrays.equals(varSource.sDbl, other.varSource.sDbl) && equal.equalGraphs(cx, thisObj, other.thisObj) && equal.equalGraphs(cx, fnOrScript, other.fnOrScript) && equal.equalGraphs(cx, scope, other.scope);
		}
	}

	private static boolean compareIdata(InterpreterData i1, InterpreterData i2) {
		return i1 == i2;
	}

	private static CallFrame captureFrameForGenerator(CallFrame frame) {
		frame.frozen = true;
		CallFrame result = frame.cloneFrozen();
		frame.frozen = false;

		// now isolate this frame from its previous context
		result.parentFrame = null;
		result.frameIndex = 0;

		return result;
	}

	private static int getShort(byte[] iCode, int pc) {
		return (iCode[pc] << 8) | (iCode[pc + 1] & 0xFF);
	}

	private static int getIndex(byte[] iCode, int pc) {
		return ((iCode[pc] & 0xFF) << 8) | (iCode[pc + 1] & 0xFF);
	}

	private static int getInt(byte[] iCode, int pc) {
		return (iCode[pc] << 24) | ((iCode[pc + 1] & 0xFF) << 16) | ((iCode[pc + 2] & 0xFF) << 8) | (iCode[pc + 3] & 0xFF);
	}

	private static int getExceptionHandler(CallFrame frame, boolean onlyFinally) {
		int[] exceptionTable = frame.idata.itsExceptionTable;
		if (exceptionTable == null) {
			// No exception handlers
			return -1;
		}

		// Icode switch in the interpreter increments PC immediately
		// and it is necessary to subtract 1 from the saved PC
		// to point it before the start of the next instruction.
		int pc = frame.pc - 1;

		// OPT: use binary search
		int best = -1, bestStart = 0, bestEnd = 0;
		for (int i = 0; i != exceptionTable.length; i += EXCEPTION_SLOT_SIZE) {
			int start = exceptionTable[i + EXCEPTION_TRY_START_SLOT];
			int end = exceptionTable[i + EXCEPTION_TRY_END_SLOT];
			if (!(start <= pc && pc < end)) {
				continue;
			}
			if (onlyFinally && exceptionTable[i + EXCEPTION_TYPE_SLOT] != 1) {
				continue;
			}
			if (best >= 0) {
				// Since handlers always nest and they never have shared end
				// although they can share start  it is sufficient to compare
				// handlers ends
				if (bestEnd < end) {
					continue;
				}
				// Check the above assumption
				if (bestStart > start) {
					Kit.codeBug(); // should be nested
				}
				if (bestEnd == end) {
					Kit.codeBug();  // no ens sharing
				}
			}
			best = i;
			bestStart = start;
			bestEnd = end;
		}
		return best;
	}

	private static void initFunction(Context cx, Scriptable scope, InterpretedFunction parent, int index) {
		InterpretedFunction fn;
		fn = InterpretedFunction.createFunction(cx, scope, parent, index);
		ScriptRuntime.initFunction(cx, scope, fn, fn.idata.itsFunctionType, parent.idata.evalScriptFlag);
	}

	static Object interpret(InterpretedFunction ifun, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!cx.hasTopCallScope()) {
			Kit.codeBug();
		}

		CallFrame frame = initFrame(cx, scope, thisObj, args, null, 0, args.length, ifun, null);
		frame.isContinuationsTopFrame = cx.isContinuationsTopCall;
		cx.isContinuationsTopCall = false;

		return interpretLoop(cx, frame, null);
	}

	public static Object resumeGenerator(Context cx, Scriptable scope, int operation, Object savedState, Object value) {
		CallFrame frame = (CallFrame) savedState;
		GeneratorState generatorState = new GeneratorState(operation, value);
		if (operation == GeneratorState.GENERATOR_CLOSE) {
			try {
				return interpretLoop(cx, frame, generatorState);
			} catch (RuntimeException e) {
				// Only propagate exceptions other than closingException
				if (e != value) {
					throw e;
				}
			}
			return Undefined.instance;
		}
		Object result = interpretLoop(cx, frame, generatorState);
		if (generatorState.returnedException != null) {
			throw generatorState.returnedException;
		}
		return result;
	}

	private static Object interpretLoop(Context cx, CallFrame frame, Object throwable) {
		// throwable holds exception object to rethrow or catch
		// It is also used for continuation restart in which case
		// it holds ContinuationJump

		final Object DBL_MRK = UniqueTag.DOUBLE_MARK;
		final Object undefined = Undefined.instance;

		final boolean instructionCounting = (cx.instructionThreshold != 0);
		// arbitrary number to add to instructionCount when calling
		// other functions
		final int INVOCATION_COST = 100;
		// arbitrary exception cost for instruction counting
		final int EXCEPTION_COST = 100;

		String stringReg = null;
		int indexReg = -1;

		if (cx.lastInterpreterFrame != null) {
			// save the top frame from the previous interpretLoop
			// invocation on the stack
			if (cx.previousInterpreterInvocations == null) {
				cx.previousInterpreterInvocations = new ObjArray();
			}
			cx.previousInterpreterInvocations.push(cx.lastInterpreterFrame);
		}

		// When restarting continuation throwable is not null and to jump
		// to the code that rewind continuation state indexReg should be set
		// to -1.
		// With the normal call throwable == null and indexReg == -1 allows to
		// catch bugs with using indeReg to access array elements before
		// initializing indexReg.

		GeneratorState generatorState = null;
		if (throwable != null) {
			if (throwable instanceof GeneratorState) {
				generatorState = (GeneratorState) throwable;

				// reestablish this call frame
				enterFrame(cx, frame, ScriptRuntime.EMPTY_OBJECTS, true);
				throwable = null;
			} else {
				// It should be continuation
				Kit.codeBug();
			}
		}

		Object interpreterResult = null;
		double interpreterResultDbl = 0.0;

		StateLoop:
		for (; ; ) {
			withoutExceptions:
			try {

				if (throwable != null) {
					// Need to return both 'frame' and 'throwable' from
					// 'processThrowable', so just added a 'throwable'
					// member in 'frame'.
					frame = processThrowable(cx, throwable, frame, indexReg, instructionCounting);
					throwable = frame.throwable;
					frame.throwable = null;
				} else {
					if (generatorState == null && frame.frozen) {
						Kit.codeBug();
					}
				}

				// Use local variables for constant values in frame
				// for faster access
				Object[] stack = frame.stack;
				double[] sDbl = frame.sDbl;
				Object[] vars = frame.varSource.stack;
				double[] varDbls = frame.varSource.sDbl;
				int[] varAttributes = frame.varSource.stackAttributes;
				byte[] iCode = frame.idata.itsICode;
				String[] strings = frame.idata.itsStringTable;

				// Use local for stackTop as well. Since execption handlers
				// can only exist at statement level where stack is empty,
				// it is necessary to save/restore stackTop only across
				// function calls and normal returns.
				int stackTop = frame.savedStackTop;

				// Store new frame in cx which is used for error reporting etc.
				cx.lastInterpreterFrame = frame;

				Loop:
				for (; ; ) {

					// Exception handler assumes that PC is already incremented
					// pass the instruction start when it searches the
					// exception handler
					int op = iCode[frame.pc++];
					jumplessRun:
					{

						// Back indent to ease implementation reading
						switch (op) {
							case Icode_GENERATOR: {
								if (!frame.frozen) {
									// First time encountering this opcode: create new generator
									// object and return
									frame.pc--; // we want to come back here when we resume
									CallFrame generatorFrame = captureFrameForGenerator(frame);
									generatorFrame.frozen = true;
									frame.result = new ES6Generator(frame.scope, generatorFrame.fnOrScript, generatorFrame, cx);
									break Loop;
								}
								// We are now resuming execution. Fall through to YIELD case.
							}
							// fall through...
							case Token.YIELD:
							case Icode_YIELD_STAR: {
								if (!frame.frozen) {
									return freezeGenerator(cx, frame, stackTop, generatorState, op == Icode_YIELD_STAR);
								}
								Object obj = thawGenerator(cx, frame, stackTop, generatorState, op);
								if (obj != Scriptable.NOT_FOUND) {
									throwable = obj;
									break withoutExceptions;
								}
								continue;
							}
							case Icode_GENERATOR_END: {
								// throw StopIteration
								frame.frozen = true;
								int sourceLine = getIndex(iCode, frame.pc);
								generatorState.returnedException = new JavaScriptException(cx, NativeIterator.getStopIterationObject(frame.scope, cx), frame.idata.itsSourceFile, sourceLine);
								break Loop;
							}
							case Icode_GENERATOR_RETURN: {
								// throw StopIteration with the value of "return"
								frame.frozen = true;
								frame.result = stack[stackTop];
								frame.resultDbl = sDbl[stackTop];
								--stackTop;

								NativeIterator.StopIteration si = new NativeIterator.StopIteration(cx, (frame.result == UniqueTag.DOUBLE_MARK) ? Double.valueOf(frame.resultDbl) : frame.result);

								int sourceLine = getIndex(iCode, frame.pc);
								generatorState.returnedException = new JavaScriptException(cx, si, frame.idata.itsSourceFile, sourceLine);
								break Loop;
							}
							case Token.THROW: {
								Object value = stack[stackTop];
								if (value == DBL_MRK) {
									value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;

								int sourceLine = getIndex(iCode, frame.pc);
								throwable = new JavaScriptException(cx, value, frame.idata.itsSourceFile, sourceLine);
								break withoutExceptions;
							}
							case Token.RETHROW: {
								indexReg += frame.localShift;
								throwable = stack[indexReg];
								break withoutExceptions;
							}
							case Token.GE:
							case Token.LE:
							case Token.GT:
							case Token.LT: {
								stackTop = doCompare(frame, op, stack, sDbl, stackTop, cx);
								continue;
							}
							case Token.IN:
							case Token.INSTANCEOF: {
								stackTop = doInOrInstanceof(cx, op, stack, sDbl, stackTop);
								continue;
							}
							case Token.EQ:
							case Token.NE: {
								--stackTop;
								boolean valBln = doEquals(stack, sDbl, stackTop, cx);
								valBln ^= (op == Token.NE);
								stack[stackTop] = valBln;
								continue;
							}
							case Token.SHEQ:
							case Token.SHNE: {
								--stackTop;
								boolean valBln = doShallowEquals(stack, sDbl, stackTop, cx);
								valBln ^= (op == Token.SHNE);
								stack[stackTop] = valBln;
								continue;
							}
							case Token.IFNE:
								if (stack_boolean(frame, stackTop--, cx)) {
									frame.pc += 2;
									continue;
								}
								break jumplessRun;
							case Token.IFEQ:
								if (!stack_boolean(frame, stackTop--, cx)) {
									frame.pc += 2;
									continue;
								}
								break jumplessRun;
							case Icode_IFEQ_POP:
								if (!stack_boolean(frame, stackTop--, cx)) {
									frame.pc += 2;
									continue;
								}
								stack[stackTop--] = null;
								break jumplessRun;
							case Token.GOTO:
								break jumplessRun;
							case Icode_GOSUB:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = frame.pc + 2;
								break jumplessRun;
							case Icode_STARTSUB:
								if (stackTop == frame.emptyStackTop + 1) {
									// Call from Icode_GOSUB: store return PC address in the local
									indexReg += frame.localShift;
									stack[indexReg] = stack[stackTop];
									sDbl[indexReg] = sDbl[stackTop];
									--stackTop;
								} else {
									// Call from exception handler: exception object is already stored
									// in the local
									if (stackTop != frame.emptyStackTop) {
										Kit.codeBug();
									}
								}
								continue;
							case Icode_RETSUB: {
								// indexReg: local to store return address
								if (instructionCounting) {
									addInstructionCount(cx, frame, 0);
								}
								indexReg += frame.localShift;
								Object value = stack[indexReg];
								if (value != DBL_MRK) {
									// Invocation from exception handler, restore object to rethrow
									throwable = value;
									break withoutExceptions;
								}
								// Normal return from GOSUB
								frame.pc = (int) sDbl[indexReg];
								if (instructionCounting) {
									frame.pcPrevBranch = frame.pc;
								}
								continue;
							}
							case Icode_POP:
								stack[stackTop] = null;
								stackTop--;
								continue;
							case Icode_POP_RESULT:
								frame.result = stack[stackTop];
								frame.resultDbl = sDbl[stackTop];
								stack[stackTop] = null;
								--stackTop;
								continue;
							case Icode_DUP:
								stack[stackTop + 1] = stack[stackTop];
								sDbl[stackTop + 1] = sDbl[stackTop];
								stackTop++;
								continue;
							case Icode_DUP2:
								stack[stackTop + 1] = stack[stackTop - 1];
								sDbl[stackTop + 1] = sDbl[stackTop - 1];
								stack[stackTop + 2] = stack[stackTop];
								sDbl[stackTop + 2] = sDbl[stackTop];
								stackTop += 2;
								continue;
							case Icode_SWAP: {
								Object o = stack[stackTop];
								stack[stackTop] = stack[stackTop - 1];
								stack[stackTop - 1] = o;
								double d = sDbl[stackTop];
								sDbl[stackTop] = sDbl[stackTop - 1];
								sDbl[stackTop - 1] = d;
								continue;
							}
							case Token.RETURN:
								frame.result = stack[stackTop];
								frame.resultDbl = sDbl[stackTop];
								--stackTop;
								break Loop;
							case Token.RETURN_RESULT:
								break Loop;
							case Icode_RETUNDEF:
								frame.result = undefined;
								break Loop;
							case Token.BITNOT: {
								int rIntValue = stack_int32(frame, stackTop, cx);
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = ~rIntValue;
								continue;
							}
							case Token.BITAND:
							case Token.BITOR:
							case Token.BITXOR:
							case Token.LSH:
							case Token.RSH: {
								stackTop = doBitOp(frame, op, stack, sDbl, stackTop, cx);
								continue;
							}
							case Token.NULLISH_COALESCING:
								stackTop = doNullishCoalescing(frame, stack, sDbl, stackTop);
								continue;
							case Token.URSH: {
								double lDbl = stack_double(frame, stackTop - 1, cx);
								int rIntValue = stack_int32(frame, stackTop, cx) & 0x1F;
								stack[--stackTop] = DBL_MRK;
								sDbl[stackTop] = ScriptRuntime.toUint32(lDbl) >>> rIntValue;
								continue;
							}
							case Token.NEG:
							case Token.POS: {
								double rDbl = stack_double(frame, stackTop, cx);
								stack[stackTop] = DBL_MRK;
								if (op == Token.NEG) {
									rDbl = -rDbl;
								}
								sDbl[stackTop] = rDbl;
								continue;
							}
							case Token.ADD:
								--stackTop;
								doAdd(stack, sDbl, stackTop, cx);
								continue;
							case Token.SUB:
							case Token.MUL:
							case Token.DIV:
							case Token.MOD:
							case Token.POW: {
								stackTop = doArithmetic(cx, frame, op, stack, sDbl, stackTop);
								continue;
							}
							case Token.NOT:
								stack[stackTop] = !stack_boolean(frame, stackTop, cx);
								continue;
							case Token.BINDNAME:
								stack[++stackTop] = ScriptRuntime.bind(cx, frame.scope, stringReg);
								continue;
							case Token.STRICT_SETNAME:
							case Token.SETNAME: {
								Object rhs = stack[stackTop];
								if (rhs == DBL_MRK) {
									rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								Scriptable lhs = (Scriptable) stack[stackTop];
								stack[stackTop] = op == Token.SETNAME ? ScriptRuntime.setName(cx, frame.scope, lhs, rhs, stringReg) : ScriptRuntime.strictSetName(cx, frame.scope, lhs, rhs, stringReg);
								continue;
							}
							case Icode_SETCONST: {
								Object rhs = stack[stackTop];
								if (rhs == DBL_MRK) {
									rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								Scriptable lhs = (Scriptable) stack[stackTop];
								stack[stackTop] = ScriptRuntime.setConst(cx, lhs, rhs, stringReg);
								continue;
							}
							case Token.DELPROP:
							case Icode_DELNAME: {
								stackTop = doDelName(cx, frame, op, stack, sDbl, stackTop);
								continue;
							}
							case Token.GETPROPNOWARN: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.getObjectPropNoWarn(cx, frame.scope, lhs, stringReg);
								continue;
							}
							case Token.GETPROP: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.getObjectProp(cx, frame.scope, lhs, stringReg);
								continue;
							}
							case Token.GETOPTIONAL: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.getObjectPropOptional(cx, frame.scope, lhs, stringReg);
								continue;
							}
							case Token.SETPROP: {
								Object rhs = stack[stackTop];
								if (rhs == DBL_MRK) {
									rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.setObjectProp(cx, frame.scope, lhs, stringReg, rhs);
								continue;
							}
							case Icode_PROP_INC_DEC: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.propIncrDecr(cx, frame.scope, lhs, stringReg, iCode[frame.pc]);
								++frame.pc;
								continue;
							}
							case Token.GETELEM: {
								stackTop = doGetElem(cx, frame, stack, sDbl, stackTop);
								continue;
							}
							case Token.SETELEM: {
								stackTop = doSetElem(cx, frame, stack, sDbl, stackTop);
								continue;
							}
							case Icode_ELEM_INC_DEC: {
								stackTop = doElemIncDec(cx, frame, iCode, stack, sDbl, stackTop);
								continue;
							}
							case Token.GET_REF: {
								Ref ref = (Ref) stack[stackTop];
								stack[stackTop] = ScriptRuntime.refGet(cx, ref);
								continue;
							}
							case Token.SET_REF: {
								Object value = stack[stackTop];
								if (value == DBL_MRK) {
									value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								Ref ref = (Ref) stack[stackTop];
								stack[stackTop] = ScriptRuntime.refSet(cx, frame.scope, ref, value);
								continue;
							}
							case Token.DEL_REF: {
								Ref ref = (Ref) stack[stackTop];
								stack[stackTop] = ScriptRuntime.refDel(cx, ref);
								continue;
							}
							case Icode_REF_INC_DEC: {
								Ref ref = (Ref) stack[stackTop];
								stack[stackTop] = ScriptRuntime.refIncrDecr(cx, frame.scope, ref, iCode[frame.pc]);
								++frame.pc;
								continue;
							}
							case Token.LOCAL_LOAD:
								++stackTop;
								indexReg += frame.localShift;
								stack[stackTop] = stack[indexReg];
								sDbl[stackTop] = sDbl[indexReg];
								continue;
							case Icode_LOCAL_CLEAR:
								indexReg += frame.localShift;
								stack[indexReg] = null;
								continue;
							case Icode_NAME_AND_THIS:
								// stringReg: name
								++stackTop;
								stack[stackTop] = ScriptRuntime.getNameFunctionAndThis(cx, frame.scope, stringReg);
								++stackTop;
								stack[stackTop] = cx.lastStoredScriptable();
								continue;
							case Icode_PROP_AND_THIS: {
								Object obj = stack[stackTop];
								if (obj == DBL_MRK) {
									obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								// stringReg: property
								stack[stackTop] = ScriptRuntime.getPropFunctionAndThis(cx, frame.scope, obj, stringReg);
								++stackTop;
								stack[stackTop] = cx.lastStoredScriptable();
								continue;
							}
							case Icode_ELEM_AND_THIS: {
								Object obj = stack[stackTop - 1];
								if (obj == DBL_MRK) {
									obj = ScriptRuntime.wrapNumber(sDbl[stackTop - 1]);
								}
								Object id = stack[stackTop];
								if (id == DBL_MRK) {
									id = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop - 1] = ScriptRuntime.getElemFunctionAndThis(cx, frame.scope, obj, id);
								stack[stackTop] = cx.lastStoredScriptable();
								continue;
							}
							case Icode_VALUE_AND_THIS: {
								Object value = stack[stackTop];
								if (value == DBL_MRK) {
									value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.getValueFunctionAndThis(cx, value);
								++stackTop;
								stack[stackTop] = cx.lastStoredScriptable();
								continue;
							}
							case Icode_CALLSPECIAL: {
								if (instructionCounting) {
									cx.instructionCount += INVOCATION_COST;
								}
								stackTop = doCallSpecial(cx, frame, stack, sDbl, stackTop, iCode, indexReg);
								continue;
							}
							case Token.CALL:
							case Icode_TAIL_CALL:
							case Token.REF_CALL: {
								if (instructionCounting) {
									cx.instructionCount += INVOCATION_COST;
								}
								// stack change: function thisObj arg0 .. argN -> result
								// indexReg: number of arguments
								stackTop -= 1 + indexReg;

								// CALL generation ensures that fun and funThisObj
								// are already Scriptable and Callable objects respectively
								Callable fun = (Callable) stack[stackTop];
								Scriptable funThisObj = (Scriptable) stack[stackTop + 1];
								if (op == Token.REF_CALL) {
									Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 2, indexReg);
									stack[stackTop] = ScriptRuntime.callRef(cx, funThisObj, fun, outArgs);
									continue;
								}
								Scriptable calleeScope = frame.scope;
								if (frame.useActivation) {
									calleeScope = ScriptableObject.getTopLevelScope(frame.scope);
								}
								if (fun instanceof InterpretedFunction ifun) {
									CallFrame callParentFrame = frame;
									if (op == Icode_TAIL_CALL) {
										// In principle tail call can re-use the current
										// frame and its stack arrays but it is hard to
										// do properly. Any exceptions that can legally
										// happen during frame re-initialization including
										// StackOverflowException during innocent looking
										// System.arraycopy may leave the current frame
										// data corrupted leading to undefined behaviour
										// in the catch code bellow that unwinds JS stack
										// on exceptions. Then there is issue about frame release
										// end exceptions there.
										// To avoid frame allocation a released frame
										// can be cached for re-use which would also benefit
										// non-tail calls but it is not clear that this caching
										// would gain in performance due to potentially
										// bad interaction with GC.
										callParentFrame = frame.parentFrame;
										// Release the current frame. See Bug #344501 to see why
										// it is being done here.
										exitFrame(cx, frame, null);
									}
									CallFrame calleeFrame = initFrame(cx, calleeScope, funThisObj, stack, sDbl, stackTop + 2, indexReg, ifun, callParentFrame);
									if (op != Icode_TAIL_CALL) {
										frame.savedStackTop = stackTop;
										frame.savedCallOp = op;
									}
									frame = calleeFrame;
									continue StateLoop;
								}


								if (fun instanceof IdFunctionObject ifun) {
									// Bug 405654 -- make best effort to keep Function.apply and
									// Function.call within this interpreter loop invocation
									if (BaseFunction.isApplyOrCall(ifun)) {
										Callable applyCallable = ScriptRuntime.getCallable(cx, funThisObj);
										if (applyCallable instanceof InterpretedFunction iApplyCallable) {
											frame = initFrameForApplyOrCall(cx, frame, indexReg, stack, sDbl, stackTop, op, calleeScope, ifun, iApplyCallable);
											continue StateLoop;
										}
									}
								}

								// Bug 447697 -- make best effort to keep __noSuchMethod__ within this
								// interpreter loop invocation
								if (fun instanceof ScriptRuntime.NoSuchMethodShim noSuchMethodShim) {
									// get the shim and the actual method
									Callable noSuchMethodMethod = noSuchMethodShim.noSuchMethodMethod;
									// if the method is in fact an InterpretedFunction
									if (noSuchMethodMethod instanceof InterpretedFunction ifun) {
										frame = initFrameForNoSuchMethod(cx, frame, indexReg, stack, sDbl, stackTop, op, funThisObj, calleeScope, noSuchMethodShim, ifun);
										continue StateLoop;
									}
								}

								cx.lastInterpreterFrame = frame;
								frame.savedCallOp = op;
								frame.savedStackTop = stackTop;
								stack[stackTop] = fun.call(cx, calleeScope, funThisObj, getArgsArray(stack, sDbl, stackTop + 2, indexReg));

								continue;
							}
							case Token.NEW: {
								if (instructionCounting) {
									cx.instructionCount += INVOCATION_COST;
								}
								// stack change: function arg0 .. argN -> newResult
								// indexReg: number of arguments
								stackTop -= indexReg;

								Object lhs = stack[stackTop];
								if (lhs instanceof InterpretedFunction f) {
									Scriptable newInstance = f.createObject(cx, frame.scope);
									CallFrame calleeFrame = initFrame(cx, frame.scope, newInstance, stack, sDbl, stackTop + 1, indexReg, f, frame);

									stack[stackTop] = newInstance;
									frame.savedStackTop = stackTop;
									frame.savedCallOp = op;
									frame = calleeFrame;
									continue StateLoop;
								}
								if (!(lhs instanceof Function fun)) {
									if (lhs == DBL_MRK) {
										lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
									}
									throw ScriptRuntime.notFunctionError(cx, lhs);
								}

								Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 1, indexReg);
								stack[stackTop] = fun.construct(cx, frame.scope, outArgs);
								continue;
							}
							case Token.TYPEOF: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.typeof(cx, lhs).toString();
								continue;
							}
							case Icode_TYPEOFNAME:
								stack[++stackTop] = ScriptRuntime.typeofName(cx, frame.scope, stringReg).toString();
								continue;
							case Token.STRING:
								stack[++stackTop] = stringReg;
								continue;
							case Icode_SHORTNUMBER:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = getShort(iCode, frame.pc);
								frame.pc += 2;
								continue;
							case Icode_INTNUMBER:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = getInt(iCode, frame.pc);
								frame.pc += 4;
								continue;
							case Token.NUMBER:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = frame.idata.itsDoubleTable[indexReg];
								continue;
							case Token.NAME:
								stack[++stackTop] = ScriptRuntime.name(cx, frame.scope, stringReg);
								continue;
							case Icode_NAME_INC_DEC:
								stack[++stackTop] = ScriptRuntime.nameIncrDecr(cx, frame.scope, stringReg, iCode[frame.pc]);
								++frame.pc;
								continue;
							case Icode_SETCONSTVAR1:
								indexReg = iCode[frame.pc++];
								// fallthrough
							case Token.SETCONSTVAR:
								stackTop = doSetConstVar(frame, stack, sDbl, stackTop, vars, varDbls, varAttributes, indexReg, cx);
								continue;
							case Icode_SETVAR1:
								indexReg = iCode[frame.pc++];
								// fallthrough
							case Token.SETVAR:
								stackTop = doSetVar(cx, frame, stack, sDbl, stackTop, vars, varDbls, varAttributes, indexReg);
								continue;
							case Icode_GETVAR1:
								indexReg = iCode[frame.pc++];
								// fallthrough
							case Token.GETVAR:
								stackTop = doGetVar(frame, stack, sDbl, stackTop, vars, varDbls, indexReg, cx);
								continue;
							case Icode_VAR_INC_DEC: {
								stackTop = doVarIncDec(cx, frame, stack, sDbl, stackTop, vars, varDbls, varAttributes, indexReg);
								continue;
							}
							case Icode_ZERO:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = 0;
								continue;
							case Icode_ONE:
								++stackTop;
								stack[stackTop] = DBL_MRK;
								sDbl[stackTop] = 1;
								continue;
							case Token.NULL:
								stack[++stackTop] = null;
								continue;
							case Token.THIS:
								stack[++stackTop] = frame.thisObj;
								continue;
							case Token.THISFN:
								stack[++stackTop] = frame.fnOrScript;
								continue;
							case Token.FALSE:
								stack[++stackTop] = Boolean.FALSE;
								continue;
							case Token.TRUE:
								stack[++stackTop] = Boolean.TRUE;
								continue;
							case Icode_UNDEF:
								stack[++stackTop] = undefined;
								continue;
							case Token.ENTERWITH: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								frame.scope = ScriptRuntime.enterWith(cx, frame.scope, lhs);
								continue;
							}
							case Token.LEAVEWITH:
								frame.scope = ScriptRuntime.leaveWith(frame.scope);
								continue;
							case Token.CATCH_SCOPE: {
								// stack top: exception object
								// stringReg: name of exception variable
								// indexReg: local for exception scope
								--stackTop;
								indexReg += frame.localShift;

								boolean afterFirstScope = (frame.idata.itsICode[frame.pc] != 0);
								Throwable caughtException = (Throwable) stack[stackTop + 1];
								Scriptable lastCatchScope;
								if (!afterFirstScope) {
									lastCatchScope = null;
								} else {
									lastCatchScope = (Scriptable) stack[indexReg];
								}
								stack[indexReg] = ScriptRuntime.newCatchScope(cx, frame.scope, caughtException, lastCatchScope, stringReg);
								++frame.pc;
								continue;
							}
							case Token.ENUM_INIT_KEYS:
							case Token.ENUM_INIT_VALUES:
							case Token.ENUM_INIT_ARRAY:
							case Token.ENUM_INIT_VALUES_IN_ORDER: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								indexReg += frame.localShift;
								int enumType = op == Token.ENUM_INIT_KEYS ? ScriptRuntime.ENUMERATE_KEYS : op == Token.ENUM_INIT_VALUES ? ScriptRuntime.ENUMERATE_VALUES : op == Token.ENUM_INIT_VALUES_IN_ORDER ? ScriptRuntime.ENUMERATE_VALUES_IN_ORDER : ScriptRuntime.ENUMERATE_ARRAY;
								stack[indexReg] = ScriptRuntime.enumInit(cx, frame.scope, lhs, enumType);
								continue;
							}
							case Token.ENUM_NEXT:
							case Token.ENUM_ID: {
								indexReg += frame.localShift;
								IdEnumeration val = (IdEnumeration) stack[indexReg];
								++stackTop;
								stack[stackTop] = (op == Token.ENUM_NEXT) ? val.next(cx) : val.getId(cx);
								continue;
							}
							case Token.REF_SPECIAL: {
								//stringReg: name of special property
								Object obj = stack[stackTop];
								if (obj == DBL_MRK) {
									obj = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								stack[stackTop] = ScriptRuntime.specialRef(cx, frame.scope, obj, stringReg);
								continue;
							}
							case Icode_SCOPE_LOAD:
								indexReg += frame.localShift;
								frame.scope = (Scriptable) stack[indexReg];
								continue;
							case Icode_SCOPE_SAVE:
								indexReg += frame.localShift;
								stack[indexReg] = frame.scope;
								continue;
							case Icode_CLOSURE_EXPR:
								InterpretedFunction fn = InterpretedFunction.createFunction(cx, frame.scope, frame.fnOrScript, indexReg);
								if (fn.idata.itsFunctionType == FunctionNode.ARROW_FUNCTION) {
									stack[++stackTop] = new ArrowFunction(cx, frame.scope, fn, frame.thisObj);
								} else {
									stack[++stackTop] = fn;
								}
								continue;
							case Icode_CLOSURE_STMT:
								initFunction(cx, frame.scope, frame.fnOrScript, indexReg);
								continue;
							case Token.REGEXP:
								Object re = frame.idata.itsRegExpLiterals[indexReg];
								stack[++stackTop] = ScriptRuntime.wrapRegExp(cx, frame.scope, re);
								continue;
							case Icode_TEMPLATE_LITERAL_CALLSITE:
								Object[] templateLiterals = frame.idata.itsTemplateLiterals;
								stack[++stackTop] = ScriptRuntime.getTemplateLiteralCallSite(cx, frame.scope, templateLiterals, indexReg);
								continue;
							case Icode_LITERAL_NEW:
								// indexReg: number of values in the literal
								++stackTop;
								stack[stackTop] = new int[indexReg];
								++stackTop;
								stack[stackTop] = new Object[indexReg];
								sDbl[stackTop] = 0;
								continue;
							case Icode_LITERAL_SET: {
								Object value = stack[stackTop];
								if (value == DBL_MRK) {
									value = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								int i = (int) sDbl[stackTop];
								((Object[]) stack[stackTop])[i] = value;
								sDbl[stackTop] = i + 1;
								continue;
							}
							case Icode_LITERAL_GETTER: {
								Object value = stack[stackTop];
								--stackTop;
								int i = (int) sDbl[stackTop];
								((Object[]) stack[stackTop])[i] = value;
								((int[]) stack[stackTop - 1])[i] = -1;
								sDbl[stackTop] = i + 1;
								continue;
							}
							case Icode_LITERAL_SETTER: {
								Object value = stack[stackTop];
								--stackTop;
								int i = (int) sDbl[stackTop];
								((Object[]) stack[stackTop])[i] = value;
								((int[]) stack[stackTop - 1])[i] = +1;
								sDbl[stackTop] = i + 1;
								continue;
							}
							case Token.ARRAYLIT:
							case Icode_SPARE_ARRAYLIT:
							case Token.OBJECTLIT: {
								Object[] data = (Object[]) stack[stackTop];
								--stackTop;
								int[] getterSetters = (int[]) stack[stackTop];
								Object val;
								if (op == Token.OBJECTLIT) {
									Object[] ids = (Object[]) frame.idata.literalIds[indexReg];
									val = ScriptRuntime.newObjectLiteral(cx, frame.scope, ids, data, getterSetters);
								} else {
									int[] skipIndexces = null;
									if (op == Icode_SPARE_ARRAYLIT) {
										skipIndexces = (int[]) frame.idata.literalIds[indexReg];
									}
									val = ScriptRuntime.newArrayLiteral(cx, frame.scope, data, skipIndexces);
								}
								stack[stackTop] = val;
								continue;
							}
							case Icode_ENTERDQ: {
								Object lhs = stack[stackTop];
								if (lhs == DBL_MRK) {
									lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
								}
								--stackTop;
								frame.scope = ScriptRuntime.enterDotQuery(lhs, frame.scope, cx);
								continue;
							}
							case Icode_LEAVEDQ: {
								boolean valBln = stack_boolean(frame, stackTop, cx);
								Object x = ScriptRuntime.updateDotQuery(valBln, frame.scope);
								if (x != null) {
									stack[stackTop] = x;
									frame.scope = ScriptRuntime.leaveDotQuery(frame.scope);
									frame.pc += 2;
									continue;
								}
								// reset stack and PC to code after ENTERDQ
								--stackTop;
								break jumplessRun;
							}
							case Icode_LINE:
								frame.pcSourceLineStart = frame.pc;
								frame.pc += 2;
								continue;
							case Icode_REG_IND_C0:
								indexReg = 0;
								continue;
							case Icode_REG_IND_C1:
								indexReg = 1;
								continue;
							case Icode_REG_IND_C2:
								indexReg = 2;
								continue;
							case Icode_REG_IND_C3:
								indexReg = 3;
								continue;
							case Icode_REG_IND_C4:
								indexReg = 4;
								continue;
							case Icode_REG_IND_C5:
								indexReg = 5;
								continue;
							case Icode_REG_IND1:
								indexReg = 0xFF & iCode[frame.pc];
								++frame.pc;
								continue;
							case Icode_REG_IND2:
								indexReg = getIndex(iCode, frame.pc);
								frame.pc += 2;
								continue;
							case Icode_REG_IND4:
								indexReg = getInt(iCode, frame.pc);
								frame.pc += 4;
								continue;
							case Icode_REG_STR_C0:
								stringReg = strings[0];
								continue;
							case Icode_REG_STR_C1:
								stringReg = strings[1];
								continue;
							case Icode_REG_STR_C2:
								stringReg = strings[2];
								continue;
							case Icode_REG_STR_C3:
								stringReg = strings[3];
								continue;
							case Icode_REG_STR1:
								stringReg = strings[0xFF & iCode[frame.pc]];
								++frame.pc;
								continue;
							case Icode_REG_STR2:
								stringReg = strings[getIndex(iCode, frame.pc)];
								frame.pc += 2;
								continue;
							case Icode_REG_STR4:
								stringReg = strings[getInt(iCode, frame.pc)];
								frame.pc += 4;
								continue;
							default:
								throw new RuntimeException("Unknown icode : " + op + " @ pc : " + (frame.pc - 1));
						}  // end of interpreter switch

					} // end of jumplessRun label block

					// This should be reachable only for jump implementation
					// when pc points to encoded target offset
					if (instructionCounting) {
						addInstructionCount(cx, frame, 2);
					}
					int offset = getShort(iCode, frame.pc);
					if (offset != 0) {
						// -1 accounts for pc pointing to jump opcode + 1
						frame.pc += offset - 1;
					} else {
						frame.pc = frame.idata.longJumps.getExistingInt(frame.pc);
					}
					if (instructionCounting) {
						frame.pcPrevBranch = frame.pc;
					}
					continue;

				} // end of Loop: for

				exitFrame(cx, frame, null);
				interpreterResult = frame.result;
				interpreterResultDbl = frame.resultDbl;
				if (frame.parentFrame != null) {
					frame = frame.parentFrame;
					if (frame.frozen) {
						frame = frame.cloneFrozen();
					}
					setCallResult(frame, interpreterResult, interpreterResultDbl);
					interpreterResult = null; // Help GC
					continue;
				}
				break;

			}  // end of interpreter withoutExceptions: try
			catch (Throwable ex) {
				if (throwable != null) {
					// This is serious bug and it is better to track it ASAP
					ex.printStackTrace(System.err);
					throw new IllegalStateException();
				}
				throwable = ex;
			}

			// This should be reachable only after above catch or from
			// finally when it needs to propagate exception or from
			// explicit throw
			if (throwable == null) {
				Kit.codeBug();
			}

			// Exception type
			final int EX_CATCH_STATE = 2; // Can execute JS catch
			final int EX_FINALLY_STATE = 1; // Can execute JS finally
			final int EX_NO_JS_STATE = 0; // Terminate JS execution

			int exState;

			if (generatorState != null && generatorState.operation == GeneratorState.GENERATOR_CLOSE && throwable == generatorState.value) {
				exState = EX_FINALLY_STATE;
			} else if (throwable instanceof JavaScriptException) {
				exState = EX_CATCH_STATE;
			} else if (throwable instanceof EcmaError) {
				// an offical ECMA error object,
				exState = EX_CATCH_STATE;
			} else if (throwable instanceof EvaluatorException) {
				exState = EX_CATCH_STATE;
			} else if (throwable instanceof RuntimeException) {
				exState = EX_FINALLY_STATE;
			} else if (throwable instanceof Error) {
				exState = EX_NO_JS_STATE;
			} else {
				exState = EX_FINALLY_STATE;
			}

			if (instructionCounting) {
				try {
					addInstructionCount(cx, frame, EXCEPTION_COST);
				} catch (RuntimeException ex) {
					throwable = ex;
					exState = EX_FINALLY_STATE;
				} catch (Error ex) {
					// Error from instruction counting
					//     => unconditionally terminate JS
					throwable = ex;
					exState = EX_NO_JS_STATE;
				}
			}

			for (; ; ) {
				if (exState != EX_NO_JS_STATE) {
					boolean onlyFinally = (exState != EX_CATCH_STATE);
					indexReg = getExceptionHandler(frame, onlyFinally);
					if (indexReg >= 0) {
						// We caught an exception, restart the loop
						// with exception pending the processing at the loop
						// start
						continue StateLoop;
					}
				}
				// No allowed exception handlers in this frame, unwind
				// to parent and try to look there

				exitFrame(cx, frame, throwable);

				frame = frame.parentFrame;
				if (frame == null) {
					break;
				}
			}

			break;

		} // end of StateLoop: for(;;)

		// Do cleanups/restorations before the final return or throw

		if (cx.previousInterpreterInvocations != null && cx.previousInterpreterInvocations.size() != 0) {
			cx.lastInterpreterFrame = cx.previousInterpreterInvocations.pop();
		} else {
			// It was the last interpreter frame on the stack
			cx.lastInterpreterFrame = null;
			// Force GC of the value cx.previousInterpreterInvocations
			cx.previousInterpreterInvocations = null;
		}

		if (throwable != null) {
			if (throwable instanceof RuntimeException) {
				throw (RuntimeException) throwable;
			}
			// Must be instance of Error or code bug
			throw (Error) throwable;
		}

		return (interpreterResult != DBL_MRK) ? interpreterResult : ScriptRuntime.wrapNumber(interpreterResultDbl);
	}

	private static int doInOrInstanceof(Context cx, int op, Object[] stack, double[] sDbl, int stackTop) {
		Object rhs = stack[stackTop];
		if (rhs == UniqueTag.DOUBLE_MARK) {
			rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		--stackTop;
		Object lhs = stack[stackTop];
		if (lhs == UniqueTag.DOUBLE_MARK) {
			lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		boolean valBln;
		if (op == Token.IN) {
			valBln = ScriptRuntime.in(cx, lhs, rhs);
		} else {
			valBln = ScriptRuntime.instanceOf(cx, lhs, rhs);
		}
		stack[stackTop] = valBln;
		return stackTop;
	}

	private static int doCompare(CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop, Context cx) {
		--stackTop;
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		boolean valBln;
		object_compare:
		{
			number_compare:
			{
				double rDbl, lDbl;
				if (rhs == UniqueTag.DOUBLE_MARK) {
					rDbl = sDbl[stackTop + 1];
					lDbl = stack_double(frame, stackTop, cx);
				} else if (lhs == UniqueTag.DOUBLE_MARK) {
					rDbl = ScriptRuntime.toNumber(cx, rhs);
					lDbl = sDbl[stackTop];
				} else {
					break number_compare;
				}
				switch (op) {
					case Token.GE:
						valBln = (lDbl >= rDbl);
						break object_compare;
					case Token.LE:
						valBln = (lDbl <= rDbl);
						break object_compare;
					case Token.GT:
						valBln = (lDbl > rDbl);
						break object_compare;
					case Token.LT:
						valBln = (lDbl < rDbl);
						break object_compare;
					default:
						throw Kit.codeBug();
				}
			}
			valBln = switch (op) {
				case Token.GE -> ScriptRuntime.cmp_LE(cx, rhs, lhs);
				case Token.LE -> ScriptRuntime.cmp_LE(cx, lhs, rhs);
				case Token.GT -> ScriptRuntime.cmp_LT(cx, rhs, lhs);
				case Token.LT -> ScriptRuntime.cmp_LT(cx, lhs, rhs);
				default -> throw Kit.codeBug();
			};
		}
		stack[stackTop] = valBln;
		return stackTop;
	}

	private static int doBitOp(CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop, Context cx) {
		int lIntValue = stack_int32(frame, stackTop - 1, cx);
		int rIntValue = stack_int32(frame, stackTop, cx);
		stack[--stackTop] = UniqueTag.DOUBLE_MARK;
		sDbl[stackTop] = switch (op) {
			case Token.BITAND -> lIntValue & rIntValue;
			case Token.BITOR -> lIntValue | rIntValue;
			case Token.BITXOR -> lIntValue ^ rIntValue;
			case Token.LSH -> lIntValue << rIntValue;
			case Token.RSH -> lIntValue >> rIntValue;
			default -> lIntValue;
		};
		return stackTop;
	}

	private static int doNullishCoalescing(CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
		Object a = frame.stack[stackTop - 1];
		Object b = frame.stack[stackTop];
		stack[--stackTop] = a == null || Undefined.isUndefined(a) ? b : a;
		return stackTop;
	}

	private static int doDelName(Context cx, CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
		Object rhs = stack[stackTop];
		if (rhs == UniqueTag.DOUBLE_MARK) {
			rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		--stackTop;
		Object lhs = stack[stackTop];
		if (lhs == UniqueTag.DOUBLE_MARK) {
			lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		stack[stackTop] = ScriptRuntime.delete(cx, frame.scope, lhs, rhs, op == Icode_DELNAME);
		return stackTop;
	}

	private static int doGetElem(Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
		--stackTop;
		Object lhs = stack[stackTop];
		if (lhs == UniqueTag.DOUBLE_MARK) {
			lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		Object value;
		Object id = stack[stackTop + 1];
		if (id != UniqueTag.DOUBLE_MARK) {
			value = ScriptRuntime.getObjectElem(cx, frame.scope, lhs, id);
		} else {
			double d = sDbl[stackTop + 1];
			value = ScriptRuntime.getObjectIndex(cx, frame.scope, lhs, d);
		}
		stack[stackTop] = value;
		return stackTop;
	}

	private static int doSetElem(Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop) {
		stackTop -= 2;
		Object rhs = stack[stackTop + 2];
		if (rhs == UniqueTag.DOUBLE_MARK) {
			rhs = ScriptRuntime.wrapNumber(sDbl[stackTop + 2]);
		}
		Object lhs = stack[stackTop];
		if (lhs == UniqueTag.DOUBLE_MARK) {
			lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		Object value;
		Object id = stack[stackTop + 1];
		if (id != UniqueTag.DOUBLE_MARK) {
			value = ScriptRuntime.setObjectElem(cx, frame.scope, lhs, id, rhs);
		} else {
			double d = sDbl[stackTop + 1];
			value = ScriptRuntime.setObjectIndex(cx, frame.scope, lhs, d, rhs);
		}
		stack[stackTop] = value;
		return stackTop;
	}

	private static int doElemIncDec(Context cx, CallFrame frame, byte[] iCode, Object[] stack, double[] sDbl, int stackTop) {
		Object rhs = stack[stackTop];
		if (rhs == UniqueTag.DOUBLE_MARK) {
			rhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		--stackTop;
		Object lhs = stack[stackTop];
		if (lhs == UniqueTag.DOUBLE_MARK) {
			lhs = ScriptRuntime.wrapNumber(sDbl[stackTop]);
		}
		stack[stackTop] = ScriptRuntime.elemIncrDecr(cx, lhs, rhs, frame.scope, iCode[frame.pc]);
		++frame.pc;
		return stackTop;
	}

	private static int doCallSpecial(Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop, byte[] iCode, int indexReg) {
		int callType = iCode[frame.pc] & 0xFF;
		boolean isNew = (iCode[frame.pc + 1] != 0);
		int sourceLine = getIndex(iCode, frame.pc + 2);

		// indexReg: number of arguments
		if (isNew) {
			// stack change: function arg0 .. argN -> newResult
			stackTop -= indexReg;

			Object function = stack[stackTop];
			if (function == UniqueTag.DOUBLE_MARK) {
				function = ScriptRuntime.wrapNumber(sDbl[stackTop]);
			}
			Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 1, indexReg);
			stack[stackTop] = ScriptRuntime.newSpecial(cx, frame.scope, function, outArgs, callType);
		} else {
			// stack change: function thisObj arg0 .. argN -> result
			stackTop -= 1 + indexReg;

			// Call code generation ensure that stack here
			// is ... Callable Scriptable
			Scriptable functionThis = (Scriptable) stack[stackTop + 1];
			Callable function = (Callable) stack[stackTop];
			Object[] outArgs = getArgsArray(stack, sDbl, stackTop + 2, indexReg);
			stack[stackTop] = ScriptRuntime.callSpecial(cx, frame.scope, function, functionThis, outArgs, frame.thisObj, callType, frame.idata.itsSourceFile, sourceLine);
		}
		frame.pc += 4;
		return stackTop;
	}

	private static int doSetConstVar(CallFrame frame, Object[] stack, double[] sDbl, int stackTop, Object[] vars, double[] varDbls, int[] varAttributes, int indexReg, Context cx) {
		if (!frame.useActivation) {
			if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
				throw Context.reportRuntimeError1("msg.var.redecl", frame.idata.argNames[indexReg], cx);
			}
			if ((varAttributes[indexReg] & ScriptableObject.UNINITIALIZED_CONST) != 0) {
				vars[indexReg] = stack[stackTop];
				varAttributes[indexReg] &= ~ScriptableObject.UNINITIALIZED_CONST;
				varDbls[indexReg] = sDbl[stackTop];
			}
		} else {
			Object val = stack[stackTop];
			if (val == UniqueTag.DOUBLE_MARK) {
				val = ScriptRuntime.wrapNumber(sDbl[stackTop]);
			}
			String stringReg = frame.idata.argNames[indexReg];
			if (frame.scope instanceof ConstProperties cp) {
				cp.putConst(cx, stringReg, frame.scope, val);
			} else {
				throw Kit.codeBug();
			}
		}
		return stackTop;
	}

	private static int doSetVar(Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop, Object[] vars, double[] varDbls, int[] varAttributes, int indexReg) {
		if (!frame.useActivation) {
			if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
				vars[indexReg] = stack[stackTop];
				varDbls[indexReg] = sDbl[stackTop];
			}
		} else {
			Object val = stack[stackTop];
			if (val == UniqueTag.DOUBLE_MARK) {
				val = ScriptRuntime.wrapNumber(sDbl[stackTop]);
			}
			String stringReg = frame.idata.argNames[indexReg];
			frame.scope.put(cx, stringReg, frame.scope, val);
		}
		return stackTop;
	}

	private static int doGetVar(CallFrame frame, Object[] stack, double[] sDbl, int stackTop, Object[] vars, double[] varDbls, int indexReg, Context cx) {
		++stackTop;
		if (!frame.useActivation) {
			stack[stackTop] = vars[indexReg];
			sDbl[stackTop] = varDbls[indexReg];
		} else {
			String stringReg = frame.idata.argNames[indexReg];
			stack[stackTop] = frame.scope.get(cx, stringReg, frame.scope);
		}
		return stackTop;
	}

	private static int doVarIncDec(Context cx, CallFrame frame, Object[] stack, double[] sDbl, int stackTop, Object[] vars, double[] varDbls, int[] varAttributes, int indexReg) {
		// indexReg : varindex
		++stackTop;
		int incrDecrMask = frame.idata.itsICode[frame.pc];
		if (!frame.useActivation) {
			Object varValue = vars[indexReg];
			double d;
			if (varValue == UniqueTag.DOUBLE_MARK) {
				d = varDbls[indexReg];
			} else {
				d = ScriptRuntime.toNumber(cx, varValue);
			}
			double d2 = ((incrDecrMask & Node.DECR_FLAG) == 0) ? d + 1.0 : d - 1.0;
			boolean post = ((incrDecrMask & Node.POST_FLAG) != 0);
			if ((varAttributes[indexReg] & ScriptableObject.READONLY) == 0) {
				if (varValue != UniqueTag.DOUBLE_MARK) {
					vars[indexReg] = UniqueTag.DOUBLE_MARK;
				}
				varDbls[indexReg] = d2;
				stack[stackTop] = UniqueTag.DOUBLE_MARK;
				sDbl[stackTop] = post ? d : d2;
			} else {
				if (post && varValue != UniqueTag.DOUBLE_MARK) {
					stack[stackTop] = varValue;
				} else {
					stack[stackTop] = UniqueTag.DOUBLE_MARK;
					sDbl[stackTop] = post ? d : d2;
				}
			}
		} else {
			String varName = frame.idata.argNames[indexReg];
			stack[stackTop] = ScriptRuntime.nameIncrDecr(cx, frame.scope, varName, incrDecrMask);
		}
		++frame.pc;
		return stackTop;
	}

	/**
	 * Call __noSuchMethod__.
	 */
	private static CallFrame initFrameForNoSuchMethod(Context cx, CallFrame frame, int indexReg, Object[] stack, double[] sDbl, int stackTop, int op, Scriptable funThisObj, Scriptable calleeScope, ScriptRuntime.NoSuchMethodShim noSuchMethodShim, InterpretedFunction ifun) {
		// create an args array from the stack
		Object[] argsArray = null;
		// exactly like getArgsArray except that the first argument
		// is the method name from the shim
		int shift = stackTop + 2;
		Object[] elements = new Object[indexReg];
		for (int i = 0; i < indexReg; ++i, ++shift) {
			Object val = stack[shift];
			if (val == UniqueTag.DOUBLE_MARK) {
				val = ScriptRuntime.wrapNumber(sDbl[shift]);
			}
			elements[i] = val;
		}
		argsArray = new Object[2];
		argsArray[0] = noSuchMethodShim.methodName;
		argsArray[1] = cx.newArray(calleeScope, elements);

		// exactly the same as if it's a regular InterpretedFunction
		CallFrame callParentFrame = frame;
		if (op == Icode_TAIL_CALL) {
			callParentFrame = frame.parentFrame;
			exitFrame(cx, frame, null);
		}
		// init the frame with the underlying method with the
		// adjusted args array and shim's function
		CallFrame calleeFrame = initFrame(cx, calleeScope, funThisObj, argsArray, null, 0, 2, ifun, callParentFrame);
		if (op != Icode_TAIL_CALL) {
			frame.savedStackTop = stackTop;
			frame.savedCallOp = op;
		}
		return calleeFrame;
	}

	private static boolean doEquals(Object[] stack, double[] sDbl, int stackTop, Context cx) {
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		if (rhs == UniqueTag.DOUBLE_MARK) {
			if (lhs == UniqueTag.DOUBLE_MARK) {
				return (sDbl[stackTop] == sDbl[stackTop + 1]);
			}
			return ScriptRuntime.eqNumber(cx, sDbl[stackTop + 1], lhs);
		}
		if (lhs == UniqueTag.DOUBLE_MARK) {
			return ScriptRuntime.eqNumber(cx, sDbl[stackTop], rhs);
		}
		return ScriptRuntime.eq(cx, lhs, rhs);
	}

	private static boolean doShallowEquals(Object[] stack, double[] sDbl, int stackTop, Context cx) {
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		final Object DBL_MRK = UniqueTag.DOUBLE_MARK;
		double rdbl, ldbl;
		if (rhs == DBL_MRK) {
			rdbl = sDbl[stackTop + 1];
			if (lhs == DBL_MRK) {
				ldbl = sDbl[stackTop];
			} else if (lhs instanceof Number) {
				ldbl = ((Number) lhs).doubleValue();
			} else {
				return false;
			}
		} else if (lhs == DBL_MRK) {
			ldbl = sDbl[stackTop];
			if (rhs instanceof Number) {
				rdbl = ((Number) rhs).doubleValue();
			} else {
				return false;
			}
		} else {
			return ScriptRuntime.shallowEq(cx, lhs, rhs);
		}
		return (ldbl == rdbl);
	}

	private static CallFrame processThrowable(Context cx, Object throwable, CallFrame frame, int indexReg, boolean instructionCounting) {
		// Recovering from exception, indexReg contains
		// the index of handler

		if (indexReg >= 0) {
			// Normal exception handler, transfer
			// control appropriately

			if (frame.frozen) {
				// XXX Deal with exceptios!!!
				frame = frame.cloneFrozen();
			}

			int[] table = frame.idata.itsExceptionTable;

			frame.pc = table[indexReg + EXCEPTION_HANDLER_SLOT];
			if (instructionCounting) {
				frame.pcPrevBranch = frame.pc;
			}

			frame.savedStackTop = frame.emptyStackTop;
			int scopeLocal = frame.localShift + table[indexReg + EXCEPTION_SCOPE_SLOT];
			int exLocal = frame.localShift + table[indexReg + EXCEPTION_LOCAL_SLOT];
			frame.scope = (Scriptable) frame.stack[scopeLocal];
			frame.stack[exLocal] = throwable;

			throwable = null;
		}
		frame.throwable = throwable;
		return frame;
	}

	private static Object freezeGenerator(Context cx, CallFrame frame, int stackTop, GeneratorState generatorState, boolean yieldStar) {
		if (generatorState.operation == GeneratorState.GENERATOR_CLOSE) {
			// Error: no yields when generator is closing
			throw ScriptRuntime.typeError0(cx, "msg.yield.closing");
		}
		// return to our caller (which should be a method of NativeGenerator)
		frame.frozen = true;
		frame.result = frame.stack[stackTop];
		frame.resultDbl = frame.sDbl[stackTop];
		frame.savedStackTop = stackTop;
		frame.pc--; // we want to come back here when we resume
		ScriptRuntime.exitActivationFunction(cx);
		final Object result = (frame.result != UniqueTag.DOUBLE_MARK) ? frame.result : ScriptRuntime.wrapNumber(frame.resultDbl);
		if (yieldStar) {
			return new ES6Generator.YieldStarResult(result);
		}
		return result;
	}

	private static Object thawGenerator(Context cx, CallFrame frame, int stackTop, GeneratorState generatorState, int op) {
		// we are resuming execution
		frame.frozen = false;
		int sourceLine = getIndex(frame.idata.itsICode, frame.pc);
		frame.pc += 2; // skip line number data
		if (generatorState.operation == GeneratorState.GENERATOR_THROW) {
			// processing a call to <generator>.throw(exception): must
			// act as if exception was thrown from resumption point.
			return new JavaScriptException(cx, generatorState.value, frame.idata.itsSourceFile, sourceLine);
		}
		if (generatorState.operation == GeneratorState.GENERATOR_CLOSE) {
			return generatorState.value;
		}
		if (generatorState.operation != dev.latvian.mods.rhino.GeneratorState.GENERATOR_SEND) {
			throw Kit.codeBug();
		}
		if ((op == Token.YIELD) || (op == Icode_YIELD_STAR)) {
			frame.stack[stackTop] = generatorState.value;
		}
		return Scriptable.NOT_FOUND;
	}

	private static CallFrame initFrameForApplyOrCall(Context cx, CallFrame frame, int indexReg, Object[] stack, double[] sDbl, int stackTop, int op, Scriptable calleeScope, IdFunctionObject ifun, InterpretedFunction iApplyCallable) {
		Scriptable applyThis;
		if (indexReg != 0) {
			Object obj = stack[stackTop + 2];
			if (obj == UniqueTag.DOUBLE_MARK) {
				obj = ScriptRuntime.wrapNumber(sDbl[stackTop + 2]);
			}
			applyThis = ScriptRuntime.toObjectOrNull(cx, obj, frame.scope);
		} else {
			applyThis = null;
		}
		if (applyThis == null) {
			// This covers the case of args[0] == (null|undefined) as well.
			applyThis = cx.getTopCallScope();
		}
		if (op == Icode_TAIL_CALL) {
			exitFrame(cx, frame, null);
			frame = frame.parentFrame;
		} else {
			frame.savedStackTop = stackTop;
			frame.savedCallOp = op;
		}
		final CallFrame calleeFrame;
		if (BaseFunction.isApply(ifun)) {
			Object[] callArgs = indexReg < 2 ? ScriptRuntime.EMPTY_OBJECTS : ScriptRuntime.getApplyArguments(cx, stack[stackTop + 3]);
			calleeFrame = initFrame(cx, calleeScope, applyThis, callArgs, null, 0, callArgs.length, iApplyCallable, frame);
		} else {
			// Shift args left
			for (int i = 1; i < indexReg; ++i) {
				stack[stackTop + 1 + i] = stack[stackTop + 2 + i];
				sDbl[stackTop + 1 + i] = sDbl[stackTop + 2 + i];
			}
			int argCount = indexReg < 2 ? 0 : indexReg - 1;
			calleeFrame = initFrame(cx, calleeScope, applyThis, stack, sDbl, stackTop + 2, argCount, iApplyCallable, frame);
		}

		return calleeFrame;
	}

	private static CallFrame initFrame(Context cx, Scriptable callerScope, Scriptable thisObj, Object[] args, double[] argsDbl, int argShift, int argCount, InterpretedFunction fnOrScript, CallFrame parentFrame) {
		CallFrame frame = new CallFrame(cx, thisObj, fnOrScript, parentFrame);
		frame.initializeArgs(cx, callerScope, args, argsDbl, argShift, argCount);
		enterFrame(cx, frame, args, false);
		return frame;
	}

	private static void enterFrame(Context cx, CallFrame frame, Object[] args, boolean continuationRestart) {
		boolean usesActivation = frame.idata.itsNeedsActivation;
		if (usesActivation) {
			Scriptable scope = frame.scope;
			if (scope == null) {
				Kit.codeBug();
			} else if (continuationRestart) {
				// Walk the parent chain of frame.scope until a NativeCall is
				// found. Normally, frame.scope is a NativeCall when called
				// from initFrame() for a debugged or activatable function.
				// However, when called from interpretLoop() as part of
				// restarting a continuation, it can also be a NativeWith if
				// the continuation was captured within a "with" or "catch"
				// block ("catch" implicitly uses NativeWith to create a scope
				// to expose the exception variable).
				for (; ; ) {
					if (scope instanceof NativeWith) {
						scope = scope.getParentScope();
						if (scope == null || (frame.parentFrame != null && frame.parentFrame.scope == scope)) {
							// If we get here, we didn't find a NativeCall in
							// the call chain before reaching parent frame's
							// scope. This should not be possible.
							Kit.codeBug();
							break; // Never reached, but keeps the static analyzer
							// happy about "scope" not being null 5 lines above.
						}
					} else {
						break;
					}
				}
			}
			// Enter activation only when itsNeedsActivation true,
			// since debugger should not interfere with activation
			// chaining
			if (usesActivation) {
				ScriptRuntime.enterActivationFunction(cx, scope);
			}
		}
	}

	private static void exitFrame(Context cx, CallFrame frame, Object throwable) {
		if (frame.idata.itsNeedsActivation) {
			ScriptRuntime.exitActivationFunction(cx);
		}
	}

	private static void setCallResult(CallFrame frame, Object callResult, double callResultDbl) {
		if (frame.savedCallOp == Token.CALL) {
			frame.stack[frame.savedStackTop] = callResult;
			frame.sDbl[frame.savedStackTop] = callResultDbl;
		} else if (frame.savedCallOp == Token.NEW) {
			// If construct returns scriptable,
			// then it replaces on stack top saved original instance
			// of the object.
			if (callResult instanceof Scriptable) {
				frame.stack[frame.savedStackTop] = callResult;
			}
		} else {
			Kit.codeBug();
		}
		frame.savedCallOp = 0;
	}

	private static int stack_int32(CallFrame frame, int i, Context cx) {
		Object x = frame.stack[i];
		if (x == UniqueTag.DOUBLE_MARK) {
			return ScriptRuntime.toInt32(frame.sDbl[i]);
		}
		return ScriptRuntime.toInt32(cx, x);
	}

	private static double stack_double(CallFrame frame, int i, Context cx) {
		Object x = frame.stack[i];
		if (x != UniqueTag.DOUBLE_MARK) {
			return ScriptRuntime.toNumber(cx, x);
		}
		return frame.sDbl[i];
	}

	private static boolean stack_boolean(CallFrame frame, int i, Context cx) {
		Object x = Wrapper.unwrapped(frame.stack[i]);

		if (Boolean.TRUE.equals(x)) {
			return true;
		} else if (Boolean.FALSE.equals(x)) {
			return false;
		} else if (x == UniqueTag.DOUBLE_MARK) {
			double d = frame.sDbl[i];
			return !Double.isNaN(d) && d != 0.0;
		} else if (x == null || x == Undefined.instance) {
			return false;
		} else if (x instanceof Number) {
			double d = ((Number) x).doubleValue();
			return (!Double.isNaN(d) && d != 0.0);
		} else {
			return ScriptRuntime.toBoolean(cx, x);
		}
	}

	private static void doAdd(Object[] stack, double[] sDbl, int stackTop, Context cx) {
		Object rhs = stack[stackTop + 1];
		Object lhs = stack[stackTop];
		double d;
		boolean leftRightOrder;
		if (rhs == UniqueTag.DOUBLE_MARK) {
			d = sDbl[stackTop + 1];
			if (lhs == UniqueTag.DOUBLE_MARK) {
				sDbl[stackTop] += d;
				return;
			}
			leftRightOrder = true;
			// fallthrough to object + number code
		} else if (lhs == UniqueTag.DOUBLE_MARK) {
			d = sDbl[stackTop];
			lhs = rhs;
			leftRightOrder = false;
			// fallthrough to object + number code
		} else {
			if (lhs instanceof Scriptable || rhs instanceof Scriptable) {
				stack[stackTop] = ScriptRuntime.add(cx, lhs, rhs);

				// the next two else if branches are a bit more tricky
				// to reduce method calls
			} else if (lhs instanceof CharSequence) {
				if (rhs instanceof CharSequence) {
					stack[stackTop] = new ConsString((CharSequence) lhs, (CharSequence) rhs);
				} else {
					stack[stackTop] = new ConsString((CharSequence) lhs, ScriptRuntime.toCharSequence(cx, rhs));
				}
			} else if (rhs instanceof CharSequence) {
				stack[stackTop] = new ConsString(ScriptRuntime.toCharSequence(cx, lhs), (CharSequence) rhs);

			} else {
				double lDbl = (lhs instanceof Number) ? ((Number) lhs).doubleValue() : ScriptRuntime.toNumber(cx, lhs);
				double rDbl = (rhs instanceof Number) ? ((Number) rhs).doubleValue() : ScriptRuntime.toNumber(cx, rhs);
				stack[stackTop] = UniqueTag.DOUBLE_MARK;
				sDbl[stackTop] = lDbl + rDbl;
			}
			return;
		}

		// handle object(lhs) + number(d) code
		if (lhs instanceof Scriptable) {
			rhs = ScriptRuntime.wrapNumber(d);
			if (!leftRightOrder) {
				Object tmp = lhs;
				lhs = rhs;
				rhs = tmp;
			}
			stack[stackTop] = ScriptRuntime.add(cx, lhs, rhs);
		} else if (lhs instanceof CharSequence) {
			CharSequence rstr = ScriptRuntime.numberToString(cx, d, 10);
			if (leftRightOrder) {
				stack[stackTop] = new ConsString((CharSequence) lhs, rstr);
			} else {
				stack[stackTop] = new ConsString(rstr, (CharSequence) lhs);
			}
		} else {
			double lDbl = (lhs instanceof Number) ? ((Number) lhs).doubleValue() : ScriptRuntime.toNumber(cx, lhs);
			stack[stackTop] = UniqueTag.DOUBLE_MARK;
			sDbl[stackTop] = lDbl + d;
		}
	}

	private static int doArithmetic(Context cx, CallFrame frame, int op, Object[] stack, double[] sDbl, int stackTop) {
		double rDbl = stack_double(frame, stackTop, cx);
		--stackTop;
		double lDbl = stack_double(frame, stackTop, cx);
		stack[stackTop] = UniqueTag.DOUBLE_MARK;
		sDbl[stackTop] = switch (op) {
			case Token.SUB -> lDbl - rDbl;
			case Token.MUL -> lDbl * rDbl;
			case Token.DIV -> lDbl / rDbl;
			case Token.MOD -> lDbl % rDbl;
			case Token.POW -> Math.pow(lDbl, rDbl);
			default -> lDbl;
		};
		return stackTop;
	}

	private static Object[] getArgsArray(Object[] stack, double[] sDbl, int shift, int count) {
		if (count == 0) {
			return ScriptRuntime.EMPTY_OBJECTS;
		}
		Object[] args = new Object[count];
		for (int i = 0; i != count; ++i, ++shift) {
			Object val = stack[shift];
			if (val == UniqueTag.DOUBLE_MARK) {
				val = ScriptRuntime.wrapNumber(sDbl[shift]);
			}
			args[i] = val;
		}
		return args;
	}

	private static void addInstructionCount(Context cx, CallFrame frame, int extra) {
		cx.instructionCount += frame.pc - frame.pcPrevBranch + extra;
		if (cx.instructionCount > cx.instructionThreshold) {
			cx.observeInstructionCount(cx.instructionCount);
			cx.instructionCount = 0;
		}
	}

	// data for parsing
	InterpreterData itsData;

	@Override
	public Object compile(CompilerEnvirons compilerEnv, ScriptNode tree, boolean returnFunction, Context cx) {
		CodeGenerator cgen = new CodeGenerator();
		itsData = cgen.compile(compilerEnv, tree, returnFunction, cx);
		return itsData;
	}

	@Override
	public Script createScriptObject(Object bytecode, Object staticSecurityDomain) {
		if (bytecode != itsData) {
			Kit.codeBug();
		}
		return InterpretedFunction.createScript(itsData, staticSecurityDomain);
	}

	@Override
	public void setEvalScriptFlag(Script script) {
		((InterpretedFunction) script).idata.evalScriptFlag = true;
	}

	@Override
	public Function createFunctionObject(Context cx, Scriptable scope, Object bytecode, Object staticSecurityDomain) {
		if (bytecode != itsData) {
			Kit.codeBug();
		}
		return InterpretedFunction.createFunction(cx, scope, itsData, staticSecurityDomain);
	}

	@Override
	public void captureStackInfo(Context cx, RhinoException ex) {
		if (cx == null || cx.lastInterpreterFrame == null) {
			// No interpreter invocations
			ex.interpreterStackInfo = null;
			ex.interpreterLineData = null;
			return;
		}
		// has interpreter frame on the stack
		CallFrame[] array;
		if (cx.previousInterpreterInvocations == null || cx.previousInterpreterInvocations.size() == 0) {
			array = new CallFrame[1];
		} else {
			int previousCount = cx.previousInterpreterInvocations.size();
			if (cx.previousInterpreterInvocations.peek() == cx.lastInterpreterFrame) {
				// It can happen if exception was generated after
				// frame was pushed to cx.previousInterpreterInvocations
				// but before assignment to cx.lastInterpreterFrame.
				// In this case frames has to be ignored.
				--previousCount;
			}
			array = new CallFrame[previousCount + 1];
			cx.previousInterpreterInvocations.toArray(array);
		}
		array[array.length - 1] = (CallFrame) cx.lastInterpreterFrame;

		int interpreterFrameCount = 0;
		for (int i = 0; i != array.length; ++i) {
			interpreterFrameCount += 1 + array[i].frameIndex;
		}

		int[] linePC = new int[interpreterFrameCount];
		// Fill linePC with pc positions from all interpreter frames.
		// Start from the most nested frame
		int linePCIndex = interpreterFrameCount;
		for (int i = array.length; i != 0; ) {
			--i;
			CallFrame frame = array[i];
			while (frame != null) {
				--linePCIndex;
				linePC[linePCIndex] = frame.pcSourceLineStart;
				frame = frame.parentFrame;
			}
		}
		if (linePCIndex != 0) {
			Kit.codeBug();
		}

		ex.interpreterStackInfo = array;
		ex.interpreterLineData = linePC;
	}

	@Override
	public String getSourcePositionFromStack(Context cx, int[] linep) {
		CallFrame frame = (CallFrame) cx.lastInterpreterFrame;
		InterpreterData idata = frame.idata;
		if (frame.pcSourceLineStart >= 0) {
			linep[0] = getIndex(idata.itsICode, frame.pcSourceLineStart);
		} else {
			linep[0] = 0;
		}
		return idata.itsSourceFile;
	}

	@Override
	public String getPatchedStack(RhinoException ex, String nativeStackTrace) {
		String tag = "dev.latvian.mods.rhino.Interpreter.interpretLoop";
		StringBuilder sb = new StringBuilder(nativeStackTrace.length() + 1000);
		String lineSeparator = System.lineSeparator();

		CallFrame[] array = (CallFrame[]) ex.interpreterStackInfo;
		int[] linePC = ex.interpreterLineData;
		int arrayIndex = array.length;
		int linePCIndex = linePC.length;
		int offset = 0;
		while (arrayIndex != 0) {
			--arrayIndex;
			int pos = nativeStackTrace.indexOf(tag, offset);
			if (pos < 0) {
				break;
			}

			// Skip tag length
			pos += tag.length();
			// Skip until the end of line
			for (; pos != nativeStackTrace.length(); ++pos) {
				char c = nativeStackTrace.charAt(pos);
				if (c == '\n' || c == '\r') {
					break;
				}
			}
			sb.append(nativeStackTrace, offset, pos);
			offset = pos;

			CallFrame frame = array[arrayIndex];
			while (frame != null) {
				if (linePCIndex == 0) {
					Kit.codeBug();
				}
				--linePCIndex;
				InterpreterData idata = frame.idata;
				sb.append(lineSeparator);
				sb.append("\tat script");
				if (idata.itsName != null && idata.itsName.length() != 0) {
					sb.append('.');
					sb.append(idata.itsName);
				}
				sb.append('(');
				sb.append(idata.itsSourceFile);
				int pc = linePC[linePCIndex];
				if (pc >= 0) {
					// Include line info only if available
					sb.append(':');
					sb.append(getIndex(idata.itsICode, pc));
				}
				sb.append(')');
				frame = frame.parentFrame;
			}
		}
		sb.append(nativeStackTrace.substring(offset));

		return sb.toString();
	}

	@Override
	public List<String> getScriptStack(RhinoException ex) {
		ScriptStackElement[][] stack = getScriptStackElements(ex);
		List<String> list = new ArrayList<>(stack.length);
		String lineSeparator = System.lineSeparator();
		for (ScriptStackElement[] group : stack) {
			StringBuilder sb = new StringBuilder();
			for (ScriptStackElement elem : group) {
				elem.renderJavaStyle(sb);
				sb.append(lineSeparator);
			}
			list.add(sb.toString());
		}
		return list;
	}

	public ScriptStackElement[][] getScriptStackElements(RhinoException ex) {
		if (ex.interpreterStackInfo == null) {
			return null;
		}

		List<ScriptStackElement[]> list = new ArrayList<>();

		CallFrame[] array = (CallFrame[]) ex.interpreterStackInfo;
		int[] linePC = ex.interpreterLineData;
		int arrayIndex = array.length;
		int linePCIndex = linePC.length;
		while (arrayIndex != 0) {
			--arrayIndex;
			CallFrame frame = array[arrayIndex];
			List<ScriptStackElement> group = new ArrayList<>();
			while (frame != null) {
				if (linePCIndex == 0) {
					Kit.codeBug();
				}
				--linePCIndex;
				InterpreterData idata = frame.idata;
				String fileName = idata.itsSourceFile;
				String functionName = null;
				int lineNumber = -1;
				int pc = linePC[linePCIndex];
				if (pc >= 0) {
					lineNumber = getIndex(idata.itsICode, pc);
				}
				if (idata.itsName != null && idata.itsName.length() != 0) {
					functionName = idata.itsName;
				}
				frame = frame.parentFrame;
				group.add(new ScriptStackElement(fileName, functionName, lineNumber));
			}
			list.add(group.toArray(new ScriptStackElement[0]));
		}
		return list.toArray(new ScriptStackElement[list.size()][]);
	}
}

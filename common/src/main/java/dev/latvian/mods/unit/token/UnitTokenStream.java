package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.ColorUnit;
import dev.latvian.mods.unit.FixedNumberUnit;
import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.VariableUnit;
import dev.latvian.mods.unit.function.FuncUnit;
import dev.latvian.mods.unit.operator.OpUnit;

import java.util.LinkedList;
import java.util.Stack;

public final class UnitTokenStream {
	private static boolean isString(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c == '$' || c == '.';
	}

	private static boolean isHex(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
	}

	public final UnitContext context;

	public final String input;
	public final CharStream charStream;
	public final LinkedList<Unit> infix;
	public final Stack<Unit> resultStack;
	public final Unit unit;

	public UnitTokenStream(UnitContext context, String input) {
		this.context = context;
		this.input = input;
		this.charStream = new CharStream(input.toCharArray());
		this.infix = new LinkedList<>();
		this.resultStack = new Stack<>();

		var current = new StringBuilder();

		while (true) {
			var c = charStream.next();

			if (c == 0) {
				break;
			}

			var symbol = UnitSymbol.read(c, charStream);

			if (symbol == UnitSymbol.HASH) {
				if (isHex(charStream.peek(1)) && isHex(charStream.peek(2)) && isHex(charStream.peek(3)) && isHex(charStream.peek(4)) && isHex(charStream.peek(5)) && isHex(charStream.peek(6))) {
					var alpha = isHex(charStream.peek(7)) && isHex(charStream.peek(8));

					current.append('#');

					for (var i = 0; i < (alpha ? 8 : 6); i++) {
						current.append(charStream.next());
					}

					var color = Long.decode(current.toString()).intValue();
					current.setLength(0);

					infix.add(new ColorUnit(color, alpha));
				} else {
					throw new IllegalStateException("Invalid color code @ " + charStream.position);
				}
			} else {
				if (symbol != null && current.length() > 0) {
					infix.add(createTokenFromString(current.toString()));
					current.setLength(0);
				}

				if (symbol == UnitSymbol.SUB && (infix.isEmpty() || infix.getLast().shouldNegate())) {
					if (!infix.isEmpty() && infix.getLast() instanceof FixedNumberUnit num) {
						infix.removeLast();
						infix.add(FixedNumberUnit.ofFixed(-num.value));
					} else {
						infix.add(UnitSymbol.NEGATE.op.create());
					}
				} else if (symbol != null && symbol.op != null) {
					infix.add(symbol.op.create());
				} else if (symbol != null) {
					infix.add(symbol.unit);
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			infix.add(createTokenFromString(current.toString()));
			current.setLength(0);
		}

		if (infix.size() == 1) {
			unit = infix.getFirst();
			return;
		}

		/*
		if (context.isDebug()) {
			context.debugInfo("Raw Infix", infix);
		}

		var newInfix = new LinkedList<UnitToken>();
		UnitToken lastNewInfixToken = null;

		while (!infix.isEmpty()) {
			var token = infix.poll();

			if (lastNewInfixToken == SymbolUnitToken.NEGATE || lastNewInfixToken == SymbolUnitToken.BIT_NOT || lastNewInfixToken == SymbolUnitToken.BOOL_NOT) {
				newInfix.removeLast();
				newInfix.add(SymbolUnitToken.LP);
				newInfix.add(FixedNumberUnit.ZERO);
				newInfix.add(lastNewInfixToken);
				newInfix.add(token);
				newInfix.add(SymbolUnitToken.RP);
				lastNewInfixToken = SymbolUnitToken.RP;
			} else {
				newInfix.add(token);
				lastNewInfixToken = token;
			}
		}

		infix.clear();
		infix.addAll(newInfix);
		 */

		if (context.isDebug()) {
			context.debugInfo("Infix", infix);
		}

		Stack<Unit> operatorsStack = new Stack<>();
		LinkedList<Unit> postfix = new LinkedList<>();

		for (Unit next : infix) {
			if (context.isDebug()) {
				context.debugInfo("> " + next);
			}

			if (UnitSymbol.COMMA.is(next)) {
				// We're inside a method call. Because method call arguments are always going to evaluate regardless of what is outside the method,
				// we move operators from the stack to the queue up till we find the start of the method.
				while (!UnitSymbol.LP.is(operatorsStack.peek())) {
					postfix.add(operatorsStack.pop());

					if (context.isDebug()) {
						context.debugInfo("Operator Stack", operatorsStack);
						context.debugInfo("Operand Stack", postfix);
					}
				}
			}
			// Handle operators.
			else if (next instanceof OpUnit nextOperator) {
				// Pop from stack -> output for all operators with >= precedence over nextOperator
				boolean pushedCurrent = false;
				while (!operatorsStack.isEmpty()) {
					var o = operatorsStack.peek() instanceof OpUnit t ? t : null;

					if (o != null) {
						if (o.hasHigherPrecedenceThan(nextOperator)) {
							postfix.add(operatorsStack.pop());

							if (context.isDebug()) {
								context.debugInfo("Operator Stack", operatorsStack);
								context.debugInfo("Operand Stack", postfix);
							}
						} else {
							pushedCurrent = true;

							if (!nextOperator.shouldSkip()) {
								operatorsStack.push(nextOperator);

								if (context.isDebug()) {
									context.debugInfo("Operator Stack", operatorsStack);
									context.debugInfo("Operand Stack", postfix);
								}
							}

							break;
						}
					} else {
						break;
					}
				}

				if (!pushedCurrent && !nextOperator.shouldSkip()) {
					operatorsStack.push(next);

					if (context.isDebug()) {
						context.debugInfo("Operator Stack", operatorsStack);
						context.debugInfo("Operand Stack", postfix);
					}
				}
			}

			// Left-bracket
			else if (UnitSymbol.LP.is(next)) {
				operatorsStack.push(UnitSymbol.LP.unit);

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}
			} else if (UnitSymbol.RP.is(next)) {
				// Pop from stack -> queue until we reach the opening parenthesis.
				while (!UnitSymbol.LP.is(operatorsStack.peek())) {
					var value = operatorsStack.pop();

					if (!UnitSymbol.LP.is(value) && !UnitSymbol.RP.is(value)) {
						postfix.add(value);

						if (context.isDebug()) {
							context.debugInfo("Operator Stack", operatorsStack);
							context.debugInfo("Operand Stack", postfix);
						}
					}
				}

				// Pop opening parenthesis
				operatorsStack.pop();

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}

				// If the last entry in the operators stack is a function, pop it onto the output queue.
				if (!operatorsStack.isEmpty() && operatorsStack.peek() instanceof FuncUnit) {
					postfix.add(operatorsStack.pop());

					if (context.isDebug()) {
						context.debugInfo("Operator Stack", operatorsStack);
						context.debugInfo("Operand Stack", postfix);
					}
				}
			} else {
				// negate?

				postfix.add(next);

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}

				/*
				StringBuilder functionStringBuilder = new StringBuilder(next);
				while (!iterator.isEmpty()) {
					var future = iterator.peek();

					// If the character is invalid/not part of the function/variable, we do not want to process it here.
					// This is why lastCharacter is only set when a valid character is found.

					// End function declaration.
					// eg. math.sin(    - last character designates the end of a function, so we push to the operatorsStack.
					if (future == SymbolUnitToken.LP) {
						operatorsStack.push(functionStringBuilder.toString());
						break;
					}

					// End variable declaration, send variable to the outputQueue
					else if (future == SymbolUnitToken.COMMA || future == SymbolUnitToken.RP || isOperatorCharacter(future)) {
						outputQueue.add(functionStringBuilder.toString());
						break;
					}

					// The character being processed is part of the function/variable - keep going.
					else {
						lastToken = future.toString();
						functionStringBuilder.append(iterator.poll());
					}
				}

				String functionStringBuilderResult = functionStringBuilder.toString();

				// If the variable is the last thing in our MoLang, add it now
				// todo: does this defeat the purpose of a queue
				if (functionStringBuilderResult.startsWith(variablePrefix) && (outputQueue.isEmpty() || !Objects.equals(outputQueue.get(Math.max(0, outputQueue.size() - 1)), functionStringBuilderResult))) {
					outputQueue.add(functionStringBuilderResult);
				}
				 */
			}
		}

		// stack -> output
		while (!operatorsStack.isEmpty()) {
			var last = operatorsStack.pop();

			if (!UnitSymbol.LP.is(last) && !UnitSymbol.RP.is(last)) {
				postfix.add(last);
			}
		}

		if (context.isDebug()) {
			context.debugInfo("Postfix", postfix);
		}

		for (var token : postfix) {
			token.interpret(this);

			if (context.isDebug()) {
				context.debugInfo("Result Stack", resultStack);
			}
		}

		var lastUnit = resultStack.pop();
		this.unit = lastUnit.optimize();
	}

	public Unit getUnit() {
		return unit;
	}

	private Unit createTokenFromString(String input) {
		var constant = FixedNumberUnit.CONSTANTS.get(input);

		if (constant != null) {
			return constant;
		}

		try {
			return FixedNumberUnit.ofFixed(Double.parseDouble(input));
		} catch (Exception ex) {
			FuncUnit func = context.getFunction(input);
			return func == null ? VariableUnit.of(input) : func;
		}
	}

	@Override
	public String toString() {
		return infix.toString();
	}
}

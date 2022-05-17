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
				while (!UnitSymbol.LP.is(operatorsStack.peek())) {
					postfix.add(operatorsStack.pop());

					if (context.isDebug()) {
						context.debugInfo("Operator Stack", operatorsStack);
						context.debugInfo("Operand Stack", postfix);
					}
				}
			} else if (next instanceof OpUnit nextOperator) {
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
			} else if (UnitSymbol.LP.is(next)) {
				operatorsStack.push(UnitSymbol.LP.unit);

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}
			} else if (UnitSymbol.RP.is(next)) {
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

				operatorsStack.pop();

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}

				if (!operatorsStack.isEmpty() && operatorsStack.peek() instanceof FuncUnit) {
					postfix.add(operatorsStack.pop());

					if (context.isDebug()) {
						context.debugInfo("Operator Stack", operatorsStack);
						context.debugInfo("Operand Stack", postfix);
					}
				}
			} else if (next instanceof FuncUnit) {
				operatorsStack.push(next);

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}
			} else {
				// negate?

				postfix.add(next);

				if (context.isDebug()) {
					context.debugInfo("Operator Stack", operatorsStack);
					context.debugInfo("Operand Stack", postfix);
				}
			}
		}

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

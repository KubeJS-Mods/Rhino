package dev.latvian.mods.unit.token;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public record PostfixUnitToken(List<UnitToken> infix, boolean group) implements InterpretableUnitToken {
	@Override
	public Unit interpret(UnitContext context) {
		/*
		if (infix.size() == 1 && infix.get(0) instanceof InterpretableUnitToken it) {
			return it.interpret(context);
		} else if (infix.size() == 3 && infix.get(1) instanceof SymbolUnitToken op && op.isOp() && infix.get(0) instanceof InterpretableUnitToken itl && infix.get(2) instanceof InterpretableUnitToken itr) {
			var unit = op.createOpUnit();

			if (unit == null) {
				throw new IllegalStateException("Failed to create operator unit!");
			}

			unit.left = itl.interpret(context);
			unit.right = itr.interpret(context);
			return unit;
		}
		 */

		if (context.isDebug()) {
			context.pushDebug();
			context.debugInfo("Infix: " + infix);
		}

		var opStack = new Stack<SymbolUnitToken>();
		var postfixStack = new ArrayList<UnitToken>(infix.size());

		for (var token : infix) {
			if (token instanceof SymbolUnitToken symbol && symbol.isOp()) {
				while (!opStack.empty() && symbol.precedence >= opStack.peek().precedence) {
					postfixStack.add(opStack.pop());

					if (context.isDebug()) {
						context.debugInfo("Op Stack: " + opStack);
						context.debugInfo("Postfix: " + postfixStack);
					}
				}

				opStack.push(symbol);
			} else if (token instanceof InterpretableUnitToken it) {
				postfixStack.add(it);

				if (context.isDebug()) {
					context.debugInfo("Op Stack: " + opStack);
					context.debugInfo("Postfix: " + postfixStack);
				}
			} else {
				if (context.isDebug()) {
					context.popDebug();
				}

				throw new IllegalStateException("Invalid token: " + token);
			}
		}

		while (!opStack.empty()) {
			postfixStack.add(opStack.pop());

			if (context.isDebug()) {
				context.debugInfo("Op Stack: " + opStack);
				context.debugInfo("Postfix: " + postfixStack);
			}
		}

		var resultStack = new Stack<Unit>();

		for (var token : postfixStack) {
			if (token instanceof SymbolUnitToken symbol && symbol.isOp()) {
				var op = symbol.createOpUnit();

				if (op == null) {
					if (context.isDebug()) {
						context.popDebug();
					}

					throw new IllegalStateException("Failed to create operator unit from token " + symbol + "!");
				}

				op.right = resultStack.pop();
				op.left = resultStack.pop();
				resultStack.push(op);
			} else if (token instanceof InterpretableUnitToken it) {
				resultStack.push(it.interpret(context));
			}
		}

		if (context.isDebug()) {
			context.popDebug();
		}

		return resultStack.pop();
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("Postfix[");

		if (group) {
			builder.append('(');
		}

		for (var i = 0; i < infix.size(); i++) {
			if (i > 0) {
				builder.append(' ');
			}

			builder.append(infix.get(i));
		}

		if (group) {
			builder.append(')');
		}

		builder.append(']');
		return builder.toString();
	}
}

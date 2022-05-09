package dev.latvian.mods.unit;

import dev.latvian.mods.unit.operator.OpUnit;
import dev.latvian.mods.unit.token.SymbolUnitToken;
import dev.latvian.mods.unit.token.UnitToken;
import dev.latvian.mods.unit.token.UnitTokenStream;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OpGroupUnit extends Unit {
	public static Unit interpret(Unit current, UnitTokenStream stream, @Nullable UnitToken endToken) {
		if (stream.peekToken() instanceof SymbolUnitToken symbol && symbol.operatorUnit != null) {
			OpGroupUnit unit = new OpGroupUnit();
			unit.group = endToken != null;
			unit.units.add(current);

			while (true) {
				if (stream.peekToken() == endToken) {
					break;
				} else {
					Unit op = stream.nextUnit();
					Unit val = stream.nextUnit();

					if (op instanceof OpUnit opUnit) {
						unit.units.add(val);
						unit.ops.add(opUnit);
					} else {
						throw new IllegalStateException("Expected operator, got '" + op + "' insteead!");
					}
				}
			}

			return unit;
		}

		return current;
	}

	public boolean group = false;
	public final List<Unit> units = new ArrayList<>();
	public final List<OpUnit> ops = new ArrayList<>();

	@Override
	public double get(UnitVariables variables) {
		return 0D;
	}

	@Override
	public Unit optimize() {
		// TODO: Figure out how to sort operators
		return this;
	}

	@Override
	public void toString(StringBuilder builder) {
		if (group) {
			builder.append('(');
		}

		units.get(0).toString(builder);

		for (int i = 0; i < ops.size(); i++) {
			builder.append(' ');
			ops.get(i).toString(builder);
			builder.append(' ');
			units.get(i + 1).toString(builder);
		}

		if (group) {
			builder.append(')');
		}
	}
}

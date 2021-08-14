package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.unit.Unit;
import dev.latvian.mods.rhino.util.unit.UnitVariables;

public class UnitTest {
	public static void main(String[] args) throws Exception {
		UnitVariables variables = new UnitVariables();
		variables.set("abc", Unit.fixed(20));
		variables.set("c", Unit.fixed(40));
		variables.set("d", Unit.fixed(2));

		printUnit("-123", variables);
		printUnit("-$abc", variables);
		printUnit("($abc + 123)", variables);
		printUnit("($abc * $abc)", variables);
		printUnit("max(40, ($abc * $abc))", variables);
		printUnit("(($abc - 30) / min($c, $d))", variables);
		printUnit("(3 ** 2)", variables);
		printUnit("floor(-random())", variables);
		printUnit("random()", variables);
		printUnit("PI", variables);

		printUnit("time()", variables);
		Thread.sleep(350L);
		printUnit("time()", variables);

		printUnit("(abs((time() * 0.01)) % 300)", variables);
		printUnit("if((random() > 0.5), 10, 100)", variables);
	}

	private static void printUnit(String string, UnitVariables variables) {
		Unit unit = Unit.parse(string, variables);
		System.out.println("'" + unit + "' == " + unit.get());
	}
}

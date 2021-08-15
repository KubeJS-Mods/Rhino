package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.unit.FixedUnit;
import dev.latvian.mods.rhino.util.unit.Unit;
import dev.latvian.mods.rhino.util.unit.UnitStorage;

public class UnitTest {
	public static void main(String[] args) throws Exception {
		UnitStorage storage = new UnitStorage();
		storage.setVariable("abc", FixedUnit.of(20));
		storage.setVariable("c", FixedUnit.of(40));
		storage.setVariable("d", FixedUnit.of(2));

		printUnit("-123", storage);
		printUnit("-$abc", storage);
		printUnit("($abc + 123)", storage);
		printUnit("($abc * $abc)", storage);
		printUnit("max(40, ($abc * $abc))", storage);
		printUnit("(($abc - 30) / min($c, $d))", storage);
		printUnit("(3 ** 2)", storage);
		printUnit("floor(-random())", storage);
		printUnit("random()", storage);
		printUnit("PI", storage);

		printUnit("time()", storage);
		Thread.sleep(350L);
		printUnit("time()", storage);

		printUnit("(abs((time() * 1.1)) % 300)", storage);
		printUnit("if((random() > 0.5), 10, 100)", storage);
		printUnit("color(30, 49, $c, 255)", storage);
		printUnit("#00FF00", storage);
		printUnit("#4400FF00", storage);
	}

	private static void printUnit(String string, UnitStorage storage) {
		Unit unit = storage.parse(string);
		System.out.println("'" + unit + "' == " + unit.get());
	}
}

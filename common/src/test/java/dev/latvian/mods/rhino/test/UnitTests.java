package dev.latvian.mods.rhino.test;

import dev.latvian.mods.unit.EmptyVariableSet;
import dev.latvian.mods.unit.UnitContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class UnitTests {
	public static double eval(String input) {
		return UnitContext.DEFAULT.parse(input).get(EmptyVariableSet.INSTANCE);
	}

	public static void assertEval(String input, double expected) {
		double eval = eval(input);
		Assertions.assertTrue(Math.abs(expected - eval) < 0.00001, "expected: " + expected + " but was: " + eval);
	}

	public static void assertStream(String input, String... expected) {
		Assertions.assertEquals(Arrays.asList(expected), UnitContext.DEFAULT.createStream(input).toTokenStrings());
	}
	/* Tests that still need to be written:
// simple eq
console.printUnit('5==5.0')
console.printUnit('5 == 5.0')

// functions
console.printUnit('sin($test*10)*5')
console.printUnit('$test<0.5?-30:40')
console.printUnit('($test<0.5?($test2<0.5?1.5:-4):($test3<0.5*-3?1.5:-4))')
console.printUnit('(sin((time() * 1.1)) * (($screenW - 32) / 2))')

// test sub vs negate
console.printUnit('-2')
console.printUnit('2 - 2')
console.printUnit('-2 - 2')
console.printUnit('2 - --2')
console.printUnit('-   (2**7) - (-2)')

// color
console.printUnit('#FF0044')
console.printUnit('#FFFF0044')
	 */

	@Test
	@DisplayName("Ternary Token Stream")
	public void testTernaryTokenStream() {
		assertStream("0 < 5 ? 1.5 : 2", "0.0", "<", "5.0", "?", "1.5", ":", "2.0");
	}

	// @Test
	@DisplayName("Order of operations")
	public void testOrderOfOperations() {
		assertEval("4 - (2 + 8* 2 - 1) / 5", 0.6);
	}

	@Test
	@DisplayName("abs Function")
	public void testAbsFunction() {
		assertEval("abs(-4.0)", 4.0);
	}
}

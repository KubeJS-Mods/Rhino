package dev.latvian.mods.rhino.test;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.VariableSet;
import dev.latvian.mods.unit.function.RoundedTimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class UnitTests {
	public static UnitContext CONTEXT = UnitContext.DEFAULT.sub();
	public static VariableSet VARIABLE_SET = new VariableSet();
	public static final double $test = 3.5D * Math.random();
	public static final double $test2 = 2D * Math.random();
	public static final double $test3 = 100D * Math.random();

	static {
		CONTEXT.pushDebug();
		VARIABLE_SET.set("$test", $test);
		VARIABLE_SET.set("$test2", $test2);
		VARIABLE_SET.set("$test3", $test3);
	}

	public static double eval(String input) {
		System.out.println("Input: " + input);
		Unit unit = CONTEXT.parse(input);
		System.out.println("Result: " + unit);
		double eval = unit.get(VARIABLE_SET);
		System.out.println("Eval: " + eval);
		return eval;
	}

	public static void assertEval(String input, double expected) {
		Assertions.assertEquals(expected, eval(input), 0.00001D);
	}

	@Test
	@DisplayName("Number")
	public void number() {
		assertEval("2.8", 2.8);
	}

	@Test
	@DisplayName("Negative Number")
	public void negativeNumber() {
		assertEval("-2", -2.0);
	}

	@Test
	@DisplayName("Variable")
	public void variable() {
		assertEval("$test", $test);
	}

	@Test
	@DisplayName("Negative Variable")
	public void negativeVariable() {
		assertEval("-$test", -$test);
	}

	@Test
	@DisplayName("Subtraction")
	public void subtraction() {
		assertEval("4 -2.5", 1.5);
	}

	@Test
	@DisplayName("NegSubtraction")
	public void negSubtraction() {
		assertEval("-2.3 -3", -5.3);
	}

	@Test
	@DisplayName("DoubleNegSubtraction")
	public void doubleNegSubtraction() {
		assertEval("-7 - --2", -9.0);
	}

	@Test
	@DisplayName("Integer == Fraction")
	public void integerEqFraction() {
		assertEval("5==5.0", 1.0);
	}

	@Test
	@DisplayName("Integer == Fraction (Spaces)")
	public void integerEqFractionSpaces() {
		assertEval("5 == 5.0", 1.0);
	}

	@Test
	@DisplayName("Color RGB")
	public void colorRGB() {
		assertEval("#FF0044", 0xFFFF0044);
	}

	@Test
	@DisplayName("Color ARGB")
	public void colorARGB() {
		assertEval("#FFFF0044", 0xFFFF0044);
	}

	@Test
	@DisplayName("Color Function RGB")
	public void colorFunc3() {
		assertEval("rgb(1, 0, 0.5)", 0xFFFF007F);
	}

	@Test
	@DisplayName("Color Function RGBA")
	public void colorFunc4() {
		assertEval("rgb(1, 0, 0.5, 0.5)", 0x7FFF007F);
	}

	@Test
	@DisplayName("Color Function RGB+A")
	public void colorFunc2() {
		assertEval("rgb(#99FF007F, 0.5)", 0x7FFF007F);
	}

	@Test
	@DisplayName("Simple order of operations I")
	public void simpleOrderOfOperations1() {
		assertEval("2 + 3 * 4", 14);
	}

	@Test
	@DisplayName("Simple order of operations II")
	public void simpleOrderOfOperations2() {
		assertEval("2 + -(3 * 4)", -10);
	}

	@Test
	@DisplayName("Simple order of operations III")
	public void simpleOrderOfOperations3() {
		assertEval("(2 + 3) * 4", 20);
	}

	@Test
	@DisplayName("Complex order of operations I")
	public void orderOfOperations() {
		assertEval("4 * -(2 + 8 * 2 - 1) / 5", -13.6);
	}

	@Test
	@DisplayName("Ternary I: simple")
	public void ternary1() {
		assertEval("3 >= 2 ? 6 : 4", 6);
	}

	@Test
	@DisplayName("Ternary II: double sum")
	public void ternary2() {
		assertEval("2.4 + 3.5 * 4 == 16.4 ? 4.0 : 2.0", 4.0);
	}

	@Test
	@DisplayName("Ternary III: var/neg")
	public void ternary3() {
		assertEval("$test < 0.5 ? -30 : 40", $test < 0.5 ? -30 : 40);
	}

	@Test
	@DisplayName("Ternary IV: 3 vars")
	public void ternary4() {
		assertEval("$test < 0.5 ? $test2 < 1.5 ? 1.5 : -4 : $test3 < 50 * -3 ? 1.5 : -4", $test < 0.5 ? $test2 < 1.5 ? 1.5 : -4 : $test3 < 50 * -3 ? 1.5 : -4);
	}

	@Test
	@DisplayName("Functions I: time")
	public void functions1() {
		assertEval("roundedTime()", RoundedTimeUnit.time());
	}

	@Test
	@DisplayName("Functions II: sin")
	public void functions2() {
		assertEval("sin(3.0)", Math.sin(3D));
	}

	@Test
	@DisplayName("Functions III: negative abs")
	public void functions3() {
		assertEval("-abs(-4.0)", -4.0);
	}

	@Test
	@DisplayName("Functions IV: sin/var combo")
	public void functions4() {
		assertEval("sin($test*10)*5", Math.sin($test * 10D) * 5D);
	}

	@Test
	@DisplayName("Functions V: sin/time/var combo")
	public void functions5() {
		assertEval("sin(roundedTime() * 1.1) * (($test - 32) / 2)", Math.sin(RoundedTimeUnit.time() * 1.1D) * (($test - 32D) / 2D));
	}

	@Test
	@DisplayName("Semicolon I: Two numbers")
	public void semicolon1() {
		assertEval("5; 6", 6);
	}

	@Test
	@DisplayName("Semicolon II: Variable setter")
	public void semicolon2() {
		assertEval("$setTest = 4 * 5; $setTest * 2", 40);
	}

	@Test
	@DisplayName("Semicolon III: Variable multiplier")
	public void semicolon3() {
		assertEval("$setTest = 8; $setTest *= 0.5; $setTest * 3", 12);
	}
}

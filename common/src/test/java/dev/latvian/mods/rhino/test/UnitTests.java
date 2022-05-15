package dev.latvian.mods.rhino.test;

import dev.latvian.mods.unit.Unit;
import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.VariableSet;
import dev.latvian.mods.unit.function.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UnitTests {
	public static UnitContext CONTEXT = UnitContext.DEFAULT.sub();
	public static VariableSet VARIABLE_SET = new VariableSet();
	public static final double TEST_VAR = 3.5D * Math.random();
	public static final double TEST_VAR_2 = 2D * Math.random();
	public static final double TEST_VAR_3 = 100D * Math.random();

	static {
		CONTEXT.debug = true;
		VARIABLE_SET.set("$test", TEST_VAR);
		VARIABLE_SET.set("$test2", TEST_VAR_2);
		VARIABLE_SET.set("$test3", TEST_VAR_3);
	}

	public static double eval(String input) {
		System.out.println("Parsing: " + input);
		Unit unit = CONTEXT.parse(input);
		System.out.println("Result: " + unit);
		return unit.get(VARIABLE_SET);
	}

	public static void assertEval(String input, double expected) {
		Assertions.assertEquals(expected, eval(input), 0.00001D);
	}

	@Test
	@DisplayName("Order of operations")
	public void orderOfOperations() {
		assertEval("4 - (2 + 8* 2 - 1) / 5", 0.6);
	}

	@Test
	@DisplayName("abs Function")
	public void absFunction() {
		assertEval("abs(-4.0)", 4.0);
	}

	@Test
	@DisplayName("sin Function")
	public void sinFunction() {
		assertEval("sin($test*10)*5", Math.sin(TEST_VAR * 10D) * 5D);
	}

	@Test
	@DisplayName("Variable Ternary")
	public void variableTernary() {
		assertEval("$test<0.5?-30:40", TEST_VAR < 0.5 ? -30 : 40);
	}

	@Test
	@DisplayName("Complex eval")
	public void complexEval() {
		assertEval("$test<0.5?($test2<1.5?1.5:-4):($test3<50*-3?1.5:-4)", TEST_VAR < 0.5 ? (TEST_VAR_2 < 1.5 ? 1.5 : -4) : (TEST_VAR_3 < 50 * -3 ? 1.5 : -4));
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
		assertEval("#FF0044", 0xFF0044);
	}

	@Test
	@DisplayName("Color ARGB")
	public void colorARGB() {
		assertEval("#FFFF0044", 0xFFFF0044);
	}

	@Test
	@DisplayName("Negate")
	public void negate() {
		assertEval("-2", -2.0);
	}

	@Test
	@DisplayName("Subtraction")
	public void subtraction() {
		assertEval("4 -2", 2.0);
	}

	@Test
	@DisplayName("NegSubtraction")
	public void negSubtraction() {
		assertEval("-2 -2", -4.0);
	}

	@Test
	@DisplayName("DoubleNegSubtraction")
	public void doubleNegSubtraction() {
		assertEval("-7 - --2", -9.0);
	}

	@Test
	@DisplayName("time function")
	public void timeFunction() {
		assertEval("sin(time() * 1.1) * (($test - 32) / 2)", Math.sin(TimeUnit.time() * 1.1D) * ((TEST_VAR - 32D) / 2D));
	}
}

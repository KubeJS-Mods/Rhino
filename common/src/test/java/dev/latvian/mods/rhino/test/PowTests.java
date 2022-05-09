package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PowTests {
	public static RhinoTest TEST = new RhinoTest("pow");

	@Test
	@DisplayName("Both Whole")
	public void bothWhole() {
		TEST.test("bothWhole", """
				let a = 10
				let b = 3
				let c = a ** b
				console.info(c)
				""", """
				1000.0
				""");
	}

	@Test
	@DisplayName("Fraction Exponent")
	public void fractionExponent() {
		TEST.test("fractionExponent", """
				let a = 0.5
				let b = 0.5
				let c = a ** b
				console.info(c)
				""", """
				0.7071067811865476
				""");
	}

	@Test
	@DisplayName("Fraction Base")
	public void fractionBase() {
		TEST.test("fractionBase", """
				let a = 2.5
				let b = 3
				let c = a ** b
				console.info(c)
				""", """
				15.625
				""");
	}

	@Test
	@DisplayName("Zero Exponent")
	public void zeroExponent() {
		TEST.test("zeroExponent", """
				let a = 100
				let b = 0
				let c = a ** b
				console.info(c)
				""", """
				1.0
				""");
	}

	@Test
	@DisplayName("Negative Exponent")
	public void negativeExponent() {
		TEST.test("negativeExponent", """
				let a = 400
				let b = -1
				let c = a ** b
				console.info(c)
				""", """
				0.0025
				""");
	}
}

package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class PowTests {
	public static final RhinoTest TEST = new RhinoTest("pow");

	@Test
	public void bothWhole() {
		TEST.test("bothWhole", """
			let a = 10
			let b = 3
			let c = a ** b
			console.info(c)
			""", """
			1000
			""");
	}

	@Test
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
	public void zeroExponent() {
		TEST.test("zeroExponent", """
			let a = 100
			let b = 0
			let c = a ** b
			console.info(c)
			""", """
			1
			""");
	}

	@Test
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

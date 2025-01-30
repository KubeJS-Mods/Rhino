package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StaticTests {
	public static final RhinoTest TEST = new RhinoTest("static");

	@Test
	public void get() {
		TEST.test("get", """
			console.info(StaticUtils.immutableInt)
			""", """
			40
			""");
	}

	@Test
	public void set() {
		TEST.test("set", """
			StaticUtils.mutableInt = 30
			console.info(StaticUtils.mutableInt)
			""", """
			30
			""");
	}

	@Test
	public void call() {
		TEST.test("call", """
			StaticUtils.test(console)
			""", """
			hi
			""");
	}
}

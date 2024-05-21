package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GenericTests {
	public static final RhinoTest TEST = new RhinoTest("generics");

	@Test
	@Order(1)
	public void init() {
		TEST.test("init", """
			shared.testObject = {
				a: -39, b: 2, c: 3439438
			}
						
			shared.testList = console.testList
			""", "");
	}

	@Test
	public void arrayArg() {
		TEST.test("arrayArg", "console.genericArrayArg(['a', 'b']);", "Generic array:\n[W[a], W[b]]");
	}

	@Test
	public void arrayArgUnwrapped() {
		TEST.test("arrayArgUnwrapped", "console.genericArrayArg('a');", "Generic array:\n[W[a]]");
	}

	@Test
	public void arrayArgList() {
		TEST.test("arrayArgList", "console.genericArrayArg(console.testList);", "Generic array:\n[W[abc], W[def], W[ghi]]");
	}

	@Test
	public void listArg() {
		TEST.test("listArg", "console.genericListArg(['a', 'b']);", "Generic list:\n[W[a], W[b]]");
	}

	@Test
	public void listArgUnwrapped() {
		TEST.test("listArgUnwrapped", "console.genericListArg('a');", "Generic list:\n[W[a]]");
	}

	@Test
	public void listArgList() {
		TEST.test("listArgList", "console.genericListArg(console.testList);", "Generic list:\n[W[abc], W[def], W[ghi]]");
	}

	@Test
	public void mapArg() {
		TEST.test("mapArg", "console.genericMapArg({'test': '10.5'});", "Generic map:\n{W[abc]: 10}");
	}
}

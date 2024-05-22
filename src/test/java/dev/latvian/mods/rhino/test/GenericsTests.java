package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GenericsTests {
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
		TEST.test("arrayArg", "console.genericsArrayArg(['a', 'b']);", "Generics array:\n[W[a], W[b]]");
	}

	@Test
	public void arrayArgUnwrapped() {
		TEST.test("arrayArgUnwrapped", "console.genericsArrayArg('a');", "Generics array:\n[W[a]]");
	}

	@Test
	public void arrayArgList() {
		TEST.test("arrayArgList", "console.genericsArrayArg(console.testList);", "Generics array:\n[W[abc], W[def], W[ghi]]");
	}

	@Test
	public void listArg() {
		TEST.test("listArg", "console.genericsListArg(['a', 'b']);", "Generics list:\n[W[a], W[b]]");
	}

	@Test
	public void listArgUnwrapped() {
		TEST.test("listArgUnwrapped", "console.genericsListArg('a');", "Generics list:\n[W[a]]");
	}

	@Test
	public void listArgList() {
		TEST.test("listArgList", "console.genericsListArg(console.testList);", "Generics list:\n[W[abc], W[def], W[ghi]]");
	}

	@Test
	public void setArg() {
		TEST.test("setArg", "console.genericsSetArg(['a', 'b']);", "Generics set:\n[W[a], W[b]]");
	}

	@Test
	public void setArgUnwrapped() {
		TEST.test("setArgUnwrapped", "console.genericsSetArg('a');", "Generics set:\n[W[a]]");
	}

	@Test
	public void setArgList() {
		TEST.test("setArgList", "console.genericsSetArg(console.testList);", "Generics set:\n[W[abc], W[def], W[ghi]]");
	}

	@Test
	public void mapArg() {
		TEST.test("mapArg", "console.genericsMapArg({'test': '10.5'});", "Generics map:\n{W[M[test]]: 10}");
	}

	@Test
	public void mapArgMap() {
		TEST.test("mapArgMap", "console.genericsMapArg(console.testMap);", "Generics map:\n{W[M[test]]: 10}");
	}

	@Test
	public void materialHolder() {
		TEST.test("materialHolder", "console.registerMaterial('minecraft:iron');", "Registered material: minecraft:iron");
	}
}

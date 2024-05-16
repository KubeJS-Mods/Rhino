package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptionalChainingTests {
	public static final RhinoTest TEST = new RhinoTest("optionalChaining");

	@Test
	@Order(1)
	public void init() {
		TEST.test("init", """
			shared.a = { b: { c: 'd' } }
			shared.e = { f: {} }
			shared.h = null
			""", "");
	}

	@Test
	@Order(2)
	public void shouldError() {
		TEST.test("shouldError", """
			console.info(shared.a.b.c)
			console.info(shared.e.f.g)
			console.info(shared.h.i.j)
			""", """
			d
			undefined
			Error: TypeError: Cannot read property "i" from null (optionalChaining/shouldError#3)
			""");
	}

	@Test
	@Order(2)
	public void shouldntError() {
		TEST.test("shouldntError", """
			console.info(shared.a?.b?.c)
			console.info(shared.e?.f?.g)
			console.info(shared.h?.i?.j)
			""", """
			d
			undefined
			undefined
			""");
	}
}

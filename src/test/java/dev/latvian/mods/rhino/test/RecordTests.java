package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecordTests {
	public static final RhinoTest TEST = new RhinoTest("records");

	@Test
	public void emptyObject() {
		TEST.test("emptyObject", """
			console.printRecord({})
			""", """
			TestRecord[num=420, str=Optional.empty, subRecord=null]
			""");
	}

	@Test
	public void object() {
		TEST.test("object", """
			console.printRecord({str: 'hello'})
			""", """
			TestRecord[num=420, str=Optional[hello], subRecord=null]
			""");
	}

	@Test
	public void objectWithSub() {
		TEST.test("objectWithSub", """
			console.printRecord({sub: {num: 5}})
			""", """
			TestRecord[num=420, str=Optional.empty, subRecord=TestRecord[num=5, str=Optional.empty, subRecord=null]]
			""");
	}

	@Test
	public void consumer() {
		TEST.test("object", """
			console.printRecord(r => { r.str = 'hello' })
			""", """
			TestRecord[num=420, str=Optional[hello], subRecord=null]
			""");
	}

	@Test
	public void array() {
		TEST.test("object", """
			console.printRecord([5, 'hi'])
			""", """
			TestRecord[num=5, str=Optional[hi], subRecord=null]
			""");
	}

	@Test
	public void mix() {
		TEST.test("object", """
			console.printRecord([5, 'hi', r => r.sub = {num: -50}])
			""", """
			TestRecord[num=5, str=Optional[hi], subRecord=TestRecord[num=420, str=Optional.empty, subRecord=TestRecord[num=-50, str=Optional.empty, subRecord=null]]]
			""");
	}
}

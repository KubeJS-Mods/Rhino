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
			TestRecord[num=0, str=null, sub=null]
			""");
	}

	@Test
	public void object() {
		TEST.test("object", """
			console.printRecord({str: 'hello'})
			""", """
			TestRecord[num=0, str=hello, sub=null]
			""");
	}

	@Test
	public void objectWithSub() {
		TEST.test("objectWithSub", """
			console.printRecord({sub: {num: 5}})
			""", """
			TestRecord[num=0, str=null, sub=TestRecord[num=5, str=null, sub=null]]
			""");
	}

	@Test
	public void consumer() {
		TEST.test("object", """
			console.printRecord(r => { r.str = 'hello' })
			""", """
			TestRecord[num=0, str=hello, sub=null]
			""");
	}

	@Test
	public void array() {
		TEST.test("object", """
			console.printRecord([5, 'hi'])
			""", """
			TestRecord[num=5, str=hi, sub=null]
			""");
	}

	@Test
	public void mix() {
		TEST.test("object", """
			console.printRecord([5, 'hi', r => r.sub = {num: -50}])
			""", """
			TestRecord[num=5, str=hi, sub=TestRecord[num=0, str=null, sub=TestRecord[num=-50, str=null, sub=null]]]
			""");
	}
}

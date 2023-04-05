package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiscTests {
	public static final RhinoTest TEST = new RhinoTest("misc").shareScope();

	@Test
	public void testFunctionAssignment() {
		TEST.test("functionAssignment",
				"""
						let x = () => {};
						x.abc = 1;
						console.info(x.abc);
						""",
				"1.0"
		);
	}
  
  @Test
	public void testDelete() {
		TEST.test("delete", "let x = {a: 1}; delete x.a; console.info(x.a);", "undefined");
	}

	@Order(1)
	public void init() {
		TEST.test("init", """
				const testObject = {
					a: -39, b: 2, c: 3439438
				}
								
				let testList = console.testList
				    
				for (let string of testList) {
					console.info(string)
				}
				""", """
				abc
				def
				ghi
				""");
	}

	@Test
	public void array() {
		TEST.test("array", """
				for (let x of console.testArray) {
					console.info(x)
				}
				""", """
				abc
				def
				ghi
				""");
	}

	@Test
	public void enums() {
		TEST.test("enums", """
				console.theme = 'Dark'
				console.info(console.theme === 'DaRK')
				""", """
				Set theme to DARK
				true
				""");
	}

	@Test
	@Order(2)
	public void arrayLength() {
		TEST.test("arrayLength", """
				console.info('init ' + testList.length)
				testList.add('abcawidawidaiwdjawd')
				console.info('add ' + testList.length)
				testList.push('abcawidawidaiwdjawd')
				console.info('push ' + testList.length)
				""", """
				init 3
				add 4
				push 5
				""");
	}

	@Test
	@Order(3)
	public void popUnshiftMap() {
		TEST.test("popUnshiftMap", """
				console.info('pop ' + testList.pop() + ' ' + testList.length)
				console.info('shift ' + testList.shift() + ' ' + testList.length)
				console.info('map ' + testList.concat(['xyz']).reverse().map(e => e.toUpperCase()).join(" | "))
				""", """
				pop abcawidawidaiwdjawd 4
				shift abc 3
				map XYZ | ABCAWIDAWIDAIWDJAWD | GHI | DEF
				""");
	}

	@Test
	@Order(4)
	public void keysValuesEntries() {
		TEST.test("keysValuesEntries", """
				console.info(Object.keys(testObject))
				console.info(Object.values(testObject))
				console.info(Object.entries(testObject))
				""", """
				[a, b, c]
				[-39.0, 2.0, 3439438.0]
				[[a, -39.0], [b, 2.0], [c, 3439438.0]]
				""");
	}

	@Test
	@Order(4)
	public void deconstruction() {
		TEST.test("deconstruction", """
				for (let [key, value] of Object.entries(testObject)) {
					console.info(`${key} : ${value}`)
				}
				""", """
				a : -39
				b : 2
				c : 3439438
				""");
	}

	@Test
	@Order(4)
	public void typeWrappers() {
		TEST.test("typeWrappers", """
				console.printMaterial('wood')
				console.printMaterial('stone')
				console.printMaterial('wood')
				""", """
				wood#0037c6ad
				stone#068af865
				wood#0037c6ad
				""");
	}
}

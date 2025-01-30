package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.reflect.Modifier;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiscTests {
	public static final RhinoTest TEST = new RhinoTest("misc");

	@Test
	public void testFunctionAssignment() {
		TEST.test("functionAssignment",
			"""
				let x = () => {};
				x.abc = 1;
				console.info(x.abc);
				""",
			"1"
		);
	}

	@Test
	public void testDelete() {
		TEST.test("delete", "let x = {a: 1}; delete x.a; console.info(x.a);", "undefined");
	}

	@Test
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
			
			shared.testObject = testObject
			shared.testList = testList
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
			console.info('init ' + shared.testList.length)
			shared.testList.add('abcawidawidaiwdjawd')
			console.info('add ' + shared.testList.length)
			shared.testList.push('abcawidawidaiwdjawd')
			console.info('push ' + shared.testList.length)
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
			console.info('pop ' + shared.testList.pop() + ' ' + shared.testList.length)
			console.info('shift ' + shared.testList.shift() + ' ' + shared.testList.length)
			console.info('map ' + shared.testList.concat(['xyz']).reverse().map(e => e.toUpperCase()).join(" | "))
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
			console.info(Object.keys(shared.testObject))
			console.info(Object.values(shared.testObject))
			console.info(Object.entries(shared.testObject))
			""", """
			['a', 'b', 'c']
			[-39, 2, 3439438]
			[['a', -39], ['b', 2], ['c', 3439438]]
			""");
	}

	@Test
	@Order(4)
	public void deconstruction() {
		TEST.test("deconstruction", """
			for (let [key, value] of Object.entries(shared.testObject)) {
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

	@Test
	public void jsonStringifyWithNestedArrays() {
		TEST.test("jsonStringifyWithNestedArrays", """
			const thing = {nested: [1, 2, 3]};
			console.info(JSON.stringify(thing));
			""", "{\"nested\":[1.0,2.0,3.0]}");
	}

	@Test
	public void types() {
		for (var method : GenericObject.class.getDeclaredMethods()) {
			if (!Modifier.isStatic(method.getModifiers())) {
				GenericObject.test = method.getName();
				GenericObject.test(method.getName(), method.getGenericReturnType());
			}
		}

		GenericObject.test = "";
	}

	@Test
	public void varargs() {
		TEST.test("varargs", """
			console.varargTest("hi", 1, 2, 3);
			""", "VarArg Ints hi: [1, 2, 3]");
	}

	@Test
	public void get() {
		TEST.test("get", """
			console.info(console.immutableInt)
			""", """
			40
			""");
	}

	@Test
	public void set() {
		TEST.test("set", """
			console.mutableInt = 30.5
			console.info(console.mutableInt)
			""", """
			30
			""");
	}
}

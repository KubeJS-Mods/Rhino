package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Tests for array and object-likes.
 */
@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArrayObjectTests {
	public static final RhinoTest TEST = new RhinoTest("arrays");

	@Test
	public void arrayAt() {
		TEST.test("arrayAt", """
			const a = [5, 12, 8, 130, 44];
			console.info(a.at(2));
			console.info(a.at(-2));
			console.info(a.at(99));
			console.info(a.at());
			""", """
			8
			130
			undefined
			5
			""");
	}

	@Test
	public void arrayCopy() {
		TEST.test("arrayCopy", """
			const a = [3, 1, 2];
			const r = a.toReversed();
			console.info(r.join(','));
			console.info(a.join(','));
			const s = a.toSorted();
			console.info(s.join(','));
			const sd = a.toSorted((x, y) => y - x);
			console.info(sd.join(','));
			console.info(a.join(','));
			const sp = [1, 2, 3, 4, 5].toSpliced(1, 2, 'x', 'y', 'z');
			console.info(sp.join(','));
			const w = a.with(1, 99);
			console.info(w.join(','));
			console.info(a.join(','));
			const h = [1, , 3].toReversed();
			console.info(JSON.stringify(h) + ' ' + (1 in h));
			try {
				a.with(5, 0);
			} catch (e) {
				console.info('range error');
			}
			""", """
			2,1,3
			3,1,2
			1,2,3
			3,2,1
			3,1,2
			1,x,y,z,4,5
			3,99,2
			3,1,2
			[3,null,1] true
			range error
			""");
	}

	@Test
	public void arrayFindLast() {
		TEST.test("arrayFindLast", """
			const a = [5, 12, 50, 130, 44];
			console.info(a.findLast(x => x > 45));
			console.info(a.findLastIndex(x => x > 45));
			console.info(a.findLast(x => x > 1000));
			console.info(a.findLastIndex(x => x > 1000));
			""", """
			130
			3
			undefined
			-1
			""");
	}

	@Test
	public void arrayFlat() {
		TEST.test("arrayFlat", """
			console.info(JSON.stringify([1, 2, [3, 4]].flat()));
			console.info(JSON.stringify([1, 2, [3, [4, 5]]].flat()));
			console.info(JSON.stringify([1, 2, [3, [4, [5, 6]]]].flat(2)));
			console.info(JSON.stringify([1, 2, [3, [4, [5, [6]]]]].flat(Infinity)));
			console.info(JSON.stringify([1, 2, , 4, 5].flat()));
			""", """
			[1,2,3,4]
			[1,2,3,[4,5]]
			[1,2,3,4,[5,6]]
			[1,2,3,4,5,6]
			[1,2,4,5]
			""");
	}

	@Test
	public void arrayFlatMap() {
		TEST.test("arrayFlatMap", """
			const a = [1, 2, 3, 4];
			console.info(JSON.stringify(a.flatMap(x => [x, x * 2])));
			console.info(JSON.stringify(a.flatMap(x => [[x * 2]])));
			""", """
			[1,2,2,4,3,6,4,8]
			[[2],[4],[6],[8]]
			""");
	}

	@Test
	public void arrayFromSparse() {
		TEST.test("arrayFromSparse", """
			const a = Array.from([1, , 3]);
			console.info(1 in a);
			console.info(a[1]);
			console.info(JSON.stringify(Array.from([1, , 3], x => typeof x)));
			""", """
			true
			undefined
			["number","undefined","number"]
			""");
	}

	@Test
	public void numberKeyedObjectValues() {
		TEST.test("numberKeyedObjectValues", """
			const obj = { 0: 50, 1: 25, 2: 18, 3: 7 }
			console.info(Object.keys(obj).join(','))
			console.info(Object.values(obj).join(','))
			console.info(Object.entries(obj).map(e => e.join(':')).join(','))
			""", """
			0,1,2,3
			50,25,18,7
			0:50,1:25,2:18,3:7
			""");
	}

	@Test
	public void objectFromEntries() {
		TEST.test("objectFromEntries", """
			const o = Object.fromEntries([['a', 1], ['b', 2]]);
			console.info(JSON.stringify(o));
			const m = new Map([['x', 'y']]);
			console.info(JSON.stringify(Object.fromEntries(m)));
			const o2 = Object.fromEntries(Object.entries({ q: 7 }));
			console.info(o2.q);
			""", """
			{"a":1,"b":2}
			{"x":"y"}
			7
			""");
	}

	@Test
	public void objectGetOwnPropertyDescriptors() {
		TEST.test("objectGetOwnPropertyDescriptors", """
			const o = { a: 1 };
			Object.defineProperty(o, 'b', { value: 2, enumerable: false });
			const descs = Object.getOwnPropertyDescriptors(o);
			console.info(descs.a.value);
			console.info(descs.a.enumerable);
			console.info(descs.b.value);
			console.info(descs.b.enumerable);
			""", """
			1
			true
			2
			false
			""");
	}

	@Test
	public void objectGroupBy() {
		TEST.test("objectGroupBy", """
			const inventory = [
				{ name: 'asparagus', type: 'vegetables', quantity: 5 },
				{ name: 'bananas', type: 'fruit', quantity: 0 },
				{ name: 'goat', type: 'meat', quantity: 23 },
				{ name: 'cherries', type: 'fruit', quantity: 5 }
			];
			const byType = Object.groupBy(inventory, i => i.type);
			console.info(Object.keys(byType).join(','));
			console.info(byType.fruit.map(i => i.name).join(','));
			console.info(Object.getPrototypeOf(byType));
			const byIndex = Object.groupBy([10, 20], (v, i) => i);
			console.info(byIndex[0][0] + ',' + byIndex[1][0]);
			try {
				Object.groupBy([1], 'nope');
			} catch (e) {
				console.info('type error');
			}
			""", """
			vegetables,fruit,meat
			bananas,cherries
			null
			10,20
			type error
			""");
	}

	@Test
	public void objectHasOwn() {
		TEST.test("objectHasOwn", """
			const o = { a: 1 };
			console.info(Object.hasOwn(o, 'a'));
			console.info(Object.hasOwn(o, 'b'));
			console.info(Object.hasOwn(o, 'toString'));
			console.info(Object.hasOwn([1, 2], 1));
			console.info(Object.hasOwn([1, 2], 5));
			""", """
			true
			false
			false
			true
			false
			""");
	}

	@Test
	public void jsonStringifyNumbers() {
		TEST.test("jsonStringifyNumbers", """
			console.info(JSON.stringify(50.0))
			console.info(JSON.stringify(1.5))
			console.info(JSON.stringify({ a: 1, b: 2.5 }))
			console.info(JSON.stringify([1, 2, 3]))
			console.info(JSON.stringify({ a: Infinity, b: NaN, c: 1/0 }))
			""", """
			50
			1.5
			{"a":1,"b":2.5}
			[1,2,3]
			{"a":null,"b":null,"c":null}
			""");
	}

	@Test
	public void jsonStringifySpecial() {
		TEST.test("jsonStringifySpecial", """
			console.info(JSON.stringify(undefined))
			console.info(JSON.stringify(function () {}))
			console.info(JSON.stringify(Symbol('x')))
			console.info(JSON.stringify({ a: null, b: undefined, c: () => 4, d: 'ok' }))
			console.info(JSON.stringify([1, undefined, () => 4, 2]))
			""", """
			undefined
			undefined
			undefined
			{"a":null,"d":"ok"}
			[1,null,null,2]
			""");
	}

	@Test
	public void jsonStringifyWithNestedArrays() {
		TEST.test("jsonStringifyWithNestedArrays", """
			const thing = {nested: [1, 2, 3]};
			console.info(JSON.stringify(thing));
			""", "{\"nested\":[1,2,3]}");
	}

	@Test
	@Order(1)
	public void javaListInit() {
		TEST.test("javaListInit", """
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
	@Order(2)
	public void javaListLength() {
		TEST.test("javaListLength", """
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
	public void javaListPopShiftMap() {
		TEST.test("javaListPopShiftMap", """
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
	@Order(5)
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
	@Order(6)
	public void javaArrayIteration() {
		TEST.test("javaArrayIteration", """
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
	public void functionNameLengthConfigurable() {
		TEST.test("functionNameLengthConfigurable", """
			function foo(a, b) {}
			console.info(foo.name + ' ' + foo.length);
			const nameDesc = Object.getOwnPropertyDescriptor(foo, 'name');
			console.info(nameDesc.configurable + ' ' + nameDesc.writable + ' ' + nameDesc.enumerable);
			const lenDesc = Object.getOwnPropertyDescriptor(foo, 'length');
			console.info(lenDesc.configurable + ' ' + lenDesc.writable + ' ' + lenDesc.enumerable);
			delete foo.name;
			console.info('[' + foo.name + ']');
			delete foo.length;
			// after deletion the inherited Function.prototype.length (0) shows through
			console.info(foo.length);
			const protoDesc = Object.getOwnPropertyDescriptor(Function.prototype, 'name');
			console.info(protoDesc.configurable);
			""", """
			foo 2
			true false false
			true false false
			[]
			0
			true
			""");
	}

	@Test
	public void indexedAccessorInLiteral() {
		TEST.test("indexedAccessorInLiteral", """
			const o = {
				get 0() { return 'zero'; },
				get x() { return 'ex'; }
			};
			console.info(o[0]);
			console.info(o.x);
			let captured = null;
			const o2 = { set 1(v) { captured = v; } };
			o2[1] = 'one';
			console.info(captured);
			""", """
			zero
			ex
			one
			""");
	}

}

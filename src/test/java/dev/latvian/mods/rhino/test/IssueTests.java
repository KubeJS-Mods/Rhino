package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for issues reported on the KubeJS-Mods/Rhino tracker.
 */
@SuppressWarnings("unused")
public class IssueTests {
	public static final RhinoTest TEST = new RhinoTest("issues");

	@Test
	public void numberKeyedObjectValues() {
		// https://github.com/KubeJS-Mods/Rhino/issues/69
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
	public void charCodeAt() {
		// https://github.com/KubeJS-Mods/Rhino/issues/42
		TEST.test("charCodeAt", """
			console.info('0'.charCodeAt(0))
			console.info('0'.charCodeAt(0) + 1)
			console.info(typeof '0'.charCodeAt(0))
			console.info(''.charCodeAt(0))
			""", """
			48
			49
			number
			NaN
			""");
	}

	@Test
	public void symbolDescription() {
		// https://github.com/KubeJS-Mods/Rhino/issues/43
		TEST.test("symbolDescription", """
			console.info(Symbol('desc').description)
			console.info(Symbol.iterator.description)
			console.info(Symbol.for('foo').description)
			console.info(`${Symbol('foo').description}bar`)
			console.info(Symbol().description)
			console.info(Symbol.keyFor(Symbol.for('foo')))
			""", """
			desc
			Symbol.iterator
			foo
			foobar
			undefined
			foo
			""");
	}

	@Test
	public void jsonStringifyUnserializable() {
		// https://github.com/KubeJS-Mods/Rhino/issues/39
		TEST.test("jsonStringifyUnserializable", """
			console.info(JSON.stringify(undefined))
			console.info(JSON.stringify(function () {}))
			console.info(JSON.stringify(Symbol('x')))
			console.info(JSON.stringify({ a: 1, b: undefined, c: () => 4, d: 'ok' }))
			console.info(JSON.stringify([1, undefined, () => 4, 2]))
			""", """
			undefined
			undefined
			undefined
			{"a":1,"d":"ok"}
			[1,null,null,2]
			""");
	}

	@Test
	public void jsonStringifyNumbers() {
		TEST.test("jsonStringifyNumbers", """
			console.info(JSON.stringify(50))
			console.info(JSON.stringify(1.5))
			console.info(JSON.stringify({ a: 1, b: 2.5 }))
			console.info(JSON.stringify([1, 2, 3]))
			console.info(JSON.stringify(NaN))
			console.info(JSON.stringify(Infinity))
			""", """
			50
			1.5
			{"a":1,"b":2.5}
			[1,2,3]
			null
			null
			""");
	}

	@Test
	public void treeMapWithNonStringKeys() {
		// https://github.com/KubeJS-Mods/Rhino/issues/67
		TEST.test("treeMapWithNonStringKeys", """
			let map = StaticUtils.treeMapWithUuidKeys()
			map.computeIfAbsent(StaticUtils.uuid('40bd7d77-23f0-4e1c-ae64-4af2d4b0a18d'), k => 'computed')
			console.info(map.size())
			console.info(map.get(StaticUtils.uuid('40bd7d77-23f0-4e1c-ae64-4af2d4b0a18d')))
			""", """
			2
			computed
			""");
	}

	@Test
	public void overriddenDefaultMethod() {
		// https://github.com/KubeJS-Mods/Rhino/issues/65
		TEST.test("overriddenDefaultMethod", """
			console.info(StaticUtils.callDefaulted({}))
			console.info(StaticUtils.callDefaulted({ someMethod: () => 'overridden' }))
			console.info(StaticUtils.callDefaultedAbstract({ someMethod: () => 'overridden', abstractMethod: () => 'js abstract' }))
			""", """
			default
			overridden
			overridden,js abstract
			""");
	}
}

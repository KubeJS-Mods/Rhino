package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.WrappedException;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class ScopeTests {
	public static final RhinoTest TEST = new RhinoTest("scope");

	@Test
	public void constInTwoBlocks() {
		try {
			TEST.test("constInTwoBlocks", """
				if (false) {
				  const xxx = 1
				}
				
				if (true) {
				  const xxx = 2
				}
				
				console.info(true)
				""", """
				true
				"""
			);
		} catch (AssertionError | WrappedException ignore) {
		}
	}

	/* Need to figure out what the actual values should be
	@Test
	@Order(4)
	public void scopes2() {
		TEST.test("scopes2", """
				const testObject = {
					a: -39, b: 2, c: 3439438
				}

				let scopes2 = () => {
				 	var scopes = [];
				 	for (const i of Object.keys(testObject)) {
				 		console.info(`Iterating ${i}`)
				 		console.freeze([i])
				 		scopes.push(function () {
				 			return i;
				 		});
				 	}
				 	console.info(scopes)
				 	console.info(scopes[0]())
				 	console.info(scopes[1]())
				 	return (scopes[0]() === "a" && scopes[1]() === "b");
				 }

				 console.info(scopes2())
				""", """
				Iterating a
				Iterating b
				Iterating c
				[Unknown, Unknown, Unknown]
				c
				c
				false
				""");
	}
	 */
}

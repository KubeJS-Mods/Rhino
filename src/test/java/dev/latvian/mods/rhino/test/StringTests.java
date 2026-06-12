package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

/**
 * Tests for String and RegExp.
 */
@SuppressWarnings("unused")
public class StringTests {
	public static final RhinoTest TEST = new RhinoTest("strings");

	@Test
	public void stringAt() {
		TEST.test("stringAt", """
			const s = 'abcde';
			console.info(s.at(0));
			console.info(s.at(-1));
			console.info(s.at(10));
			""", """
			a
			e
			undefined
			""");
	}

	@Test
	public void stringReplaceAll() {
		TEST.test("stringReplaceAll", """
			console.info('aabbcc'.replaceAll('b', '.'));
			console.info('aabbcc'.replaceAll('', '-'));
			console.info('aabbcc'.replaceAll(/b/g, '.'));
			console.info('x=1, y=2'.replaceAll(/(\\w)=(\\d)/g, '$2:$1'));
			console.info('aabbcc'.replaceAll('b', m => m.toUpperCase()));
			try {
				'aabbcc'.replaceAll(/b/, '.');
			} catch (e) {
				console.info('type error');
			}
			""", """
			aa..cc
			-a-a-b-b-c-c-
			aa..cc
			1:x, 2:y
			aaBBcc
			type error
			""");
	}

	@Test
	public void regexpConstructorWithFlags() {
		TEST.test("regexpConstructorWithFlags", """
			const base = /ab+c/g;
			const re = new RegExp(base, 'i');
			console.info(re.source);
			console.info(re.global);
			console.info(re.ignoreCase);
			console.info(re.test('ABBC'));
			const copy = new RegExp(base);
			console.info(copy.source + ' ' + copy.global);
			console.info(new RegExp(/x/m, undefined).multiline);
			""", """
			ab+c
			false
			true
			true
			ab+c true
			true
			""");
	}
}

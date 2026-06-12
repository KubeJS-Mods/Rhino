package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiscTests {
	public static final RhinoTest TEST = new RhinoTest("misc");

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
	public void errorCause() {
		TEST.test("errorCause", """
			const e = new Error('msg', { cause: 'root' });
			console.info(e.cause);
			console.info(e.message);
			const e2 = new Error('msg2');
			console.info('cause' in e2);
			const e3 = new TypeError('bad', { cause: 42 });
			console.info(e3.cause);
			const e4 = new Error('m', { cause: undefined });
			console.info('cause' in e4);
			""", """
			root
			msg
			false
			42
			true
			""");
	}

	@Test
	public void errorIsError() {
		TEST.test("errorIsError", """
			console.info(Error.isError(new Error('x')));
			console.info(Error.isError(new TypeError('x')));
			console.info(Error.isError({ name: 'Error', message: 'x' }));
			console.info(Error.isError('Error'));
			console.info(Error.isError());
			console.info(Error.isError(null));
			""", """
			true
			true
			false
			false
			false
			false
			""");
	}

	@Test
	public void functionAssign() {
		TEST.test("functionAssign", """
			let x = () => {};
			x.abc = 1;
			console.info(x.abc);
			""", "1");
	}

	@Test
	public void functionProto() {
		TEST.test("functionProto", """
			function F() {}
			const p = { x: 1 };
			F.__proto__ = p;
			console.info(F.x);
			const o = {};
			o.__proto__ = { y: 2 };
			console.info(o.y);
			""", """
			1
			2
			""");
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

	@Test
	public void testDelete() {
		TEST.test("delete", "let x = {a: 1}; delete x.a; console.info(x.a);", "undefined");
	}

	@Test
	public void mathF16round() {
		TEST.test("mathF16round", """
			console.info(Math.f16round(5.5));
			console.info(Math.f16round(5.05));
			console.info(Math.f16round(0.1));
			console.info(Math.f16round(NaN));
			console.info(Math.f16round(Infinity));
			console.info(Math.f16round(-Infinity));
			console.info(1 / Math.f16round(-0));
			console.info(Math.f16round(65520));
			console.info(Math.f16round(65519.999));
			console.info(Math.f16round(5.960464477539063e-8));
			console.info(Math.f16round(2.980232238769531e-8));
			console.info(Math.f16round(1.337));
			console.info(Math.f16round());
			""", """
			5.5
			5.05078125
			0.0999755859375
			NaN
			Infinity
			-Infinity
			-Infinity
			Infinity
			65504
			5.960464477539063e-8
			0
			1.3369140625
			NaN
			""");
	}

	@Test
	public void symbolDescription() {
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
	public void privateInnerClassAccess() {
		TEST.test("privateInnerClassAccess", """
			console.info(console.immutableTestList.size())
			for(let s of console.immutableTestList) {
			  console.info(s)
			}
			""", """
			3
			abc
			def
			ghi
			""");
	}

	@Test
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
	public void varargs() {
		TEST.test("varargs", """
			console.varargTest("hi", 1, 2, 3);
			""", "VarArg Ints hi: [1, 2, 3]");
	}
}

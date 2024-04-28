package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.Test;

@SuppressWarnings("unused")
public class NBTTests {
	public static final RhinoTest TEST = new RhinoTest("nbt");

	@Test
	public void compound() {
		TEST.test("compound", """
				let compoundTagTest = NBT.compoundTag()
								
				const testObject = {
					a: -39, b: '2', c: 3439438.1
				}
				    
				compoundTagTest.merge(testObject);
				console.info(compoundTagTest)
				""", """
				{a:-39.0d,b:"2",c:3439438.1d}
				""");
	}

	@Test
	public void list() {
		TEST.test("list", """
				let listTagTest = NBT.listTag()
				    
				listTagTest.push('a')
				listTagTest.push('b')
				listTagTest.push('c')
				    
				console.info(listTagTest)
				""", """
				["a","b","c"]
				""");
	}
}

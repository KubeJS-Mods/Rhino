package dev.latvian.mods.rhino.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ScopeTests {
	public static RhinoTest TEST = new RhinoTest("scope");

	@Test
	@DisplayName("Const in Two Blocks")
	public void constInTwoBlocks() {
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
				""");
	}
}

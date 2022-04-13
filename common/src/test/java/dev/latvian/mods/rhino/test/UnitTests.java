package dev.latvian.mods.rhino.test;

import dev.latvian.mods.unit.UnitContext;
import dev.latvian.mods.unit.token.UnitTokenStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class UnitTests {

	@Test
	@DisplayName("Ternary Token Stream")
	public void testTernaryTokenStream() {
		UnitTokenStream stream = UnitContext.DEFAULT.createStream("0 < 5 ? 1.5 : 2");
		Assertions.assertEquals(Arrays.asList("0.0", "<", "5.0", "?", "1.5", ":", "2.0"), stream.toTokenStrings());
	}
}

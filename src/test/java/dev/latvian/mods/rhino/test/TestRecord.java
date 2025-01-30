package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.RemapForJS;

import java.util.Optional;

public record TestRecord(int num, Optional<String> str, @RemapForJS("sub") TestRecord subRecord) {
	public static final TestRecord DEFAULT = new TestRecord(420, Optional.empty(), null);
}

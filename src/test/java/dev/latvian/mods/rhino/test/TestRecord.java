package dev.latvian.mods.rhino.test;

import java.util.Optional;

public record TestRecord(int num, Optional<String> str, TestRecord sub) {
}

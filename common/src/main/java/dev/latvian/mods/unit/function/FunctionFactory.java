package dev.latvian.mods.unit.function;

import java.util.function.Supplier;

public record FunctionFactory(String name, Supplier<FuncUnit> factory) {
}

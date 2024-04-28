package dev.latvian.mods.unit;

import org.jetbrains.annotations.Nullable;

public class EmptyVariableSet extends VariableSet {

	public static final EmptyVariableSet INSTANCE = new EmptyVariableSet();

	private EmptyVariableSet() {
	}

	@Override
	public VariableSet set(String name, Unit value) {
		return this;
	}

	@Override
	public VariableSet set(String name, double value) {
		return this;
	}

	@Override
	public MutableNumberUnit setMutable(String name, double initialValue) {
		return new MutableNumberUnit(initialValue);
	}

	@Override
	@Nullable
	public Unit get(String entry) {
		return null;
	}

	@Override
	public VariableSet createSubset() {
		return new VariableSet();
	}
}

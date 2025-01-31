package dev.latvian.mods.rhino.test;

import java.util.AbstractList;
import java.util.List;

public class PrivateListTest {
	public static final String[] ELEMENTS = {"abc", "def", "ghi"};
	public static final List<String> TEST_LIST = new TestList();

	private static class TestList extends AbstractList<String> {
		@Override
		public String get(int i) {
			return ELEMENTS[i];
		}

		@Override
		public int size() {
			return ELEMENTS.length;
		}
	}
}

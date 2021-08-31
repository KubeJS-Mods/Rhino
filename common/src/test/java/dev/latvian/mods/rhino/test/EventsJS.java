package dev.latvian.mods.rhino.test;

import dev.latvian.mods.rhino.util.DataObject;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.RemapForJS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventsJS {
	public List<Consumer<Object>> lastCallback = new ArrayList<>();
	private final DynamicMap<DynamicMap<Integer>> dynamicMap0 = new DynamicMap<>(s1 -> new DynamicMap<>(s2 -> s1.hashCode() + s2.hashCode()));
	public ResourceLocation someIdField = null;

	public void listen(String id, Consumer<Object> callback) {
		lastCallback.add(callback);
		System.out.println(id + ": " + callback);
	}

	public void testList(List<Object> strings) {
		System.out.println(strings.size());
	}

	public void testArray(int[] strings) {
		System.out.println(strings.length);
	}

	public void testMap(Map<String, Object> strings) {
		System.out.println(strings.size());
	}

	public String getAbc() {
		return "ABC";
	}

	public boolean isAbcd() {
		return true;
	}

	public void setAbc(String val) {
	}

	public static class DataTest {
		public int someInt;
		public String someString;
	}

	public void testData(DataObject data) {
		for (DataTest test : data.createDataObjectList(DataTest::new)) {
			System.out.println("Test: " + test.someString + " : " + test.someInt);
		}
	}

	public int[] getNumberList() {
		return new int[]{20, 94, 3034, -3030};
	}

	public int setNumberList(int[] i) {
		System.out.println("Set number list: " + Arrays.toString(i));
		return 0;
	}

	public DynamicMap<DynamicMap<Integer>> getDynamicMap() {
		return dynamicMap0;
	}

	@RemapForJS("testWrapper")
	public void testWrapper123(ResourceLocation item, int a, int b, int c) {
		System.out.println("Testing wrapper: " + item + ", " + a + ", " + b + ", " + c);
	}

	@RemapForJS("testWrapper2")
	public void testWrapper123(ResourceLocation[] item) {
		System.out.println("Testing wrapper: " + Arrays.asList(item));
	}

	@RemapForJS("testWrapper3")
	public void testWrapper123(ResourceLocation[][][] item) {
		System.out.println("Testing wrapper: " + Arrays.asList(item));
	}

	public void setSomeId(String id) {
		System.out.println("Some ID set to (String): " + id);
	}

	public void setSomeId(ResourceLocation id) {
		System.out.println("Some ID set to: " + id);
	}

	public void testRLArray(String id, ResourceLocation[] ids) {
		System.out.println(id + ": " + Arrays.toString(ids));
	}

	public TestMapLike getMapLike() {
		return new TestMapLike();
	}

	public String getTestString() {
		return new String(new char[]{'a', 'b', 'c'});
	}
}
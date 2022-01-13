package dev.latvian.mods.rhino.mod.util;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface NBTWrapper {
	@Nullable
	static Object fromTag(@Nullable Tag t) {
		if (t == null || t == EndTag.INSTANCE) {
			return null;
		} else if (t instanceof StringTag) {
			return t.getAsString();
		} else if (t instanceof NumericTag) {
			return ((NumericTag) t).getAsNumber();
		}

		return t;
	}

	@Nullable
	static Tag toTag(@Nullable Object v) {
		if (v == null || v == EndTag.INSTANCE) {
			return null;
		} else if (v instanceof Tag) {
			return (Tag) v;
		} else if (v instanceof String) {
			return StringTag.valueOf(v.toString());
		} else if (v instanceof Number) {
			return DoubleTag.valueOf(((Number) v).doubleValue());
		} else if (v instanceof Boolean) {
			return ByteTag.valueOf((Boolean) v);
		} else if (v instanceof Map) {
			return compoundTag((Map) v);
		} else if (v instanceof List<?>) {
			return listTag((List) v);
		}

		return null;
	}

	static Tag compoundTag() {
		return new OrderedCompoundTag();
	}

	static Tag compoundTag(Map<String, Object> map) {
		OrderedCompoundTag tag = new OrderedCompoundTag();

		for (Map.Entry<String, Object> e : map.entrySet()) {
			tag.put(e.getKey(), toTag(e.getValue()));
		}

		return tag;
	}

	static Tag listTag() {
		return new ListTag();
	}

	static Tag listTag(List<Object> list) {
		ListTag tag = new ListTag();

		for (Object v : list) {
			tag.add(toTag(v));
		}

		return tag;
	}

	static Tag byteTag(byte v) {
		return ByteTag.valueOf(v);
	}

	static Tag b(byte v) {
		return ByteTag.valueOf(v);
	}

	static Tag shortTag(short v) {
		return ShortTag.valueOf(v);
	}

	static Tag s(short v) {
		return ShortTag.valueOf(v);
	}

	static Tag intTag(int v) {
		return IntTag.valueOf(v);
	}

	static Tag i(int v) {
		return IntTag.valueOf(v);
	}

	static Tag longTag(long v) {
		return LongTag.valueOf(v);
	}

	static Tag l(long v) {
		return LongTag.valueOf(v);
	}

	static Tag floatTag(float v) {
		return FloatTag.valueOf(v);
	}

	static Tag f(float v) {
		return FloatTag.valueOf(v);
	}

	static Tag doubleTag(double v) {
		return DoubleTag.valueOf(v);
	}

	static Tag d(double v) {
		return DoubleTag.valueOf(v);
	}

	static Tag stringTag(String v) {
		return StringTag.valueOf(v);
	}

	static Tag intArrayTag(int[] v) {
		return new IntArrayTag(v);
	}

	static Tag ia(int[] v) {
		return new IntArrayTag(v);
	}

	static Tag longArrayTag(long[] v) {
		return new LongArrayTag(v);
	}

	static Tag la(long[] v) {
		return new LongArrayTag(v);
	}

	static Tag byteArrayTag(byte[] v) {
		return new ByteArrayTag(v);
	}

	static Tag ba(byte[] v) {
		return new ByteArrayTag(v);
	}
}

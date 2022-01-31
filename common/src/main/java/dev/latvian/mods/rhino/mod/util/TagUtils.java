package dev.latvian.mods.rhino.mod.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public class TagUtils {
	public static Object unwrap(@Nullable Tag t) {
		if (t == null || t instanceof EndTag) {
			return null;
		} else if (t instanceof StringTag) {
			return t.getAsString();
		} else if (t instanceof NumericTag) {
			return ((NumericTag) t).getAsNumber();
		}

		return t;
	}

	public static Tag wrap(@Nullable Object o) {
		if (o instanceof Tag) {
			return (Tag) o;
		} else if (o instanceof Number) {
			return DoubleTag.valueOf(((Number) o).doubleValue());
		} else if (o instanceof CharSequence) {
			return StringTag.valueOf(o.toString());
		}

		return null;
	}

	public static JsonElement toJson(@Nullable Tag t) {
		if (t == null || t instanceof EndTag) {
			return JsonNull.INSTANCE;
		} else if (t instanceof StringTag) {
			return new JsonPrimitive(t.getAsString());
		} else if (t instanceof NumericTag) {
			return new JsonPrimitive(((NumericTag) t).getAsNumber());
		} else if (t instanceof CollectionTag<?> l) {
			JsonArray array = new JsonArray();

			for (Tag tag : l) {
				array.add(toJson(tag));
			}

			return array;
		} else if (t instanceof CompoundTag c) {
			JsonObject object = new JsonObject();

			for (String key : c.getAllKeys()) {
				object.add(key, toJson(c.get(key)));
			}

			return object;
		}

		return JsonNull.INSTANCE;
	}
}

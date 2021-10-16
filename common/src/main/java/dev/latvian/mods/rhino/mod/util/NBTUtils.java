package dev.latvian.mods.rhino.mod.util;

import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class NBTUtils {
	@Nullable
	public static Tag toNBT(@Nullable Object o) {
		if (o instanceof Tag) {
			return (Tag) o;
		} else if (o instanceof NBTSerializable) {
			return ((NBTSerializable) o).toNBT();
		} else if (o instanceof CharSequence || o instanceof Character) {
			return StringTag.valueOf(o.toString());
		} else if (o instanceof Boolean) {
			return ByteTag.valueOf((Boolean) o ? (byte) 1 : (byte) 0);
		} else if (o instanceof Number) {
			Number number = (Number) o;

			if (number instanceof Byte) {
				return ByteTag.valueOf(number.byteValue());
			} else if (number instanceof Short) {
				return ShortTag.valueOf(number.shortValue());
			} else if (number instanceof Integer) {
				return IntTag.valueOf(number.intValue());
			} else if (number instanceof Long) {
				return LongTag.valueOf(number.longValue());
			} else if (number instanceof Float) {
				return FloatTag.valueOf(number.floatValue());
			}

			return DoubleTag.valueOf(number.doubleValue());
		} else if (o instanceof Map) {
			CompoundTag tag = new OrderedCompoundTag();

			for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
				Tag nbt1 = NBTUtils.toNBT(entry.getValue());

				if (nbt1 != null) {
					tag.put(String.valueOf(entry.getKey()), nbt1);
				}
			}

			return tag;
		} else if (o instanceof Collection) {
			return toNBT((Collection<?>) o);
		}

		return null;
	}

	public static CollectionTag<?> toNBT(Collection<?> c) {
		if (c.isEmpty()) {
			return new ListTag();
		}

		Tag[] values = new Tag[c.size()];
		int s = 0;
		byte commmonId = -1;

		for (Object o : c) {
			values[s] = toNBT(o);

			if (values[s] != null) {
				if (commmonId == -1) {
					commmonId = values[s].getId();
				} else if (commmonId != values[s].getId()) {
					commmonId = 0;
				}

				s++;
			}
		}

		if (commmonId == NbtType.INT) {
			int[] array = new int[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsInt();
			}

			return new IntArrayTag(array);
		} else if (commmonId == NbtType.BYTE) {
			byte[] array = new byte[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsByte();
			}

			return new ByteArrayTag(array);
		} else if (commmonId == NbtType.LONG) {
			long[] array = new long[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsLong();
			}

			return new LongArrayTag(array);
		} else if (commmonId == 0 || commmonId == -1) {
			return new ListTag();
		}

		ListTag nbt = new ListTag();

		for (Tag nbt1 : values) {
			if (nbt1 == null) {
				return nbt;
			}

			nbt.add(nbt1);
		}

		return nbt;
	}

	public static void quoteAndEscapeForJS(StringBuilder stringBuilder, String string) {
		int start = stringBuilder.length();
		stringBuilder.append(' ');
		char c = 0;

		for (int i = 0; i < string.length(); ++i) {
			char d = string.charAt(i);
			if (d == '\\') {
				stringBuilder.append('\\');
			} else if (d == '"' || d == '\'') {
				if (c == 0) {
					c = d == '\'' ? '"' : '\'';
				}

				if (c == d) {
					stringBuilder.append('\\');
				}
			}

			stringBuilder.append(d);
		}

		if (c == 0) {
			c = '\'';
		}

		stringBuilder.setCharAt(start, c);
		stringBuilder.append(c);
	}

	private static TagType<?> convertType(TagType<?> tagType) {
		return tagType == CompoundTag.TYPE ? COMPOUND_TYPE : tagType == ListTag.TYPE ? LIST_TYPE : tagType;
	}

	private static final TagType<OrderedCompoundTag> COMPOUND_TYPE = new TagType<OrderedCompoundTag>() {
		@Override
		public OrderedCompoundTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
			nbtAccounter.accountBits(384L);
			if (i > 512) {
				throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
			} else {
				Map<String, Tag> map = new LinkedHashMap<>();

				byte b;
				while ((b = dataInput.readByte()) != 0) {
					String string = dataInput.readUTF();
					nbtAccounter.accountBits(224L + 16L * string.length());
					TagType<?> tagType = convertType(TagTypes.getType(b));
					Tag tag = tagType.load(dataInput, i + 1, nbtAccounter);

					if (map.put(string, tag) != null) {
						nbtAccounter.accountBits(288L);
					}
				}

				return new OrderedCompoundTag(map);
			}
		}

		@Override
		public String getName() {
			return "COMPOUND";
		}

		@Override
		public String getPrettyName() {
			return "TAG_Compound";
		}
	};

	private static final TagType<ListTag> LIST_TYPE = new TagType<ListTag>() {
		@Override
		public ListTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
			nbtAccounter.accountBits(296L);
			if (i > 512) {
				throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
			} else {
				byte b = dataInput.readByte();
				int j = dataInput.readInt();
				if (b == 0 && j > 0) {
					throw new RuntimeException("Missing type on ListTag");
				} else {
					nbtAccounter.accountBits(32L * (long) j);
					TagType<?> tagType = convertType(TagTypes.getType(b));
					ListTag list = new ListTag();

					for (int k = 0; k < j; ++k) {
						list.add(tagType.load(dataInput, i + 1, nbtAccounter));
					}

					return list;
				}
			}
		}

		@Override
		public String getName() {
			return "LIST";
		}

		@Override
		public String getPrettyName() {
			return "TAG_List";
		}
	};

	@Nullable
	public static OrderedCompoundTag read(FriendlyByteBuf buf) {
		int i = buf.readerIndex();
		byte b = buf.readByte();
		if (b == 0) {
			return null;
		} else {
			buf.readerIndex(i);

			try {
				DataInputStream stream = new DataInputStream(new ByteBufInputStream(buf));

				byte b1 = stream.readByte();
				if (b1 == 0) {
					return null;
				} else {
					stream.readUTF();
					TagType<?> tagType = convertType(TagTypes.getType(b1));

					if (tagType != COMPOUND_TYPE) {
						return null;
					}

					return COMPOUND_TYPE.load(stream, 0, NbtAccounter.UNLIMITED);
				}
			} catch (IOException var5) {
				throw new EncoderException(var5);
			}
		}
	}
}
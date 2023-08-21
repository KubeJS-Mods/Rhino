package dev.latvian.mods.rhino.mod.wrapper;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public interface UUIDWrapper {
	static String toString(@Nullable UUID id) {
		if (id != null) {
			long msb = id.getMostSignificantBits();
			long lsb = id.getLeastSignificantBits();
			StringBuilder sb = new StringBuilder(32);
			digits(sb, msb >> 32, 8);
			digits(sb, msb >> 16, 4);
			digits(sb, msb, 4);
			digits(sb, lsb >> 48, 4);
			digits(sb, lsb, 12);
			return sb.toString();
		}

		return "";
	}

	static void digits(StringBuilder sb, long val, int digits) {
		long hi = 1L << (digits * 4);
		String s = Long.toHexString(hi | (val & (hi - 1)));
		sb.append(s, 1, s.length());
	}

	@Nullable
	static UUID fromString(Object o) {
		if (o instanceof UUID) {
			return (UUID) o;
		} else if (o == null) {
			return null;
		}

		String s = String.valueOf(o);

		if (!(s.length() == 32 || s.length() == 36)) {
			return null;
		}

		try {
			if (s.indexOf('-') != -1) {
				return UUID.fromString(s);
			}

			int l = s.length();
			StringBuilder sb = new StringBuilder(36);
			for (int i = 0; i < l; i++) {
				sb.append(s.charAt(i));
				if (i == 7 || i == 11 || i == 15 || i == 19) {
					sb.append('-');
				}
			}

			return UUID.fromString(sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
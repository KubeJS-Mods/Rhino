/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.typedarrays;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.IdScriptableObject;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Undefined;

import java.io.Serial;

/**
 * This class is the abstract parent for all views of the array. It shows a view of the underlying
 * NativeArrayBuffer. Many views may simultaneously share the same buffer, and changes to one will affect all.
 */

public abstract class NativeArrayBufferView extends IdScriptableObject {
	@Serial
	private static final long serialVersionUID = 6884475582973958419L;

	private static Boolean useLittleEndian = null;

	/**
	 * Many view objects can share the same backing array
	 */
	protected final NativeArrayBuffer arrayBuffer;
	/**
	 * The offset, in bytes, from the start of the backing array
	 */
	protected final int offset;
	/**
	 * The length, in bytes, of the portion of the backing array that we use
	 */
	protected final int byteLength;

	public NativeArrayBufferView() {
		arrayBuffer = new NativeArrayBuffer();
		offset = 0;
		byteLength = 0;
	}

	protected NativeArrayBufferView(NativeArrayBuffer ab, int offset, int byteLength) {
		this.offset = offset;
		this.byteLength = byteLength;
		this.arrayBuffer = ab;
	}

	/**
	 * Return the buffer that backs this view.
	 */
	public NativeArrayBuffer getBuffer() {
		return arrayBuffer;
	}

	/**
	 * Return the offset in bytes from the start of the buffer that this view represents.
	 */
	public int getByteOffset() {
		return offset;
	}

	/**
	 * Return the length, in bytes, of the part of the buffer that this view represents.
	 */
	public int getByteLength() {
		return byteLength;
	}

	protected static boolean useLittleEndian() {
		if (useLittleEndian == null) {
			Context ctx = Context.getCurrentContext();
			// for some unit tests this might be null
			if (ctx == null) {
				return false;
			}
			useLittleEndian = ctx.hasFeature(Context.FEATURE_LITTLE_ENDIAN);
		}
		return useLittleEndian;
	}

	protected static boolean isArg(Object[] args, int i) {
		return ((args.length > i) && !Undefined.instance.equals(args[i]));
	}

	// Property dispatcher

	@Override
	protected int getMaxInstanceId() {
		return MAX_INSTANCE_ID;
	}

	@Override
	protected String getInstanceIdName(int id) {
		return switch (id) {
			case Id_buffer -> "buffer";
			case Id_byteOffset -> "byteOffset";
			case Id_byteLength -> "byteLength";
			default -> super.getInstanceIdName(id);
		};
	}

	@Override
	protected Object getInstanceIdValue(int id) {
		return switch (id) {
			case Id_buffer -> arrayBuffer;
			case Id_byteOffset -> ScriptRuntime.wrapInt(offset);
			case Id_byteLength -> ScriptRuntime.wrapInt(byteLength);
			default -> super.getInstanceIdValue(id);
		};
	}

	@Override
	protected int findInstanceIdInfo(String s) {
		int id = switch (s) {
			case "buffer" -> Id_buffer;
			case "byteOffset" -> Id_byteOffset;
			case "byteLength" -> Id_byteLength;
			default -> 0;
		};

		if (id == 0) {
			return super.findInstanceIdInfo(s);
		}

		return instanceIdInfo(READONLY | PERMANENT, id);
	}

	private static final int Id_buffer = 1;
	private static final int Id_byteOffset = 2;
	private static final int Id_byteLength = 3;

	// to be visible by subclasses
	protected static final int MAX_INSTANCE_ID = Id_byteLength;

	// #/string_id_map#
}

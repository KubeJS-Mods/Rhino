/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

/**
 * Class instances represent serializable tags to mark special Object values.
 * <p>
 * Compatibility note: under jdk 1.1 use
 * dev.latvian.mods.rhino.serialize.ScriptableInputStream to read serialized
 * instances of UniqueTag as under this JDK version the default
 * ObjectInputStream would not restore them correctly as it lacks support
 * for readResolve method
 */
public final class UniqueTag {

	private static final int ID_NOT_FOUND = 1;
	/**
	 * Tag to mark non-existing values.
	 */
	public static final UniqueTag NOT_FOUND = new UniqueTag(ID_NOT_FOUND);
	private static final int ID_NULL_VALUE = 2;
	/**
	 * Tag to distinguish between uninitialized and null values.
	 */
	public static final UniqueTag NULL_VALUE = new UniqueTag(ID_NULL_VALUE);
	private static final int ID_DOUBLE_MARK = 3;
	/**
	 * Tag to indicate that a object represents "double" with the real value
	 * stored somewhere else.
	 */
	public static final UniqueTag DOUBLE_MARK = new UniqueTag(ID_DOUBLE_MARK);

	private final int tagId;

	private UniqueTag(int tagId) {
		this.tagId = tagId;
	}

	// Overridden for better debug printouts
	@Override
	public String toString() {
		String name = switch (tagId) {
			case ID_NOT_FOUND -> "NOT_FOUND";
			case ID_NULL_VALUE -> "NULL_VALUE";
			case ID_DOUBLE_MARK -> "DOUBLE_MARK";
			default -> throw Kit.codeBug();
		};
		return super.toString() + ": " + name;
	}

}


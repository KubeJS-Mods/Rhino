/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public class ConsString {
	public static String flatten(CharSequence left, CharSequence right) {
		int l = left.length();
		int r = right.length();
		var chars = new char[l + r];
		left.toString().getChars(0, l, chars, 0);
		right.toString().getChars(0, r, chars, l);
		return new String(chars);
	}
}

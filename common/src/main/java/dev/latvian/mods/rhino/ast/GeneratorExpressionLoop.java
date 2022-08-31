/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

/**
 *
 */
public class GeneratorExpressionLoop extends ForInLoop {

	public GeneratorExpressionLoop() {
	}

	public GeneratorExpressionLoop(int pos) {
		super(pos);
	}

	public GeneratorExpressionLoop(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns whether the loop is a for-each loop
	 */
	@Override
	public boolean isForEach() {
		return false;
	}

	/**
	 * Sets whether the loop is a for-each loop
	 */
	@Override
	public void setIsForEach(boolean isForEach) {
		throw new UnsupportedOperationException("this node type does not support for each");
	}
}

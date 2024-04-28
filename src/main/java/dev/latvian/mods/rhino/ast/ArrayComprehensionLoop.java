/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

import dev.latvian.mods.rhino.Token;

/**
 * AST node for a single 'for (foo in bar)' loop construct in a JavaScript 1.7
 * Array comprehension. This node type is almost equivalent to a
 * {@link ForInLoop}, except that it has no body statement.
 * Node type is {@link Token#FOR}.
 */
public class ArrayComprehensionLoop extends ForInLoop {

	public ArrayComprehensionLoop() {
	}

	public ArrayComprehensionLoop(int pos) {
		super(pos);
	}

	public ArrayComprehensionLoop(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns {@code null} for loop body
	 *
	 * @return loop body (always {@code null} for this node type)
	 */
	@Override
	public AstNode getBody() {
		return null;
	}

	/**
	 * Throws an exception on attempts to set the loop body.
	 *
	 * @param body loop body
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void setBody(AstNode body) {
		throw new UnsupportedOperationException("this node type has no body");
	}
}

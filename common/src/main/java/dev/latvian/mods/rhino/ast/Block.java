/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

import dev.latvian.mods.rhino.Token;

/**
 * A block statement delimited by curly braces.  The node position is the
 * position of the open-curly, and the length extends to the position of
 * the close-curly.  Node type is {@link Token#BLOCK}.
 *
 * <pre><i>Block</i> :
 *     <b>{</b> Statement* <b>}</b></pre>
 */
public class Block extends AstNode {

	{
		this.type = Token.BLOCK;
	}

	public Block() {
	}

	public Block(int pos) {
		super(pos);
	}

	public Block(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Alias for {@link #addChild}.
	 */
	public void addStatement(AstNode statement) {
		addChild(statement);
	}
}

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

import dev.latvian.mods.rhino.Token;

/**
 * AST node for a Tagged Template Literal.
 * <p>Node type is {@link Token#TAGGED_TEMPLATE_LITERAL}.</p>
 */
public class TaggedTemplateLiteral extends AstNode {

	private AstNode target;
	private AstNode templateLiteral;

	{
		type = Token.TAGGED_TEMPLATE_LITERAL;
	}

	public TaggedTemplateLiteral() {
	}

	public TaggedTemplateLiteral(int pos) {
		super(pos);
	}

	public TaggedTemplateLiteral(int pos, int len) {
		super(pos, len);
	}

	public AstNode getTarget() {
		return target;
	}

	public void setTarget(AstNode target) {
		this.target = target;
	}

	public AstNode getTemplateLiteral() {
		return templateLiteral;
	}

	public void setTemplateLiteral(AstNode templateLiteral) {
		this.templateLiteral = templateLiteral;
	}

	/**
	 * Visits this node.
	 */
	@Override
	public void visit(NodeVisitor v) {
		if (v.visit(this)) {
			target.visit(v);
			templateLiteral.visit(v);
		}
	}
}
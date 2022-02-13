/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.ast.ArrayComprehension;
import dev.latvian.mods.rhino.ast.ArrayComprehensionLoop;
import dev.latvian.mods.rhino.ast.ArrayLiteral;
import dev.latvian.mods.rhino.ast.Assignment;
import dev.latvian.mods.rhino.ast.AstNode;
import dev.latvian.mods.rhino.ast.AstRoot;
import dev.latvian.mods.rhino.ast.AstSymbol;
import dev.latvian.mods.rhino.ast.Block;
import dev.latvian.mods.rhino.ast.CatchClause;
import dev.latvian.mods.rhino.ast.ConditionalExpression;
import dev.latvian.mods.rhino.ast.ContinueStatement;
import dev.latvian.mods.rhino.ast.DestructuringForm;
import dev.latvian.mods.rhino.ast.DoLoop;
import dev.latvian.mods.rhino.ast.ElementGet;
import dev.latvian.mods.rhino.ast.EmptyExpression;
import dev.latvian.mods.rhino.ast.ExpressionStatement;
import dev.latvian.mods.rhino.ast.ForInLoop;
import dev.latvian.mods.rhino.ast.ForLoop;
import dev.latvian.mods.rhino.ast.FunctionCall;
import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.GeneratorExpression;
import dev.latvian.mods.rhino.ast.GeneratorExpressionLoop;
import dev.latvian.mods.rhino.ast.IfStatement;
import dev.latvian.mods.rhino.ast.InfixExpression;
import dev.latvian.mods.rhino.ast.Jump;
import dev.latvian.mods.rhino.ast.Label;
import dev.latvian.mods.rhino.ast.LabeledStatement;
import dev.latvian.mods.rhino.ast.LetNode;
import dev.latvian.mods.rhino.ast.Name;
import dev.latvian.mods.rhino.ast.NewExpression;
import dev.latvian.mods.rhino.ast.NumberLiteral;
import dev.latvian.mods.rhino.ast.ObjectLiteral;
import dev.latvian.mods.rhino.ast.ObjectProperty;
import dev.latvian.mods.rhino.ast.ParenthesizedExpression;
import dev.latvian.mods.rhino.ast.PropertyGet;
import dev.latvian.mods.rhino.ast.RegExpLiteral;
import dev.latvian.mods.rhino.ast.ReturnStatement;
import dev.latvian.mods.rhino.ast.Scope;
import dev.latvian.mods.rhino.ast.ScriptNode;
import dev.latvian.mods.rhino.ast.StringLiteral;
import dev.latvian.mods.rhino.ast.SwitchCase;
import dev.latvian.mods.rhino.ast.SwitchStatement;
import dev.latvian.mods.rhino.ast.TaggedTemplateLiteral;
import dev.latvian.mods.rhino.ast.TemplateCharacters;
import dev.latvian.mods.rhino.ast.TemplateLiteral;
import dev.latvian.mods.rhino.ast.ThrowStatement;
import dev.latvian.mods.rhino.ast.TryStatement;
import dev.latvian.mods.rhino.ast.UnaryExpression;
import dev.latvian.mods.rhino.ast.VariableDeclaration;
import dev.latvian.mods.rhino.ast.VariableInitializer;
import dev.latvian.mods.rhino.ast.WhileLoop;
import dev.latvian.mods.rhino.ast.WithStatement;
import dev.latvian.mods.rhino.ast.Yield;

import java.util.ArrayList;
import java.util.List;

/**
 * This class rewrites the parse tree into an IR suitable for codegen.
 *
 * @author Mike McCabe
 * @author Norris Boyd
 * @see Node
 */
public final class IRFactory extends Parser {
	private static final int LOOP_DO_WHILE = 0;
	private static final int LOOP_WHILE = 1;
	private static final int LOOP_FOR = 2;

	private static final int ALWAYS_TRUE_BOOLEAN = 1;
	private static final int ALWAYS_FALSE_BOOLEAN = -1;

	public IRFactory() {
		super();
	}

	public IRFactory(CompilerEnvirons env) {
		this(env, env.getErrorReporter());
	}

	public IRFactory(CompilerEnvirons env, ErrorReporter errorReporter) {
		super(env, errorReporter);
	}

	/**
	 * Transforms the tree into a lower-level IR suitable for codegen.
	 * Optionally generates the encoded source.
	 */
	public ScriptNode transformTree(AstRoot root) {
		currentScriptOrFn = root;
		this.inUseStrictDirective = root.isInStrictMode();
		return (ScriptNode) transform(root);
	}

	// Might want to convert this to polymorphism - move transform*
	// functions into the AstNode subclasses.  OTOH that would make
	// IR transformation part of the public AST API - desirable?
	// Another possibility:  create AstTransformer interface and adapter.
	public Node transform(AstNode node) {
		return switch (node.getType()) {
			case Token.EMPTY, Token.COMMENT, Token.BREAK, Token.NAME, Token.TRUE, Token.FALSE, Token.THIS, Token.NULL, Token.NUMBER -> node;
			case Token.ARRAYCOMP -> transformArrayComp((ArrayComprehension) node);
			case Token.ARRAYLIT -> transformArrayLiteral((ArrayLiteral) node);
			case Token.BLOCK -> transformBlock(node);
			case Token.CALL -> transformFunctionCall((FunctionCall) node);
			case Token.CONTINUE -> transformContinue((ContinueStatement) node);
			case Token.DO -> transformDoLoop((DoLoop) node);
			case Token.FOR -> node instanceof ForInLoop ? transformForInLoop((ForInLoop) node) : transformForLoop((ForLoop) node);
			case Token.FUNCTION -> transformFunction((FunctionNode) node);
			case Token.GENEXPR -> transformGenExpr((GeneratorExpression) node);
			case Token.GETELEM -> transformElementGet((ElementGet) node);
			case Token.GETPROP -> transformPropertyGet((PropertyGet) node);
			case Token.HOOK -> transformCondExpr((ConditionalExpression) node);
			case Token.IF -> transformIf((IfStatement) node);
			case Token.NEW -> transformNewExpr((NewExpression) node);
			case Token.OBJECTLIT -> transformObjectLiteral((ObjectLiteral) node);
			case Token.TEMPLATE_LITERAL -> transformTemplateLiteral((TemplateLiteral) node);
			case Token.TAGGED_TEMPLATE_LITERAL -> transformTemplateLiteralCall((TaggedTemplateLiteral) node);
			case Token.REGEXP -> transformRegExp((RegExpLiteral) node);
			case Token.RETURN -> transformReturn((ReturnStatement) node);
			case Token.SCRIPT -> transformScript((ScriptNode) node);
			case Token.STRING -> transformString((StringLiteral) node);
			case Token.SWITCH -> transformSwitch((SwitchStatement) node);
			case Token.THROW -> transformThrow((ThrowStatement) node);
			case Token.TRY -> transformTry((TryStatement) node);
			case Token.WHILE -> transformWhileLoop((WhileLoop) node);
			case Token.WITH -> transformWith((WithStatement) node);
			case Token.YIELD, Token.YIELD_STAR -> transformYield((Yield) node);
			default -> {
				if (node instanceof ExpressionStatement n) {
					yield transformExprStmt(n);
				} else if (node instanceof Assignment n) {
					yield transformAssignment(n);
				} else if (node instanceof UnaryExpression n) {
					yield transformUnary(n);
				} else if (node instanceof InfixExpression n) {
					yield transformInfix(n);
				} else if (node instanceof VariableDeclaration n) {
					yield transformVariables(n);
				} else if (node instanceof ParenthesizedExpression n) {
					yield transformParenExpr(n);
				} else if (node instanceof LabeledStatement n) {
					yield transformLabeledStatement(n);
				} else if (node instanceof LetNode n) {
					yield transformLetNode(n);
				}

				throw new IllegalArgumentException("Can't transform: " + node + " (" + node.getClass().getName() + ")");
			}
		};
	}

	private Node transformArrayComp(ArrayComprehension node) {
		// An array comprehension expression such as
		//
		//   [expr for (x in foo) for each ([y, z] in bar) if (cond)]
		//
		// is rewritten approximately as
		//
		// new Scope(ARRAYCOMP) {
		//   new Node(BLOCK) {
		//     let tmp1 = new Array;
		//     for (let x in foo) {
		//       for each (let tmp2 in bar) {
		//         if (cond) {
		//           tmp1.push([y, z] = tmp2, expr);
		//         }
		//       }
		//     }
		//   }
		//   createName(tmp1)
		// }

		int lineno = node.getLineno();
		Scope scopeNode = createScopeNode(Token.ARRAYCOMP, lineno);
		String arrayName = currentScriptOrFn.getNextTempName();
		pushScope(scopeNode);
		try {
			defineSymbol(Token.LET, arrayName);
			Node block = new Node(Token.BLOCK, lineno);
			Node newArray = createCallOrNew(Token.NEW, createName("Array"));
			Node init = new Node(Token.EXPR_VOID, createAssignment(Token.ASSIGN, createName(arrayName), newArray), lineno);
			block.addChildToBack(init);
			block.addChildToBack(arrayCompTransformHelper(node, arrayName));
			scopeNode.addChildToBack(block);
			scopeNode.addChildToBack(createName(arrayName));
			return scopeNode;
		} finally {
			popScope();
		}
	}

	private Node arrayCompTransformHelper(ArrayComprehension node, String arrayName) {
		int lineno = node.getLineno();
		Node expr = transform(node.getResult());

		List<ArrayComprehensionLoop> loops = node.getLoops();
		int numLoops = loops.size();

		// Walk through loops, collecting and defining their iterator symbols.
		Node[] iterators = new Node[numLoops];
		Node[] iteratedObjs = new Node[numLoops];

		for (int i = 0; i < numLoops; i++) {
			ArrayComprehensionLoop acl = loops.get(i);

			AstNode iter = acl.getIterator();
			String name;
			if (iter.getType() == Token.NAME) {
				name = iter.getString();
			} else {
				// destructuring assignment
				name = currentScriptOrFn.getNextTempName();
				defineSymbol(Token.LP, name);
				expr = createBinary(Token.COMMA, createAssignment(Token.ASSIGN, iter, createName(name)), expr);
			}
			Node init = createName(name);
			// Define as a let since we want the scope of the variable to
			// be restricted to the array comprehension
			defineSymbol(Token.LET, name);
			iterators[i] = init;
			iteratedObjs[i] = transform(acl.getIteratedObject());
		}

		// generate code for tmpArray.push(body)
		Node call = createCallOrNew(Token.CALL, createPropertyGet(createName(arrayName), null, "push", 0));

		Node body = new Node(Token.EXPR_VOID, call, lineno);

		if (node.getFilter() != null) {
			body = createIf(transform(node.getFilter()), body, null, lineno);
		}

		// Now walk loops in reverse to build up the body statement.
		int pushed = 0;
		try {
			for (int i = numLoops - 1; i >= 0; i--) {
				ArrayComprehensionLoop acl = loops.get(i);
				Scope loop = createLoopNode(null,  // no label
						acl.getLineno());
				pushScope(loop);
				pushed++;
				body = createForIn(Token.LET, loop, iterators[i], iteratedObjs[i], body, acl.isForEach(), acl.isForOf());
			}
		} finally {
			for (int i = 0; i < pushed; i++) {
				popScope();
			}
		}

		// Now that we've accumulated any destructuring forms,
		// add expr to the call node; it's pushed on each iteration.
		call.addChildToBack(expr);
		return body;
	}

	private Node transformArrayLiteral(ArrayLiteral node) {
		if (node.isDestructuring()) {
			return node;
		}
		List<AstNode> elems = node.getElements();
		Node array = new Node(Token.ARRAYLIT);
		List<Integer> skipIndexes = null;
		for (int i = 0; i < elems.size(); ++i) {
			AstNode elem = elems.get(i);
			if (elem.getType() != Token.EMPTY) {
				array.addChildToBack(transform(elem));
			} else {
				if (skipIndexes == null) {
					skipIndexes = new ArrayList<>();
				}
				skipIndexes.add(i);
			}
		}
		array.putIntProp(Node.DESTRUCTURING_ARRAY_LENGTH, node.getDestructuringLength());
		if (skipIndexes != null) {
			int[] skips = new int[skipIndexes.size()];
			for (int i = 0; i < skipIndexes.size(); i++) {
				skips[i] = skipIndexes.get(i);
			}
			array.putProp(Node.SKIP_INDEXES_PROP, skips);
		}
		return array;
	}

	private Node transformAssignment(Assignment node) {
		AstNode left = removeParens(node.getLeft());
		Node target;
		if (isDestructuring(left)) {
			target = left;
		} else {
			target = transform(left);
		}
		return createAssignment(node.getType(), target, transform(node.getRight()));
	}

	private Node transformBlock(AstNode node) {
		if (node instanceof Scope) {
			pushScope((Scope) node);
		}
		try {
			List<Node> kids = new ArrayList<>();
			for (Node kid : node) {
				kids.add(transform((AstNode) kid));
			}
			node.removeChildren();
			for (Node kid : kids) {
				node.addChildToBack(kid);
			}
			return node;
		} finally {
			if (node instanceof Scope) {
				popScope();
			}
		}
	}

	private Node transformCondExpr(ConditionalExpression node) {
		Node test = transform(node.getTestExpression());
		Node ifTrue = transform(node.getTrueExpression());
		Node ifFalse = transform(node.getFalseExpression());
		return createCondExpr(test, ifTrue, ifFalse);
	}

	private Node transformContinue(ContinueStatement node) {
		return node;
	}

	private Node transformDoLoop(DoLoop loop) {
		loop.setType(Token.LOOP);
		pushScope(loop);
		try {
			Node body = transform(loop.getBody());
			Node cond = transform(loop.getCondition());
			return createLoop(loop, LOOP_DO_WHILE, body, cond, null, null);
		} finally {
			popScope();
		}
	}

	private Node transformElementGet(ElementGet node) {
		// OPT: could optimize to createPropertyGet
		// iff elem is string that can not be number
		Node target = transform(node.getTarget());
		Node element = transform(node.getElement());
		return new Node(Token.GETELEM, target, element);
	}

	private Node transformExprStmt(ExpressionStatement node) {
		Node expr = transform(node.getExpression());
		return new Node(node.getType(), expr, node.getLineno());
	}

	private Node transformForInLoop(ForInLoop loop) {
		loop.setType(Token.LOOP);
		pushScope(loop);
		try {
			int declType = -1;
			AstNode iter = loop.getIterator();
			if (iter instanceof VariableDeclaration) {
				declType = iter.getType();
			}
			Node lhs = transform(iter);
			Node obj = transform(loop.getIteratedObject());
			Node body = transform(loop.getBody());
			return createForIn(declType, loop, lhs, obj, body, loop.isForEach(), loop.isForOf());
		} finally {
			popScope();
		}
	}

	private Node transformForLoop(ForLoop loop) {
		loop.setType(Token.LOOP);
		// XXX: Can't use pushScope/popScope here since 'createFor' may split
		// the scope
		Scope savedScope = currentScope;
		currentScope = loop;
		try {
			Node init = transform(loop.getInitializer());
			Node test = transform(loop.getCondition());
			Node incr = transform(loop.getIncrement());
			Node body = transform(loop.getBody());
			return createFor(loop, init, test, incr, body);
		} finally {
			currentScope = savedScope;
		}
	}

	private Node transformFunction(FunctionNode fn) {
		int index = currentScriptOrFn.addFunction(fn);

		PerFunctionVariables savedVars = new PerFunctionVariables(fn);
		try {
			// If we start needing to record much more codegen metadata during
			// function parsing, we should lump it all into a helper class.
			Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);
			fn.removeProp(Node.DESTRUCTURING_PARAMS);

			int lineno = fn.getBody().getLineno();
			++nestingOfFunction;  // only for body, not params
			Node body = transform(fn.getBody());

			if (destructuring != null) {
				body.addChildToFront(new Node(Token.EXPR_VOID, destructuring, lineno));
			}

			int syntheticType = fn.getFunctionType();
			Node pn = initFunction(fn, index, body, syntheticType);
			return pn;

		} finally {
			--nestingOfFunction;
			savedVars.restore();
		}
	}

	private Node transformFunctionCall(FunctionCall node) {
		Node call = createCallOrNew(Token.CALL, transform(node.getTarget()));
		call.setLineno(node.getLineno());
		List<AstNode> args = node.getArguments();
		for (AstNode arg : args) {
			call.addChildToBack(transform(arg));
		}
		return call;
	}

	private Node transformGenExpr(GeneratorExpression node) {
		Node pn;

		FunctionNode fn = new FunctionNode();
		fn.setSourceName(currentScriptOrFn.getNextTempName());
		fn.setIsGenerator();
		fn.setFunctionType(FunctionNode.FUNCTION_EXPRESSION);
		fn.setRequiresActivation();

		int index = currentScriptOrFn.addFunction(fn);

		PerFunctionVariables savedVars = new PerFunctionVariables(fn);
		try {
			// If we start needing to record much more codegen metadata during
			// function parsing, we should lump it all into a helper class.
			Node destructuring = (Node) fn.getProp(Node.DESTRUCTURING_PARAMS);
			fn.removeProp(Node.DESTRUCTURING_PARAMS);

			int lineno = node.lineno;
			++nestingOfFunction;  // only for body, not params
			Node body = genExprTransformHelper(node);

			if (destructuring != null) {
				body.addChildToFront(new Node(Token.EXPR_VOID, destructuring, lineno));
			}

			int syntheticType = fn.getFunctionType();
			pn = initFunction(fn, index, body, syntheticType);
		} finally {
			--nestingOfFunction;
			savedVars.restore();
		}

		Node call = createCallOrNew(Token.CALL, pn);
		call.setLineno(node.getLineno());
		return call;
	}

	private Node genExprTransformHelper(GeneratorExpression node) {
		int lineno = node.getLineno();
		Node expr = transform(node.getResult());

		List<GeneratorExpressionLoop> loops = node.getLoops();
		int numLoops = loops.size();

		// Walk through loops, collecting and defining their iterator symbols.
		Node[] iterators = new Node[numLoops];
		Node[] iteratedObjs = new Node[numLoops];

		for (int i = 0; i < numLoops; i++) {
			GeneratorExpressionLoop acl = loops.get(i);

			AstNode iter = acl.getIterator();
			String name;
			if (iter.getType() == Token.NAME) {
				name = iter.getString();
			} else {
				name = currentScriptOrFn.getNextTempName();
				defineSymbol(Token.LP, name);
				expr = createBinary(Token.COMMA, createAssignment(Token.ASSIGN, iter, createName(name)), expr);
			}
			Node init = createName(name);
			// Define as a let since we want the scope of the variable to
			// be restricted to the array comprehension
			defineSymbol(Token.LET, name);
			iterators[i] = init;
			iteratedObjs[i] = transform(acl.getIteratedObject());
		}

		// generate code for tmpArray.push(body)
		Node yield = new Node(Token.YIELD, expr, node.getLineno());

		Node body = new Node(Token.EXPR_VOID, yield, lineno);

		if (node.getFilter() != null) {
			body = createIf(transform(node.getFilter()), body, null, lineno);
		}

		// Now walk loops in reverse to build up the body statement.
		int pushed = 0;
		try {
			for (int i = numLoops - 1; i >= 0; i--) {
				GeneratorExpressionLoop acl = loops.get(i);
				Scope loop = createLoopNode(null,  // no label
						acl.getLineno());
				pushScope(loop);
				pushed++;
				body = createForIn(Token.LET, loop, iterators[i], iteratedObjs[i], body, acl.isForEach(), acl.isForOf());
			}
		} finally {
			for (int i = 0; i < pushed; i++) {
				popScope();
			}
		}

		return body;
	}

	private Node transformIf(IfStatement n) {
		Node cond = transform(n.getCondition());
		Node ifTrue = transform(n.getThenPart());
		Node ifFalse = null;
		if (n.getElsePart() != null) {
			ifFalse = transform(n.getElsePart());
		}
		return createIf(cond, ifTrue, ifFalse, n.getLineno());
	}

	private Node transformInfix(InfixExpression node) {
		Node left = transform(node.getLeft());
		Node right = transform(node.getRight());
		return createBinary(node.getType(), left, right);
	}

	private Node transformLabeledStatement(LabeledStatement ls) {
		Label label = ls.getFirstLabel();
		Node statement = transform(ls.getStatement());

		// Make a target and put it _after_ the statement node.  Add in the
		// LABEL node, so breaks get the right target.
		Node breakTarget = Node.newTarget();
		Node block = new Node(Token.BLOCK, label, statement, breakTarget);
		label.target = breakTarget;

		return block;
	}

	private Node transformLetNode(LetNode node) {
		pushScope(node);
		try {
			Node vars = transformVariableInitializers(node.getVariables());
			node.addChildToBack(vars);
			if (node.getBody() != null) {
				node.addChildToBack(transform(node.getBody()));
			}
			return node;
		} finally {
			popScope();
		}
	}

	private Node transformNewExpr(NewExpression node) {
		Node nx = createCallOrNew(Token.NEW, transform(node.getTarget()));
		nx.setLineno(node.getLineno());
		for (AstNode arg : node.getArguments()) {
			nx.addChildToBack(transform(arg));
		}
		if (node.getInitializer() != null) {
			nx.addChildToBack(transformObjectLiteral(node.getInitializer()));
		}
		return nx;
	}

	private Node transformObjectLiteral(ObjectLiteral node) {
		if (node.isDestructuring()) {
			return node;
		}
		// createObjectLiteral rewrites its argument as object
		// creation plus object property entries, so later compiler
		// stages don't need to know about object literals.
		List<ObjectProperty> elems = node.getElements();
		Node object = new Node(Token.OBJECTLIT);
		Object[] properties;
		if (elems.isEmpty()) {
			properties = ScriptRuntime.emptyArgs;
		} else {
			int size = elems.size(), i = 0;
			properties = new Object[size];
			for (ObjectProperty prop : elems) {
				properties[i++] = getPropKey(prop.getLeft());

				Node right = transform(prop.getRight());
				if (prop.isGetterMethod()) {
					right = createUnary(Token.GET, right);
				} else if (prop.isSetterMethod()) {
					right = createUnary(Token.SET, right);
				} else if (prop.isNormalMethod()) {
					right = createUnary(Token.METHOD, right);
				}
				object.addChildToBack(right);
			}
		}
		object.putProp(Node.OBJECT_IDS_PROP, properties);
		return object;
	}

	private Object getPropKey(Node id) {
		Object key;
		if (id instanceof Name) {
			String s = ((Name) id).getIdentifier();
			key = ScriptRuntime.getIndexObject(s);
		} else if (id instanceof StringLiteral) {
			String s = ((StringLiteral) id).getValue();
			key = ScriptRuntime.getIndexObject(s);
		} else if (id instanceof NumberLiteral) {
			double n = ((NumberLiteral) id).getNumber();
			key = ScriptRuntime.getIndexObject(n);
		} else {
			throw Kit.codeBug();
		}
		return key;
	}

	private Node transformParenExpr(ParenthesizedExpression node) {
		AstNode expr = node.getExpression();
		while (expr instanceof ParenthesizedExpression) {
			expr = ((ParenthesizedExpression) expr).getExpression();
		}
		Node result = transform(expr);
		result.putProp(Node.PARENTHESIZED_PROP, Boolean.TRUE);
		return result;
	}

	private Node transformPropertyGet(PropertyGet node) {
		Node target = transform(node.getTarget());
		String name = node.getProperty().getIdentifier();
		return createPropertyGet(target, null, name, 0);
	}

	private Node transformTemplateLiteral(TemplateLiteral node) {
		List<AstNode> elems = node.getElements();
		// start with an empty string to ensure ToString() for each substitution
		Node pn = Node.newString("");
		for (AstNode elem : elems) {
			if (elem.getType() != Token.TEMPLATE_CHARS) {
				pn = createBinary(Token.ADD, pn, transform(elem));
			} else {
				TemplateCharacters chars = (TemplateCharacters) elem;
				// skip empty parts, e.g. `ε${expr}ε` where ε denotes the empty string
				String value = chars.getValue();
				if (value.length() > 0) {
					pn = createBinary(Token.ADD, pn, Node.newString(value));
				}
			}
		}
		return pn;
	}

	private Node transformTemplateLiteralCall(TaggedTemplateLiteral node) {
		Node call = createCallOrNew(Token.CALL, transform(node.getTarget()));
		call.setLineno(node.getLineno());
		TemplateLiteral templateLiteral = (TemplateLiteral) node.getTemplateLiteral();
		List<AstNode> elems = templateLiteral.getElements();
		// Node callSite = new Node(Token.TEMPLATE_LITERAL_CALL);
		// call.addChildToBack(callSite);
		call.addChildToBack(templateLiteral);
		for (AstNode elem : elems) {
			if (elem.getType() != Token.TEMPLATE_CHARS) {
				call.addChildToBack(transform(elem));
			} else {
				TemplateCharacters chars = (TemplateCharacters) elem;
				// callSite.addChildToBack(elem);
			}
		}
		currentScriptOrFn.addTemplateLiteral(templateLiteral);
		return call;
	}

	private Node transformRegExp(RegExpLiteral node) {
		currentScriptOrFn.addRegExp(node);
		return node;
	}

	private Node transformReturn(ReturnStatement node) {
		AstNode rv = node.getReturnValue();
		Node value = rv == null ? null : transform(rv);
		return rv == null ? new Node(Token.RETURN, node.getLineno()) : new Node(Token.RETURN, value, node.getLineno());
	}

	private Node transformScript(ScriptNode node) {
		if (currentScope != null) {
			Kit.codeBug();
		}
		currentScope = node;
		Node body = new Node(Token.BLOCK);
		for (Node kid : node) {
			body.addChildToBack(transform((AstNode) kid));
		}
		node.removeChildren();
		Node children = body.getFirstChild();
		if (children != null) {
			node.addChildrenToBack(children);
		}
		return node;
	}

	private Node transformString(StringLiteral node) {
		return Node.newString(node.getValue());
	}

	private Node transformSwitch(SwitchStatement node) {
		// The switch will be rewritten from:
		//
		// switch (expr) {
		//   case test1: statements1;
		//   ...
		//   default: statementsDefault;
		//   ...
		//   case testN: statementsN;
		// }
		//
		// to:
		//
		// {
		//     switch (expr) {
		//       case test1: goto label1;
		//       ...
		//       case testN: goto labelN;
		//     }
		//     goto labelDefault;
		//   label1:
		//     statements1;
		//   ...
		//   labelDefault:
		//     statementsDefault;
		//   ...
		//   labelN:
		//     statementsN;
		//   breakLabel:
		// }
		//
		// where inside switch each "break;" without label will be replaced
		// by "goto breakLabel".
		//
		// If the original switch does not have the default label, then
		// after the switch he transformed code would contain this goto:
		//     goto breakLabel;
		// instead of:
		//     goto labelDefault;

		Node switchExpr = transform(node.getExpression());
		node.addChildToBack(switchExpr);

		Node block = new Node(Token.BLOCK, node, node.getLineno());

		for (SwitchCase sc : node.getCases()) {
			AstNode expr = sc.getExpression();
			Node caseExpr = null;

			if (expr != null) {
				caseExpr = transform(expr);
			}

			List<AstNode> stmts = sc.getStatements();
			Node body = new Block();
			if (stmts != null) {
				for (AstNode kid : stmts) {
					body.addChildToBack(transform(kid));
				}
			}
			addSwitchCase(block, caseExpr, body);
		}
		closeSwitch(block);
		return block;
	}

	private Node transformThrow(ThrowStatement node) {
		Node value = transform(node.getExpression());
		return new Node(Token.THROW, value, node.getLineno());
	}

	private Node transformTry(TryStatement node) {
		Node tryBlock = transform(node.getTryBlock());

		Node catchBlocks = new Block();
		for (CatchClause cc : node.getCatchClauses()) {
			String varName = cc.getVarName().getIdentifier();

			Node catchCond;
			AstNode ccc = cc.getCatchCondition();
			if (ccc != null) {
				catchCond = transform(ccc);
			} else {
				catchCond = new EmptyExpression();
			}

			Node body = transform(cc.getBody());

			catchBlocks.addChildToBack(createCatch(varName, catchCond, body, cc.getLineno()));
		}
		Node finallyBlock = null;
		if (node.getFinallyBlock() != null) {
			finallyBlock = transform(node.getFinallyBlock());
		}
		return createTryCatchFinally(tryBlock, catchBlocks, finallyBlock, node.getLineno());
	}

	private Node transformUnary(UnaryExpression node) {
		int type = node.getType();
		Node child = transform(node.getOperand());
		if (type == Token.INC || type == Token.DEC) {
			return createIncDec(type, node.isPostfix(), child);
		}
		return createUnary(type, child);
	}

	private Node transformVariables(VariableDeclaration node) {
		transformVariableInitializers(node);

		// Might be most robust to have parser record whether it was
		// a variable declaration statement, possibly as a node property.
		AstNode parent = node.getParent();
		return node;
	}

	private Node transformVariableInitializers(VariableDeclaration node) {
		for (VariableInitializer var : node.getVariables()) {
			AstNode target = var.getTarget();
			AstNode init = var.getInitializer();

			Node left;
			if (var.isDestructuring()) {
				left = target;
			} else {
				left = transform(target);
			}

			Node right = null;
			if (init != null) {
				right = transform(init);
			}

			if (var.isDestructuring()) {
				if (right == null) {  // TODO:  should this ever happen?
					node.addChildToBack(left);
				} else {
					Node d = createDestructuringAssignment(node.getType(), left, right);
					node.addChildToBack(d);
				}
			} else {
				if (right != null) {
					left.addChildToBack(right);
				}
				node.addChildToBack(left);
			}
		}
		return node;
	}

	private Node transformWhileLoop(WhileLoop loop) {
		loop.setType(Token.LOOP);
		pushScope(loop);
		try {
			Node cond = transform(loop.getCondition());
			Node body = transform(loop.getBody());
			return createLoop(loop, LOOP_WHILE, body, cond, null, null);
		} finally {
			popScope();
		}
	}

	private Node transformWith(WithStatement node) {
		Node expr = transform(node.getExpression());
		Node stmt = transform(node.getStatement());
		return createWith(expr, stmt, node.getLineno());
	}

	private Node transformYield(Yield node) {
		Node kid = node.getValue() == null ? null : transform(node.getValue());
		if (kid != null) {
			return new Node(node.getType(), kid, node.getLineno());
		}
		return new Node(node.getType(), node.getLineno());
	}

	/**
	 * If caseExpression argument is null it indicates a default label.
	 */
	private static void addSwitchCase(Node switchBlock, Node caseExpression, Node statements) {
		if (switchBlock.getType() != Token.BLOCK) {
			throw Kit.codeBug();
		}
		Jump switchNode = (Jump) switchBlock.getFirstChild();
		if (switchNode.getType() != Token.SWITCH) {
			throw Kit.codeBug();
		}

		Node gotoTarget = Node.newTarget();
		if (caseExpression != null) {
			Jump caseNode = new Jump(Token.CASE, caseExpression);
			caseNode.target = gotoTarget;
			switchNode.addChildToBack(caseNode);
		} else {
			switchNode.setDefault(gotoTarget);
		}
		switchBlock.addChildToBack(gotoTarget);
		switchBlock.addChildToBack(statements);
	}

	private static void closeSwitch(Node switchBlock) {
		if (switchBlock.getType() != Token.BLOCK) {
			throw Kit.codeBug();
		}
		Jump switchNode = (Jump) switchBlock.getFirstChild();
		if (switchNode.getType() != Token.SWITCH) {
			throw Kit.codeBug();
		}

		Node switchBreakTarget = Node.newTarget();
		// switchNode.target is only used by NodeTransformer
		// to detect switch end
		switchNode.target = switchBreakTarget;

		Node defaultTarget = switchNode.getDefault();
		if (defaultTarget == null) {
			defaultTarget = switchBreakTarget;
		}

		switchBlock.addChildAfter(makeJump(Token.GOTO, defaultTarget), switchNode);
		switchBlock.addChildToBack(switchBreakTarget);
	}

	private static Node createExprStatementNoReturn(Node expr, int lineno) {
		return new Node(Token.EXPR_VOID, expr, lineno);
	}

	private static Node createString(String string) {
		return Node.newString(string);
	}

	/**
	 * Catch clause of try/catch/finally
	 *
	 * @param varName   the name of the variable to bind to the exception
	 * @param catchCond the condition under which to catch the exception.
	 *                  May be null if no condition is given.
	 * @param stmts     the statements in the catch clause
	 * @param lineno    the starting line number of the catch clause
	 */
	private Node createCatch(String varName, Node catchCond, Node stmts, int lineno) {
		if (catchCond == null) {
			catchCond = new Node(Token.EMPTY);
		}
		return new Node(Token.CATCH, createName(varName), catchCond, stmts, lineno);
	}

	private static Node initFunction(FunctionNode fnNode, int functionIndex, Node statements, int functionType) {
		fnNode.setFunctionType(functionType);
		fnNode.addChildToBack(statements);

		int functionCount = fnNode.getFunctionCount();
		if (functionCount != 0) {
			// Functions containing other functions require activation objects
			fnNode.setRequiresActivation();
		}

		if (functionType == FunctionNode.FUNCTION_EXPRESSION) {
			Name name = fnNode.getFunctionName();
			if (name != null && name.length() != 0 && fnNode.getSymbol(name.getIdentifier()) == null) {
				// A function expression needs to have its name as a
				// variable (if it isn't already allocated as a variable).
				// See ECMA Ch. 13.  We add code to the beginning of the
				// function to initialize a local variable of the
				// function's name to the function value, but only if the
				// function doesn't already define a formal parameter, var,
				// or nested function with the same name.
				fnNode.putSymbol(new AstSymbol(Token.FUNCTION, name.getIdentifier()));
				Node setFn = new Node(Token.EXPR_VOID, new Node(Token.SETNAME, Node.newString(Token.BINDNAME, name.getIdentifier()), new Node(Token.THISFN)));
				statements.addChildrenToFront(setFn);
			}
		}

		// Add return to end if needed.
		Node lastStmt = statements.getLastChild();
		if (lastStmt == null || lastStmt.getType() != Token.RETURN) {
			statements.addChildToBack(new Node(Token.RETURN));
		}

		Node result = Node.newString(Token.FUNCTION, fnNode.getName());
		result.putIntProp(Node.FUNCTION_PROP, functionIndex);
		return result;
	}

	/**
	 * Create loop node. The code generator will later call
	 * createWhile|createDoWhile|createFor|createForIn
	 * to finish loop generation.
	 */
	private Scope createLoopNode(Node loopLabel, int lineno) {
		Scope result = createScopeNode(Token.LOOP, lineno);
		if (loopLabel != null) {
			((Jump) loopLabel).setLoop(result);
		}
		return result;
	}

	private static Node createFor(Scope loop, Node init, Node test, Node incr, Node body) {
		if (init.getType() == Token.LET) {
			// rewrite "for (let i=s; i < N; i++)..." as
			// "let (i=s) { for (; i < N; i++)..." so that "s" is evaluated
			// outside the scope of the for.
			Scope let = Scope.splitScope(loop);
			let.setType(Token.LET);
			let.addChildrenToBack(init);
			let.addChildToBack(createLoop(loop, LOOP_FOR, body, test, new Node(Token.EMPTY), incr));
			return let;
		}
		return createLoop(loop, LOOP_FOR, body, test, init, incr);
	}

	private static Node createLoop(Jump loop, int loopType, Node body, Node cond, Node init, Node incr) {
		Node bodyTarget = Node.newTarget();
		Node condTarget = Node.newTarget();
		if (loopType == LOOP_FOR && cond.getType() == Token.EMPTY) {
			cond = new Node(Token.TRUE);
		}
		Jump IFEQ = new Jump(Token.IFEQ, cond);
		IFEQ.target = bodyTarget;
		Node breakTarget = Node.newTarget();

		loop.addChildToBack(bodyTarget);
		loop.addChildrenToBack(body);
		if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
			// propagate lineno to condition
			loop.addChildrenToBack(new Node(Token.EMPTY, loop.getLineno()));
		}
		loop.addChildToBack(condTarget);
		loop.addChildToBack(IFEQ);
		loop.addChildToBack(breakTarget);

		loop.target = breakTarget;
		Node continueTarget = condTarget;

		if (loopType == LOOP_WHILE || loopType == LOOP_FOR) {
			// Just add a GOTO to the condition in the do..while
			loop.addChildToFront(makeJump(Token.GOTO, condTarget));

			if (loopType == LOOP_FOR) {
				int initType = init.getType();
				if (initType != Token.EMPTY) {
					if (initType != Token.VAR && initType != Token.LET) {
						init = new Node(Token.EXPR_VOID, init);
					}
					loop.addChildToFront(init);
				}
				Node incrTarget = Node.newTarget();
				loop.addChildAfter(incrTarget, body);
				if (incr.getType() != Token.EMPTY) {
					incr = new Node(Token.EXPR_VOID, incr);
					loop.addChildAfter(incr, incrTarget);
				}
				continueTarget = incrTarget;
			}
		}

		loop.setContinue(continueTarget);
		return loop;
	}

	/**
	 * Generate IR for a for..in loop.
	 */
	private Node createForIn(int declType, Node loop, Node lhs, Node obj, Node body, boolean isForEach, boolean isForOf) {
		int destructuring = -1;
		int destructuringLen = 0;
		Node lvalue;
		int type = lhs.getType();
		if (type == Token.VAR || type == Token.LET) {
			Node kid = lhs.getLastChild();
			int kidType = kid.getType();
			if (kidType == Token.ARRAYLIT || kidType == Token.OBJECTLIT) {
				type = destructuring = kidType;
				lvalue = kid;
				if (kid instanceof ArrayLiteral) {
					destructuringLen = ((ArrayLiteral) kid).getDestructuringLength();
				}
			} else if (kidType == Token.NAME) {
				lvalue = Node.newString(Token.NAME, kid.getString());
			} else {
				reportError("msg.bad.for.in.lhs");
				return null;
			}
		} else if (type == Token.ARRAYLIT || type == Token.OBJECTLIT) {
			destructuring = type;
			lvalue = lhs;
			if (lhs instanceof ArrayLiteral) {
				destructuringLen = ((ArrayLiteral) lhs).getDestructuringLength();
			}
		} else {
			lvalue = makeReference(lhs);
			if (lvalue == null) {
				reportError("msg.bad.for.in.lhs");
				return null;
			}
		}

		Node localBlock = new Node(Token.LOCAL_BLOCK);
		int initType = isForEach ? Token.ENUM_INIT_VALUES : isForOf ? Token.ENUM_INIT_VALUES_IN_ORDER : (destructuring != -1 ? Token.ENUM_INIT_ARRAY : Token.ENUM_INIT_KEYS);
		Node init = new Node(initType, obj);
		init.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
		Node cond = new Node(Token.ENUM_NEXT);
		cond.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
		Node id = new Node(Token.ENUM_ID);
		id.putProp(Node.LOCAL_BLOCK_PROP, localBlock);

		Node newBody = new Node(Token.BLOCK);
		Node assign;
		if (destructuring != -1) {
			assign = createDestructuringAssignment(declType, lvalue, id);
			if (!isForEach && !isForOf && (destructuring == Token.OBJECTLIT || destructuringLen != 2)) {
				// destructuring assignment is only allowed in for..each or
				// with an array type of length 2 (to hold key and value)
				reportError("msg.bad.for.in.destruct");
			}
		} else {
			assign = simpleAssignment(lvalue, id);
		}
		newBody.addChildToBack(new Node(Token.EXPR_VOID, assign));
		newBody.addChildToBack(body);

		loop = createLoop((Jump) loop, LOOP_WHILE, newBody, cond, null, null);
		loop.addChildToFront(init);
		if (type == Token.VAR || type == Token.LET) {
			loop.addChildToFront(lhs);
		}
		localBlock.addChildToBack(loop);

		return localBlock;
	}

	/**
	 * Try/Catch/Finally
	 * <p>
	 * The IRFactory tries to express as much as possible in the tree;
	 * the responsibilities remaining for Codegen are to add the Java
	 * handlers: (Either (but not both) of TARGET and FINALLY might not
	 * be defined)
	 * <p>
	 * - a catch handler for javascript exceptions that unwraps the
	 * exception onto the stack and GOTOes to the catch target
	 * <p>
	 * - a finally handler
	 * <p>
	 * ... and a goto to GOTO around these handlers.
	 */
	private Node createTryCatchFinally(Node tryBlock, Node catchBlocks, Node finallyBlock, int lineno) {
		boolean hasFinally = (finallyBlock != null) && (finallyBlock.getType() != Token.BLOCK || finallyBlock.hasChildren());

		// short circuit
		if (tryBlock.getType() == Token.BLOCK && !tryBlock.hasChildren() && !hasFinally) {
			return tryBlock;
		}

		boolean hasCatch = catchBlocks.hasChildren();

		// short circuit
		if (!hasFinally && !hasCatch) {
			// bc finally might be an empty block...
			return tryBlock;
		}

		Node handlerBlock = new Node(Token.LOCAL_BLOCK);
		Jump pn = new Jump(Token.TRY, tryBlock, lineno);
		pn.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);

		if (hasCatch) {
			// jump around catch code
			Node endCatch = Node.newTarget();
			pn.addChildToBack(makeJump(Token.GOTO, endCatch));

			// make a TARGET for the catch that the tcf node knows about
			Node catchTarget = Node.newTarget();
			pn.target = catchTarget;
			// mark it
			pn.addChildToBack(catchTarget);

			//
			//  Given
			//
			//   try {
			//       tryBlock;
			//   } catch (e if condition1) {
			//       something1;
			//   ...
			//
			//   } catch (e if conditionN) {
			//       somethingN;
			//   } catch (e) {
			//       somethingDefault;
			//   }
			//
			//  rewrite as
			//
			//   try {
			//       tryBlock;
			//       goto after_catch:
			//   } catch (x) {
			//       with (newCatchScope(e, x)) {
			//           if (condition1) {
			//               something1;
			//               goto after_catch;
			//           }
			//       }
			//   ...
			//       with (newCatchScope(e, x)) {
			//           if (conditionN) {
			//               somethingN;
			//               goto after_catch;
			//           }
			//       }
			//       with (newCatchScope(e, x)) {
			//           somethingDefault;
			//           goto after_catch;
			//       }
			//   }
			// after_catch:
			//
			// If there is no default catch, then the last with block
			// arround  "somethingDefault;" is replaced by "rethrow;"

			// It is assumed that catch handler generation will store
			// exeception object in handlerBlock register

			// Block with local for exception scope objects
			Node catchScopeBlock = new Node(Token.LOCAL_BLOCK);

			// expects catchblocks children to be (cond block) pairs.
			Node cb = catchBlocks.getFirstChild();
			boolean hasDefault = false;
			int scopeIndex = 0;
			while (cb != null) {
				int catchLineNo = cb.getLineno();

				Node name = cb.getFirstChild();
				Node cond = name.getNext();
				Node catchStatement = cond.getNext();
				cb.removeChild(name);
				cb.removeChild(cond);
				cb.removeChild(catchStatement);

				// Add goto to the catch statement to jump out of catch
				// but prefix it with LEAVEWITH since try..catch produces
				// "with"code in order to limit the scope of the exception
				// object.
				catchStatement.addChildToBack(new Node(Token.LEAVEWITH));
				catchStatement.addChildToBack(makeJump(Token.GOTO, endCatch));

				// Create condition "if" when present
				Node condStmt;
				if (cond.getType() == Token.EMPTY) {
					condStmt = catchStatement;
					hasDefault = true;
				} else {
					condStmt = createIf(cond, catchStatement, null, catchLineNo);
				}

				// Generate code to create the scope object and store
				// it in catchScopeBlock register
				Node catchScope = new Node(Token.CATCH_SCOPE, name, createUseLocal(handlerBlock));
				catchScope.putProp(Node.LOCAL_BLOCK_PROP, catchScopeBlock);
				catchScope.putIntProp(Node.CATCH_SCOPE_PROP, scopeIndex);
				catchScopeBlock.addChildToBack(catchScope);

				// Add with statement based on catch scope object
				catchScopeBlock.addChildToBack(createWith(createUseLocal(catchScopeBlock), condStmt, catchLineNo));

				// move to next cb
				cb = cb.getNext();
				++scopeIndex;
			}
			pn.addChildToBack(catchScopeBlock);
			if (!hasDefault) {
				// Generate code to rethrow if no catch clause was executed
				Node rethrow = new Node(Token.RETHROW);
				rethrow.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
				pn.addChildToBack(rethrow);
			}

			pn.addChildToBack(endCatch);
		}

		if (hasFinally) {
			Node finallyTarget = Node.newTarget();
			pn.setFinally(finallyTarget);

			// add jsr finally to the try block
			pn.addChildToBack(makeJump(Token.JSR, finallyTarget));

			// jump around finally code
			Node finallyEnd = Node.newTarget();
			pn.addChildToBack(makeJump(Token.GOTO, finallyEnd));

			pn.addChildToBack(finallyTarget);
			Node fBlock = new Node(Token.FINALLY, finallyBlock);
			fBlock.putProp(Node.LOCAL_BLOCK_PROP, handlerBlock);
			pn.addChildToBack(fBlock);

			pn.addChildToBack(finallyEnd);
		}
		handlerBlock.addChildToBack(pn);
		return handlerBlock;
	}

	private Node createWith(Node obj, Node body, int lineno) {
		setRequiresActivation();
		Node result = new Node(Token.BLOCK, lineno);
		result.addChildToBack(new Node(Token.ENTERWITH, obj));
		Node bodyNode = new Node(Token.WITH, body, lineno);
		result.addChildrenToBack(bodyNode);
		result.addChildToBack(new Node(Token.LEAVEWITH));
		return result;
	}

	private static Node createIf(Node cond, Node ifTrue, Node ifFalse, int lineno) {
		int condStatus = isAlwaysDefinedBoolean(cond);
		if (condStatus == ALWAYS_TRUE_BOOLEAN) {
			return ifTrue;
		} else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
			if (ifFalse != null) {
				return ifFalse;
			}
			// Replace if (false) xxx by empty block
			return new Node(Token.BLOCK, lineno);
		}

		Node result = new Node(Token.BLOCK, lineno);
		Node ifNotTarget = Node.newTarget();
		Jump IFNE = new Jump(Token.IFNE, cond);
		IFNE.target = ifNotTarget;

		result.addChildToBack(IFNE);
		result.addChildrenToBack(ifTrue);

		if (ifFalse != null) {
			Node endTarget = Node.newTarget();
			result.addChildToBack(makeJump(Token.GOTO, endTarget));
			result.addChildToBack(ifNotTarget);
			result.addChildrenToBack(ifFalse);
			result.addChildToBack(endTarget);
		} else {
			result.addChildToBack(ifNotTarget);
		}

		return result;
	}

	private static Node createCondExpr(Node cond, Node ifTrue, Node ifFalse) {
		int condStatus = isAlwaysDefinedBoolean(cond);
		if (condStatus == ALWAYS_TRUE_BOOLEAN) {
			return ifTrue;
		} else if (condStatus == ALWAYS_FALSE_BOOLEAN) {
			return ifFalse;
		}
		return new Node(Token.HOOK, cond, ifTrue, ifFalse);
	}

	private static Node createUnary(int nodeType, Node child) {
		int childType = child.getType();
		switch (nodeType) {
			case Token.DELPROP: {
				Node n;
				if (childType == Token.NAME) {
					// Transform Delete(Name "a")
					//  to Delete(Bind("a"), String("a"))
					child.setType(Token.BINDNAME);
					Node left = child;
					Node right = Node.newString(child.getString());
					n = new Node(nodeType, left, right);
				} else if (childType == Token.GETPROP || childType == Token.GETELEM) {
					Node left = child.getFirstChild();
					Node right = child.getLastChild();
					child.removeChild(left);
					child.removeChild(right);
					n = new Node(nodeType, left, right);
				} else if (childType == Token.GET_REF) {
					Node ref = child.getFirstChild();
					child.removeChild(ref);
					n = new Node(Token.DEL_REF, ref);
				} else {
					// Always evaluate delete operand, see ES5 11.4.1 & bug #726121
					n = new Node(nodeType, new Node(Token.TRUE), child);
				}
				return n;
			}
			case Token.TYPEOF:
				if (childType == Token.NAME) {
					child.setType(Token.TYPEOFNAME);
					return child;
				}
				break;
			case Token.BITNOT:
				if (childType == Token.NUMBER) {
					int value = ScriptRuntime.toInt32(child.getDouble());
					child.setDouble(~value);
					return child;
				}
				break;
			case Token.NEG:
				if (childType == Token.NUMBER) {
					child.setDouble(-child.getDouble());
					return child;
				}
				break;
			case Token.NOT: {
				int status = isAlwaysDefinedBoolean(child);
				if (status != 0) {
					int type;
					if (status == ALWAYS_TRUE_BOOLEAN) {
						type = Token.FALSE;
					} else {
						type = Token.TRUE;
					}
					if (childType == Token.TRUE || childType == Token.FALSE) {
						child.setType(type);
						return child;
					}
					return new Node(type);
				}
				break;
			}
		}
		return new Node(nodeType, child);
	}

	private Node createCallOrNew(int nodeType, Node child) {
		int type = Node.NON_SPECIALCALL;
		if (child.getType() == Token.NAME) {
			String name = child.getString();
			if (name.equals("eval")) {
				type = Node.SPECIALCALL_EVAL;
			} else if (name.equals("With")) {
				type = Node.SPECIALCALL_WITH;
			}
		} else if (child.getType() == Token.GETPROP) {
			String name = child.getLastChild().getString();
			if (name.equals("eval")) {
				type = Node.SPECIALCALL_EVAL;
			}
		}
		Node node = new Node(nodeType, child);
		if (type != Node.NON_SPECIALCALL) {
			// Calls to these functions require activation objects.
			setRequiresActivation();
			node.putIntProp(Node.SPECIALCALL_PROP, type);
		}
		return node;
	}

	private static Node createIncDec(int nodeType, boolean post, Node child) {
		child = makeReference(child);
		int childType = child.getType();

		switch (childType) {
			case Token.NAME, Token.GETPROP, Token.GETELEM, Token.GET_REF -> {
				Node n = new Node(nodeType, child);
				int incrDecrMask = 0;
				if (nodeType == Token.DEC) {
					incrDecrMask |= Node.DECR_FLAG;
				}
				if (post) {
					incrDecrMask |= Node.POST_FLAG;
				}
				n.putIntProp(Node.INCRDECR_PROP, incrDecrMask);
				return n;
			}
		}
		throw Kit.codeBug();
	}

	private Node createPropertyGet(Node target, String namespace, String name, int memberTypeFlags) {
		if (namespace == null && memberTypeFlags == 0) {
			if (target == null) {
				return createName(name);
			}
			checkActivationName(name, Token.GETPROP);
			if (ScriptRuntime.isSpecialProperty(name)) {
				Node ref = new Node(Token.REF_SPECIAL, target);
				ref.putProp(Node.NAME_PROP, name);
				return new Node(Token.GET_REF, ref);
			}
			return new Node(Token.GETPROP, target, Node.newString(name));
		}
		Node elem = Node.newString(name);
		memberTypeFlags |= Node.PROPERTY_FLAG;
		return createMemberRefGet(target, namespace, elem, memberTypeFlags);
	}

	private Node createMemberRefGet(Node target, String namespace, Node elem, int memberTypeFlags) {
		/*
		Node nsNode = null;
		if (namespace != null) {
			// See 11.1.2 in ECMA 357
			if (namespace.equals("*")) {
				nsNode = new Node(Token.NULL);
			} else {
				nsNode = createName(namespace);
			}
		}

		Node ref;
		if (target == null) {
			if (namespace == null) {
				ref = new Node(Token.REF_NAME, elem);
			} else {
				ref = new Node(Token.REF_NS_NAME, nsNode, elem);
			}
		} else {
			if (namespace == null) {
				ref = new Node(Token.REF_MEMBER, target, elem);
			} else {
				ref = new Node(Token.REF_NS_MEMBER, target, nsNode, elem);
			}
		}
		if (memberTypeFlags != 0) {
			ref.putIntProp(Node.MEMBER_TYPE_PROP, memberTypeFlags);
		}
		*/
		return new Node(Token.GET_REF, elem); // ref
	}

	private static Node createBinary(int nodeType, Node left, Node right) {
		switch (nodeType) {

			case Token.ADD:
				// numerical addition and string concatenation
				if (left.type == Token.STRING) {
					String s2;
					if (right.type == Token.STRING) {
						s2 = right.getString();
					} else if (right.type == Token.NUMBER) {
						s2 = ScriptRuntime.numberToString(right.getDouble(), 10);
					} else {
						break;
					}
					String s1 = left.getString();
					left.setString(s1.concat(s2));
					return left;
				} else if (left.type == Token.NUMBER) {
					if (right.type == Token.NUMBER) {
						left.setDouble(left.getDouble() + right.getDouble());
						return left;
					} else if (right.type == Token.STRING) {
						String s1, s2;
						s1 = ScriptRuntime.numberToString(left.getDouble(), 10);
						s2 = right.getString();
						right.setString(s1.concat(s2));
						return right;
					}
				}
				// can't do anything if we don't know  both types - since
				// 0 + object is supposed to call toString on the object and do
				// string concantenation rather than addition
				break;

			case Token.SUB:
				// numerical subtraction
				if (left.type == Token.NUMBER) {
					double ld = left.getDouble();
					if (right.type == Token.NUMBER) {
						//both numbers
						left.setDouble(ld - right.getDouble());
						return left;
					} else if (ld == 0.0) {
						// first 0: 0-x -> -x
						return new Node(Token.NEG, right);
					}
				} else if (right.type == Token.NUMBER) {
					if (right.getDouble() == 0.0) {
						//second 0: x - 0 -> +x
						// can not make simply x because x - 0 must be number
						return new Node(Token.POS, left);
					}
				}
				break;

			case Token.MUL:
				// numerical multiplication
				if (left.type == Token.NUMBER) {
					double ld = left.getDouble();
					if (right.type == Token.NUMBER) {
						//both numbers
						left.setDouble(ld * right.getDouble());
						return left;
					} else if (ld == 1.0) {
						// first 1: 1 *  x -> +x
						return new Node(Token.POS, right);
					}
				} else if (right.type == Token.NUMBER) {
					if (right.getDouble() == 1.0) {
						//second 1: x * 1 -> +x
						// can not make simply x because x - 0 must be number
						return new Node(Token.POS, left);
					}
				}
				// can't do x*0: Infinity * 0 gives NaN, not 0
				break;

			case Token.DIV:
				// number division
				if (right.type == Token.NUMBER) {
					double rd = right.getDouble();
					if (left.type == Token.NUMBER) {
						// both constants -- just divide, trust Java to handle x/0
						left.setDouble(left.getDouble() / rd);
						return left;
					} else if (rd == 1.0) {
						// second 1: x/1 -> +x
						// not simply x to force number convertion
						return new Node(Token.POS, left);
					}
				}
				break;

			case Token.AND: {
				// Since x && y gives x, not false, when Boolean(x) is false,
				// and y, not Boolean(y), when Boolean(x) is true, x && y
				// can only be simplified if x is defined. See bug 309957.

				int leftStatus = isAlwaysDefinedBoolean(left);
				if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
					// if the first one is false, just return it
					return left;
				} else if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
					// if first is true, set to second
					return right;
				}
				break;
			}

			case Token.OR: {
				// Since x || y gives x, not true, when Boolean(x) is true,
				// and y, not Boolean(y), when Boolean(x) is false, x || y
				// can only be simplified if x is defined. See bug 309957.

				int leftStatus = isAlwaysDefinedBoolean(left);
				if (leftStatus == ALWAYS_TRUE_BOOLEAN) {
					// if the first one is true, just return it
					return left;
				} else if (leftStatus == ALWAYS_FALSE_BOOLEAN) {
					// if first is false, set to second
					return right;
				}
				break;
			}
		}

		return new Node(nodeType, left, right);
	}

	private Node createAssignment(int assignType, Node left, Node right) {
		Node ref = makeReference(left);
		if (ref == null) {
			if (left.getType() == Token.ARRAYLIT || left.getType() == Token.OBJECTLIT) {
				if (assignType != Token.ASSIGN) {
					reportError("msg.bad.destruct.op");
					return right;
				}
				return createDestructuringAssignment(-1, left, right);
			}
			reportError("msg.bad.assign.left");
			return right;
		}
		left = ref;

		int assignOp;
		switch (assignType) {
			case Token.ASSIGN:
				return simpleAssignment(left, right);
			case Token.ASSIGN_BITOR:
				assignOp = Token.BITOR;
				break;
			case Token.ASSIGN_BITXOR:
				assignOp = Token.BITXOR;
				break;
			case Token.ASSIGN_BITAND:
				assignOp = Token.BITAND;
				break;
			case Token.ASSIGN_LSH:
				assignOp = Token.LSH;
				break;
			case Token.ASSIGN_RSH:
				assignOp = Token.RSH;
				break;
			case Token.ASSIGN_URSH:
				assignOp = Token.URSH;
				break;
			case Token.ASSIGN_ADD:
				assignOp = Token.ADD;
				break;
			case Token.ASSIGN_SUB:
				assignOp = Token.SUB;
				break;
			case Token.ASSIGN_MUL:
				assignOp = Token.MUL;
				break;
			case Token.ASSIGN_DIV:
				assignOp = Token.DIV;
				break;
			case Token.ASSIGN_MOD:
				assignOp = Token.MOD;
				break;
			default:
				throw Kit.codeBug();
		}

		int nodeType = left.getType();
		switch (nodeType) {
			case Token.NAME -> {
				Node op = new Node(assignOp, left, right);
				Node lvalueLeft = Node.newString(Token.BINDNAME, left.getString());
				return new Node(Token.SETNAME, lvalueLeft, op);
			}
			case Token.GETPROP, Token.GETELEM -> {
				Node obj = left.getFirstChild();
				Node id = left.getLastChild();

				int type = nodeType == Token.GETPROP ? Token.SETPROP_OP : Token.SETELEM_OP;

				Node opLeft = new Node(Token.USE_STACK);
				Node op = new Node(assignOp, opLeft, right);
				return new Node(type, obj, id, op);
			}
			case Token.GET_REF -> {
				ref = left.getFirstChild();
				checkMutableReference(ref);
				Node opLeft = new Node(Token.USE_STACK);
				Node op = new Node(assignOp, opLeft, right);
				return new Node(Token.SET_REF_OP, ref, op);
			}
		}

		throw Kit.codeBug();
	}

	private static Node createUseLocal(Node localBlock) {
		if (Token.LOCAL_BLOCK != localBlock.getType()) {
			throw Kit.codeBug();
		}
		Node result = new Node(Token.LOCAL_LOAD);
		result.putProp(Node.LOCAL_BLOCK_PROP, localBlock);
		return result;
	}

	private static Jump makeJump(int type, Node target) {
		Jump n = new Jump(type);
		n.target = target;
		return n;
	}

	private static Node makeReference(Node node) {
		int type = node.getType();
		switch (type) {
			case Token.NAME:
			case Token.GETPROP:
			case Token.GETELEM:
			case Token.GET_REF:
				return node;
			case Token.CALL:
				node.setType(Token.REF_CALL);
				return new Node(Token.GET_REF, node);
		}
		// Signal caller to report error
		return null;
	}

	// Check if Node always mean true or false in boolean context
	private static int isAlwaysDefinedBoolean(Node node) {
		switch (node.getType()) {
			case Token.FALSE:
			case Token.NULL:
				return ALWAYS_FALSE_BOOLEAN;
			case Token.TRUE:
				return ALWAYS_TRUE_BOOLEAN;
			case Token.NUMBER: {
				double num = node.getDouble();
				if (!Double.isNaN(num) && num != 0.0) {
					return ALWAYS_TRUE_BOOLEAN;
				}
				return ALWAYS_FALSE_BOOLEAN;
			}
		}
		return 0;
	}

	// Check if node is the target of a destructuring bind.
	boolean isDestructuring(Node n) {
		return n instanceof DestructuringForm && ((DestructuringForm) n).isDestructuring();
	}
}

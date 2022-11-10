/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ErrorReporter;
import dev.latvian.mods.rhino.EvaluatorException;

import java.util.ArrayList;
import java.util.List;

/**
 * An error reporter that gathers the errors and warnings for later display.
 * This a useful {@link ErrorReporter} when the
 * {@link CompilerEnvirons} is set to
 * ide-mode (for IDEs).
 *
 * @author Steve Yegge
 */
public class ErrorCollector implements IdeErrorReporter {

	private final List<ParseProblem> errors = new ArrayList<>();

	/**
	 * This is not called during AST generation.
	 * {@link #warning(String, String, int, int)} is used instead.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void warning(String message, String sourceName, int offset, int length) {
		errors.add(new ParseProblem(ParseProblem.Type.Warning, message, sourceName, offset, length));
	}

	/**
	 * This is not called during AST generation.
	 * {@link #warning(String, String, int, int)} is used instead.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void error(Context cx, String message, String sourceName, int line, String lineSource, int lineOffset) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void error(String message, String sourceName, int fileOffset, int length) {
		errors.add(new ParseProblem(ParseProblem.Type.Error, message, sourceName, fileOffset, length));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EvaluatorException runtimeError(Context cx, String message, String sourceName, int line, String lineSource, int lineOffset) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the list of errors and warnings produced during parsing.
	 */
	public List<ParseProblem> getErrors() {
		return errors;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(errors.size() * 100);
		for (ParseProblem pp : errors) {
			sb.append(pp.toString()).append("\n");
		}
		return sb.toString();
	}
}

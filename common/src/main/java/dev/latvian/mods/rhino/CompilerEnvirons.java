/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

public class CompilerEnvirons {
	public CompilerEnvirons() {
		errorReporter = DefaultErrorReporter.instance;
		reservedKeywordAsIdentifier = true;
	}

	public void initFromContext(Context cx) {
		setErrorReporter(cx.getErrorReporter());
		reservedKeywordAsIdentifier = cx.hasFeature(Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER);
		allowMemberExprAsFunctionName = cx.hasFeature(Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME);
		strictMode = cx.hasFeature(Context.FEATURE_STRICT_MODE);
		warningAsError = cx.hasFeature(Context.FEATURE_WARNING_AS_ERROR);
	}

	public final ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	public void setErrorReporter(ErrorReporter errorReporter) {
		if (errorReporter == null) {
			throw new IllegalArgumentException();
		}
		this.errorReporter = errorReporter;
	}

	public final boolean isReservedKeywordAsIdentifier() {
		return reservedKeywordAsIdentifier;
	}

	/**
	 * Extension to ECMA: if 'function &lt;name&gt;' is not followed
	 * by '(', assume &lt;name&gt; starts a {@code memberExpr}
	 */
	public final boolean isAllowMemberExprAsFunctionName() {
		return allowMemberExprAsFunctionName;
	}

	public final boolean isStrictMode() {
		return strictMode;
	}

	public void setStrictMode(boolean strict) {
		strictMode = strict;
	}

	public final boolean reportWarningAsError() {
		return warningAsError;
	}

	private ErrorReporter errorReporter;

	private boolean reservedKeywordAsIdentifier;
	private boolean allowMemberExprAsFunctionName;
	private boolean strictMode;
	private boolean warningAsError;
}

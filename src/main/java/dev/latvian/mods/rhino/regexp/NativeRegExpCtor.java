/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.regexp;

import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.TopLevel;
import dev.latvian.mods.rhino.Undefined;

/**
 * This class implements the RegExp constructor native object.
 * <p>
 * Revision History:
 * Implementation in C by Brendan Eich
 * Initial port to Java by Norris Boyd from jsregexp.c version 1.36
 * Merged up to version 1.38, which included Unicode support.
 * Merged bug fixes in version 1.39.
 * Merged JSFUN13_BRANCH changes up to 1.32.2.11
 *
 * @author Brendan Eich
 * @author Norris Boyd
 */
class NativeRegExpCtor extends BaseFunction {
	private static final int Id_multiline = 1;
	private static final int Id_STAR = 2;  // #string=$*#
	private static final int Id_input = 3;
	private static final int Id_UNDERSCORE = 4;  // #string=$_#
	private static final int Id_lastMatch = 5;
	private static final int Id_AMPERSAND = 6;  // #string=$&#
	private static final int Id_lastParen = 7;

	// #string_id_map#
	private static final int Id_PLUS = 8;  // #string=$+#
	private static final int Id_leftContext = 9;
	private static final int Id_BACK_QUOTE = 10; // #string=$`#
	private static final int Id_rightContext = 11;
	private static final int Id_QUOTE = 12; // #string=$'#
	private static final int DOLLAR_ID_BASE = 12;
	private static final int Id_DOLLAR_1 = DOLLAR_ID_BASE + 1; // #string=$1#
	private static final int Id_DOLLAR_2 = DOLLAR_ID_BASE + 2; // #string=$2#
	private static final int Id_DOLLAR_3 = DOLLAR_ID_BASE + 3; // #string=$3#
	private static final int Id_DOLLAR_4 = DOLLAR_ID_BASE + 4; // #string=$4#
	private static final int Id_DOLLAR_5 = DOLLAR_ID_BASE + 5; // #string=$5#
	private static final int Id_DOLLAR_6 = DOLLAR_ID_BASE + 6; // #string=$6#
	private static final int Id_DOLLAR_7 = DOLLAR_ID_BASE + 7; // #string=$7#
	private static final int Id_DOLLAR_8 = DOLLAR_ID_BASE + 8; // #string=$8#
	private static final int Id_DOLLAR_9 = DOLLAR_ID_BASE + 9; // #string=$9#
	private static final int MAX_INSTANCE_ID = DOLLAR_ID_BASE + 9;

	private int multilineAttr = PERMANENT;
	private int starAttr = PERMANENT;
	private int inputAttr = PERMANENT;
	private int underscoreAttr = PERMANENT;

	NativeRegExpCtor() {
	}

	@Override
	public String getFunctionName() {
		return "RegExp";
	}

	@Override
	public int getLength() {
		return 2;
	}

	@Override
	public int getArity() {
		return 2;
	}

	// #/string_id_map#

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (args.length > 0 && args[0] instanceof NativeRegExp && (args.length == 1 || args[1] == Undefined.INSTANCE)) {
			return args[0];
		}
		return construct(cx, scope, args);
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		NativeRegExp re = new NativeRegExp();
		re.compile(cx, scope, args);
		ScriptRuntime.setBuiltinProtoAndParent(cx, scope, re, TopLevel.Builtins.RegExp);
		return re;
	}

	@Override
	protected int getMaxInstanceId() {
		return super.getMaxInstanceId() + MAX_INSTANCE_ID;
	}

	@Override
	protected int findInstanceIdInfo(String s, Context cx) {
		int id = switch (s) {
			case "multiline" -> Id_multiline;
			case "$*" -> Id_STAR;
			case "input" -> Id_input;
			case "$_" -> Id_UNDERSCORE;
			case "lastMatch" -> Id_lastMatch;
			case "$&" -> Id_AMPERSAND;
			case "lastParen" -> Id_lastParen;
			case "$+" -> Id_PLUS;
			case "leftContext" -> Id_leftContext;
			case "$`" -> Id_BACK_QUOTE;
			case "rightContext" -> Id_rightContext;
			case "$'" -> Id_QUOTE;
			case "$1" -> Id_DOLLAR_1;
			case "$2" -> Id_DOLLAR_2;
			case "$3" -> Id_DOLLAR_3;
			case "$4" -> Id_DOLLAR_4;
			case "$5" -> Id_DOLLAR_5;
			case "$6" -> Id_DOLLAR_6;
			case "$7" -> Id_DOLLAR_7;
			case "$8" -> Id_DOLLAR_8;
			case "$9" -> Id_DOLLAR_9;
			default -> 0;
		};

		if (id == 0) {
			return super.findInstanceIdInfo(s, cx);
		}

		int attr = switch (id) {
			case Id_multiline -> multilineAttr;
			case Id_STAR -> starAttr;
			case Id_input -> inputAttr;
			case Id_UNDERSCORE -> underscoreAttr;
			default -> PERMANENT | READONLY;
		};

		return instanceIdInfo(attr, super.getMaxInstanceId() + id);
	}

	@Override
	protected String getInstanceIdName(int id) {
		int shifted = id - super.getMaxInstanceId();

		if (1 <= shifted && shifted <= MAX_INSTANCE_ID) {
			return switch (shifted) {
				case Id_multiline -> "multiline";
				case Id_STAR -> "$*";
				case Id_input -> "input";
				case Id_UNDERSCORE -> "$_";
				case Id_lastMatch -> "lastMatch";
				case Id_AMPERSAND -> "$&";
				case Id_lastParen -> "lastParen";
				case Id_PLUS -> "$+";
				case Id_leftContext -> "leftContext";
				case Id_BACK_QUOTE -> "$`";
				case Id_rightContext -> "rightContext";
				case Id_QUOTE -> "$'";
				case Id_DOLLAR_1 -> "$1";
				case Id_DOLLAR_2 -> "$2";
				case Id_DOLLAR_3 -> "$3";
				case Id_DOLLAR_4 -> "$4";
				case Id_DOLLAR_5 -> "$5";
				case Id_DOLLAR_6 -> "$6";
				case Id_DOLLAR_7 -> "$7";
				case Id_DOLLAR_8 -> "$8";
				case Id_DOLLAR_9 -> "$9";
				default -> super.getInstanceIdName(id);
			};
		}

		return super.getInstanceIdName(id);
	}

	@Override
	protected Object getInstanceIdValue(int id, Context cx) {
		int shifted = id - super.getMaxInstanceId();
		if (1 <= shifted && shifted <= MAX_INSTANCE_ID) {
			RegExp impl = cx.getRegExp();
			Object stringResult;
			switch (shifted) {
				case Id_multiline:
				case Id_STAR:
					return impl.multiline;

				case Id_input:
				case Id_UNDERSCORE:
					stringResult = impl.input;
					break;

				case Id_lastMatch:
				case Id_AMPERSAND:
					stringResult = impl.lastMatch;
					break;

				case Id_lastParen:
				case Id_PLUS:
					stringResult = impl.lastParen;
					break;

				case Id_leftContext:
				case Id_BACK_QUOTE:
					stringResult = impl.leftContext;
					break;

				case Id_rightContext:
				case Id_QUOTE:
					stringResult = impl.rightContext;
					break;

				default: {
					// Must be one of $1..$9, convert to 0..8
					int substring_number = shifted - DOLLAR_ID_BASE - 1;
					stringResult = impl.getParenSubString(substring_number);
					break;
				}
			}
			return (stringResult == null) ? "" : stringResult.toString();
		}
		return super.getInstanceIdValue(id, cx);
	}

	@Override
	protected void setInstanceIdValue(int id, Object value, Context cx) {
		int shifted = id - super.getMaxInstanceId();
		switch (shifted) {
			case Id_multiline:
			case Id_STAR:
				cx.getRegExp().multiline = ScriptRuntime.toBoolean(cx, value);
				return;

			case Id_input:
			case Id_UNDERSCORE:
				cx.getRegExp().input = ScriptRuntime.toString(cx, value);
				return;

			case Id_lastMatch:
			case Id_AMPERSAND:
			case Id_lastParen:
			case Id_PLUS:
			case Id_leftContext:
			case Id_BACK_QUOTE:
			case Id_rightContext:
			case Id_QUOTE:
				return;
			default:
				int substring_number = shifted - DOLLAR_ID_BASE - 1;
				if (0 <= substring_number && substring_number <= 8) {
					return;
				}
		}
		super.setInstanceIdValue(id, value, cx);
	}

	@Override
	protected void setInstanceIdAttributes(int id, int attr, Context cx) {
		int shifted = id - super.getMaxInstanceId();
		switch (shifted) {
			case Id_multiline:
				multilineAttr = attr;
				return;
			case Id_STAR:
				starAttr = attr;
				return;
			case Id_input:
				inputAttr = attr;
				return;
			case Id_UNDERSCORE:
				underscoreAttr = attr;
				return;

			case Id_lastMatch:
			case Id_AMPERSAND:
			case Id_lastParen:
			case Id_PLUS:
			case Id_leftContext:
			case Id_BACK_QUOTE:
			case Id_rightContext:
			case Id_QUOTE:
				// non-configurable + non-writable
				return;
			default:
				int substring_number = shifted - DOLLAR_ID_BASE - 1;
				if (0 <= substring_number && substring_number <= 8) {
					// non-configurable + non-writable
					return;
				}
		}
		super.setInstanceIdAttributes(id, attr, cx);
	}
}

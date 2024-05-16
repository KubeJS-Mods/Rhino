/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.regexp;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.Kit;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Undefined;

/**
 *
 */
public class RegExp {
	public static final int RA_MATCH = 1;
	public static final int RA_REPLACE = 2;
	public static final int RA_SEARCH = 3;

	private static NativeRegExp createRegExp(Context cx, Scriptable scope, Object[] args, int optarg, boolean forceFlat) {
		NativeRegExp re;
		Scriptable topScope = ScriptableObject.getTopLevelScope(scope);
		if (args.length == 0 || args[0] == Undefined.INSTANCE) {
			RECompiled compiled = NativeRegExp.compileRE(cx, "", "", false);
			re = new NativeRegExp(topScope, compiled, cx);
		} else if (args[0] instanceof NativeRegExp) {
			re = (NativeRegExp) args[0];
		} else {
			String src = ScriptRuntime.toString(cx, args[0]);
			String opt;
			if (optarg < args.length) {
				args[0] = src;
				opt = ScriptRuntime.toString(cx, args[optarg]);
			} else {
				opt = null;
			}
			RECompiled compiled = NativeRegExp.compileRE(cx, src, opt, forceFlat);
			re = new NativeRegExp(topScope, compiled, cx);
		}
		return re;
	}

	/**
	 * Analog of C match_or_replace.
	 */
	private static Object matchOrReplace(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, RegExp reImpl, GlobData data, NativeRegExp re) {
		String str = data.str;
		data.global = (re.getFlags() & NativeRegExp.JSREG_GLOB) != 0;
		int[] indexp = {0};
		Object result = null;
		if (data.mode == RA_SEARCH) {
			result = re.executeRegExp(cx, scope, reImpl, str, indexp, NativeRegExp.TEST);
			if (result != null && result.equals(Boolean.TRUE)) {
				result = reImpl.leftContext.length;
			} else {
				result = -1;
			}
		} else if (data.global) {
			re.lastIndex = ScriptRuntime.zeroObj;
			for (int count = 0; indexp[0] <= str.length(); count++) {
				result = re.executeRegExp(cx, scope, reImpl, str, indexp, NativeRegExp.TEST);
				if (result == null || !result.equals(Boolean.TRUE)) {
					break;
				}
				if (data.mode == RA_MATCH) {
					match_glob(data, cx, scope, count, reImpl);
				} else {
					if (data.mode != RA_REPLACE) {
						Kit.codeBug();
					}
					SubString lastMatch = reImpl.lastMatch;
					int leftIndex = data.leftIndex;
					int leftlen = lastMatch.index - leftIndex;
					data.leftIndex = lastMatch.index + lastMatch.length;
					replace_glob(data, cx, scope, reImpl, leftIndex, leftlen);
				}
				if (reImpl.lastMatch.length == 0) {
					if (indexp[0] == str.length()) {
						break;
					}
					indexp[0]++;
				}
			}
		} else {
			result = re.executeRegExp(cx, scope, reImpl, str, indexp, ((data.mode == RA_REPLACE) ? NativeRegExp.TEST : NativeRegExp.MATCH));
		}

		return result;
	}

	/*
	 * Analog of match_glob() in jsstr.c
	 */
	private static void match_glob(GlobData mdata, Context cx, Scriptable scope, int count, RegExp reImpl) {
		if (mdata.arrayobj == null) {
			mdata.arrayobj = cx.newArray(scope, 0);
		}
		SubString matchsub = reImpl.lastMatch;
		String matchstr = matchsub.toString();
		mdata.arrayobj.put(cx, count, mdata.arrayobj, matchstr);
	}

	/*
	 * Analog of replace_glob() in jsstr.c
	 */
	private static void replace_glob(GlobData rdata, Context cx, Scriptable scope, RegExp reImpl, int leftIndex, int leftlen) {
		int replen;
		String lambdaStr;
		if (rdata.lambda != null) {
			// invoke lambda function with args lastMatch, $1, $2, ... $n,
			// leftContext.length, whole string.
			SubString[] parens = reImpl.parens;
			int parenCount = (parens == null) ? 0 : parens.length;
			Object[] args = new Object[parenCount + 3];
			args[0] = reImpl.lastMatch.toString();
			for (int i = 0; i < parenCount; i++) {
				SubString sub = parens[i];
				if (sub != null) {
					args[i + 1] = sub.toString();
				} else {
					args[i + 1] = Undefined.INSTANCE;
				}
			}
			args[parenCount + 1] = reImpl.leftContext.length;
			args[parenCount + 2] = rdata.str;
			// This is a hack to prevent expose of reImpl data to
			// JS function which can run new regexps modifing
			// regexp that are used later by the engine.
			// TODO: redesign is necessary
			if (reImpl != cx.getRegExp()) {
				Kit.codeBug();
			}
			RegExp re2 = new RegExp();
			re2.multiline = reImpl.multiline;
			re2.input = reImpl.input;
			ScriptRuntime.setRegExpProxy(cx, re2);
			try {
				Scriptable parent = ScriptableObject.getTopLevelScope(scope);
				Object result = rdata.lambda.call(cx, parent, parent, args);
				lambdaStr = ScriptRuntime.toString(cx, result);
			} finally {
				ScriptRuntime.setRegExpProxy(cx, reImpl);
			}
			replen = lambdaStr.length();
		} else {
			lambdaStr = null;
			replen = rdata.repstr.length();
			if (rdata.dollar >= 0) {
				int[] skip = new int[1];
				int dp = rdata.dollar;
				do {
					SubString sub = interpretDollar(cx, reImpl, rdata.repstr, dp, skip);
					if (sub != null) {
						replen += sub.length - skip[0];
						dp += skip[0];
					} else {
						++dp;
					}
					dp = rdata.repstr.indexOf('$', dp);
				} while (dp >= 0);
			}
		}

		int growth = leftlen + replen + reImpl.rightContext.length;
		StringBuilder charBuf = rdata.charBuf;
		if (charBuf == null) {
			charBuf = new StringBuilder(growth);
			rdata.charBuf = charBuf;
		} else {
			charBuf.ensureCapacity(rdata.charBuf.length() + growth);
		}

		charBuf.append(reImpl.leftContext.str, leftIndex, leftIndex + leftlen);
		if (rdata.lambda != null) {
			charBuf.append(lambdaStr);
		} else {
			do_replace(rdata, cx, reImpl);
		}
	}

	private static SubString interpretDollar(Context cx, RegExp res, String da, int dp, int[] skip) {
		char dc;
		int num, tmp;

		if (da.charAt(dp) != '$') {
			Kit.codeBug();
		}

		/* Allow a real backslash (literal "\\") to escape "$1" etc. */
		int daL = da.length();
		if (dp + 1 >= daL) {
			return null;
		}
		/* Interpret all Perl match-induced dollar variables. */
		dc = da.charAt(dp + 1);
		if (NativeRegExp.isDigit(dc)) {
			int cp;
			int parenCount = (res.parens == null) ? 0 : res.parens.length;
			num = dc - '0';
			if (num > parenCount) {
				return null;
			}
			cp = dp + 2;
			if ((dp + 2) < daL) {
				dc = da.charAt(dp + 2);
				if (NativeRegExp.isDigit(dc)) {
					tmp = 10 * num + (dc - '0');
					if (tmp <= parenCount) {
						cp++;
						num = tmp;
					}
				}
			}
			if (num == 0) {
				return null;  /* $0 or $00 is not valid */
			}
			/* Adjust num from 1 $n-origin to 0 array-index-origin. */
			num--;
			skip[0] = cp - dp;
			return res.getParenSubString(num);
		}

		skip[0] = 2;
		return switch (dc) {
			case '$' -> new SubString("$");
			case '&' -> res.lastMatch;
			case '+' -> res.lastParen;
			case '`' -> res.leftContext;
			case '\'' -> res.rightContext;
			default -> null;
		};
	}

	/**
	 * Analog of do_replace in jsstr.c
	 */
	private static void do_replace(GlobData rdata, Context cx, RegExp regExp) {
		StringBuilder charBuf = rdata.charBuf;
		int cp = 0;
		String da = rdata.repstr;
		int dp = rdata.dollar;
		if (dp != -1) {
			int[] skip = new int[1];
			do {
				int len = dp - cp;
				charBuf.append(da, cp, dp);
				cp = dp;
				SubString sub = interpretDollar(cx, regExp, da, dp, skip);
				if (sub != null) {
					len = sub.length;
					if (len > 0) {
						charBuf.append(sub.str, sub.index, sub.index + len);
					}
					cp += skip[0];
					dp += skip[0];
				} else {
					++dp;
				}
				dp = da.indexOf('$', dp);
			} while (dp >= 0);
		}
		int daL = da.length();
		if (daL > cp) {
			charBuf.append(da, cp, daL);
		}
	}

	/*
	 * Used by js_split to find the next split point in target,
	 * starting at offset ip and looking either for the given
	 * separator substring, or for the next re match.  ip and
	 * matchlen must be reference variables (assumed to be arrays of
	 * length 1) so they can be updated in the leading whitespace or
	 * re case.
	 *
	 * Return -1 on end of string, >= 0 for a valid index of the next
	 * separator occurrence if found, or the string length if no
	 * separator is found.
	 */
	private static int find_split(Context cx, Scriptable scope, String target, String separator, RegExp reProxy, Scriptable re, int[] ip, int[] matchlen, boolean[] matched, String[][] parensp) {
		int i = ip[0];
		int length = target.length();

		/*
		 * Stop if past end of string.  If at end of string, we will
		 * return target length, so that
		 *
		 *  "ab,".split(',') => new Array("ab", "")
		 *
		 * and the resulting array converts back to the string "ab,"
		 * for symmetry.  NB: This differs from perl, which drops the
		 * trailing empty substring if the LIMIT argument is omitted.
		 */
		if (i > length) {
			return -1;
		}

		/*
		 * Match a regular expression against the separator at or
		 * above index i.  Return -1 at end of string instead of
		 * trying for a match, so we don't get stuck in a loop.
		 */
		if (re != null) {
			return reProxy.find_split(cx, scope, target, separator, re, ip, matchlen, matched, parensp);
		}

		/*
		 * Special case: if sep is the empty string, split str into
		 * one character substrings.  Let our caller worry about
		 * whether to split once at end of string into an empty
		 * substring.
		 *
		 * For 1.2 compatibility, at the end of the string, we return the length as
		 * the result, and set the separator length to 1 -- this allows the caller
		 * to include an additional null string at the end of the substring list.
		 */
		if (separator.length() == 0) {
			return (i == length) ? -1 : i + 1;
		}

		/* Punt to j.l.s.indexOf; return target length if separator is
		 * not found.
		 */
		if (ip[0] >= length) {
			return length;
		}

		i = target.indexOf(separator, ip[0]);

		return (i != -1) ? i : length;
	}

	protected String input;         /* input string to match (perl $_, GC root) */
	protected boolean multiline;     /* whether input contains newlines (perl $*) */
	protected SubString[] parens;        /* Vector of SubString; last set of parens matched (perl $1, $2) */
	protected SubString lastMatch;     /* last string matched (perl $&) */
	protected SubString lastParen;     /* last paren matched (perl $+) */
	protected SubString leftContext;   /* input to left of last match (perl $`) */
	protected SubString rightContext;  /* input to right of last match (perl $') */

	public boolean isRegExp(Scriptable obj) {
		return obj instanceof NativeRegExp;
	}

	public Object compileRegExp(Context cx, String source, String flags) {
		return NativeRegExp.compileRE(cx, source, flags, false);
	}

	public Scriptable wrapRegExp(Context cx, Scriptable scope, Object compiled) {
		return new NativeRegExp(scope, (RECompiled) compiled, cx);
	}

	public Object action(Context cx, Scriptable scope, Scriptable thisObj, Object[] args, int actionType) {
		GlobData data = new GlobData();
		data.mode = actionType;
		data.str = ScriptRuntime.toString(cx, thisObj);

		switch (actionType) {
			case RA_MATCH -> {
				int optarg = Integer.MAX_VALUE;
				NativeRegExp re = createRegExp(cx, scope, args, optarg, false);
				Object rval = matchOrReplace(cx, scope, thisObj, args, this, data, re);
				return data.arrayobj == null ? rval : data.arrayobj;
			}
			case RA_SEARCH -> {
				int optarg = Integer.MAX_VALUE;
				NativeRegExp re = createRegExp(cx, scope, args, optarg, false);
				return matchOrReplace(cx, scope, thisObj, args, this, data, re);
			}
			case RA_REPLACE -> {
				boolean useRE = args.length > 0 && args[0] instanceof NativeRegExp;
				NativeRegExp re = null;
				String search = null;
				if (useRE) {
					re = createRegExp(cx, scope, args, 2, true);
				} else {
					Object arg0 = args.length < 1 ? Undefined.INSTANCE : args[0];
					search = ScriptRuntime.toString(cx, arg0);
				}

				Object arg1 = args.length < 2 ? Undefined.INSTANCE : args[1];
				String repstr = null;
				Function lambda = null;
				if (arg1 instanceof Function && (!(arg1 instanceof NativeRegExp))) {
					lambda = (Function) arg1;
				} else {
					repstr = ScriptRuntime.toString(cx, arg1);
				}

				data.lambda = lambda;
				data.repstr = repstr;
				data.dollar = repstr == null ? -1 : repstr.indexOf('$');
				data.charBuf = null;
				data.leftIndex = 0;

				Object val;
				if (useRE) {
					val = matchOrReplace(cx, scope, thisObj, args, this, data, re);
				} else {
					String str = data.str;
					int index = str.indexOf(search);
					if (index >= 0) {
						int slen = search.length();
						this.parens = null;
						this.lastParen = null;
						this.leftContext = new SubString(str, 0, index);
						this.lastMatch = new SubString(str, index, slen);
						this.rightContext = new SubString(str, index + slen, str.length() - index - slen);
						val = Boolean.TRUE;
					} else {
						val = Boolean.FALSE;
					}
				}

				if (data.charBuf == null) {
					if (data.global || val == null || !val.equals(Boolean.TRUE)) {
						/* Didn't match even once. */
						return data.str;
					}
					SubString lc = this.leftContext;
					replace_glob(data, cx, scope, this, lc.index, lc.length);
				}
				SubString rc = this.rightContext;
				data.charBuf.append(rc.str, rc.index, rc.index + rc.length);
				return data.charBuf.toString();
			}
			default -> throw Kit.codeBug();
		}
	}

	public int find_split(Context cx, Scriptable scope, String target, String separator, Scriptable reObj, int[] ip, int[] matchlen, boolean[] matched, String[][] parensp) {
		int i = ip[0];
		int length = target.length();
		int result;

		NativeRegExp re = (NativeRegExp) reObj;
		again:
		while (true) {  // imitating C label
			/* JS1.2 deviated from Perl by never matching at end of string. */
			int ipsave = ip[0]; // reuse ip to save object creation
			ip[0] = i;
			Object ret = re.executeRegExp(cx, scope, this, target, ip, NativeRegExp.TEST);
			if (!Boolean.TRUE.equals(ret)) {
				// Mismatch: ensure our caller advances i past end of string.
				ip[0] = ipsave;
				matchlen[0] = 1;
				matched[0] = false;
				return length;
			}
			i = ip[0];
			ip[0] = ipsave;
			matched[0] = true;

			SubString sep = this.lastMatch;
			matchlen[0] = sep.length;
			if (matchlen[0] == 0) {
				/*
				 * Empty string match: never split on an empty
				 * match at the start of a find_split cycle.  Same
				 * rule as for an empty global match in
				 * match_or_replace.
				 */
				if (i == ip[0]) {
					/*
					 * "Bump-along" to avoid sticking at an empty
					 * match, but don't bump past end of string --
					 * our caller must do that by adding
					 * sep->length to our return value.
					 */
					if (i == length) {
						result = -1;
						break;
					}
					i++;
					continue again; // imitating C goto
				}
			}
			// PR_ASSERT((size_t)i >= sep->length);
			result = i - matchlen[0];
			break;
		}
		int size = (parens == null) ? 0 : parens.length;
		parensp[0] = new String[size];
		for (int num = 0; num < size; num++) {
			SubString parsub = getParenSubString(num);
			parensp[0][num] = parsub.toString();
		}
		return result;
	}

	/**
	 * Analog of REGEXP_PAREN_SUBSTRING in C jsregexp.h.
	 * Assumes zero-based; i.e., for $3, i==2
	 */
	SubString getParenSubString(int i) {
		if (parens != null && i < parens.length) {
			SubString parsub = parens[i];
			if (parsub != null) {
				return parsub;
			}
		}
		return new SubString();
	}

	/*
	 * See ECMA 15.5.4.8.  Modified to match JS 1.2 - optionally takes
	 * a limit argument and accepts a regular expression as the split
	 * argument.
	 */
	public Object js_split(Context cx, Scriptable scope, String target, Object[] args) {
		// create an empty Array to return;
		Scriptable result = cx.newArray(scope, 0);

		// Use the second argument as the split limit, if given.
		boolean limited = (args.length > 1) && (args[1] != Undefined.INSTANCE);
		long limit = 0;  // Initialize to avoid warning.
		if (limited) {
			/* Clamp limit between 0 and 1 + string length. */
			limit = ScriptRuntime.toUint32(cx, args[1]);
			if (limit == 0) {
				return result;
			}
			if (limit > target.length()) {
				limit = 1 + target.length();
			}
		}

		// return an array consisting of the target if no separator given
		if (args.length < 1 || args[0] == Undefined.INSTANCE) {
			result.put(cx, 0, result, target);
			return result;
		}

		String separator = null;
		int[] matchlen = new int[1];
		Scriptable re = null;
		RegExp reProxy = null;
		if (args[0] instanceof Scriptable) {
			reProxy = cx.getRegExp();
			if (reProxy != null) {
				Scriptable test = (Scriptable) args[0];
				if (reProxy.isRegExp(test)) {
					re = test;
				}
			}
		}
		if (re == null) {
			separator = ScriptRuntime.toString(cx, args[0]);
			matchlen[0] = separator.length();
		}

		// split target with separator or re
		int[] ip = {0};
		int match;
		int len = 0;
		boolean[] matched = {false};
		String[][] parens = {null};
		while ((match = find_split(cx, scope, target, separator, reProxy, re, ip, matchlen, matched, parens)) >= 0) {
			if ((limited && len >= limit) || (match > target.length())) {
				break;
			}

			String substr;
			if (target.length() == 0) {
				substr = target;
			} else {
				substr = target.substring(ip[0], match);
			}

			result.put(cx, len, result, substr);
			len++;
			/*
			 * Imitate perl's feature of including parenthesized substrings
			 * that matched part of the delimiter in the new array, after the
			 * split substring that was delimited.
			 */
			if (re != null && matched[0]) {
				int size = parens[0].length;
				for (int num = 0; num < size; num++) {
					if (limited && len >= limit) {
						break;
					}
					result.put(cx, len, result, parens[0][num]);
					len++;
				}
				matched[0] = false;
			}
			ip[0] = match + matchlen[0];
		}
		return result;
	}
}


final class GlobData {
	int mode;      /* input: return index, match object, or void */
	boolean global;    /* output: whether regexp was global */
	String str;       /* output: 'this' parameter object as string */

	// match-specific data

	Scriptable arrayobj;

	// replace-specific data

	Function lambda;        /* replacement function object or null */
	String repstr;        /* replacement string */
	int dollar = -1;   /* -1 or index of first $ in repstr */
	StringBuilder charBuf;       /* result characters, null initially */
	int leftIndex;     /* leftContext index, always 0 for JS1.2 */
}

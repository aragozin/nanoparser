package org.gridkit.nanoparser;

public class Tokens {

	public static TokenMatcher matcher(String text) {
		return new StringMatcher(text);
	}

	public static TokenMatcher regExMatcher(String text) {
		return new RegExMatcher(text);
	}
	
	public static TokenMatcher whitespace() {
		return new Whitespace();
	}
	
	public static TokenMatcher any() {
		return new LumpingAnyMatcher();
	}
	
	public static TokenMatcher comment(String start, String end) {
		return new MultiLineCommentMatcher(matcher(start), matcher(end));
	}
	
}

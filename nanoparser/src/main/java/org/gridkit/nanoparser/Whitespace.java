package org.gridkit.nanoparser;

class Whitespace implements TokenMatcher {

	@Override
	public CharSet firstCharacter() {
		return new CharSet(' ');
	}

	@Override
	public int match(CharSequence cs, int offset) {
		if (Character.isWhitespace(cs.charAt(offset))) {
			return 1;
		}
		else {
			return -1;
		}
	}
	
	@Override
	public String toString() {
		return " ";
	}
}

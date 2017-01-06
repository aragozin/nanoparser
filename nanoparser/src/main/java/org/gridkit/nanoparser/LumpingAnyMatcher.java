package org.gridkit.nanoparser;

/**
 * This {@link TokenMatcher} matches any text. Whitespace and alphanumeric are
 * lumped into multi character token. Other symbols are producing single char tokens.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class LumpingAnyMatcher implements TokenMatcher {

	@Override
	public CharSet firstCharacter() {
		return CharSet.ALL;
	}

	@Override
	public int match(CharSequence cs, int offset) {
		int noffs = offset + 1;
		int m = mode(cs.charAt(offset));
		while(noffs < cs.length()) {
			char ch = cs.charAt(noffs);
			int mm = mode(ch);
			if (mm == 0 || mm != m) {
				break;
			}
			++noffs;
		}
		return offset - noffs;
	}
	
	int mode(char ch) {
		if (ch == '\n') {
			return 0;
		}
		else if (Character.isWhitespace(ch)) {
			return 1;
		}
		else if (Character.isLetter(ch) || Character.isDigit(ch)) {
			return 2;
		}
		else {
			return 0;
		}
	}
}

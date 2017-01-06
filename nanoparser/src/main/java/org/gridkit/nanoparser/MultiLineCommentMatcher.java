package org.gridkit.nanoparser;

class MultiLineCommentMatcher implements TokenMatcher {

	private final TokenMatcher start;
	private final TokenMatcher end;
	
	public MultiLineCommentMatcher(TokenMatcher start, TokenMatcher end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public CharSet firstCharacter() {
		return start.firstCharacter();
	}

	@Override
	public int match(CharSequence cs, int offset) {
		int n = start.match(cs, offset);
		if (n > 0) {
			int noffs = offset + n;
			while(noffs < cs.length()) {
				int m = end.match(cs, noffs);
				if (m > 0) {
					noffs += m;
					break;
				}
				++noffs;
			}
			return noffs - offset;
		}
		return -1;
	}
	
	@Override
	public String toString() {
		return start + " ... " + end;
	}
}

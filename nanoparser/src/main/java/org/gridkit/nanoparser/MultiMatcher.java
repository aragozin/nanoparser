package org.gridkit.nanoparser;

import java.util.Arrays;

class MultiMatcher implements TokenMatcher {

    private final TokenMatcher[] matchers;
    private final CharSet firstChar;


    public MultiMatcher(TokenMatcher... matchers) {
        this.matchers = matchers;
        CharSet fc = new CharSet();
        for(TokenMatcher tm: matchers) {
            fc = fc.union(tm.firstCharacter());
        }
        firstChar = fc;
    }

    public MultiMatcher append(TokenMatcher tm) {
        TokenMatcher[] mm = Arrays.copyOf(matchers, matchers.length + 1);
        mm[mm.length - 1] = tm;
        return new MultiMatcher(mm);
    }

    @Override
    public CharSet firstCharacter() {
        return firstChar;
    }

    @Override
    public int match(CharSequence cs, int offset) {
        for(TokenMatcher tc: matchers) {
            int n = tc.match(cs, offset);
            if (n > 0) {
                return n;
            }
        }
        return -1;
    }
}

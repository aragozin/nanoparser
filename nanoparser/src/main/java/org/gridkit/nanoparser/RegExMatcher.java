package org.gridkit.nanoparser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RegExMatcher implements TokenMatcher {

    private final Matcher matcher;

    public RegExMatcher(String pattern) {
        matcher = Pattern.compile(pattern).matcher("");
    }

    @Override
    public CharSet firstCharacter() {
        return CharSet.ALL;
    }

    @Override
    public int match(CharSequence cs, int offset) {
        matcher.reset(cs);
        matcher.region(offset, cs.length());
        if (matcher.lookingAt()) {
            return matcher.end() - matcher.start();
        }
        return -1;
    }

    @Override
    public String toString() {
        return matcher.pattern().pattern();
    }
}

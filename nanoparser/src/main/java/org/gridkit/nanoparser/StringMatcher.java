package org.gridkit.nanoparser;

class StringMatcher implements TokenMatcher {

    private final char[] text;

    public StringMatcher(String text) {
        if (text.length() == 0) {
            throw new IllegalArgumentException("String must not be empty");
        }
        this.text = new char[text.length()];
        for(int i = 0; i != this.text.length; ++i) {
            this.text[i] = text.charAt(i);
        }
    }

    @Override
    public CharSet firstCharacter() {
        return new CharSet(text[0]);
    }

    @Override
    public int match(CharSequence cs, int offset) {
        if (offset + text.length > cs.length()) {
            return -1;
        }
        for(int i = 0; i != text.length; ++i) {
            if (cs.charAt(offset + i) != text[i]) {
                return -1;
            }
        }
        return text.length;
    }

    @Override
    public String toString() {
        return new String(text);
    }
}

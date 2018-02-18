package org.gridkit.nanoparser;

/**
 * Wrapper over {@link CharSequence} used to trace
 * current parsing coordinates.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SourceReader {

    private CharSequence text;
    private int offset;
    private int line;
    private int pos;

    public SourceReader(CharSequence text) {
        this.text = text;
    }

    public int getOffset() {
        return offset;
    }

    public int getLine() {
        return line;
    }

    public int getPos() {
        return pos;
    }

    public boolean endOfStream() {
        return text.length() <= offset;
    }

    public Token matchToken(TokenMatcher matcher) {
        if (offset < text.length()) {
            int n = matcher.match(text, offset);
            if (n > 0) {
                PToken t = makeToken(n);
                offset += n;
                for(int i = 0; i != t.body.length(); ++i) {
                    if (t.body.charAt(0) == '\n') {
                        ++line;
                        pos = 0;
                    }
                    else {
                        ++pos;
                    }
                }
                return t;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    public Token matchToken(TokenMatcher[] multiToken, TokenMatcher skip) {
        if (multiToken.length == 1) {
            return matchToken(multiToken[0]);
        }
        else {

            int soffs = offset;
            int sline = line;
            int spos = pos;

            Token[] r = new Token[multiToken.length];
            for(int i = 0; i != multiToken.length; ++i) {
                r[i] = matchToken(multiToken[i]);
                if (r[i] == null) {
                    // not matched
                    offset = soffs;
                    line = sline;
                    pos = spos;
                    return null;
                }
                // skip skippable
                while(matchToken(skip) != null) {};
            }

            MToken mtkn = new MToken(r);
            mtkn.text = text;
            mtkn.body = text.subSequence(soffs, offset).toString();
            mtkn.offset = soffs;
            mtkn.line = sline;
            mtkn.pos = spos;
            return mtkn;
        }
    }

    private PToken makeToken(int n) {
        PToken t = new PToken();
        t.text = text;
        t.body = text.subSequence(offset, offset + n).toString();
        t.offset = offset;
        t.line = line;
        t.pos = pos;
        return t;
    }

    public Token emptyToken() {
        PToken t = new PToken();
        t.text = text;
        t.body = "";
        t.offset = offset;
        t.line = line;
        t.pos = pos;
        return t;
    }

    @Override
    public String toString() {
        return text.subSequence(offset, text.length()).toString();
    }

    private static class PToken implements Token {

        CharSequence text;
        String body;
        int offset;
        int line;
        int pos;

        @Override
        public String tokenBody() {
            return body;
        }

        @Override
        public CharSequence source() {
            return text;
        }

        @Override
        public int line() {
            return line + 1;
        }

        @Override
        public int pos() {
            return pos;
        }

        @Override
        public int offset() {
            return offset;
        }

        @Override
        public String excerpt() {
            return excerpt(60);
        }

        @Override
        public String excerpt(int excerptLengthLimit) {
            return ParserException.formatTokenExcertp(this, excerptLengthLimit);
        }

        @Override
        public String toString() {
            return body;
        }
    }

    private static class MToken extends PToken implements MultiToken {

        private final Token[] subtokens;

        public MToken(Token[] subtokens) {
            this.subtokens = subtokens;
        }

        @Override
        public Token[] tokens() {
            return subtokens;
        }
    }
}

package org.gridkit.nanoparser;

import java.util.regex.Matcher;

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
    
    public Token matchToken(Matcher matcher) {
        matcher.reset(text);
        matcher.region(offset, text.length());
        if (matcher.lookingAt()) {
            PToken t = new PToken();
            t.text = text;
            t.body = matcher.group(0);
            t.offset = offset;
            t.line = line;
            t.pos = pos;
            offset += t.body.length();
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
    
    public Token emptyToken() {
        PToken t = new PToken();
        t.text = text;
        t.body = "";
        t.offset = offset;
        t.line = line;
        t.pos = pos;
        return t;
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
}

package org.gridkit.nanoparser.regex;

import java.util.Arrays;

import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.ReflectionActionSource;
import org.gridkit.nanoparser.SemanticExpection;
import org.gridkit.nanoparser.Token;

public class RegExParser extends ReflectionActionSource<Void> {
    
    public static final SyntaticScope CHARSET = NanoGrammar.newParseTable()
            .glueOp("UNION").rank(2)            
            .term("PCLASS", "~(\\\\d|\\\\D|\\\\s|\\\\S|\\\\w|\\\\W)")
            .term("PCLASS", "~\\\\(p|P)\\{\\w+\\}")
            .term("ESCAPE", "~\\\\(0\\d\\d?\\d?|x\\p{XDigit}+|u\\p{XDigit}\\p{XDigit}\\p{XDigit}\\p{XDigit}|t|n|r|f|a|e|c.)")
            .term("ESCAPE", "~\\\\.")
            .enclosure("INVERTED", "[^", "]")
            .enclosure("UNION", "[", "]")
            .infixOp("-").rank(4)
            .infixOp("&&").rank(3)
            .term("CHAR", "~.")
            .toScope();
            

    public static final SyntaticScope PATTERN = NanoGrammar.newParseTable()
            .glueOp("SEQ").rank(3)
            .term("PCLASS", "~(\\\\d|\\\\D|\\\\s|\\\\S|\\\\w|\\\\W)")
            .term("PCLASS", "~\\\\(p|P)\\{\\w+\\}")
            .term("ESCAPE", "~\\\\(0\\d\\d?\\d?|x\\p{XDigit}+|u\\p{XDigit}\\p{XDigit}\\p{XDigit}\\p{XDigit}|t|n|r|f|a|e)")
            .term("BACKREF", "\\\\d+")
            .term("BACKREF", "\\k<\\w+>")
            // TODO \Q escape notation \E
            .term("ESCAPE", "~\\\\.")
            .enclosure("INVERTED", "[^", "]").scope(CHARSET)
            .enclosure("UNION", "[", "]").scope(CHARSET)
            .term("BCONTROL", "~(\\^|\\$|\\\\b|\\\\B|\\\\A|\\\\G|\\\\Z|\\\\z)")
            .enclosure("GROUP", "~\\(\\?\\<\\w+\\>", ")")
            .enclosure("GROUP", "(?:", ")")
            .enclosure("GROUP", "(?=", ")")
            .enclosure("GROUP", "(?!", ")")
            .enclosure("GROUP", "(?<=", ")")
            .enclosure("GROUP", "(?<!", ")")
            .enclosure("GROUP", "(?>", ")")
            .enclosure("GROUP", "(", ")") // should be last
            .infixOp("|").rank(1)
            // Reluctant quantifiers
            .postfixOp("QUANTIFIER", "??").rank(5)
            .postfixOp("QUANTIFIER", "*?").rank(5)
            .postfixOp("QUANTIFIER", "+?").rank(5)
            .postfixOp("QUANTIFIER", "~\\{[0-9, ]+\\}\\?").rank(5)
            // Possessive quantifiers
            .postfixOp("QUANTIFIER", "?+").rank(5)
            .postfixOp("QUANTIFIER", "*+").rank(5)
            .postfixOp("QUANTIFIER", "++").rank(5)
            .postfixOp("QUANTIFIER", "~\\{[0-9, ]+\\}\\+").rank(5)
            // Greedy quantifiers
            .postfixOp("QUANTIFIER", "?").rank(5)
            .postfixOp("QUANTIFIER", "*").rank(5)
            .postfixOp("QUANTIFIER", "+").rank(5)
            .postfixOp("QUANTIFIER", "~\\{[0-9, ]+\\}").rank(5)
            // Should be last
            .term("CHAR", "~.")
            .toScope();
            
    
    // Char set operations
    
    @Term("CHAR")
    public CharCode charCode(String ch) {
        return new CharCode(ch.charAt(0));
    }

    @Term("ESCAPE")
    public CharEscape escape(String tkn) {
        return new CharEscape(tkn);
    }

    @Term("PCLASS")
    public PseudoClass pclass(String tkn) {
        return new PseudoClass(tkn);
    }

    @Convertion
    public CharSet[] list(CharSet a) {
        return new CharSet[]{a};
    }

    @Binary("UNION")
    public CharSet union(CharSet a, CharSet b) {
    	if (a instanceof CharSetUnion) {
    		return union((CharSetUnion)a, b);
    	}
    	else {
    		return new CharSetUnion(a, b);
    	}
    }

    private CharSet union(CharSetUnion a, CharSet b) {
        CharSet[] r = Arrays.copyOf(a.elements, a.elements.length + 1);
        r[r.length - 1] = b;
        return new CharSetUnion(r);
    }
    
    @Unary("UNION")
    public CharSet union(CharSet a) {
    	return a instanceof CharSetUnion ? a : new CharSetUnion(a);
    }

    @Unary("INVERTED")
    public CharSet invert(CharSet a) {
        return new CharSetInvertion(a);
    }

    @Binary("-")
    public CharSet charRange(SingleChar a, SingleChar b) {
        return new CharRange(a, b);
    }

    @Binary("&&")
    public CharSet intersect(CharSet a, CharSet b) {
        return new CharSetIntersection(a, b);
    }
    
    // Pattern operations

    @Convertion
    public PatternElement charMatch(CharSet cs) {
        return new MatchChar(cs);
    }
    
    @Term("CHAR")
    public PatternElement charMatch(String tkn) {
    	if (".".equals(tkn)) {
    		return new DotMatch();
    	}
    	else {
    		return new MatchChar(new CharCode(tkn.charAt(0)));
    	}
    }
    
    @Binary("SEQ")
    public PatternElement seq(@Convertible PatternElement a, @Convertible PatternElement b) {
        return new PatternSeq(a, b);
    }
    
    @Term("BACKREF")
    public PatternElement backref(String tkn) {
        return new Backref(tkn);
    }
    
    @Term("BCONTROL")
    public BoundaryMatch boundaryMatch(String tkn) {
        return new BoundaryMatch(tkn);
    }

    @Unary("GROUP")
    public Group group(@Source Token tkn, @Convertible PatternElement e) {
        String spec = tkn.tokenBody().substring(1);
        return new Group(e, spec);
    }
    
    @Binary("|")
    public Eigther eigther(@Convertible PatternElement a, @Convertible PatternElement b) {
        return new Eigther(a, b);
    }
    
    @Unary("QUANTIFIER")
    public Quantifier quantifier(@Source Token tkn, @Convertible PatternElement a) {
    	if (a instanceof Quantifier) {
    		throw new SemanticExpection("Second quantifier is not allowed");
    	}
        return new Quantifier(a, tkn.tokenBody());
    }
    
    @Unary(NanoGrammar.ACTION_EVAL)
    public PatternElement eval(@Convertible PatternElement a) {
        return a;
    }
    
    public static abstract class CharSet {
        
    }

    public static class CharSetUnion extends CharSet{
        
        public final CharSet[] elements;

        public CharSetUnion(CharSet... elements) {
            this.elements = elements;
        }
        
        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
        	sb.append("[");
        	for(CharSet cs: elements) {
        		sb.append(cs);
        	}
        	sb.append("]");
        	return sb.toString();
        }        
    }
    
    public static class CharSetIntersection extends CharSet {
        
        public final CharSet[] elements;

        public CharSetIntersection(CharSet... elements) {
            this.elements = elements;
        }

        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
        	for(CharSet cs: elements) {
        		sb.append(cs);
        		sb.append("&&");
        	}
        	sb.setLength(sb.length() - 2);
        	return sb.toString();
        }
    }

    public static class CharSetInvertion extends CharSet {
        
        public final CharSet[] elements;

        public CharSetInvertion(CharSet... elements) {
            this.elements = elements;
        }
        
        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
        	sb.append("[^");
        	for(CharSet cs: elements) {
        		sb.append(cs);
        	}
        	sb.append("]");
        	return sb.toString();
        }
    }
    
    public static abstract class SingleChar extends CharSet {
    	
    }
    
    public static class CharCode extends SingleChar {
        
        public final char code;

        public CharCode(char c) {
            this.code = c;
        }
        
        @Override
        public String toString() {
        	return String.valueOf(code);
        }
    }

    public static class CharEscape extends SingleChar {
        
        public final String escapeSpec;

        public CharEscape(String escapeSpec) {
            this.escapeSpec = escapeSpec;
        }
        
        @Override
        public String toString() {
        	return escapeSpec;
        }
    }
    
    public static class CharRange extends CharSet {
        
        public final SingleChar from;
        public final SingleChar to;

        public CharRange(SingleChar from, SingleChar to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public String toString() {
        	return from + "-" + to;
        }
    }

    public static class PseudoClass extends CharSet {
        
        public final String pclass;

        public PseudoClass(String pclass) {
            this.pclass = pclass;
        }
        
        @Override
        public String toString() {
        	return pclass;
        }
    }
    
    public static abstract class PatternElement {
        
    }
    
    public static class PatternSeq extends PatternElement {
        
        public final PatternElement[] patterns;

        public PatternSeq(PatternElement... patterns) {
            this.patterns = patterns;
        }
        
        @Override
        public String toString() {
        	StringBuilder sb = new StringBuilder();
        	for(PatternElement e: patterns) {
        		sb.append(e);
        	}
        	return sb.toString();
        }
    }
    
    public static class DotMatch extends PatternElement {
    	
    	@Override
    	public String toString() {
    		return ".";
    	}
    }
    
    public static class MatchChar extends PatternElement {
        
        public final CharSet charset;

        public MatchChar(CharSet charset) {
            this.charset = charset;
        }
        
        @Override
        public String toString() {
        	return charset.toString();
        }
    }

    public static class BoundaryMatch extends PatternElement {
        
        public final String boundarySpec;

        public BoundaryMatch(String boundarySpec) {
            super();
            this.boundarySpec = boundarySpec;
        }
        
        @Override
        public String toString() {
        	return boundarySpec;
        }
    }
    
    public static class Eigther extends PatternElement {
        
        public final PatternElement a;
        public final PatternElement b;

        public Eigther(PatternElement a, PatternElement b) {
            this.a = a;
            this.b = b;
        }
        
        @Override
        public String toString() {
        	return a + "|" + b;
        }
    }

    public static class Group extends PatternElement {
        
        public final PatternElement pattern;
        public final String gspec;

        public Group(PatternElement pattern, String gspec) {
            this.pattern = pattern;
            this.gspec = gspec;
        }
        
        @Override
        public String toString() {
        	return "(" + gspec + pattern + ")";
        }
    }
    
    public static class Backref extends PatternElement {
        
        public final String refSpec;

        public Backref(String refSpec) {
            this.refSpec = refSpec;
        }
        
        @Override
        public String toString() {
        	return refSpec;
        }
    }
    
    public static class Quantifier extends PatternElement {

        public final PatternElement pattern;
        public final String qspec;
        
        public Quantifier(PatternElement pattern, String qspec) {
            this.pattern = pattern;
            this.qspec = qspec;
        }
        
        @Override
        public String toString() {
        	return pattern + qspec;
        }
    }
}
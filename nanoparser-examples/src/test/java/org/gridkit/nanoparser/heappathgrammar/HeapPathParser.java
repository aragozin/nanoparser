package org.gridkit.nanoparser.heappathgrammar;

import java.util.Arrays;

import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.NanoParser;
import org.gridkit.nanoparser.ReflectionActionSource;

public class HeapPathParser extends ReflectionActionSource<Void> {

    public static final SyntaticScope QUOTED_STRING = NanoGrammar.newParseTable()
            .term("~[^\\\\\'\"]+")
            .term("ESCAPE", "~\\\\x[a-fA-F0-9][a-fA-F0-9]")
            .term("ESCAPE", "~\\\\.")
            .glueOp("CONCAT")
            .toScope();
            
    public static final SyntaticScope TYPE_MATCH = NanoGrammar.newParseTable()
            // leading tilde (~) in token use for RegEx
            .skip("~\\s") // ignore white spaces
            .term("~[a-zA-Z_][a-zA-Z0-9_]*") // Java name (only ASCII charset
            .term("**") // any name match 
            .term("*") // non-dot match
            .prefixOp("+").rank(4) // subclass matcher
            .infixOp(".").rank(3) 
            .infixOp("|").rank(2) // conjunction
            .toScope();

    public static final SyntaticScope BRACKET_GRAMMAR = NanoGrammar.newParseTable().toLazyScope();            
    
    public static final SyntaticScope HEAPPATH_GRAMMAR = NanoGrammar.newParseTable()
            // leading tilde (~) in token use for RegEx
            .skip("~\\s") // ignore white spaces
            .enclosure("STRING", "\"", "\"").scope(QUOTED_STRING)
            .term("NAME", "~[a-zA-Z_][a-zA-Z0-9_]*") // Java name (only ASCII charset
            .term("**", "**") // wild card
            .term("*", "*") // wild card
            .enclosure("TYPEMATCH", "(", ")")
                .implicitPrefixOp(".", true) // implicit dot
                .implicitOpRank(4) // should match normal dot operator
                .scope(TYPE_MATCH)
            
            .enclosure("[]", "[", "]")
            .implicitPrefixOp(".", true) // implicit dot
                .implicitOpRank(4) // should match normal dot operator
                .scope(BRACKET_GRAMMAR)
                            
            .infixOp(".").rank(4) // dot operator
            .postfixOp("?entrySet").rank(4) // map iteration

            .toLazyScope();

    public static final SyntaticScope HEAPPATH_MULTI = NanoGrammar.newParseTable()
            .include(HEAPPATH_GRAMMAR)
            .separator(";")            
            .toScope();
    
    static { 
        NanoGrammar.extendTable(BRACKET_GRAMMAR)
            .include(HEAPPATH_GRAMMAR)
            .enclosure("STRING", "\"", "\"").scope(QUOTED_STRING)
            .term("NUM", "~\\d+") // simple decimal token, integer only
            .infixOp("=").rank(3)
            .infixOp("&&").rank(2) // disjunction
            .infixOp("||").rank(1) // conjunction
            .toScope();
        ;
    }

    private NanoParser<Void> parser = new NanoParser<Void>(HEAPPATH_MULTI, this) {
        
        // tracing
        int deepth;

        @Override
        protected Error mapTermAction(Class<?> type, ParseNode node, int bestParsed) {
            trace("mapTermAction: " + type.getSimpleName() + " | " + node.toString());
            ++deepth;
            Error error = super.mapTermAction(type, node, bestParsed);
            --deepth;
            trace(error == null ? "-> OK" : "-> ERROR: " + error);
            return error;
        }

        @Override
        protected Error mapUnaryAction(Class<?> type, ParseNode node, int bestParsed) {
            trace("mapUnaryAction: " + type.getSimpleName() + " | " + node.toString());
            ++deepth;
            Error error = super.mapUnaryAction(type, node, bestParsed);
            --deepth;
            trace(error == null ? "-> OK" : "-> ERROR: " + error);
            return error;
        }

        @Override
        protected Error mapBinaryAction(Class<?> type, ParseNode node, int bestParsed) {
            trace("mapBinaryAction: " + type.getSimpleName() + " | " + node.toString());
            ++deepth;
            Error error = super.mapBinaryAction(type, node, bestParsed);
            --deepth;
            trace(error == null ? "-> OK" : "-> ERROR: " + error);
            return error;
        }
        
        protected void trace(String text) {
            if (trace) {
                indent();
                System.out.println(text);
            }
        }

        protected void indent() {
            if (trace) {
                for(int i = 0; i != deepth; ++i) {
                    System.out.print("  ");
                }
            }
        }        
    };
    
    private boolean trace = true;
    
    public HeapPathStep[] parsePath(String expression) {        
        return parser.parse(null, HeapPathStep[].class, expression);        
    }
    
    @Binary("CONCAT")
    public String concat(String a, String b) {
    	return a + b;
    }
    
    @Term("NAME")
    public HeapPathStep fieldStep(String name) {
        return new FieldStep(name);
    }

    @Term("*")
    public HeapPathStep anyFieldStep(String name) {
        return new FieldStep("*");
    }

    @Term("**")
    public HeapPathStep anyPathStep(String name) {
        return new AnyPathStep();
    }

    @Unary(NanoGrammar.ACTION_EVAL)
    public HeapPathStep[] rootConversion(@Convertible HeapPathStep[] step) {
        return step;
    }
    
    @Binary(".")
    public HeapPathStep[] dotStep(@Convertible HeapPathStep[] a, HeapPathStep b) {
        HeapPathStep[] r = Arrays.copyOf(a, a.length + 1);
        r[r.length - 1] = b;
        return r;
    }

    @Unary("?entrySet")
    public HeapPathStep[] entrySetStep(@Convertible HeapPathStep[] a) {
        HeapPathStep[] r = Arrays.copyOf(a, a.length + 1);
        r[r.length - 1] = new MapEntryStep();
        return r;
    }
    
    @Term("*")
    public BracketIndex anyIndex(String name) {
        return new BracketIndex(-1);
    }

    @Term("NUM")
    public BracketIndex numericIndex(String name) {
        return new BracketIndex(Integer.valueOf(name));
    }
    
    @Unary("[]")
    public HeapPathStep indexOp(BracketIndex index) {
        return new IndexStep(index.index);
    }

    @Unary("[]")
    public HeapPathStep predicateOp(Predicate predicate) {
        return new PredicateStep(predicate);
    }
    
    @Binary("=")
    public Predicate eqPredicate(@Convertible HeapPathStep[] path, String value) {
        return new EqPredicate(path, value);
    }

    @Binary("=")
    public Predicate eqPredicate(@Convertible HeapPathStep[] path, Number value) {
        return new EqPredicate(path, value);
    }

    @Binary("&&")
    public Predicate andPredicate(@Convertible Predicate a, @Convertible Predicate b) {
        return new AndPredicate(a, b);
    }

    @Binary("||")
    public Predicate orPredicate(@Convertible Predicate a, @Convertible Predicate b) {
        return new OrPredicate(a, b);
    }
    
    @Convertion
    public Predicate existsPredicate(HeapPathStep path) {
        return new ExistsPredicate(new HeapPathStep[]{path});
    }

    @Convertion
    public Predicate existsPredicate(HeapPathStep[] path) {
        return new ExistsPredicate(path);
    }
    
    @Term("NUM")
    public Number num(String param) {
        if (param.indexOf('.') >= 0) {
            return Double.valueOf(param);
        }
        else {
            return Long.valueOf(param);
        }
    }

    @Unary("STRING")
    public String string(String param) {
        return param;
    }

    @Term("ESCAPE")
    public String escapeChar(String escape) {
        if (escape.startsWith("\\x")) {
            int ch = Integer.parseInt(escape.substring(2), 16);
            return String.valueOf((char)ch);
        }
        else {
            return escape.substring(1);
        }
    }
    
    @Term("NAME")
    public String namePattern(String param) {
        return param;
    }

    @Convertion()
    public ClassMatch asteriskPattern(String pattern) {
        return new ClassMatch(pattern, false);
    }

    @Binary(".")
    public ClassMatch classMath(@Convertible ClassMatch a, String b) {
        return new ClassMatch(a.getPattern() + "." + b, false);
    }

    @Binary("|")
    public TypeMatch typeConjunction(@Convertible  TypeMatch a, @Convertible TypeMatch b) {
        if (a instanceof TypeConjunction) {
            TypeMatch[] elements = ((TypeConjunction) a).getElements();
            elements = Arrays.copyOf(elements, elements.length + 1);
            elements[elements.length - 1] = b;
            return new TypeConjunction(elements);
        }
        else {
            return new TypeConjunction(new TypeMatch[]{a, b});
        }
    }

    @Unary("+")
    public ClassMatch matchSubclasses(@Convertible ClassMatch a) {
        return new ClassMatch(a.getPattern(), true);
    }

    @Unary("TYPEMATCH")
    public HeapPathStep typeMatch2step(@Convertible TypeMatch match) {
        return new TypeMatchStep(match);
    }

    public static class BracketIndex {
        public final int index;
        public BracketIndex(int index) {
            this.index = index;
        }
    }
}

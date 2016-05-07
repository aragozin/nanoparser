package org.gridkit.nanoparser;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.junit.Test;

public class NanoParserTest {

    @Test
    public void test_basic_op_rank() {
        
        SyntaticScope scope = NanoGrammar.buildTable()
                .term("[A-Z]")
                .skip("\\s")
                .infixOp("+", "\\+")
                .infixOp("*", "\\*").rank(2).toScope();
        
        NanoParser<Void> parser = new NanoParser<Void>(new SimpleParser(), scope);
        
        assertParseResult(parser, "A+B", "[A+B]");
        assertParseResult(parser, "A+B+C", "[[A+B]+C]");
        assertParseResult(parser, "A+B*C", "[A+[B*C]]");
        assertParseResult(parser, "A + B * C", "[A+[B*C]]");
        assertParseResult(parser, "A+B*C+D", "[[A+[B*C]]+D]");
    }

    @Test
    public void test_parents() {
        
        SyntaticScope scope = NanoGrammar.buildTable()
                .term("[A-Z]")
                .skip("\\s")
                .infixOp("+", "\\+")
                .infixOp("*", "\\*").rank(2)
                .enclosure("()", "\\(", "\\)")
                .toScope();
        
        NanoParser<Void> parser = new NanoParser<Void>(new SimpleParser(), scope);
        
        assertParseResult(parser, "A+B", "[A+B]");
        assertParseResult(parser, "A+B+C", "[[A+B]+C]");
        assertParseResult(parser, "A+B*C", "[A+[B*C]]");
        assertParseResult(parser, "(A+B)*C", "[[A+B]*C]");
        assertParseResult(parser, "(A+B)*(C+D)+X", "[[[A+B]*[C+D]]+X]");
        assertParseResult(parser, "(A+B)*((C+D)+X)", "[[A+B]*[[C+D]+X]]");
    }

    @Test
    public void test_quited_sting() {
        
        SyntaticScope quoted = NanoGrammar.buildTable()
                .term("[^\\\\']+")
                .term("ESCAPE", "\\\\.?")
                .glueOp("CONCAT")
                .toScope();
        
        SyntaticScope scope = NanoGrammar.buildTable()
                .term("[A-Z]")
                .skip("\\s")
                .infixOp("+", "\\+")
                .infixOp("*", "\\*").rank(2)
                .enclosure("()", "\\(", "\\)")
                .enclosure("", "\\'", "\\'").scope(quoted)
                .toScope();
        
        NanoParser<Void> parser = new NanoParser<Void>(new SimpleParser(), scope);
        
        assertParseResult(parser, "A+'BCD'", "[A+BCD]");
        assertParseResult(parser, "A + ' B C D '", "[A+ B C D ]");
        assertParseResult(parser, "A + 'B\\'CD'", "[A+B'CD]");
        assertParseResult(parser, "(A + 'B\\'CD') * X", "[[A+B'CD]*X]");
        assertParseResult(parser, "'B\\'CD\\\\' + X", "[B'CD\\+X]");
    }

    @Test
    public void test_function_call() {
        
        SyntaticScope quoted = NanoGrammar.buildTable()
                .term("[^\\\\']+")
                .term("ESCAPE", "\\\\.?")
                .glueOp("CONCAT")
                .toScope();
        
        SyntaticScope scope = NanoGrammar.buildTable()
                .term("[A-Z]")
                .skip("\\s")
                .infixOp("+", "\\+")
                .infixOp("*", "\\*").rank(2)
                .enclosure("()", "\\(", "\\)")
                .enclosure("\\'", "\\'").scope(quoted)
                .toLazyScope();

        SyntaticScope functionArgs = NanoGrammar.buildTable()
                .clone(scope)
                .infixOp("COMMA", ",").rank(0) // reduced rank
                .toScope();
        
        NanoGrammar.extendTable(scope)
                .enclosure("CALL", "[A-Za-z]+\\(", "\\)").scope(functionArgs);
        
        NanoParser<Void> parser = new NanoParser<Void>(new SimpleParser(), scope);
        
        assertParseResult(parser, "A+x(C,D,E)", "[A+x[C, D, E]]");
        assertParseResult(parser, "x(A)+B", "[x[A]+B]");
        assertParseResult(parser, "bcd('1 2 3',D + E, F*G) + X", "[bcd[1 2 3, [D+E], [F*G]]+X]");
        assertParseResult(parser, "bcd(xyz('1', sdf(X)))", "bcd[xyz[1, sdf[X]]]");
    }

    private void assertParseResult(NanoParser<Void> parser, String source, String results) {
        Assertions.assertThat(parser.parse(null, String.class, source)).isEqualTo(results);
    }

    public static class SimpleParser extends ReflectionActionHandler<Void> {
        
        @Binary("+")
        public String strPlus(String op, String left, String right) {
            return "[" +  left + "+" + right + "]";
        }

        @Binary("*")
        public String strMult(String op, String left, String right) {
            return "[" +  left + "*" + right + "]";
        }

        @Unary("()")
        public String asIs(String op, String param) {
            return param;
        }

        @Unary("ESCAPE")
        public String escape(String op, String param) {
            return op.substring(1);
        }

        @Unary("QUOTE")
        public String quote(String op, String param) {
            return param;
        }

        @Binary("CONCAT")
        public String concat(String op, String a, String b) {
            return a + b;
        }

        @Binary("COMMA")
        public String[] join(String opName, String[] head, String tail) {
            String[] nhead = Arrays.copyOf(head, head.length + 1);
            nhead[head.length] = tail;
            return nhead;
        }
        
        @Convertion
        public String[] wrap(String val) {
            return new String[]{val};
        }

        @Unary("CALL")
        public String call(String op, String[] args) {
            return op.substring(0, op.length() - 1) + Arrays.toString(args);            
        }
    }
 }

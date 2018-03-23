package org.gridkit.nanoparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SimpleInterpolationParserTest {

    private final NanoParser<Map<String, String>> PARSER = new NanoParser<Map<String,String>>(InterpolationHandler.SYNTAX, new InterpolationHandler());

    @SuppressWarnings("serial")
    private final Map<String, String> PROPS = new HashMap<String, String>() {{

        put("abc", "123");
        put("prop.abc", "ABC");
        put("prop.bcd", "BCD");

    }};


    @Parameters(name = "{0} -> {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();

        addCase(cases, "literal value", null)
            .result("literal value");

        addCase(cases, "$prop.abc", null)
            .result("ABC");

        addCase(cases, "$abc", null)
            .result("123");

        addCase(cases, "${prop.abc}", null)
            .result("ABC");

        addCase(cases, "${abc}", null)
            .result("123");

        addCase(cases, "${prop.abc}X", null)
            .result("ABCX");

        addCase(cases, "X$prop.abc", null)
            .result("XABC");

        addCase(cases, "X${prop.abc}", null)
            .result("XABC");

        addCase(cases, "${prop.abc}${prop.bcd}", null)
            .result("ABCBCD");

        addCase(cases, "This is $${prop.abc} '${prop.abc}'", null)
            .result("This is $${prop.abc} 'ABC'");

        addCase(cases, "Missing prop ${unknown}", "Unknown property 'unknown'")
            .sourceRef("Missing prop ${unknown}",
                       "             ^");

        addCase(cases, "Invalid syntax $ prop.name", "Required one of ['Expr', 'Id'] but found 'String'")
            .sourceRef("Invalid syntax $ prop.name",
                       "                ^");

        return cases;
    }

    private static CaseBuilder addCase(List<Object[]> cases, String expression, String error) {
        final Object[] c = new Object[]{expression, error, null, null};
        cases.add(c);
        return new CaseBuilder() {
            @Override
            public void sourceRef(String line1, String line2) {
                c[2] = line1 + "\n" + line2 + "\n";
            }

            @Override
            public void result(String result) {
                c[3] = result;
            }
        };
    }

    public interface CaseBuilder {

        public void sourceRef(String line1, String line2);

        public void result(String result);
    }

    String expression;
    String errorMessage;
    String sourceReference;
    String exepectedValue;

    public SimpleInterpolationParserTest(String expression, String errorMessage, String sourceReference, String exepectedValue) {
        this.expression = expression.replace("\\n", "\n");
        this.errorMessage = errorMessage;
        this.sourceReference = sourceReference;
        this.exepectedValue = exepectedValue;
    }

    @Test
    public void verify() {
        NanoParser<Map<String, String>> parser = PARSER;

        try {
            String val = parser.parse(PROPS, String.class, expression);
            if (exepectedValue != null) {
                Assertions.assertThat(val).isEqualTo(exepectedValue);
            } else {
                Assert.fail("ParseException is expected");
            }
        }
        catch(ParserException e) {
            try {
                Assert.assertEquals(errorMessage, e.getMessage());
                if (sourceReference != null) {
                    Assert.assertEquals(sourceReference, e.getToken().excerpt());
                }
            }
            catch(AssertionError ae) {
                System.err.println(e.formatVerboseErrorMessage());
                throw ae;
            }
        }
    }

    private static class InterpolationHandler extends ReflectionActionSource<Map<String, String>> {

        static final SyntaticScope SYNTAX = NanoGrammar.newParseTable()
                .token("$$")
                .infixOrPrefixOp("$").rank(2)
                .term("EXPR", "~\\{.+?\\}")
                .term("ID", "~[_A-Za-z0-9.]+")
                .term("~[^${_.\\w]+?") // match longer string as long as no ambiguous characters
                .term("~.") // match any not matched character one by one
                .glueOp("glue").rank(1)
                .toScope();

        @Term("$$")
        public String doubleDollar() {
            return "$$";
        }

        @Unary("$")
        public String interpolate(@Context Map<String, String> conf, @Source Token tkn, Id id) {
            String key = id.toString();
            return readKey(tkn, conf, key);
        }

        @Binary("$")
        public String interpolate(@Context Map<String, String> conf, @Source Token tkn, String pref, Id id) {
            String key = id.toString();
            return pref + readKey(tkn, conf, key);
        }

        @Unary("$")
        public String interpolate(@Context Map<String, String> conf, @Source Token tkn, Expr expr) {
            String key = expr.toString();
            key = key.substring(1, key.length() - 1);
            return readKey(tkn, conf, key);
        }

        @Binary("$")
        public String interpolate(@Context Map<String, String> conf, @Source Token tkn, String pref, Expr expr) {
            String key = expr.toString();
            key = key.substring(1, key.length() - 1);
            return pref + readKey(tkn, conf, key);
        }

        private String readKey(Token token, Map<String, String> conf, String key) {
            String val = conf.get(key);
            if (val == null) {
                throw new ParserException(token, "Unknown property '" + key + "'");
            }
            return val;
        }

        @Term("EXPR")
        public Expr expr(String text) {
            return new Expr(text);
        }

        @Term("EXPR")
        public String exprAsText(String text) {
            return text;
        }

        @Term("ID")
        public Id id(String text) {
            return new Id(text);
        }

        @Term("ID")
        public String idAsString(String text) {
            return text;
        }

        @Binary("glue")
        public String glue(String a, String b) {
            return a + b;
        }
    }

    private static class Id {

        final String id;

        public Id(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static class Expr {

        final String expr;

        public Expr(String expr) {
            this.expr = expr;
        }

        @Override
        public String toString() {
            return expr;
        }
    }
}

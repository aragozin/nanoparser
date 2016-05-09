package org.gridkit.nanoparser;

import java.util.ArrayList;
import java.util.List;

import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NanoParserErrorTest extends ReflectionActionHandler<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
            .skip("\\s") // ignore white spaces
            .term("DECIMAL", "\\d+") // simple decimal token
            .infixOp("+", "\\+")
            .infixOrPrefixOp("-", "\\-")
            .infixOp("*", "\\*").rank(2)
            .enclosure("\\(", "\\)")
            .toScope();
    
    @Unary("DECIMAL")
    public Integer toInt(String opbody, String param) {
        return Integer.valueOf(param);
    }
    
    @Binary("+")
    public Integer plus(String opbody, Integer a, Integer b) {
        return a + b;
    }

    @Unary("-")
    public Integer minus(String opbody, Integer a) {
        return -a;
    }

    @Binary("-")
    public Integer minus(String opbody, Integer a, Integer b) {
        return a - b;
    }

    @Binary("*")
    public Integer mult(String opbody, Integer a, Integer b) {
        return a * b;
    }

    @Parameters(name = "{0} -> {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();
        addCase(cases, "+2", "Missing left argument for '+'")
        .sourceRef("+2", 
                   "^");
        addCase(cases, "3 * (+2 - 1)", "Missing left argument for '+'")
        .sourceRef("3 * (+2 - 1)", 
                   "     ^");

        return cases;
    }
    
    private static CaseBuilder addCase(List<Object[]> cases, String expression, String error) {
        final Object[] c = new Object[]{expression, error, null};
        cases.add(c);        
        return new CaseBuilder() {
            @Override
            public void sourceRef(String line1, String line2) {
                c[2] = line1 + "\n" + line2 + "\n";
            }
        };
    }
    
    public interface CaseBuilder {
        
        public void sourceRef(String line1, String line2);
    }

    String expression;
    String errorMessage;
    String sourceReference;

    public NanoParserErrorTest(String expression, String errorMessage, String sourceReference) {
        this.expression = expression;
        this.errorMessage = errorMessage;
        this.sourceReference = sourceReference;
    }

    @Test
    public void verify() {
        NanoParser<Void> parser = new NanoParser<Void>(this, SIMPLE_GRAMMAR);
        
        try {
            parser.parse(null, Integer.class, expression);
            Assert.fail("ParseException is expected");
        }
        catch(ParserException e) {
            Assert.assertEquals(errorMessage, e.getMessage());
            if (sourceReference != null) {
                Assert.assertEquals(sourceReference, e.formatSourceReference());
            }
        }
    }
}

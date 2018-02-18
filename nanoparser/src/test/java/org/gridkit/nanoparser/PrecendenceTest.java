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
public class PrecendenceTest extends ReflectionActionSource<Void> {


    public static final SyntaticScope TEST_GRAMMAR = NanoGrammar.newParseTable()
            .skip("~\\s") // ignore white spaces
            .term("~\\d+") // simple decimal token
            .infixOp("OP", "I1").rank(1)
            .infixOp("OP", "I2").rank(2)
            .infixOp("OP", "I3").rank(3)
            .infixOp("OP", "I4").rank(4)
            .infixOp("OP", "I5").rank(5)

            .infixOrPrefixOp("OP", "i1").rank(1)
            .infixOrPrefixOp("OP", "i2").rank(2)
            .infixOrPrefixOp("OP", "i3").rank(3)
            .infixOrPrefixOp("OP", "i4").rank(4)
            .infixOrPrefixOp("OP", "i5").rank(5)

            .prefixOp("OP", "p1").rank(1)
            .prefixOp("OP", "p2").rank(2)
            .prefixOp("OP", "p3").rank(3)
            .prefixOp("OP", "p4").rank(4)
            .prefixOp("OP", "p5").rank(5)

            .postfixOp("OP", "P1").rank(1)
            .postfixOp("OP", "P2").rank(2)
            .postfixOp("OP", "P3").rank(3)
            .postfixOp("OP", "P4").rank(4)
            .postfixOp("OP", "P5").rank(5)

            .enclosure("()", "(", ")")
            .toScope();

    @Unary("()")
    public String par(String arg) {
        return "(" + arg + ")";
    }

    @Unary("OP")
    public String par(@Source Token tkn, String arg) {
        return "(" + tkn + " " + arg + ")";
    }

    @Binary("OP")
    public String par(@Source Token tkn, String a, String b) {
        return "(" + a + " " + tkn + " " + b + ")";
    }

    @Parameters(name = "{0} == {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();
        addCase(cases, "1", "1");
        addCase(cases, "1 I2 2", "(1 I2 2)");
        addCase(cases, "1 I2 2 I3 3", "(1 I2 (2 I3 3))");
        addCase(cases, "1 I3 2 I2 3", "((1 I3 2) I2 3)");
        addCase(cases, "i1 2 I2 3", "(i1 (2 I2 3))");
        addCase(cases, "i2 2 I1 3", "((i2 2) I1 3)");
        addCase(cases, "p1 2 I2 3", "(p1 (2 I2 3))");
        addCase(cases, "p2 2 I1 3", "((p2 2) I1 3)");
        addCase(cases, "1 I1 2 P1", "(P1 (1 I1 2))");
        addCase(cases, "1 I1 2 P2", "(1 I1 (P2 2))");
        addCase(cases, "1 P1 P2", "(P2 (P1 1))");
        addCase(cases, "1 P2 P1", "(P1 (P2 1))");
        addCase(cases, "1 P1 I2 2", "((P1 1) I2 2)");
        addCase(cases, "1 I1 p1 2", "(1 I1 (p1 2))");
        addCase(cases, "p1 1 P1", "(P1 (p1 1))");
        addCase(cases, "p2 1 P1", "(P1 (p2 1))");
        addCase(cases, "p1 1 P2", "(p1 (P2 1))");

        return cases;
    }

    private static void addCase(List<Object[]> cases, Object... c) {
        cases.add(c);
    }

    String expression;
    String expectedResult;

    public PrecendenceTest(String expression, String expectedResult) {
        this.expression = expression;
        this.expectedResult = expectedResult;
    }

    @Test
    public void verify() {
        NanoParser<Void> parser = new NanoParser<Void>(TEST_GRAMMAR, this);

        Assert.assertEquals(expectedResult, parser.parse(null, String.class, expression));
    }
}

package org.gridkit.nanoparser.heappathgrammar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gridkit.nanoparser.ParserException;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HeapPathParserTest {

    @Parameters(name = "\"{0}\" -> {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> result = new ArrayList<Object[]>();

        addCase(result, "fieldName", "[.fieldName]");
        addCase(result, "*", "[.*]");
        addCase(result, "**", "[.**]");
        addCase(result, "inputsByName.table[*].value", "[.inputsByName, .table, [*], .value]");
        addCase(result, "inputsByName.table[*].value(**.String)", "[.inputsByName, .table, [*], .value, (**.String)]");
        addCase(result, "inputsByName.table?entrySet.value(**.String)", "[.inputsByName, .table, ?entrySet, .value, (**.String)]");
        addCase(result, "inputsByName.*.table[*].value(**.String)", "[.inputsByName, .*, .table, [*], .value, (**.String)]");
        addCase(result, "inputsByName.**.table[*].value(**.String)", "[.inputsByName, .**, .table, [*], .value, (**.String)]");
        addCase(result, "inputsByName.***.table[*].value(**.String)", null);
        addCase(result, "inputsByName.*.*.table[*].value(**.String)", "[.inputsByName, .*, .*, .table, [*], .value, (**.String)]");
        addCase(result, "inputsByName.**.*.table[*].value(**.String)", "[.inputsByName, .**, .*, .table, [*], .value, (**.String)]");
        addCase(result, "inputsByName.table?entrySet[key=123].value(**.String)", "[.inputsByName, .table, ?entrySet, [.key=123], .value, (**.String)]");
        addCase(result, "inputsByName.table?entrySet[key=abc].value(**.String)", "[.inputsByName, .table, ?entrySet, [.key=abc], .value, (**.String)]");
        addCase(result, "inputsByName.table[*][key=null].value(**.String)", "[.inputsByName, .table, [*], [.key=null], .value, (**.String)]");
        addCase(result, "[key=\"A B C\"]", "[[.key=A B C]]");
        addCase(result, "[key=\"A B C\" && value=X]", "[[(.key=A B C&&.value=X)]]");
        addCase(result, "[key=\"A B C\" && value]", "[[(.key=A B C&&.value)]]");
        addCase(result, "[key=\"A B C\" || value]", "[[(.key=A B C||.value)]]");
        addCase(result, "[key=\"A B C\" && value=1 || value=2]", "[[((.key=A B C&&.value=1)||.value=2)]]");
        addCase(result, "[key=\"A B C\" && value(**.String)]", "[[(.key=A B C&&.value(**.String))]]");
        addCase(result, "(**.String)", "[(**.String)]");
        addCase(result, "(**.String).fieldName", "[(**.String), .fieldName]");
        addCase(result, "[*][0]", "[[*], [0]]");
        addCase(result, "[*][0].fieldName", "[[*], [0], .fieldName]");
        addCase(result, "field[*][0]", "[.field, [*], [0]]");
        addCase(result, "field[*][0].fieldName", "[.field, [*], [0], .fieldName]");
        addCase(result, "*.size", "[.*, .size]");
        addCase(result, "[*](MyObject)", "[[*], (MyObject)]");
        addCase(result, "[*](+MyObject1|+MyObject2)", "[[*], (+MyObject1|+MyObject2)]");

        return result;
    }

    private static void addCase(List<Object[]> list, Object... args) {
        list.add(args);
    }

    HeapPathParser parser = new HeapPathParser();
    
    String expr;
    String expected;

    public HeapPathParserTest(String expr, String expected) {
        this.expr = expr;
        this.expected = expected;
    }

    @Test
    public void testExpr() {

        System.out.println("EXPR: " + expr);

        if (expected != null) {
            try {
                String text;
                text = Arrays.toString(parser.parsePath(expr));
                System.out.println(text);
                assertThat(text).isEqualTo(expected);
            }
            catch(ParserException e) {
                System.out.println(e.formatVerboseErrorMessage());
                throw e;
            }
        }
        else {
            try {
                System.out.println(Arrays.toString(parser.parsePath(expr)));
                Assert.fail("Exception expected");
            }
            catch(ParserException e) {
                System.out.println(e.formatVerboseErrorMessage());
                // expected
            }
        }
    }
}

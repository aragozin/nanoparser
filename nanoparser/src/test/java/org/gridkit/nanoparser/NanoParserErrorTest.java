/*
 * Copyright (C) 2016 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public class NanoParserErrorTest extends ReflectionActionSource<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
            .skip("~\\s") // ignore white spaces
            .term("DECIMAL", "~\\d+") // simple decimal token
            .infixOp("+", "+")
            .infixOp("|", "|")
            .infixOp("?", "?")
            .infixOrPrefixOp("-", "-")
            .infixOp("*", "*").rank(2)
            .enclosure("(", ")")
            .enclosure("strLen", "strLen(", ")")
            .enclosure("boom", "boom(", ")")
            .term("ALPHA", "a")       // special token
            .term("BETA", "b")        // special token
            .toScope();

    @Term("DECIMAL")
    public Integer toInt(String param) {
        return Integer.valueOf(param);
    }

    @Term("BETA")
    public String beta(String param) {
        return "b";
    }

    @Binary("+")
    public Integer plus(Integer a, Integer b) {
        return a + b;
    }

    @Unary("-")
    public Integer minus(Integer a) {
        return -a;
    }

    @Binary("-")
    public Integer minus(Integer a, Integer b) {
        return a - b;
    }

    @Binary("*")
    public Integer mult(Integer a, Integer b) {
        return a * b;
    }

    @Binary("|")
    public String concat(String a, String b) {
        return a + b;
    }

    @Unary("strLen")
    public Integer strLen(String a) {
        return a.length();
    }

    @Unary("boom")
    public Integer boom(String a) {
        throw new SemanticExpection("Boom");
    }

    @Parameters(name = "{0} -> {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();

        addCase(cases, "+2", "Missing left hand side '+'")
            .sourceRef("+2",
                       "^");

        addCase(cases, "3 * (+2 - 1)", "Missing left hand side '+'")
            .sourceRef("3 * (+2 - 1)",
                       "     ^");

        addCase(cases, "3 * ()", "Empty expression")
            .sourceRef("3 * ()",
                       "     ^");

        addCase(cases, "3 *", "Missing right hand side '*'")
            .sourceRef("3 *",
                       "  ^");

        addCase(cases, "3 + (2 *)", "Missing right hand side '*'")
            .sourceRef("3 + (2 *)",
                       "       ^");

        addCase(cases, "3 + ABC", "Cannot parse next token")
            .sourceRef("3 + ABC",
                       "    ^");

        addCase(cases, "3 + a", "No action for 'ALPHA' producing 'Integer'")
            .sourceRef("3 + a",
                       "    ^");

        addCase(cases, "a + 3", "No action for 'ALPHA' producing 'Integer'")
            .sourceRef("a + 3",
                       "^");

        addCase(cases, "a + a", "No action for 'ALPHA' producing 'Integer'")
            .sourceRef("a + a",
                       "^");

        addCase(cases, "a ? a", "No action for '?' producing 'Integer'")
            .sourceRef("a ? a",
                       "  ^");

        addCase(cases, "b | b", "Required type 'Integer' but found 'String'")
            .sourceRef("b | b",
                       "  ^");

        addCase(cases, "strLen(1 + 1)", "Required type 'String' but found 'Integer'")
            .sourceRef("strLen(1 + 1)",
                       "         ^");

        addCase(cases, "strLen(1 + 1) + 1", "Required type 'String' but found 'Integer'")
            .sourceRef("strLen(1 + 1) + 1",
                       "         ^");

        addCase(cases, "1 + strLen(1 + 1)", "Required type 'String' but found 'Integer'")
            .sourceRef("1 + strLen(1 + 1)",
                       "             ^");

        addCase(cases, "1 + boom(b | b)", "Boom")
            .sourceRef("1 + boom(b | b)",
                       "    ^");

        addCase(cases, "1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + boom(b + b) + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 2", "No action for '+' producing 'String'")
            .sourceRef("...  + 1 + 1 + 1 + 1 + boom(b + b) + 1 + 1 + 1 + 1 + 1 + ...",
                       "                              ^");

        addCase(cases, "1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + boom(b | b) + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 2", "Boom")
            .sourceRef("... + 1 + 1 + 1 + 1 + 1 + 1 + boom(b | b) + 1 + 1 + 1 + 1...",
                       "                              ^");

        addCase(cases, "strLen(1 + 1", "Syntatic scope is not closed")
            .sourceRef("strLen(1 + 1",
                       "            ^");

        addCase(cases, "(1 + 1", "Syntatic scope is not closed")
            .sourceRef("(1 + 1",
                       "      ^");

        addCase(cases, "(1 + 1\\n", "Syntatic scope is not closed")
            .sourceRef("(1 + 1",
                       "      ^");

        addCase(cases, "1 + 1)", "Cannot parse next token")
            .sourceRef("1 + 1)",
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
        this.expression = expression.replace("\\n", "\n");
        this.errorMessage = errorMessage;
        this.sourceReference = sourceReference;
    }

    @Test
    public void verify() {
        NanoParser<Void> parser = new NanoParser<Void>(SIMPLE_GRAMMAR, this);

        try {
            parser.parse(null, Integer.class, expression);
            Assert.fail("ParseException is expected");
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
}

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
public class MultiExpressionParserTest extends ReflectionActionSource<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
            .skip("~\\s") // ignore white spaces
            .term("DECIMAL", "~\\d+") // simple decimal token
            .infixOp("+")
            .infixOrPrefixOp("-")
            .infixOp("*").rank(2)
            .enclosure("(", ")")
            .separator(";") // expression separator
            .toScope();

    @Term("DECIMAL")
    public Integer toInt(String param) {
        return Integer.valueOf(param);
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

    @Parameters(name = "{0} == {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();
        addCase(cases)
            .match("1+2;", 3)
            .match("1+2*3", 7);
        addCase(cases)
            .match("1+2;", 3)
            .match("1+2*3;", 7);
        addCase(cases)
            .match("--1;", 1)
            .match("--1", 1);

        return cases;
    }

    private static CaseBuilder addCase(List<Object[]> cases) {
        CaseBuilder bc = new CaseBuilder();
        cases.add(new Object[]{bc.text, bc.results});
        return bc;
    }

    public static class CaseBuilder {

        StringBuilder text = new StringBuilder();
        List<Integer> results = new ArrayList<Integer>();

        public CaseBuilder match(String expression, int result) {
            text.append(expression);
            results.add(result);
            return this;
        }
    }

    CharSequence expressions;
    List<Integer> expectedResults;

    public MultiExpressionParserTest(CharSequence expressions, List<Integer> expectedResults) {
        this.expressions = expressions;
        this.expectedResults = expectedResults;
    }

    @Test
    public void verify() {
        NanoParser<Void> parser = new NanoParser<Void>(SIMPLE_GRAMMAR, this);
        SourceReader source = new SourceReader(expressions);

        for(Integer result: expectedResults) {
            Integer val;
            try {
                val = parser.parseNext(null, Integer.class, source);
            }
            catch(ParserException e) {
                System.out.println(e.formatVerboseErrorMessage());
                throw e;
            }
            Assert.assertEquals(result, val);
        }

        if (!source.endOfStream()) {
            Assert.fail("Some more input remains");
        }
    }
}

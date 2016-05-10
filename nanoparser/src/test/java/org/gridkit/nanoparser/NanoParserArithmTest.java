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

import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.NanoParser;
import org.gridkit.nanoparser.ReflectionActionHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class NanoParserArithmTest extends ReflectionActionHandler<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
            .skip("~\\s") // ignore white spaces
            .term("DECIMAL", "~\\d+") // simple decimal token
            .infixOp("+")
            .infixOrPrefixOp("-")
            .infixOp("*").rank(2)
            .enclosure("(", ")")
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

    @Parameters(name = "{0} == {1}")
    public static List<Object[]> getExpressions() {
        List<Object[]> cases = new ArrayList<Object[]>();
        addCase(cases, "1+2", 3);
        addCase(cases, "1+2*3", 7);
        addCase(cases, "(1 + 2)*3", 9);
        addCase(cases, "1 - 3", -2);
        addCase(cases, "1 + -3", -2);
        addCase(cases, "1 + -3 * 2", -5);
        addCase(cases, "1 + -3 + 2", 0);
        addCase(cases, "1 + -(3 + 2)", -4);
        addCase(cases, "-2 * 3 + 1", -5);
        addCase(cases, "-3 * -2", 6);
        addCase(cases, "--1", 1);

        return cases;
    }
    
    private static void addCase(List<Object[]> cases, Object... c) {
        cases.add(c);        
    }

    String expression;
    int expectedResult;

    public NanoParserArithmTest(String expression, int expectedResult) {
        this.expression = expression;
        this.expectedResult = expectedResult;
    }

    @Test
    public void verify() {
        NanoParser<Void> parser = new NanoParser<Void>(this, SIMPLE_GRAMMAR);
        
        Assert.assertEquals(Integer.valueOf(expectedResult), parser.parse(null, Integer.class, expression));        
    }
}

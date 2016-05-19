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
import java.util.Arrays;
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
public class NanoParserAdvancedArithmTest extends ReflectionActionHandler<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
            .skip("~\\s") // ignore white spaces
            .term("DECIMAL", "~\\d+") // simple decimal token
            .infixOp("+")
            .infixOrPrefixOp("-")
            .infixOp("*").rank(2)
            .enclosure("(", ")")
            .enclosure("{", "}")
                .nestedInfixOp(",")
            .enclosure("[", "]")
                .implicitPrefixOp("AT")
            .toScope();
    
    @Term("DECIMAL")
    public Integer toInt(String param) {
        return Integer.valueOf(param);
    }
    
    @Binary("+")
    public Integer plus(Integer a, Integer b) {
        return a + b;
    }

    @Binary("+")
    public int[] arrayPlus(int[] a, int[] b) {
        int[] n = new int[a.length + b.length];
        System.arraycopy(a, 0, n, 0, a.length);
        System.arraycopy(b, 0, n, a.length, b.length);
        return n;
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

    @Binary("*")
    public int[] arrayMult(int[] a, Integer b) {
        int[] n = Arrays.copyOf(a, a.length);
        for(int i = 0; i != n.length; ++i) {
            n[i] *= b;
        }
        return n;
    }

    @Binary("*")
    public int[] arrayMult(Integer b, int[] a) {
        int[] n = Arrays.copyOf(a, a.length);
        for(int i = 0; i != n.length; ++i) {
            n[i] *= b;
        }
        return n;
    }

    @Binary(",")
    public int[] join(int[] a, Integer b) {
        int[] n = Arrays.copyOf(a, a.length + 1);
        n[a.length] = b;
        return n;
    }

    @Binary("AT")
    public Integer at(int[] array, Integer index) {
        try {
            return array[index];
        }
        catch(ArrayIndexOutOfBoundsException e) {
            throw new SemanticExpection(e.getMessage());
        }
    }

    @Convertion
    public int[] int2array(Integer a) {
        return new int[]{a};
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
        
        addCase(cases, "{1, 2, 3}[1 + 0]", 2);
        addCase(cases, "({1, 2, 3})[1 *2]", 3);
        addCase(cases, "({1, 2, 3} + {4, 5, 6})[4]", 5);
        addCase(cases, "({1, 2, 3} * 2 + {3, 4, 5})[2]", 6);
        addCase(cases, "({1, 2, 3} + 2 * {3, 4, 5})[4]", 8);

        return cases;
    }
    
    private static void addCase(List<Object[]> cases, Object... c) {
        cases.add(c);        
    }

    String expression;
    int expectedResult;

    public NanoParserAdvancedArithmTest(String expression, int expectedResult) {
        this.expression = expression;
        this.expectedResult = expectedResult;
    }

    @Test
    public void verify() {
        try {
            NanoParser<Void> parser = new NanoParser<Void>(this, SIMPLE_GRAMMAR);
            
            Assert.assertEquals(Integer.valueOf(expectedResult), parser.parse(null, Integer.class, expression));
        }
        catch(ParserException e) {
            System.out.println(e.formatVerboseErrorMessage());
            throw e;
        }
    }
}

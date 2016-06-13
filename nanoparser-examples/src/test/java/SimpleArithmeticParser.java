
import java.util.Arrays;

import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.NanoParser;
import org.gridkit.nanoparser.ParserException;
import org.gridkit.nanoparser.ReflectionActionHandler;
import org.junit.Assert;
import org.junit.Test;

public class SimpleArithmeticParser extends ReflectionActionHandler<Void> {

    public static final SyntaticScope SIMPLE_GRAMMAR = NanoGrammar.newParseTable()
                          // leading tilde (~) in token use for RegEx
            .skip("~\\s") // ignore white spaces
            .term("DECIMAL", "~\\d+") // simple decimal token
            .infixOp("+", "+")
            .infixOrPrefixOp("-", "-")
            .infixOp("*", "*").rank(2)
            .enclosure("(", ")")
            .enclosure("max", "~max\\(", ")") // hard coded function
            .nestedInfixOp(",").rank(0) // comma would be accepted only with "max(...)"
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

    // Function taken multiple arguments separated by comma
    @Unary("max")
    public Integer max(int[] args) {
        int n = args[0];
        for(int i = 1; i < args.length; ++i) {
            if (args[i] > n) {
                n = args[i];
            }
        }
        return n;
    }
    
    // Comma operator to collect argument for a function
    @Binary(",")
    public int[] args(@Convertible int[] head, Integer tail) {
        int[] r = Arrays.copyOf(head, head.length + 1);
        r[head.length] = tail;
        return r;
    }

    // Trivial conversion of single value to one element list, used comma syntax
    @Convertion
    public int[] convert(Integer n) {
        return new int[]{n};
    }
    
    @Test
    public void test() {
        
        NanoParser<Void> parser = new NanoParser<Void>(this, SIMPLE_GRAMMAR);
        
        try {
            Assert.assertEquals(Integer.valueOf(3), parser.parse(null, Integer.class, "1+2"));
            Assert.assertEquals(Integer.valueOf(7), parser.parse(null, Integer.class, "1+2*3"));
            Assert.assertEquals(Integer.valueOf(9), parser.parse(null, Integer.class, "(1 + 2)*3"));
            Assert.assertEquals(Integer.valueOf(-2), parser.parse(null, Integer.class, "1 - 3"));
            Assert.assertEquals(Integer.valueOf(-2), parser.parse(null, Integer.class, "1 + -3"));
            Assert.assertEquals(Integer.valueOf(-5), parser.parse(null, Integer.class, "1 + -3 * 2"));
            Assert.assertEquals(Integer.valueOf(0), parser.parse(null, Integer.class, "1 + -3 + 2"));
            Assert.assertEquals(Integer.valueOf(-4), parser.parse(null, Integer.class, "1 + -(3 + 2)"));
            Assert.assertEquals(Integer.valueOf(-5), parser.parse(null, Integer.class, "-2 * 3 + 1"));
            Assert.assertEquals(Integer.valueOf(3), parser.parse(null, Integer.class, "max(2, 3)"));
            Assert.assertEquals(Integer.valueOf(6), parser.parse(null, Integer.class, "max(2, 2 * 3, 3)"));
            Assert.assertEquals(Integer.valueOf(4), parser.parse(null, Integer.class, "max(2, 2 * 3, 3) - 2"));
            Assert.assertEquals(Integer.valueOf(10), parser.parse(null, Integer.class, "max(2, 2 + 3, 3) * 2"));
        }
        catch(ParserException e) {
            System.out.println(e.formatVerboseErrorMessage());
            throw e;
        }
    }        
}

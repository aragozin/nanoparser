
import org.gridkit.nanoparser.NanoGrammar;
import org.gridkit.nanoparser.NanoGrammar.SyntaticScope;
import org.gridkit.nanoparser.NanoParser;
import org.gridkit.nanoparser.ReflectionActionHandler;
import org.junit.Assert;
import org.junit.Test;

public class SimpleArithmeticParser extends ReflectionActionHandler<Void> {

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

    @Test
    public void test() {
        
        NanoParser<Void> parser = new NanoParser<Void>(this, SIMPLE_GRAMMAR);
        
        Assert.assertEquals(Integer.valueOf(3), parser.parse(null, Integer.class, "1+2"));
        Assert.assertEquals(Integer.valueOf(7), parser.parse(null, Integer.class, "1+2*3"));
        Assert.assertEquals(Integer.valueOf(9), parser.parse(null, Integer.class, "(1 + 2)*3"));
        Assert.assertEquals(Integer.valueOf(-2), parser.parse(null, Integer.class, "1 - 3"));
        Assert.assertEquals(Integer.valueOf(-2), parser.parse(null, Integer.class, "1 + -3"));
        Assert.assertEquals(Integer.valueOf(-5), parser.parse(null, Integer.class, "1 + -3 * 2"));
        Assert.assertEquals(Integer.valueOf(0), parser.parse(null, Integer.class, "1 + -3 + 2"));
        Assert.assertEquals(Integer.valueOf(-4), parser.parse(null, Integer.class, "1 + -(3 + 2)"));
        Assert.assertEquals(Integer.valueOf(-5), parser.parse(null, Integer.class, "-2 * 3 + 1"));
    }
}

Nanoparser - simple library for parsing operator precendence grammar
=========

Nanoparser is simple library solving problem of parsing 
[operator precendence grammars][1] in Java.

There are number number of good compiler generator libraries for Java, such as [ANTLR][2], [jParsec][3] and other.

###Why Nanoparser?

 - Nanoparser is a library, not code generator.
 - Nanoparser handles a very narrow, but practically important class of [operator precendence grammars][1],
 - By focusing on single grammar type Nanoparser is far more simple to use.
 
###Basic example 
See [SimpleArithmeticParser.java](nanoparser-examples/src/test/java/SimpleArithmeticParser.java) for full source.
    
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




 [1]: https://en.wikipedia.org/wiki/Operator-precedence_grammar
 [2]: http://www.antlr.org/
 [3]: https://github.com/jparsec/jparsec
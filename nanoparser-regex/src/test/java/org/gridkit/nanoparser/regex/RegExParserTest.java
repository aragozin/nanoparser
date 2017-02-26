package org.gridkit.nanoparser.regex;

import org.gridkit.nanoparser.ParserException;
import org.gridkit.nanoparser.TracingNanoParser;
import org.gridkit.nanoparser.regex.RegExParser.PatternElement;
import org.junit.Assert;
import org.junit.Test;

public class RegExParserTest {


    @Test
    public void smoke() {
        
        parse("[a-z]");
        parse("[a-z&&[^c]]");
        parse("[\\n-\\t&&[^c]]");
        fail ("[\\n-\\s&&[^c]]");
        parse("[\\d&&[^c]]");
    	parse("a+bb*c");
    	parse("\\s+\\p{Letter}");
        parse("a");
        parse("abc");
        parse("ab|cd");
        parse("ab|cd+");
        parse("ab|cd++");
        fail ("ab|cd+*");
        parse("(.*)");
        parse("(?:.*)");
        parse(".*(ab|[a-z]d).*");        
        parse("a[b][^c]");
    }
    
    public PatternElement parse(String regEx) {
        try {
        	TracingNanoParser<Void> np = new TracingNanoParser<Void>(RegExParser.PATTERN, new RegExParser());
//        	np.setTraceOut(System.out);
            
            return np.parse(null, PatternElement.class, regEx);
        } catch (ParserException e) {
            System.err.println(e.formatVerboseErrorMessage());
            throw e;
        }
    }
    
    public void fail(String regEx) {
    	try {
    		parse(regEx);
    		Assert.fail("Exception expected");
    	}
    	catch(Exception e) {
    		// ok
    	}
    }
}
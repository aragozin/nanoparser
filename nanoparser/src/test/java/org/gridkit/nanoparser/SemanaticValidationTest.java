package org.gridkit.nanoparser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SemanaticValidationTest {

    @Test
    public void validate_advanced_arithm() {
        String result = SemanticValidator.validate(NanoParserAdvancedArithmTest.SIMPLE_GRAMMAR, new NanoParserAdvancedArithmTest(null, 0));
        assertEquals("", result);
    }

    @Test
    public void validate_multi_expression_parser() {
        String result = SemanticValidator.validate(MultiExpressionParserTest.SIMPLE_GRAMMAR, new MultiExpressionParserTest(null, null));
        assertEquals("", result);
    }

    @Test
    public void validate_arithm() {
        String result = SemanticValidator.validate(NanoParserArithmTest.SIMPLE_GRAMMAR, new NanoParserArithmTest(null, 0));
        assertEquals("", result);
    }

}

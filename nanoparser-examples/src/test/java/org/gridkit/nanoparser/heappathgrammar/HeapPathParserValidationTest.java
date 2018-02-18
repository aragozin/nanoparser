package org.gridkit.nanoparser.heappathgrammar;

import org.gridkit.nanoparser.SemanticValidator;
import org.junit.Assert;
import org.junit.Test;

public class HeapPathParserValidationTest {

    @Test
    public void validate_parser() {
        Assert.assertEquals("", SemanticValidator.validate(HeapPathParser.HEAPPATH_GRAMMAR, new HeapPathParser()));
    }

}

package org.gridkit.nanoparser;

import java.text.ParseException;

/**
 * This exception is indicating logical failure of 
 * semantic action. Parser would convert it to {@link ParseException}.
 *   
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SemanticExpection extends RuntimeException {

    private static final long serialVersionUID = 20160507L;

    public SemanticExpection() {
        super();
    }

    public SemanticExpection(String message, Throwable cause) {
        super(message, cause);
    }

    public SemanticExpection(String message) {
        super(message);
    }

    public SemanticExpection(Throwable cause) {
        super(cause);
    }
}

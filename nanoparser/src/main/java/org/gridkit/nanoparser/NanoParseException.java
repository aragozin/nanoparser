package org.gridkit.nanoparser;

public class NanoParseException extends RuntimeException {

    private static final long serialVersionUID = 20160508L;

    public NanoParseException() {
        super();
    }

    public NanoParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public NanoParseException(String message) {
        super(message);
    }

    public NanoParseException(Throwable cause) {
        super(cause);
    }
}

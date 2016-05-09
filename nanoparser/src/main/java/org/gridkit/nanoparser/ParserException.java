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

public class ParserException extends RuntimeException {

    private static final long serialVersionUID = 20151220L;
    
    private CharSequence parseText;
    private int line;
    private int position;
    private int offset;
    
    public ParserException(CharSequence parseText, int offset, int line, int position, String message) {
        super(message);
        this.parseText = parseText;
        this.offset = offset;
        this.line = line;
        this.position = position;
    }

    public ParserException(CharSequence parseText, int offset, int line, int position, String message, Exception e) {
        super(message, e);
        this.parseText = parseText;
        this.offset = offset;
    }
    
    public CharSequence getParseText() {
        return parseText;
    }

    public int getLine() {
        return line;
    }

    public int getPosition() {
        return position;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public String formatVerboseErrorMessage() {
        return "Error: " + getMessage() + "\n Line: " + (line + 1) + " Position" + position + "\n" + formatSourceReference();
    }
    
    /**
     * Extract a fragment of parsed text near error.
     * Result would include 2 lines, example is below
     * <pre>
     * ABC + * XYZ
     *       ^
     * <pre>
     * 
     * @return
     */
    public String formatSourceReference() {
        StringBuilder sb = new StringBuilder();
        int ls = offset;        
        while(ls >= 0) {
            if (parseText.charAt(ls) != '\n') {
                --ls;
            }
        }
        int back = offset - (ls + 1);
        boolean skipStart = false;
        if (back > 32) {
            back = 32;
            skipStart = true;
            sb.append("... ");
        }
        for(int n = offset - back; n < parseText.length(); ++n) {
            if (parseText.charAt(n) == '\n') {
                break;
            }
            if (n - offset > 16) {
                sb.append("...");
                break;
            }
            sb.append(parseText.charAt(n));
        }
        sb.append("\n");
        if (skipStart) {
            sb.append("    ");
        }
        for(int n = 0; n != back; ++n) {
            sb.append(' ');
        }
        sb.append("^\n");
        return sb.toString();
    }
}

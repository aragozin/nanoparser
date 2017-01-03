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
    
    private Token token;
    
    public ParserException(Token token, String message) {
        super(message);
        this.token = token;
    }

    public ParserException(Token token, String message, Exception e) {
        super(message, e);
        this.token = token;
    }
    
    public Token getToken() {
        return token;
    }
    
    public String formatVerboseErrorMessage() {
        return "Error: " + getMessage() + "\nLine: " + token.line() + " Position: " + token.pos() + "\n" + token.excerpt(60);
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
    public static String formatTokenExcertp(Token tkn, int lengthLimit) {
        StringBuilder sb = new StringBuilder();
        CharSequence parseText = tkn.source();
        int toffs = tkn.offset();
        int ls = tkn.offset();
        if (ls >= parseText.length()) {
            ls = parseText.length() - 1;
        }
        boolean atEol = ls >= 0 & parseText.charAt(ls) == '\n';
        if (atEol) {
            --ls;
            --toffs;
        }
        while(ls >= 0) {
            if (parseText.charAt(ls) != '\n') {
                --ls;
            }
            else {
                break;
            }
        }
        int backLimit = lengthLimit / 2;
        int back = toffs - (ls + 1);
        boolean skipStart = false;
        if (back > backLimit) {
            back = backLimit - 4;
            skipStart = true;
            sb.append("... ");
        }
        for(int n = toffs - back; n < parseText.length(); ++n) {
            if (parseText.charAt(n) == '\n') {
                break;
            }
            if (sb.length() >= lengthLimit) {
                sb.setLength(sb.length() - 3);
                sb.append("...");
                break;
            }
            sb.append(parseText.charAt(n));
        }
        sb.append("\n");
        if (skipStart) {
            sb.append("    ");
        }
        for(int n = 0; n < back; ++n) {
            sb.append(' ');
        }
        sb.append("^\n");
        return sb.toString();
    }
}

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

import java.text.ParseException;

/**
 * This exception is indicating logical failure of 
 * semantic action. Parser would convert it to {@link ParseException}.
 *   
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class SemanticExpection extends RuntimeException {

    private static final long serialVersionUID = 20160507L;

    private Token token;
    
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
    
    public SemanticExpection withToken(Token tkn) {
        this.token = tkn;
        return this;
    }
    
    public Token getToken() {
        return token;
    }
}

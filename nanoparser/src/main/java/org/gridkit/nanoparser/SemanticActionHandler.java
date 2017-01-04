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

public interface SemanticActionHandler<C> {

    public TermActionHandler<?, ?>[] enumTerm(String opId, Class<?> rType);

    public UnaryActionHandler<?, ?, ?>[] enumUnaries(String opId, Class<?> rType, Class<?> argType);

    public BinaryActionHandler<?, ?, ?, ?>[] enumBinaries(String opId, Class<?> rType, Class<?> leftType, Class<?> rightType);

    public interface ActionHandler {
        
    }
    
    public interface TermActionHandler<C, R> extends ActionHandler {
        
        public Class<R> returnType();
        
        public R apply(C parserContext, Token token);
    }
        
    public interface UnaryActionHandler<C, R, A> extends ActionHandler {

        public Class<R> returnType();
        
        public Class<A> argType();
        
        public R apply(C parserContext, Token token, A arg);
    }

    public interface BinaryActionHandler<C, R, A, B> extends ActionHandler {

        public Class<R> returnType();

        public Class<A> leftType();

        public Class<B> rightType();
        
        public R apply(C parserContext, Token token, A leftArg, B rightArg);
        
    }
}

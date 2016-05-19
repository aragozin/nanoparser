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

    public <R> ActionHandler<C, R, Object, Void> lookupTerm(String opID, Class<R> returnType);

    public <R, A> ActionHandler<C, R, A, Void> lookupUnary(String opID, Class<R> returnType, Class<A> argA);

    public <R, A, B> ActionHandler<C, R, A, B> lookupBinary(String opID, Class<R> returnType, Class<A> argA, Class<B> argB);

    public <R, T> ActionHandler<C, R, T, Void> lookupConvertor(Class<?> sourceClass, Class<R> returnType);

    public Class<?>[] enumConvertions(Class<?> targetClass);

    public Class<?>[][] enumUnaries(String opId);

    public Class<?>[][] enumBinaries(String opId);
    
    public interface ActionHandler<C, R, A, B> {
        
        public Class<A> leftType();
        
        public Class<B> rightType();
        
        public R apply(C parserContext, Token token, A left, B right);
    }
}

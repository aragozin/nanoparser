package org.gridkit.nanoparser;

import java.util.List;

public interface SemanticActionHandler<C> {

    public <R> ActionHandler<C, R, Object, Void> lookupUnary(String opID, Class<R> returnType);

    public <R> ActionHandler<C, R, Object, Object> lookupBinary(String opID, Class<R> returnType);

    public List<Class<?>> lookupConvertions(Class<?> targetClass);
    
    public <R, T> ActionHandler<C, R, T, Void> lookupConvertor(Class<?> sourceClass, Class<R> returnType);
    
    public interface ActionHandler<C, R, A, B> {
        
        public Class<A> leftType();
        
        public Class<B> rightType();
        
        public R apply(C parserContext, String operatorBody, A left, B right);
    }
}

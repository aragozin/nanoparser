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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ReflectionActionHandler<C> implements SemanticActionHandler<C> {

    private static final Class<?>[] EMPTY = new Class<?>[0];
    
    protected Map<TypedId, MethodOpHandler> terms = new LinkedHashMap<TypedId, MethodOpHandler>();
    protected Map<String, HandlerSet> unaries = new LinkedHashMap<String, HandlerSet>();
    protected Map<String, HandlerSet> binaries = new LinkedHashMap<String, HandlerSet>();
    protected Map<Class<?>, ConversionSet> convertions = new LinkedHashMap<Class<?>, ConversionSet>();
    
    public ReflectionActionHandler() {
        initMethodTables();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> ActionHandler<C, R, Object, Void> lookupTerm(String opID, Class<R> returnType) {
        for(MethodOpHandler mh: terms.values()) {
            if (opID.equals(mh.id) && returnType.isAssignableFrom(mh.returnType)) {
                return (ActionHandler) mh;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R, A> ActionHandler<C, R, A, Void> lookupUnary(String opID, Class<R> returnType, Class<A> argType) {
        for(HandlerSet set: unaries.values()) {
            for(MethodOpHandler mh: set.handlers) {
                if (opID.equals(mh.id) && returnType.isAssignableFrom(mh.returnType) && mh.leftType.isAssignableFrom(argType)) {
                    return (ActionHandler) mh;
                }
            }
        }
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R, A, B> ActionHandler<C, R, A, B> lookupBinary(String opID, Class<R> returnType, Class<A> argA, Class<B> argB) {
        for(HandlerSet set: binaries.values()) {
            for(MethodOpHandler mh: set.handlers) {
                if (opID.equals(mh.id) && returnType.isAssignableFrom(mh.returnType) && mh.leftType.isAssignableFrom(argA) && mh.rightType.isAssignableFrom(argB)) {
                    return (ActionHandler<C, R, A, B>) mh;
                }
            }                
        }
        return null;
    }
    
    @Override
    public Class<?>[] enumConvertions(Class<?> targetClass) {
        ConversionSet c = convertions.get(targetClass);
        if (c != null) {
            return new ArrayList<Class<?>>(c.handlers.keySet()).toArray(EMPTY);
        }
        else {
            return EMPTY;
        }
    }

    @Override
    public Class<?>[][] enumUnaries(String opId) {
        HandlerSet hset = unaries.get(opId);
        if (hset != null) {
            return hset.signatures;
        }
        else {
            return null;
        }
    }

    @Override
    public Class<?>[][] enumBinaries(String opId) {
        HandlerSet hset = binaries.get(opId);
        if (hset != null) {
            return hset.signatures;
        }
        else {
            return null;
        }
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R, T> ActionHandler<C, R, T, Void> lookupConvertor(Class<?> sourceClass, Class<R> returnType) {
        ConversionSet c = convertions.get(returnType);
        if (c != null) {
            return (ActionHandler)c.handlers.get(sourceClass);
        }
        return null;
    }

    private void initMethodTables() {
        for(Method m: this.getClass().getMethods()) {
            if (m.getAnnotation(Term.class) != null) {
                initTermMethod(m);
            }
            if (m.getAnnotation(Convertion.class) != null) {
                initConvertionMethod(m);
            }
            if (m.getAnnotation(Unary.class) != null) {
                initUnaryMethod(m);
            }
            if (m.getAnnotation(Binary.class) != null) {
                initBinaryMethod(m);
            }
        }
    }

    private void initTermMethod(Method m) {
        String id = m.getAnnotation(Term.class).value();
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initTermArguments();
        Class<?> target = m.getReturnType();
        if (terms.containsKey(typedId(id, target))) {
            throw new IllegalArgumentException("Ambiguous term '" + id + "' handler for type " + target.getSimpleName());
        }
        terms.put(typedId(id, target), h);
    }
    
    private void initConvertionMethod(Method m) {
        if (m.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("@Conversion method '" + m.getName() + "' should have 1 argument");
        }
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Conversion method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(this, "", m, m.getReturnType());
        h.initConversionArguments();
        
        Class<?> target = m.getReturnType();
        ConversionSet c = convertions.get(target);
        if (c == null) {
            c = new ConversionSet();
            convertions.put(target, c);
        }
        c.addHandler(m.getParameterTypes()[0], h);
    }
    
    private void initUnaryMethod(Method m) {
        String id = m.getAnnotation(Unary.class).value();
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initUnaryArgumnets();
        
        HandlerSet hset = unaries.get(id);
        if (hset == null) {
            hset = new HandlerSet();
            unaries.put(id, hset);
        }
        hset.add(h, h.returnType, h.leftType);
    }

    private void initBinaryMethod(Method m) {
        String id = m.getAnnotation(Binary.class).value();
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initBinaryArguments();
        
        HandlerSet hset = binaries.get(id);
        if (hset == null) {
            hset = new HandlerSet();
            binaries.put(id, hset);
        }
        hset.add(h, h.returnType, h.leftType, h.rightType);
    }

    private TypedId typedId(String id, Class<?> type) {
        return new TypedId(type, id);
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Unary {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Binary {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Term {
        String value();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Convertion {
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Context {
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Source {
    }
    
    private static class MethodOpHandler implements ActionHandler<Object, Object, Object, Object> {
        
        private final ReflectionActionHandler<?> host;
        private final String id;
        private final Class<Object> returnType;
        private Class<Object> leftType;
        private Class<Object> rightType;
        
        private final Method method;
        
        private int contextArg = -1;
        private int tokenArg = -1;
        private int tokenBodyArg = -1;
        private int leftArg = -1;
        private int rightArg = -1;
        
        @SuppressWarnings("unchecked")
        public MethodOpHandler(ReflectionActionHandler<?> host, String id, Method m, Class<?> returnType) {
            this.host = host;
            this.id = id;
            this.method = m;
            this.returnType = (Class<Object>) returnType;
            
            this.method.setAccessible(true);
        }

        private IllegalArgumentException methodError(String message) {
            IllegalArgumentException e = new IllegalArgumentException(message);
            String file = method.getDeclaringClass().getSimpleName();
            if (method.getDeclaringClass().getEnclosingClass() != null) {
                file = method.getDeclaringClass().getEnclosingClass().getSimpleName();
            }
            StackTraceElement se = new StackTraceElement(method.getDeclaringClass().getName(), method.getName(), file + ".java", 1);
            StackTraceElement[] trace = e.getStackTrace();
            trace = Arrays.copyOf(trace, trace.length + 1);
            System.arraycopy(trace, 0, trace, 1, trace.length - 1);
            trace[0] = se;
            e.setStackTrace(trace);
            return e;
        }
        
        public void initTermArguments() {
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            
            for(int i = 0; i != paramTypes.length; ++i) {
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Source.class) {
                        throw methodError("Method '" + method.getName() + "' only one argument annotated with @Token should have type Token");
                    }
                }
                else if (paramTypes[i] == String.class) {
                    if (tokenBodyArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' should have zero or one unannotated argument");
                    }
                    tokenBodyArg = i;
                }
                else {
                    throw methodError("Method '" + method.getName() + "' term input arg should be of type String");
                }
            }
        }

        @SuppressWarnings("unchecked")
        public void initConversionArguments() {
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            
            for(int i = 0; i != paramTypes.length; ++i) {
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Source.class) {
                        throw methodError("Method '" + method.getName() + "' argument annotated with @Source should have type Token");
                    }
                }
                else {
                    if (leftArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' - @Conversion method should have one input argument");
                    }
                    leftArg = i;
                    leftType = (Class<Object>)paramTypes[i];
                    if (leftType.isPrimitive()) {
                        throw methodError("Method '" + method.getName() + "' - @Conversion method should have one input argument of reference type");
                    }
                }
            }            
            if (leftArg < 0) {
                throw methodError("Method '" + method.getName() + "' - @Conversion method should have one input argument");
            }
        }
        
        @SuppressWarnings("unchecked")
        public void initUnaryArgumnets() {
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            
            for(int i = 0; i != paramTypes.length; ++i) {
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Token.class) {
                        throw methodError("Method '" + method.getName() + "' argument annotated with @Source should have type Token");
                    }
                }
                else {
                    if (leftArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' - @Unary method should have one input argument");
                    }
                    leftArg = i;
                    leftType = (Class<Object>)paramTypes[i];
                    if (leftType.isPrimitive()) {
                        throw methodError("Method '" + method.getName() + "' - @Unary method should have one input argument of reference type");
                    }
                }
            }            
            if (leftArg < 0) {
                throw methodError("Method '" + method.getName() + "' - @Unary method should have one input argument");
            }            
        }
        
        @SuppressWarnings("unchecked")
        public void initBinaryArguments() {
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            
            for(int i = 0; i != paramTypes.length; ++i) {
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Source.class) {
                        throw methodError("Method '" + method.getName() + "' argument annotated with @Source should have type Token");
                    }
                }
                else {
                    if (rightArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' - @Binary method should have two input arguments");
                    }
                    if (leftArg < 0) {
                        leftArg = i;
                        leftType = (Class<Object>)paramTypes[i];
                        if (leftType.isPrimitive()) {
                            throw methodError("Method '" + method.getName() + "' - @Binary method should have two input arguments of reference type");
                        }
                    }
                    else {
                        rightArg = i;
                        rightType = (Class<Object>)paramTypes[i];
                        if (rightType.isPrimitive()) {
                            throw methodError("Method '" + method.getName() + "' - @Binary method should have two input arguments of reference type");
                        }                        
                    }
                }
            }            
            if (rightArg < 0) {
                throw methodError("Method '" + method.getName() + "' - @Binary method should have one two arguments");
            }                        
        }

        private boolean isContextAnnotated(Annotation[] annotations) {
            for(Annotation a: annotations) {
                if (a instanceof Context) {
                    return true;
                }
            }
            return false;
        }

        private boolean isTokenAnnotated(Annotation[] annotations) {
            for(Annotation a: annotations) {
                if (a instanceof Source) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public Class<Object> leftType() {
            return leftType;
        }

        @Override
        public Class<Object> rightType() {
            return rightType;
        }

        @Override
        public Object apply(Object parserContext, Token token, Object left, Object right) {
            try {
                Object[] args = new Object[method.getParameterTypes().length];
                if (contextArg >= 0) {
                    args[contextArg] = parserContext;
                }
                if (tokenArg >= 0) {
                    args[tokenArg] = token;
                }
                if (tokenBodyArg >= 0) {
                    args[tokenBodyArg] = token.tokenBody();
                }
                if (leftArg >= 0) {
                    args[leftArg] = left;
                }
                if (rightArg >= 0) {
                    args[rightArg] = right;
                }
                return method.invoke(host, args);
            } catch (InvocationTargetException e) {
                throw throwUnchecked(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    class ConversionSet {
        
        Map<Class<?>, MethodOpHandler> handlers = new LinkedHashMap<Class<?>, MethodOpHandler>();

        public void addHandler(Class<?> source, MethodOpHandler handler) {
            for (Class<?> cc: handlers.keySet()) {
                if (cc.isAssignableFrom(source) || source.isAssignableFrom(cc)) {
                    throw new IllegalArgumentException("Ambigous conevrsion alternatives: " + cc.getName() + " -- " + source.getName()); 
                }
            }
            handlers.put(source, handler);
        }
    }
    
    class HandlerSet {
        
        MethodOpHandler[] handlers = new MethodOpHandler[0];
        Class<?>[][] signatures = new Class<?>[0][];
        
        public void add(MethodOpHandler handler, Class<?>... sig) {
            MethodOpHandler[] nh = Arrays.copyOf(handlers, handlers.length + 1);
            signatures = Arrays.copyOf(signatures, handlers.length + 1);
            nh[handlers.length] = handler;
            signatures[handlers.length] = sig;
            handlers = nh;
        }        
    }
    
    private static class TypedId {
        
        final Class<?> type;
        final String id;
        
        public TypedId(Class<?> type, String id) {
            this.type = type;
            this.id = id;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TypedId other = (TypedId) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return id + ":" + type.getSimpleName();
        }
    }
    
    private static RuntimeException throwUnchecked(Throwable e) {
        ReflectionActionHandler.<RuntimeException>throwAny(e);
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }
}

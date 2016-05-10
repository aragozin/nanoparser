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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflectionActionHandler<C> implements SemanticActionHandler<C> {

    protected Map<Object, MethodOpHandler> unaries = new LinkedHashMap<Object, MethodOpHandler>();
    protected Map<Object, MethodOpHandler> binaries = new LinkedHashMap<Object, MethodOpHandler>();
    protected Map<Class<?>, Convertions> convertions = new LinkedHashMap<Class<?>, Convertions>();
    
    public ReflectionActionHandler() {
        initMethodTables();
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R> ActionHandler<C, R, Object, Void> lookupUnary(String opID, Class<R> returnType) {
        for(MethodOpHandler mh: unaries.values()) {
            if (opID.equals(mh.id) && returnType.isAssignableFrom(mh.returnType)) {
                return (ActionHandler) mh;
            }
        }
        return null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <R> ActionHandler<C, R, Object, Object> lookupBinary(String opID, Class<R> returnType) {
        for(MethodOpHandler mh: binaries.values()) {
            if (opID.equals(mh.id) && returnType.isAssignableFrom(mh.returnType)) {
                return (ActionHandler<C, R, Object, Object>) mh;
            }
        }
        return null;
    }
    
    @Override
    public List<Class<?>> lookupConvertions(Class<?> targetClass) {
        Convertions c = convertions.get(targetClass);
        if (c != null) {
            return new ArrayList<Class<?>>(c.convertors.keySet());
        }
        else {
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <R, T> ActionHandler<C, R, T, Void> lookupConvertor(Class<?> sourceClass, Class<R> returnType) {
        Convertions c = convertions.get(returnType);
        if (c != null) {
            return (ActionHandler)c.convertors.get(sourceClass);
        }
        return null;
    }

    private void initMethodTables() {
        for(Method m: this.getClass().getMethods()) {
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

    private void initConvertionMethod(Method m) {
        if (m.getParameterTypes().length != 1) {
            throw new IllegalArgumentException("@Conversion method '" + m.getName() + "' should have 1 argument");
        }
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Conversion method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler("", m, m.getReturnType(), m.getParameterTypes()[0], null);
        Class<?> target = m.getReturnType();
        Convertions c = convertions.get(target);
        if (c == null) {
            c = new Convertions();
            convertions.put(target, c);
        }
        c.addHandler(m.getParameterTypes()[0], h);
    }
    
    private void initUnaryMethod(Method m) {
        Unary u = m.getAnnotation(Unary.class);
        if (m.getParameterTypes().length != 2 || m.getParameterTypes()[0] != String.class) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' should have 2 arguments: String operator body and any type input parameter");
        }
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(u.value(), m, m.getReturnType(), m.getParameterTypes()[1], null);
        Object k = h.key();
        if (unaries.containsKey(k)) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' is ambigous, same ID is already defined");
        }
        unaries.put(k, h);
    }

    private void initBinaryMethod(Method m) {
        Binary b = m.getAnnotation(Binary.class);
        if (m.getParameterTypes().length != 3 || m.getParameterTypes()[0] != String.class) {
            throw new IllegalArgumentException("@Binary method '" + m.getName() + "' should have 3 arguments: String operator body and two any type input parameters");
        }
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Binary method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(b.value(), m, m.getReturnType(), m.getParameterTypes()[1], m.getParameterTypes()[2]);
        Object k = h.key();
        if (binaries.containsKey(k)) {
            throw new IllegalArgumentException("@Binary method '" + m.getName() + "' is ambigous, same ID is already defined");
        }
        binaries.put(k, h);
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Unary {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Binary {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Convertion {
    }
    
    private class MethodOpHandler implements ActionHandler<Object, Object, Object, Object> {
        
        private final String id;
        private final Class<Object> returnType;
        private final Class<Object> leftType;
        private final Class<Object> rightType;
        
        private final Method method;
        
        @SuppressWarnings("unchecked")
        public MethodOpHandler(String id, Method m, Class<?> returnType, Class<?> left, Class<?> right) {
            this.id = id;
            this.method = m;
            this.returnType = (Class<Object>) returnType;
            this.leftType = (Class<Object>) left;
            this.rightType = (Class<Object>) right;
            
            this.method.setAccessible(true);
        }
        
        public Object key() {
            return Arrays.asList(id, returnType);
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
        public Object apply(Object parserContext, String operatorBody, Object left, Object right) {
            try {
                if (method.getParameterTypes().length == 1) {
                    Object[] args = {left};
                    return method.invoke(ReflectionActionHandler.this, args);
                }
                else if (method.getParameterTypes().length == 2) {
                    Object[] args = {operatorBody, left};
                    return method.invoke(ReflectionActionHandler.this, args);
                }
                else {
                    Object[] args = {operatorBody, left, right};
                    return method.invoke(ReflectionActionHandler.this, args);
                }
            } catch (InvocationTargetException e) {
                throw throwUnchecked(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    class Convertions {
        
        Map<Class<?>, MethodOpHandler> convertors = new LinkedHashMap<Class<?>, MethodOpHandler>();

        public void addHandler(Class<?> source, MethodOpHandler handler) {
            for (Class<?> cc: convertors.keySet()) {
                if (cc.isAssignableFrom(source) || source.isAssignableFrom(cc)) {
                    throw new IllegalArgumentException("Ambigous conevrsion alternatives: " + cc.getName() + " -- " + source.getName()); 
                }
            }
            convertors.put(source, handler);
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

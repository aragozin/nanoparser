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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ReflectionActionHandler<C> implements SemanticActionHandler<C> {

    protected List<ArgConvertor> convertors = new ArrayList<ReflectionActionHandler.ArgConvertor>();
    
    protected Map<TypedId, MethodOpHandler> termHandlers = new LinkedHashMap<TypedId, MethodOpHandler>();
    protected List<MethodOpHandler> unaryHandlers = new ArrayList<MethodOpHandler>();
    protected List<MethodOpHandler> binaryHandlers = new ArrayList<MethodOpHandler>();
    
    protected Map<List<Object>, MethodOpHandler[]> lookupCache = new HashMap<List<Object>, MethodOpHandler[]>();
    
    public ReflectionActionHandler() {
        initMethodTables();
    }

    @Override
    public TermActionHandler<?, ?>[] enumTerm(String opId, Class<?> rType) {
        List<Object> key = Arrays.<Object>asList(opId, rType);
        MethodOpHandler[] r = lookupCache.get(key);
        if (r == null) {
            List<MethodOpHandler> ml = new ArrayList<MethodOpHandler>();
            for(MethodOpHandler h: termHandlers.values()) {
                if ((opId == null || opId.equals(h.id)) && (rType == null || rType.isAssignableFrom(h.returnType))) {
                    ml.add(h);
                }
            }
            r = ml.toArray(new MethodOpHandler[ml.size()]);
            lookupCache.put(key, r);
        }
        
        return r;
    }

    @Override
    public UnariActionHandler<?, ?, ?>[] enumUnaries(String opId, Class<?> rType, Class<?> argType) {
        List<Object> key = Arrays.<Object>asList(opId, rType, argType);
        MethodOpHandler[] r = lookupCache.get(key);
        if (r == null) {
            List<MethodOpHandler> ml = new ArrayList<MethodOpHandler>();
            for(MethodOpHandler h: unaryHandlers) {
                if ((opId == null || opId.equals(h.id)) 
                 && (rType == null || rType.isAssignableFrom(h.returnType))
                 && (argType == null || h.leftType().isAssignableFrom(argType))) {
                    ml.add(h);
                }
            }
            r = ml.toArray(new MethodOpHandler[ml.size()]);
            lookupCache.put(key, r);
        }
        
        return r;
    }

    @Override
    public BinaryActionHandler<?, ?, ?, ?>[] enumBinaries(String opId, Class<?> rType, Class<?> leftType, Class<?> rightType) {
        List<Object> key = Arrays.<Object>asList(opId, rType, leftType, rightType);
        MethodOpHandler[] r = lookupCache.get(key);
        if (r == null) {
            List<MethodOpHandler> ml = new ArrayList<MethodOpHandler>();
            for(MethodOpHandler h: binaryHandlers) {
                if ((opId == null || opId.equals(h.id)) 
                 && (rType == null || rType.isAssignableFrom(h.returnType))
                 && (leftType == null || h.leftType().isAssignableFrom(leftType))
                 && (rightType == null || h.rightType().isAssignableFrom(rightType))) {
                    ml.add(h);
                }
            }
            r = ml.toArray(new MethodOpHandler[ml.size()]);
            lookupCache.put(key, r);
        }
        
        return r;
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
        for(MethodOpHandler m: new ArrayList<MethodOpHandler>(unaryHandlers)) {
            m.initConvertionPermutations(this, unaryHandlers);
        }
        for(MethodOpHandler m: new ArrayList<MethodOpHandler>(binaryHandlers)) {
            m.initConvertionPermutations(this, binaryHandlers);
        }
    }

    private void initTermMethod(Method m) {
        String id = m.getAnnotation(Term.class).value();
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType(), null, null);
        h.initTermArguments();
        Class<?> target = m.getReturnType();
        if (termHandlers.containsKey(typedId(id, target))) {
            throw new IllegalArgumentException("Ambiguous term '" + id + "' handler for type " + target.getSimpleName());
        }
        termHandlers.put(typedId(id, target), h);
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
        
        MethodArgConvertor cvt = new MethodArgConvertor(h);
        convertors.add(cvt);
    }
    
    private void initUnaryMethod(Method m) {
        String id = m.getAnnotation(Unary.class).value();
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initUnaryArgumnets();

        unaryHandlers.add(h);
    }

    private void initBinaryMethod(Method m) {
        String id = m.getAnnotation(Binary.class).value();
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initBinaryArguments();

        binaryHandlers.add(h);
    }
    
    protected void addConversions(List<Class<?>> conversions, Class<?> target) {
        for(ArgConvertor cvt: convertors) {
            if (target.isAssignableFrom(cvt.toType())) {
                conversions.add(cvt.fromType());
            }
        }
    }
    
    protected ArgConvertor getConvertor(Class<?> fromType, Class<?> toType) {
        for(ArgConvertor cvt: convertors) {
            if (cvt.fromType().isAssignableFrom(fromType) && toType.isAssignableFrom(cvt.toType())) {
                return cvt;
            }
        }
        return null;
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
    protected @interface Convertible {
        Class<?>[] value() default {};
    }
    
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Context {
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Source {
    }
    
    private static class MethodOpHandler implements TermActionHandler<Object, Object>, UnariActionHandler<Object, Object, Object>, BinaryActionHandler<Object, Object, Object, Object> {
        
        private final ReflectionActionHandler<?> host;
        private final String id;
        private final Class<Object> returnType;
        private Class<Object> leftType;
        private Class<Object> rightType;
        
        private final Method method;
        private final ArgConvertor leftConvertor;
        private final ArgConvertor rightConvertor;
        
        private int contextArg = -1;
        private int tokenArg = -1;
        private int tokenBodyArg = -1;
        private int leftArg = -1;
        private int rightArg = -1;
        
        public MethodOpHandler(ReflectionActionHandler<?> host, String id, Method m, Class<?> returnType) {
            this(host, id, m, returnType, null, null);
        }
        
        protected MethodOpHandler(MethodOpHandler proto, ArgConvertor leftCnv, ArgConvertor rightCnv) {
            this(proto.host, proto.id, proto.method, proto.returnType, leftCnv, rightCnv);
            this.contextArg = proto.contextArg;
            this.tokenArg = proto.tokenArg;
            this.tokenBodyArg = proto.tokenBodyArg;
            this.leftArg = proto.leftArg;
            this.leftType = proto.leftType;
            this.rightArg = proto.rightArg;
            this.rightType = proto.rightType;            
        }
        
        @SuppressWarnings("unchecked")
        protected MethodOpHandler(ReflectionActionHandler<?> host, String id, Method m, Class<?> returnType, ArgConvertor leftCnv, ArgConvertor rightCnv) {
            this.host = host;
            this.id = id;
            this.method = m;
            this.returnType = (Class<Object>) returnType;
            
            this.leftConvertor = leftCnv;
            this.rightConvertor = rightCnv;

            this.method.setAccessible(true);
        }

        @Override
        public Class<Object> argType() {
            return leftType();
        }

        @Override
        public Class<Object> returnType() {
            return returnType;
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
            throw e;
        }
        
        public void initTermArguments() {
            Class<?>[] paramTypes = method.getParameterTypes();
            Annotation[][] paramAnns = method.getParameterAnnotations();
            
            for(int i = 0; i != paramTypes.length; ++i) {
                if (isConvertibleAnnotated(paramAnns[i])) {
                    throw methodError("Term method '" + method.getName() + "' may not be annotated with @Convertible");
                }
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                    if (isTokenAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Context and @Source");
                    }
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
                if (isConvertibleAnnotated(paramAnns[i])) {
                    throw methodError("Conversion method '" + method.getName() + "' may not be annotated with @Convertible");
                }
                if (isContextAnnotated(paramAnns[i])) {
                    if (contextArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Context");
                    }
                    contextArg = i;
                    if (isTokenAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Context and @Source");                        
                    }
                    if (isConvertibleAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Context and @Convertible");                        
                    }
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Source.class) {
                        throw methodError("Method '" + method.getName() + "' argument annotated with @Source should have type Token");
                    }
                    if (isConvertibleAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Source and @Convertible");                        
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
                    if (isTokenAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Context and @Source");                        
                    }
                    if (isConvertibleAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Context and @Convertible");                        
                    }
                }
                else if (isTokenAnnotated(paramAnns[i])) {
                    if (tokenArg >= 0) {
                        throw methodError("Method '" + method.getName() + "' only one argument can be annotated with @Source");
                    }
                    tokenArg = i;    
                    if (paramTypes[i] != Token.class) {
                        throw methodError("Method '" + method.getName() + "' argument annotated with @Source should have type Token");
                    }
                    if (isConvertibleAnnotated(paramAnns[i])) {
                        throw methodError("Method '" + method.getName() + "' may not be annotated with @Source and @Convertible");                        
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

        public void initConvertionPermutations(ReflectionActionHandler<?> host, List<MethodOpHandler> result) {
            List<Class<?>> left = new ArrayList<Class<?>>();
            List<Class<?>> right = new ArrayList<Class<?>>();
            left.add(null);
            right.add(null);
            Convertible lc = getConvertibleAnnotation(method.getParameterAnnotations()[leftArg]);
            Convertible rc = rightArg < 0 ? null : getConvertibleAnnotation(method.getParameterAnnotations()[rightArg]);
            if (lc != null) {
                if (lc.value().length == 0) {
                    host.addConversions(left, leftType);
                }
                else {
                    left.addAll(Arrays.asList(lc.value()));
                }
            }
            if (rc != null) {
                if (rc.value().length == 0) {
                    host.addConversions(right, rightType);
                }
                else {
                    right.addAll(Arrays.asList(rc.value()));
                }
            }
            
            for(Class<?> lt: left) {
                for(Class<?> rt: right) {
                    if (lt == null && rt == null) {
                        // skip
                    }
                    else {
                        ArgConvertor leftCvt = null;
                        if (lt != null) {
                            leftCvt = host.getConvertor(lt, leftType);
                            if (leftCvt == null) {
                                throw methodError("Method '" + method.getName() + "' No conversion found " + lt.getSimpleName() + " -> " + leftType.getSimpleName());
                            }
                        }
                        ArgConvertor rightCvt = null;
                        if (rt != null) {
                            rightCvt = host.getConvertor(rt, rightType);
                            if (rightCvt == null) {
                                throw methodError("Method '" + method.getName() + "' No conversion found " + rt.getSimpleName() + " -> " + rightType.getSimpleName());
                            }
                        }
                        MethodOpHandler clone = new MethodOpHandler(this, leftCvt, rightCvt);
                        result.add(clone);
                    }
                }
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

        private boolean isConvertibleAnnotated(Annotation[] annotations) {
            for(Annotation a: annotations) {
                if (a instanceof Convertible) {
                    return true;
                }
            }
            return false;
        }
        
        private Convertible getConvertibleAnnotation(Annotation[] annotations) {
            for(Annotation a: annotations) {
                if (a instanceof Convertible) {
                    return (Convertible) a;
                }
            }
            return null;            
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public Class<Object> leftType() {
            return (Class<Object>) (leftConvertor == null ? leftType : leftConvertor.fromType());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<Object> rightType() {
            return (Class<Object>) (rightConvertor == null ? rightType : rightConvertor.fromType());
        }

        @Override
        public Object apply(Object parserContext, Token token) {
            return apply(parserContext, token, null, null);
        }
        
        @Override
        public Object apply(Object parserContext, Token token, Object arg) {
            return apply(parserContext, token, arg, null);
        }
        
        @Override
        public Object apply(Object parserContext, Token token, Object inleft, Object inright) {
            try {
                Object left = leftConvertor != null ? leftConvertor.convert(parserContext, token, inleft) : inleft;
                Object right = rightConvertor != null ? rightConvertor.convert(parserContext, token, inright) : inright;
                
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
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(id).append('[');
            if (leftType != null) {
                sb.append(leftType().getSimpleName());
            }
            if (rightType != null) {
                sb.append(',').append(rightType().getSimpleName());                
            }
            sb.append("] -> ").append(method.getName());
            return sb.toString();
        }
    }
    
    interface ArgConvertor {
        
        public Class<?> fromType();

        public Class<?> toType();
        
        public Object convert(Object parserContext, Token token, Object value);
    }
    
    class MethodArgConvertor implements ArgConvertor {
     
        private final MethodOpHandler handler;

        public MethodArgConvertor(MethodOpHandler handler) {
            super();
            this.handler = handler;
        }
        
        @Override
        public Class<?> fromType() {
            return handler.leftType;
        }

        @Override
        public Class<?> toType() {
            return handler.returnType;
        }

        @Override
        public Object convert(Object parserContext, Token token, Object value) {
            return handler.apply(parserContext, token, value);
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
    
    /**
     * Utility method
     */
    protected static <T> T[] append(T[] a, T... b) {
        T[] r = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;       
    }

    /**
     * Utility method
     */
    protected static <T> T[] append(T[] a, T b) {
        T[] r = Arrays.copyOf(a, a.length + 1);
        r[a.length] = b;
        return r;       
    }
}

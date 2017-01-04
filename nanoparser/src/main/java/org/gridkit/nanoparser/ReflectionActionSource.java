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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nanoparser.SemanticActionHandler.BinaryActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.TermActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.UnaryActionHandler;

public abstract class ReflectionActionSource<C> implements SematicActionSource<C> {

	protected final Map<TypedId, TermAction<C>> terms = new HashMap<TypedId, TermAction<C>>();
	protected final List<Converter<C>> convertors = new ArrayList<Converter<C>>();
	protected final List<UnaryAction<C>> unaries = new ArrayList<UnaryAction<C>>();
	protected final List<BinaryAction<C>> binaries = new ArrayList<BinaryAction<C>>();
	    
    public ReflectionActionSource() {
        initMethodTables();
    }

    @Override
	public Collection<TermAction<C>> enumTerms() {
		return terms.values();
	}

	@Override
	public Collection<UnaryAction<C>> enumUnaries() {
		return unaries;
	}

	@Override
	public Collection<BinaryAction<C>> enumBinaries() {
		return binaries;
	}

	@Override
	public Collection<Converter<C>> enumConverters() {
		return convertors;
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
        terms.put(typedId(id, target), new TAction<C>(h));
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
        
        convertors.add(new CAction<C>(h));
    }
    
    private void initUnaryMethod(Method m) {
        String id = m.getAnnotation(Unary.class).value();
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException("@Unary method '" + m.getName() + "' should have reference return type");
        }
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initUnaryArgumnets();

        unaries.add(new UAction<C>(h));
    }

    private void initBinaryMethod(Method m) {
        String id = m.getAnnotation(Binary.class).value();
        MethodOpHandler h = new MethodOpHandler(this, id, m, m.getReturnType());
        h.initBinaryArguments();

        binaries.add(new BAction<C>(h));
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

    /**
     * Mark parameter to receive parse token.
     * Action may have multiple parameters marked as source if multitoken is expected.
     * 
     * @author Alexey Ragozin (alexey.ragozin@gmail.com)
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Source {
    }

    private static class TAction<C> implements TermAction<C> {

    	final MethodOpHandler handler;
    	
		public TAction(MethodOpHandler handler) {
			this.handler = handler;
		}

		@Override
		public String opId() {
			return handler.id;
		}

		@Override
		public Class<?> returnType() {
			return handler.returnType;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R> TermActionHandler<C, R> handler() {
			return (TermActionHandler<C, R>) handler;
		}
    }

    private static class CAction<C> implements Converter<C> {

    	final MethodOpHandler handle;
    	
		public CAction(MethodOpHandler handle) {
			this.handle = handle;
		}

		@Override
		public Class<?> returnType() {
			return handle.returnType();
		}

		@Override
		public Class<?> inputType() {
			return handle.leftType;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R, A> UnaryActionHandler<C, R, A> handler() {
			return (UnaryActionHandler<C, R, A>) handle;
		}
    }
    
    private static class UAction<C> implements UnaryAction<C> {

    	final MethodOpHandler handle;
    	
		public UAction(MethodOpHandler handle) {
			this.handle = handle;
		}

		@Override
		public String opId() {
			return handle.id;
		}

		@Override
		public Class<?> returnType() {
			return handle.returnType;
		}

		@Override
		public Class<?> argType() {
			return handle.leftType;
		}

		@Override
		public Collection<Class<?>> convertibleArgTypes() {
			return handle.leftConvertor;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R, A> UnaryActionHandler<C, R, A> handler() {
			return (UnaryActionHandler<C, R, A>) handle;
		}
    }

    private static class BAction<C> implements BinaryAction<C> {

    	final MethodOpHandler handle;

		public BAction(MethodOpHandler handle) {
			this.handle = handle;
		}

		@Override
		public String opId() {
			return handle.id;
		}

		@Override
		public Class<?> returnType() {
			return handle.returnType;
		}

		@Override
		public Class<?> leftType() {
			return handle.leftType;
		}

		@Override
		public Class<?> rightType() {
			return handle.rightType;
		}

		@Override
		public Collection<Class<?>> convertibleLeftTypes() {
			return handle.leftConvertor;
		}

		@Override
		public Collection<Class<?>> convertibleRightTypes() {
			return handle.rightConvertor;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <R, A, B> BinaryActionHandler<C, R, A, B> handler() {
			return (BinaryActionHandler<C, R, A, B>) handle;
		}
    }
    
    private static class MethodOpHandler implements TermActionHandler<Object, Object>, UnaryActionHandler<Object, Object, Object>, BinaryActionHandler<Object, Object, Object, Object> {
        
        private final ReflectionActionSource<?> host;
        private final String id;
        private final Class<Object> returnType;
        private Class<Object> leftType;
        private Class<Object> rightType;
        
        private final Method method;
        private Collection<Class<?>> leftConvertor = Collections.emptySet();
        private Collection<Class<?>> rightConvertor = Collections.emptySet();
        
        private int contextArg = -1;
        private int[] tokenArg = {};
        private int tokenBodyArg = -1;
        private int leftArg = -1;
        private int rightArg = -1;
        
        @SuppressWarnings("unchecked")
        protected MethodOpHandler(ReflectionActionSource<?> host, String id, Method m, Class<?> returnType) {
            this.host = host;
            this.id = id;
            this.method = m;
            this.returnType = (Class<Object>) returnType;
            
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
                    tokenArg = Arrays.copyOf(tokenArg, tokenArg.length + 1);
                    tokenArg[tokenArg.length - 1] = i;
                    if (paramTypes[i] != Token.class) {
                        throw methodError("Method '" + method.getName() + "' parameter annotated @Source should have type Token");
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
                    tokenArg = Arrays.copyOf(tokenArg, tokenArg.length + 1);
                    tokenArg[tokenArg.length - 1] = i;
                    if (paramTypes[i] != Token.class) {
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
                    tokenArg = Arrays.copyOf(tokenArg, tokenArg.length + 1);
                    tokenArg[tokenArg.length - 1] = i;
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

            leftConvertor = initConverters(leftArg);
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
                    tokenArg = Arrays.copyOf(tokenArg, tokenArg.length + 1);
                    tokenArg[tokenArg.length - 1] = i;
                    if (paramTypes[i] != Token.class) {
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

            leftConvertor = initConverters(leftArg);
            rightConvertor = initConverters(rightArg);
        }
        
        private Collection<Class<?>> initConverters(int arg) {
			Convertible cc = getConvertibleAnnotation(method.getParameterAnnotations()[arg]);
        	if (cc == null) {
        		return Collections.<Class<?>>singleton(method.getParameterTypes()[arg]);
        	}
        	else {
        		if (cc.value().length == 0) {
        			return null; // special case
        		}
        		else {
	        		Class<?>[] set = new Class<?>[cc.value().length + 1];
	        		set[0] = method.getParameterTypes()[arg];
	        		int n = 1;
	        		for(Class<?> c: cc.value()) {
	        			set[n++] = c;
	        		}
	        		return Arrays.asList(set);
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
        public Class<Object> leftType() {
            return leftType;
        }

        @Override
        public Class<Object> rightType() {
            return rightType;
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
                Object left = inleft;
                Object right = inright;
                
                Object[] args = new Object[method.getParameterTypes().length];
                if (contextArg >= 0) {
                    args[contextArg] = parserContext;
                }
                applyToken(token, args);
                if (tokenBodyArg >= 0) {
                    args[tokenBodyArg] = token.tokenBody();
                }
                if (leftArg >= 0) {
                    args[leftArg] = left;
                }
                if (rightArg >= 0) {
                    args[rightArg] = right;
                }
                return postProcess(method.invoke(host, args), token);
            } catch (InvocationTargetException e) {
                throw throwUnchecked(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        
        private Object postProcess(Object v, Token token) {
        	if (v instanceof TokenAware) {
        		((TokenAware) v).setToken(token);
        	}
        	return v;
        }
        
        private void applyToken(Token tkn, Object[] args) {
        	if (tokenArg.length == 1) {
        		args[tokenArg[0]] = tkn;
        	}
        	else if (tokenArg.length > 1) {
        		if (tkn instanceof MultiToken) {
        			Token[] tkns = ((MultiToken) tkn).tokens();
        			for(int i = 0; i != args.length; ++i) {
        				if (i < tkns.length) {
        					args[tokenArg[i]] = tkns[i];
        				}
        				else {
        					args[tokenArg[i]] = null;
        				}
        			}
        		}
        		else {
        			args[tokenArg[0]] = tkn;
        			for(int i = 1; i < args.length; ++i) {
        				args[tokenArg[i]] = null;
        			}        			
        		}
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
        ReflectionActionSource.<RuntimeException>throwAny(e);
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

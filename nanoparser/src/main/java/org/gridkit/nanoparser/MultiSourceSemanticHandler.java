package org.gridkit.nanoparser;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nanoparser.SematicActionSource.BinaryAction;
import org.gridkit.nanoparser.SematicActionSource.Converter;
import org.gridkit.nanoparser.SematicActionSource.TermAction;
import org.gridkit.nanoparser.SematicActionSource.UnaryAction;

public class MultiSourceSemanticHandler<C> implements SemanticActionHandler<C> {

	private final SematicActionSource<C>[] actionSources;
	private final Map<TermKey, TermActionHandler<?, ?>[]> termHandlerCache = new HashMap<TermKey, TermActionHandler<?,?>[]>();  
	private final Map<UnaryKey, UnaryActionHandler<?, ?, ?>[]> unaryHandlerCache = new HashMap<UnaryKey, UnaryActionHandler<?,?,?>[]>();  
	private final Map<BinaryKey, BinaryActionHandler<?, ?, ?, ?>[]> binaryHandlerCache = new HashMap<BinaryKey, BinaryActionHandler<?,?,?,?>[]>();  
	
	private final Map<String, List<UnaryActionHandler<?, ?, ?>>> unaryUniverse = new HashMap<String, List<UnaryActionHandler<?,?,?>>>();
	private final Map<String, List<BinaryActionHandler<?, ?, ?, ?>>> binaryUniverse = new HashMap<String, List<BinaryActionHandler<?,?,?,?>>>();
	
	
	public MultiSourceSemanticHandler(SematicActionSource<C>... actionSources) {
		this.actionSources = actionSources;
		init();
	}

	private void init() {
		List<Converter<C>> converters = new ArrayList<SematicActionSource.Converter<C>>();
		for(SematicActionSource<C> sas: actionSources) {
			converters.addAll(sas.enumConverters());
		}
		initUniaryActions(converters);
		initBinaryActions(converters);		
	}

	private void initUniaryActions(List<Converter<C>> converters) {
		for(SematicActionSource<C> sas: actionSources) {
			for(UnaryAction<C> ua: sas.enumUnaries()) {
				addAction(ua.opId(), ua.handler());
			}
		}

		for(SematicActionSource<C> sas: actionSources) {
			for(UnaryAction<C> ua: sas.enumUnaries()) {
				processActionVariants(converters, ua);
			}
		}		
	}

	private void initBinaryActions(List<Converter<C>> converters) {
		for(SematicActionSource<C> sas: actionSources) {
			for(BinaryAction<C> ua: sas.enumBinaries()) {
				addAction(ua.opId(), ua.handler());
			}
		}

		for(SematicActionSource<C> sas: actionSources) {
			for(BinaryAction<C> ua: sas.enumBinaries()) {
				processActionVariants(converters, ua);
			}
		}		
	}
	
	private void processActionVariants(List<Converter<C>> converters, UnaryAction<C> ua) {
		if (ua.convertibleArgTypes() == null) {
			// default convertible
			for(Converter<C> c: converters) {
				if (ua.argType().isAssignableFrom(c.returnType())) {
					addAction(ua.opId(), convertedHandler(ua, c));
				}
			}
			if (ua.argType().isArray()) {
				addAction(ua.opId(), convertedHandler(ua, arrayConverter(ua.argType())));
			}
		}
		else {
			typeCycle:
			for(Class<?> c: ua.convertibleArgTypes()) {
				if (!ua.argType().isAssignableFrom(c)) {
					// ignoring default type binding
					for(Converter<C> cc: converters) {
						if (ua.argType().isAssignableFrom(cc.returnType()) && c.isAssignableFrom(cc.inputType())) {
							addAction(ua.opId(), convertedHandler(ua, cc));
							continue typeCycle;
						}
					}
					if (ua.argType().isArray() && ua.argType().getComponentType().isAssignableFrom(c)) {
						// auto array converter
						addAction(ua.opId(), convertedHandler(ua, arrayConverter(ua.argType())));
					}
				}
			}
		}		
	}

	private void processActionVariants(List<Converter<C>> converters, BinaryAction<C> ba) {
		processActionVariants(converters, ba, null);
		
		if (ba.convertibleLeftTypes() == null) {
			// default convertible
			for(Converter<C> c: converters) {
				if (ba.leftType().isAssignableFrom(c.returnType())) {
					processActionVariants(converters, ba, c);
				}
			}
			if (ba.leftType().isArray()) {
				processActionVariants(converters, ba, arrayConverter(ba.leftType()));
			}
		}
		else {
			typeCycle:
			for(Class<?> c: ba.convertibleLeftTypes()) {
				if (!ba.leftType().isAssignableFrom(c)) {
					// ignoring default type binding
					for(Converter<C> cc: converters) {
						if (ba.leftType().isAssignableFrom(cc.returnType()) && c.isAssignableFrom(cc.inputType())) {
							processActionVariants(converters, ba, cc);
							continue typeCycle;
						}
					}
					if (ba.leftType().isArray() && ba.leftType().getComponentType().isAssignableFrom(c)) {
						// auto array converter
						processActionVariants(converters, ba, arrayConverter(ba.leftType()));
					}
				}
			}
		}		
	}

	private void processActionVariants(List<Converter<C>> converters, BinaryAction<C> ba, Converter<C> ca) {
		if (ca != null) {
			addAction(ba.opId(), convertedHandler(ba, ca, null));
		}
		if (ba.convertibleRightTypes() == null) {
			// default convertible
			for(Converter<C> c: converters) {
				if (ba.rightType().isAssignableFrom(c.returnType())) {
					addAction(ba.opId(), convertedHandler(ba, ca, c));
				}
			}
			if (ba.rightType().isArray()) {
				addAction(ba.opId(), convertedHandler(ba, ca, arrayConverter(ba.rightType())));
			}
		}
		else {
			typeCycle:
			for(Class<?> c: ba.convertibleRightTypes()) {
				if (!ba.rightType().isAssignableFrom(c)) {
					// ignoring default type binding
					for(Converter<C> cc: converters) {
						if (ba.rightType().isAssignableFrom(cc.returnType()) && c.isAssignableFrom(cc.inputType())) {
							addAction(ba.opId(), convertedHandler(ba, ca, cc));
							continue typeCycle;
						}
					}
					if (ba.rightType().isArray() && ba.rightType().getComponentType().isAssignableFrom(c)) {
						// auto array converter
						addAction(ba.opId(), convertedHandler(ba, ca, arrayConverter(ba.rightType())));
					}
				}
			}
		}		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Converter<C> arrayConverter(Class<?> arrayType) {
		return new ArrayConverter(arrayType);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private UnaryActionHandler<C, ?, ?> convertedHandler(UnaryAction<C> ua, Converter<C> c) {
		return new UnaryConvertedHandler(ua.handler(), c.handler());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BinaryActionHandler<C, ?, ?, ?> convertedHandler(BinaryAction<C> ba, Converter<C> ca, Converter<C> cb) {
		return new BinaryConvertedHandler(ba.handler(), ca == null ? null : ca.handler(), cb == null ? null : cb.handler());
	}

	private void addAction(String opId, UnaryActionHandler<C, ?, ?> handler) {
		List<UnaryActionHandler<?, ?, ?>> list = unaryUniverse.get(opId);
		if (list == null) {
			unaryUniverse.put(opId, list = new ArrayList<UnaryActionHandler<?,?,?>>());
		}
		
		list.add(handler);
	}

	private void addAction(String opId, BinaryActionHandler<C, ?, ?, ?> handler) {
		List<BinaryActionHandler<?, ?, ?, ?>> list = binaryUniverse.get(opId);
		if (list == null) {
			binaryUniverse.put(opId, list = new ArrayList<BinaryActionHandler<?,?,?,?>>());
		}
		
		list.add(handler);
	}

	@Override
	public TermActionHandler<?, ?>[] enumTerm(String opId, Class<?> rType) {
		TermKey tkey = new TermKey(opId, rType);
		TermActionHandler<?, ?>[] result = termHandlerCache.get(tkey);
		if (result == null) {
			result = initTermEnum(opId, rType);
			termHandlerCache.put(tkey, result);
		}
		return result;
	}

	@Override
	public UnaryActionHandler<?, ?, ?>[] enumUnaries(String opId, Class<?> rType, Class<?> argType) {
		UnaryKey ukey = new UnaryKey(opId, rType, argType);
		UnaryActionHandler<?, ?, ?>[] result = unaryHandlerCache.get(ukey);
		if (result == null) {
			result = initUnaryEnum(opId, rType, argType);
			unaryHandlerCache.put(ukey, result);
		}
		return result;
	}

	@Override
	public BinaryActionHandler<?, ?, ?, ?>[] enumBinaries(String opId,	Class<?> rType, Class<?> leftType, Class<?> rightType) {
		BinaryKey bkey = new BinaryKey(opId, rType, leftType, rightType);
		BinaryActionHandler<?, ?, ?, ?>[] result = binaryHandlerCache.get(bkey);
		if (result == null) {
			result = initBinaryEnum(opId, rType, leftType, rightType);
			binaryHandlerCache.put(bkey, result);
		}
		return result;
	}

	protected TermActionHandler<?, ?>[] initTermEnum(String opId, Class<?> rType) {
		List<TermActionHandler<?, ?>> result = new ArrayList<TermActionHandler<?,?>>();
		
		for(SematicActionSource<C> ss: actionSources) {
			for(TermAction<C> ta: ss.enumTerms()) {
				if (ta.opId().equals(opId)) {
					if (rType == null || rType.isAssignableFrom(ta.returnType())) {
						result.add(ta.handler());
					}
				}
			}
		}
		
		return result.toArray(new TermActionHandler<?, ?>[0]);
	}

	protected UnaryActionHandler<?, ?, ?>[] initUnaryEnum(String opId, Class<?> rType, Class<?> argType) {
		List<UnaryActionHandler<?, ?, ?>> result = new ArrayList<UnaryActionHandler<?, ?, ?>>();
		
		if (unaryUniverse.containsKey(opId)) {
			for(UnaryActionHandler<?, ?, ?> h: unaryUniverse.get(opId)) {
				if (rType == null || rType.isAssignableFrom(h.returnType())) {
					if (argType == null || h.argType().isAssignableFrom(argType)) {
						result.add(h);
					}					
				}
			}
		}
		
		return result.toArray(new UnaryActionHandler<?, ?, ?>[0]);
	}

	protected BinaryActionHandler<?, ?, ?, ?>[] initBinaryEnum(String opId,	Class<?> rType, Class<?> leftType, Class<?> rightType) {
		List<BinaryActionHandler<?, ?, ?, ?>> result = new ArrayList<BinaryActionHandler<?, ?, ?, ?>>();

		if (binaryUniverse.containsKey(opId)) {
			for(BinaryActionHandler<?, ?, ?, ?> h: binaryUniverse.get(opId)) {
				if (rType == null || rType.isAssignableFrom(h.returnType())) {
					if (leftType == null || h.leftType().isAssignableFrom(leftType)) {
						if (rightType == null || h.returnType().isAssignableFrom(rightType)) {
							result.add(h);
						}
					}					
				}
			}
		}
		
		return result.toArray(new BinaryActionHandler<?, ?, ?, ?>[0]);
	}
	
	private static class TermKey {
		
		final String opId;
		final Class<?> returnType;
		
		public TermKey(String opId, Class<?> returnType) {
			this.opId = opId;
			this.returnType = returnType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((opId == null) ? 0 : opId.hashCode());
			result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
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
			TermKey other = (TermKey) obj;
			if (opId == null) {
				if (other.opId != null)
					return false;
			} else if (!opId.equals(other.opId))
				return false;
			if (returnType == null) {
				if (other.returnType != null)
					return false;
			} else if (!returnType.equals(other.returnType))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return opId + ":" + returnType.getSimpleName();
		}
	}
	
	private static class UnaryKey {
		
		final String opId;
		final Class<?> returnType;
		final Class<?> argType;
		
		public UnaryKey(String opId, Class<?> returnType, Class<?> argType) {
			this.opId = opId;
			this.returnType = returnType;
			this.argType = argType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((argType == null) ? 0 : argType.hashCode());
			result = prime * result + ((opId == null) ? 0 : opId.hashCode());
			result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
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
			UnaryKey other = (UnaryKey) obj;
			if (argType == null) {
				if (other.argType != null)
					return false;
			} else if (!argType.equals(other.argType))
				return false;
			if (opId == null) {
				if (other.opId != null)
					return false;
			} else if (!opId.equals(other.opId))
				return false;
			if (returnType == null) {
				if (other.returnType != null)
					return false;
			} else if (!returnType.equals(other.returnType))
				return false;
			return true;
		}
		
		@Override
		public String toString() {
			return opId + "(" + argType.getSimpleName() + "):" + returnType.getSimpleName();
		}		
	}
	
	private static class BinaryKey {
		
		final String opId;
		final Class<?> returnType;
		final Class<?> leftType;
		final Class<?> rightType;
		
		public BinaryKey(String opId, Class<?> returnType, Class<?> leftType, Class<?> rightType) {
			this.opId = opId;
			this.returnType = returnType;
			this.leftType = leftType;
			this.rightType = rightType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((leftType == null) ? 0 : leftType.hashCode());
			result = prime * result + ((opId == null) ? 0 : opId.hashCode());
			result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
			result = prime * result + ((rightType == null) ? 0 : rightType.hashCode());
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
			BinaryKey other = (BinaryKey) obj;
			if (leftType == null) {
				if (other.leftType != null)
					return false;
			} else if (!leftType.equals(other.leftType))
				return false;
			if (opId == null) {
				if (other.opId != null)
					return false;
			} else if (!opId.equals(other.opId))
				return false;
			if (returnType == null) {
				if (other.returnType != null)
					return false;
			} else if (!returnType.equals(other.returnType))
				return false;
			if (rightType == null) {
				if (other.rightType != null)
					return false;
			} else if (!rightType.equals(other.rightType))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return opId + "(" + leftType.getSimpleName() + ", " + rightType.getSimpleName() + "):" + returnType.getSimpleName();
		}		
	}

	private static class ArrayConverter<C, RA, R> implements UnaryActionHandler<C, RA, R>, Converter<C> {

		private final Class<R> arg;
		private final Class<RA> result;
		
		@SuppressWarnings("unchecked")
		public ArrayConverter(Class<RA> type) {
			Class<?> ca = (Class<R>) type.getComponentType();			
			this.arg = (Class<R>) (ca.isPrimitive() ? box(ca) : ca);
			this.result = type;
		}

		@Override
		public Class<?> inputType() {
			return inputType();
		}

		@Override
		@SuppressWarnings({ "unchecked", "hiding" })
		public <R, A> UnaryActionHandler<C, R, A> handler() {
			return ((UnaryActionHandler<C, R, A>)this);
		}

		@Override
		public Class<RA> returnType() {
			return result;
		}

		@Override
		public Class<R> argType() {
			return arg;
		}

		@Override
		@SuppressWarnings("unchecked")
		public RA apply(C parserContext, Token token, R arg) {
			RA rr = (RA) Array.newInstance(result.getComponentType(), 1);
			Array.set(rr, 0, arg);
			return rr;
		}
	}

	@SuppressWarnings("rawtypes")
	private static class UnaryConvertedHandler<C, R, A> implements UnaryActionHandler<C, R, A> {
		
		private final UnaryActionHandler op;
		private final UnaryActionHandler cvt;
		
		public UnaryConvertedHandler(UnaryActionHandler<C, ?, ?> op, UnaryActionHandler<C, ?, ?> cvt) {
			this.op = op;
			this.cvt = cvt;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<R> returnType() {
			return (Class<R>) op.returnType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<A> argType() {
			return (Class<A>) cvt.argType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public R apply(C parserContext, Token token, A arg) {
			Object x = cvt.apply(parserContext, token, arg);
			
			return (R) op.apply(parserContext, token, x);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class BinaryConvertedHandler<C, R, A, B> implements BinaryActionHandler<C, R, A, B> {
		
		private final BinaryActionHandler op;
		private final UnaryActionHandler cvtA;
		private final UnaryActionHandler cvtB;
		
		public BinaryConvertedHandler(BinaryActionHandler<C, ?, ?, ?> op, UnaryActionHandler<C, ?, ?> cvtA, UnaryActionHandler<C, ?, ?> cvtB) {
			this.op = op;
			this.cvtA = cvtA;
			this.cvtB = cvtB;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<R> returnType() {
			return (Class<R>) op.returnType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<A> leftType() {
			return cvtA == null ? op.leftType() : cvtA.argType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Class<B> rightType() {
			return cvtB == null ? op.rightType() :cvtB.argType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public R apply(C parserContext, Token token, A a, B b) {
			Object xa = cvtA == null ? a : cvtA.apply(parserContext, token, a);
			Object xb = cvtB == null ? b : cvtB.apply(parserContext, token, b);
			
			return (R) op.apply(parserContext, token, xa, xb);
		}
	}
	
	private static Class<?> box(Class<?> c) {
		if (c == boolean.class) {
			return Boolean.class;
		}
		else if (c == byte.class) {
			return Byte.class;
		}
		else if (c == short.class) {
			return Short.class;
		}
		else if (c == int.class) {
			return Integer.class;
		}
		else if (c == long.class) {
			return Long.class;
		}
		else if (c == float.class) {
			return Float.class;
		}
		else if (c == double.class) {
			return Double.class;
		}
		else {
			throw new RuntimeException("Unknown primitive type: " + c.getName());
		}
	}
}

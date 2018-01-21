package org.gridkit.nanoparser;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gridkit.nanoparser.SemanticActionHandler.BinaryActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.TermActionHandler;
import org.gridkit.nanoparser.SemanticActionHandler.UnaryActionHandler;
import org.gridkit.nanoparser.SematicActionSource.BinaryAction;
import org.gridkit.nanoparser.SematicActionSource.Converter;
import org.gridkit.nanoparser.SematicActionSource.TermAction;
import org.gridkit.nanoparser.SematicActionSource.UnaryAction;

class SemanticActionSolver {

	private final TypeSet typeUniverse;
	
	public SemanticActionSolver(SematicActionSource<?> source) {
		this.typeUniverse = new TypeSet(enumTypes(source));
	}

	public SemanticActionSolver(SemanticActionHandler<?> source) {
		this.typeUniverse = new TypeSet(enumTypes(source));
	}

	public TypeSet typeUniverse() {
		return setOf(typeUniverse.universe);
	}
	
	public TypeSet setOf(Class<?>... types) {
		TypeSet ts = new TypeSet(typeUniverse);
		for(Class<?> t: types) {
			boolean known = false;
			for(Class<?> nc: typeUniverse.universe) {
				if (t.isAssignableFrom(nc)) {
					ts.add(nc);
					known = true;
				}
			}
			if (!known) {
				throw new IllegalArgumentException("Unknown class: " + t.getName());
			}
		}
		return ts; 
	}
	
	private Collection<Class<?>> enumTypes(SematicActionSource<?> source) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		set.add(String.class);
		for(TermAction<?> ta: source.enumTerms()) {
			set.add(ta.returnType());
		}
		for(Converter<?> c: source.enumConverters()) {
			set.add(c.inputType());
			set.add(c.returnType());
		}
		for(UnaryAction<?> c: source.enumUnaries()) {
			set.add(c.argType());
			set.add(c.returnType());
		}
		for(BinaryAction<?> c: source.enumBinaries()) {
			set.add(c.leftType());
			set.add(c.rightType());
			set.add(c.returnType());
		}
		return set;
	}

	private Collection<Class<?>> enumTypes(SemanticActionHandler<?> source) {
		Set<Class<?>> set = new HashSet<Class<?>>();
		set.add(String.class);
		for(TermActionHandler<?, ?> ta: source.enumTerm(null, null)) {
			set.add(ta.returnType());
		}
		for(UnaryActionHandler<?, ?, ?> c: source.enumUnaries(null, null, null)) {
			set.add(c.argType());
			set.add(c.returnType());
		}
		for(BinaryActionHandler<?, ?, ?, ?> c: source.enumBinaries(null, null, null, null)) {
			set.add(c.leftType());
			set.add(c.rightType());
			set.add(c.returnType());
		}
		return set;
	}



	static class TypeSet {
		
		final Map<Class<?>, Integer> cindex;
		final Class<?>[] universe;		
		final int[][] relations;
		final boolean[] terminal;
		final boolean[] content;
		
		public TypeSet(Collection<Class<?>> classes) {
			cindex = new HashMap<Class<?>, Integer>();
			universe = classes.toArray(new Class<?>[classes.size()]);
			terminal = new boolean[universe.length];
			Arrays.fill(terminal, true);
			relations = new int[universe.length][];
			content = new boolean[universe.length];
			for(int i = 0; i != universe.length; ++i) {
				cindex.put(universe[i], i);
			}
			for(int i = 0; i != universe.length; ++i) {
				for(int j = 0; j != universe.length; ++j) {
					if (i != j) {
						if (universe[i].isAssignableFrom(universe[j])) {
							addRel(i, j);
							terminal[i] = false;
						}
					}
				}
			}
		}
		
		private void addRel(int sup, int sub) {
			int[] rel = relations[sup];
			if (rel == null) {
				relations[sup] = new int[]{sub};
			}
			else {
				rel = Arrays.copyOf(rel, rel.length + 1);
				rel[rel.length - 1] = sub;
				relations[sup] = rel;
			}			
		}

		public TypeSet(TypeSet other) {
			this.universe = other.universe;
			this.cindex = other.cindex;
			this.relations = other.relations;
			this.terminal = other.terminal;
			this.content = Arrays.copyOf(other.content, other.content.length);
		}
		
		public void clear() {
			Arrays.fill(content, false);
		}
		
		public boolean isEmpty() {
			for(boolean c: content) {
				if (c) {
					return false;
				}
			}
			return true;
		}
		
		public boolean contains(Class<?> type) {
			Integer c = cindex.get(type);
			if (c == null) {
				throw new IllegalArgumentException("Unenumerated type: " + type.getName());
			}
			if (content[c]) {
				return true;
			}
			if (relations[c] != null) {
				for(int s: relations[c]) {
					if (content[s]) {
						return true;
					}
				}
			}
			return false;
		}
		
		public void add(Class<?> type) {
			Integer c = cindex.get(type);
			if (c == null) {
				throw new IllegalArgumentException("Unenumerated type: " + type.getName());
			}
			content[c] = true;
			if (relations[c] != null) {
				for(int p: relations[c]) {
					content[p] = true;
				}
			}
		}
		
		public Iterable<Class<?>> types() {
			return new Iterable<Class<?>>() {
				
				@Override
				public Iterator<Class<?>> iterator() {
					return new It();
				}
			};
		}
		
		private class It implements Iterator<Class<?>> {

			int p = 0;
			
			@Override
			public boolean hasNext() {
				while(p < universe.length) {
					if (content[p]) {
						return true;
					}
					++p;
				}
				return false;
			}

			@Override
			public Class<?> next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				int c = p;
				++p;
				return universe[c];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		public void addAll(TypeSet other) {
			for(int i = 0; i != universe.length; ++i) {
				if (other.content[i]) {
					content[i] = true;
				}
			}			
		}
		
		@Override
		public String toString() {
			if (isEmpty()) {
				return "[]";
			}
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			for(Class<?> c: types()) {
				sb.append(c.getSimpleName());
				sb.append(", ");
			}
			sb.setLength(sb.length() - 2);
			sb.append("]");
			return sb.toString();
		}
	}
}

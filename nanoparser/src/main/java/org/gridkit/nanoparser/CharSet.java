package org.gridkit.nanoparser;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class CharSet implements Iterable<Byte> {

	public static final CharSet ALL = new CharSet(-1l, -1l);
	
	private final long bitsA;
	private final long bitsB;
	
	public CharSet() {
		this(0, 0);
	}

	private CharSet(long a, long b) {
		this.bitsA = a;
		this.bitsB = b;
	}
	
	public CharSet(char... chars) {
		long a = 0;
		long b = 0;
		for(char ch: chars) {
			int n = CharUtils.classify(ch);
			if (n < 64) {
				a |= (1l << n);
			}
			else {				
				b |= (1l << n);
			}
		}
		bitsA = a;
		bitsB = b;
	}
	
	public boolean bit(int n) {
		if (n < 64) {
			return (bitsA & (1l << n)) != 0;
		}
		else {
			return (bitsB & (1l << (n - 64))) != 0;
		}
	}
	
	public CharSet union(CharSet that) {
		long a = this.bitsA | that.bitsA;
		long b = this.bitsB | that.bitsB;
		return new CharSet(a, b);
	}
	
	public boolean matchChar(char c) {
		return bit(CharUtils.classify(c));
	}

	@Override
	public Iterator<Byte> iterator() {
		return new It(bitsA, bitsB);
	}
	
	private static class It implements Iterator<Byte> {

		private final long bitsA;
		private final long bitsB;
		private int n = 0;

		public It(long bitsA, long bitsB) {
			this.bitsA = bitsA;
			this.bitsB = bitsB;
		}

		private boolean bit(int n) {
			if (n < 64) {
				return (bitsA & (1l << n)) != 0;
			}
			else {
				return (bitsB & (1l << (n - 64))) != 0;
			}
		}

		
		@Override
		public boolean hasNext() {
			while (n < 128 ) {
				if (bit(n)) {
					return true;
				}
				++n;
			}
			return false;
		}

		@Override
		public Byte next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return (byte)(n++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}

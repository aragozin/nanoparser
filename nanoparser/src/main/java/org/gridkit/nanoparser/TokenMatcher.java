package org.gridkit.nanoparser;

public interface TokenMatcher {

	public CharSet firstCharacter();
	
	/**
	 * Try to match token in stream.
	 * @return -1 if not matched, else matched token length
	 */
	public int match(CharSequence cs, int offset);
}

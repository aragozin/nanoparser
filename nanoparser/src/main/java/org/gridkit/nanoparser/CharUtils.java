package org.gridkit.nanoparser;

public class CharUtils {

	public static final char C_CTRL = 1;
	public static final char C_WHITESPACE = 2;
	public static final char C_NON_ASCII = 16;
	
	public static char classify(char ch) {
		if (Character.isWhitespace(ch)) {
			return C_WHITESPACE;
		}
		else if (ch < ' ') {
			return C_CTRL;
		}
		else if (ch < 128) {
			return ch;
		}
		else {
			return C_NON_ASCII;
		}
	}	
}

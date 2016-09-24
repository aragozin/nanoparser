package org.gridkit.nanoparser.rulegrammar;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import org.gridkit.nanoparser.rulegrammar.AST.Lit;
import org.gridkit.nanoparser.rulegrammar.AST.Var;
import org.junit.Assert;

public class ParserTestHelper extends TestDataInline {

    public String getParseString() {
        String[] lines = textLines();
        int n = 0;
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() != 0) {
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() == 0) {
                break;
            }
            sb.append(lines[n]).append('\n');
        }
        return sb.toString();        
    }
    
    public String getAstString() {
        String[] lines = textLines();
        int n = 0;
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() != 0) {
                break;
            }
        }
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() == 0) {
                ++n;
                break;
            }
        }
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() != 0) {
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(; n < lines.length; ++n) {
            if (lines[n].trim().length() == 0) {
                break;
            }
            sb.append(lines[n]).append('\n');
        }
        return sb.toString();        
    }
    
    public void verifyAST(Object[] ast) {
        try {
            Assert.assertEquals(fixIndent(getAstString().trim(), 2), formatMultiline(ast).trim());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }    
    
    public String format(Object obj) throws IllegalArgumentException, IllegalAccessException {        
        StringBuilder sb = new StringBuilder();
        format(obj, sb, false);
        return sb.toString();
    }

    public String formatMultiline(Object[] objects) throws IllegalArgumentException, IllegalAccessException {        
        StringBuilder sb = new StringBuilder();
        for(Object obj: objects) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            format(obj, sb, true);
        }
        return fixIndent(sb.toString(), 2);
    }

    private String fixIndent(String format, int indent) {
        StringBuilder sb = new StringBuilder();
        int level = 0;
        boolean start = true;
        
        for(int i = 0; i != format.length(); ++i) {
            char ch  = format.charAt(i);
            if (ch == '\t') {
                ch = ' ';
            }
            if (ch == ' ') {
                if (start) {
                    continue;
                }
            }
            if (ch == '{' || ch == '[') {
                level += indent;
            }
            if (ch == '}' || ch == ']') {
                if (start) {
                    sb.setLength(sb.length() - indent);
                }
                level -= indent;
            }
            start = false;
            if (ch == '\n') {
                // remove trailing spaces
                while(sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
                    sb.setLength(sb.length() - 1);
                }
                sb.append(ch);
                indent(sb, level);
                start = true;
            }
            else {
                sb.append(ch);
            }
        }
        
        return sb.toString();        
    }

    private void indent(StringBuilder sb, int n) {
        for(int i = 0; i != n; ++i) {
            sb.append(' ');
        }
    }
    
    private void format(Object obj, StringBuilder sb, boolean multiLine) throws IllegalArgumentException, IllegalAccessException {
        if (obj instanceof Lit) {
            sb.append(((Lit) obj).body);
        }
        else if (obj instanceof Var) {
            sb.append(((Var) obj).body);
        }
        else if (obj instanceof String) {
            sb.append(obj);
        }
        else if (obj instanceof Boolean) {
            sb.append(obj);
        }
        else if (obj instanceof Number) {
            sb.append(obj);
        }
        else if (obj.getClass().isArray()) {
            int l = Array.getLength(obj);
            if (l == 0) {
                sb.append("[]");
            }
            else {
                sb.append("[");
                if (multiLine) {
                    sb.append("\n");
                }
                boolean fisrt = true;
                for(int i = 0; i != l; ++i) {
                    if (!fisrt) {
                        if (multiLine) {
                            sb.append(",\n");
                        }
                        else {
                            sb.append(", ");
                        }                            
                    }
                    fisrt = false;
                    format(Array.get(obj, i), sb, multiLine);
                }
                if (multiLine) {
                    sb.append("\n");
                }
                sb.append("]");
            }
        }
        else {
            Class<?> c = obj.getClass();
            sb.append("{");
            if (multiLine) {
                sb.append("\n");
            }
            boolean fisrt = true;
            Field[] fields = c.getFields();
            Arrays.sort(fields, new FieldSorter());
            for(Field f: fields) {
                Object v = f.get(obj);
                if (v != null) {
                    if (!fisrt) {
                        if (multiLine) {
                            sb.append(",\n");
                        }
                        else {
                            sb.append(", ");
                        }                            
                    }
                    fisrt = false;
                    sb.append(f.getName()).append(": ");
                    format(v, sb, multiLine);
                }
            }
            if (multiLine) {
                sb.append("\n");
            }
            sb.append("}");
        }
    }    
    
    private static class FieldSorter implements Comparator<Field> {

        @Override
        public int compare(Field o1, Field o2) {
            return o1.getName().compareTo(o2.getName());
        }        
    }
}

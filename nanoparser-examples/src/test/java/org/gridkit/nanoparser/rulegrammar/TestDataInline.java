package org.gridkit.nanoparser.rulegrammar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class TestDataInline extends TestWatcher {

    private String sourcePath;
    private String[] textLines;

    public TestDataInline() {
        this("src/test/java/");
    }

    public TestDataInline(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    protected void starting(Description description) {
        Class<?> cls = description.getTestClass();
        String methodName = description.getMethodName();
        while(textLines == null && cls != Object.class) {
            cherryPick(cls, methodName);
            cls = cls.getSuperclass();
        }
        super.starting(description);
    }

    protected void cherryPick(Class<?> cls, String methodName) {
        String name = cls.getName();
        name = sourcePath + name.replace('.', '/') + ".java";
        File f = new File(name);
        if (f.isFile()) {
            try {
                cherryPick(f, methodName);
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cherryPick(File f, String method) throws IOException {
        Pattern pattern = Pattern.compile("\\s*public\\s+void\\s+([a-zA-Z0-9_]+)\\s*\\(");
        Matcher m = pattern.matcher("");
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            List<String> sb = new ArrayList<String>();
            String line;
            boolean open = false;
            while((line = br.readLine()) != null) {
                if (!open) {
                    if (line.trim().equalsIgnoreCase("/* TESTDATA")) {
                        sb.clear();;
                        open = true;
                        continue;
                    }
                    else {
                        m.reset(line);
                        if (m.lookingAt()) {
                            String mn = m.group(1);
                            if (method.equals(mn)) {
                                // found method;
                                if (sb.size() > 0) {
                                    textLines = sb.toArray(new String[0]);
                                    return;
                                }
                            }
                            else {
                                // different method, drop buffer
                                sb.clear();
                            }
                        }
                    }
                }
                else {
                    // open
                    if (line.trim().equals("*/")) {
                        open = false;
                        continue;
                    }
                    sb.add(line);
                }
            }
        }
        finally {
            br.close();
        }
         
    }

    public String[] textLines() {
        return textLines;
    }

    public String text() {
        if (textLines == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for(String line: textLines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}

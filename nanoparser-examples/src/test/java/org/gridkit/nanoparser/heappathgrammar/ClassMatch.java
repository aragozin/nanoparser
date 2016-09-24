package org.gridkit.nanoparser.heappathgrammar;

class ClassMatch implements TypeMatch {

    private final String pattern;
    private final boolean matchSubclasses;
    
    public ClassMatch(String pattern, boolean matchSubclasses) {
        this.pattern = pattern;
        this.matchSubclasses = matchSubclasses;
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public boolean getMatchSubclasses() {
        return matchSubclasses;
    }
    
    @Override
    public String toString() {
        return (matchSubclasses ? "+" : "") + pattern;
    }
}

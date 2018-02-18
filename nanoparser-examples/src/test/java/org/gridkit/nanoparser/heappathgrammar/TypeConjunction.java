package org.gridkit.nanoparser.heappathgrammar;

public class TypeConjunction implements TypeMatch {

    private final TypeMatch[] matches;

    public TypeConjunction(TypeMatch[] matches) {
        this.matches = matches;
    }

    public TypeMatch[] getElements() {
        return matches;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(TypeMatch match: matches) {
            if (sb.length() > 0) {
                sb.append("|");
            }
            sb.append(match);
        }
        return sb.toString();
    }
}

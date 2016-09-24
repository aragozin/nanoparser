package org.gridkit.nanoparser.heappathgrammar;

class OrPredicate implements Predicate {

    private final Predicate a;
    private final Predicate b;
    
    public OrPredicate(Predicate a, Predicate b) {
        this.a = a;
        this.b = b;
    }
    
    @Override
    public String toString() {
        return "(" + a + "||" + b + ")";
    }
}

package org.gridkit.nanoparser.heappathgrammar;

class AndPredicate implements Predicate {

    private final Predicate a;
    private final Predicate b;

    public AndPredicate(Predicate a, Predicate b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return "(" + a + "&&" + b + ")";
    }
}

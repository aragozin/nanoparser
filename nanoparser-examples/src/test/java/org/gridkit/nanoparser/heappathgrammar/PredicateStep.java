package org.gridkit.nanoparser.heappathgrammar;

class PredicateStep implements HeapPathStep {

    private final Predicate predicate;

    public PredicateStep(Predicate predicate) {
        this.predicate = predicate;
    }

    public String toString() {
        return "[" + predicate + "]";
    }
}

package org.gridkit.nanoparser.heappathgrammar;

class ExistsPredicate implements Predicate {

    private final HeapPathStep[] path;
    
    public ExistsPredicate(HeapPathStep[] path) {
        this.path = path;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(HeapPathStep step: path) {
            sb.append(step);
        }
        return sb.toString();
    }
}

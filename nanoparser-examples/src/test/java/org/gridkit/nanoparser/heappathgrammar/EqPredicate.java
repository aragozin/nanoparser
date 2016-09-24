package org.gridkit.nanoparser.heappathgrammar;

class EqPredicate implements Predicate {

    private final HeapPathStep[] path;
    private final Object value;
    
    public EqPredicate(HeapPathStep[] path, Object value) {
        this.path = path;
        this.value = value;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(HeapPathStep step: path) {
            sb.append(step);
        }
        sb.append("=");
        sb.append(value);
        return sb.toString();
    }
}

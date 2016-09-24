package org.gridkit.nanoparser.heappathgrammar;

class TypeMatchStep implements HeapPathStep {

    private final TypeMatch typeMatch;

    public TypeMatchStep(TypeMatch typeMatch) {
        this.typeMatch = typeMatch;
    }
    
    @Override
    public String toString() {
        return "(" + typeMatch + ")";
    }
}

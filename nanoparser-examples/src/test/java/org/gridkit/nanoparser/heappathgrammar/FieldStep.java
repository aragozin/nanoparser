package org.gridkit.nanoparser.heappathgrammar;

class FieldStep implements HeapPathStep {

    private final String name;

    public FieldStep(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "." + name;
    }
}

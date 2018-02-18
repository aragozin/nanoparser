package org.gridkit.nanoparser.heappathgrammar;

class IndexStep implements HeapPathStep {

    private final int index;

    public IndexStep(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "[" + (index < 0 ? "*" : index) + "]";
    }
}

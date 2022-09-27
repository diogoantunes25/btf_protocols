package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import java.util.TreeSet;
import java.util.Vector;

public class BoolMultimap {

    private Vector<TreeSet<Integer>> values = new Vector<>(2);

    public BoolMultimap() {
        this.values.add(0, new TreeSet<>());
        this.values.add(1, new TreeSet<>());
    }

    /**
     * Get set for boolean b
     */
    public TreeSet<Integer> getIndex(Boolean b) {
        return this.values.elementAt(b ? 1 : 0);
    }

    public Vector<TreeSet<Integer>> getValues() {
        return this.values;
    }
}

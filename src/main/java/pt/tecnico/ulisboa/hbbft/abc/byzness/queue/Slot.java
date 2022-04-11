package pt.tecnico.ulisboa.hbbft.abc.byzness.queue;

public class Slot {

    private final byte[] value;
    private final byte[] proof;

    private boolean removed = false;

    public Slot(byte[] value, byte[] proof) {
        this.value = value;
        this.proof = proof;
    }

    public byte[] getValue() {
        return value;
    }

    public byte[] getProof() {
        return proof;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved() {
        this.removed = true;
    }
}

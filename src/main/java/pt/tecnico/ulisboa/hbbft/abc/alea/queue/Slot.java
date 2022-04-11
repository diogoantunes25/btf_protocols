package pt.tecnico.ulisboa.hbbft.abc.alea.queue;

public class Slot {

    private final long id;

    private final byte[] value;
    private final byte[] proof;

    private boolean removed = false;

    public Slot(long id, byte[] value, byte[] proof) {
        this.id = id;
        this.value = value;
        this.proof = proof;
    }

    public long getId() {
        return id;
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

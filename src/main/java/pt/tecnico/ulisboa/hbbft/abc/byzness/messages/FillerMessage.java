package pt.tecnico.ulisboa.hbbft.abc.byzness.messages;

import pt.tecnico.ulisboa.hbbft.abc.byzness.ByznessMessage;

public class FillerMessage extends ByznessMessage {

    public static final int FILLER = 432;

    private final Integer queue;
    private final Long slot;
    private final byte[] value;
    private final byte[] proof;

    public FillerMessage(String pid, Integer sender, Integer queue, Long slot, byte[] value, byte[] proof) {
        super(pid, FILLER, sender);
        this.queue = queue;
        this.slot = slot;
        this.value = value;
        this.proof = proof;
    }

    public Integer getQueue() {
        return queue;
    }

    public Long getSlot() {
        return slot;
    }

    public byte[] getValue() {
        return value;
    }

    public byte[] getProof() {
        return proof;
    }
}

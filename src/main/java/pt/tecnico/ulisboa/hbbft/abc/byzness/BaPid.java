package pt.tecnico.ulisboa.hbbft.abc.byzness;

public class BaPid {

    private final String protocolId;
    private final Integer queueId;
    private final Long slotId;
    private final Long epoch;

    public BaPid(String pid) {
        String[] components = pid.split("-");
        assert components.length == 4;
        this.protocolId = components[0];
        this.queueId = Integer.valueOf(components[1]);
        this.slotId = Long.valueOf(components[2]);
        this.epoch = Long.valueOf(components[3]);
    }

    public BaPid(String protocolId, Integer queueId, Long slotId, Long epoch) {
        this.protocolId = protocolId;
        this.queueId = queueId;
        this.slotId = slotId;
        this.epoch = epoch;
    }

    public Long getSlotId() {
        return slotId;
    }

    public Integer getQueueId() {
        return queueId;
    }

    public Long getEpoch() {
        return epoch;
    }

    @Override
    public String toString() {
        return String.format("%s-%d-%d-%d", protocolId, queueId, slotId, epoch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaPid baPid = (BaPid) o;

        if (!protocolId.equals(baPid.protocolId)) return false;
        if (!queueId.equals(baPid.queueId)) return false;
        if (!slotId.equals(baPid.slotId)) return false;
        return epoch.equals(baPid.epoch);
    }

    @Override
    public int hashCode() {
        int result = protocolId.hashCode();
        result = 31 * result + queueId.hashCode();
        result = 31 * result + slotId.hashCode();
        result = 31 * result + epoch.hashCode();
        return result;
    }
}

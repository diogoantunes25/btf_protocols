package pt.tecnico.ulisboa.hbbft.election;

import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

public class Coin {

    private final String name;
    private final GroupKey groupKey;
    private final KeyShare keyShare;

    private Map<Integer, SigShare> shares = new TreeMap<>();
    private BigInteger value;

    public Coin(String name, GroupKey groupKey, KeyShare keyShare) {
        this.name = name;
        this.groupKey = groupKey;
        this.keyShare = keyShare;
    }

    public byte[] getMyShare() {
        byte[] toSign = name.getBytes();
        return ThreshsigUtils.sigShare(toSign, keyShare).getSig().toByteArray();
    }

    public void addShare(Integer replicaId, byte[] share) {
        if (this.hasDecided()) return;
        shares.putIfAbsent(replicaId, new SigShare(replicaId + 1, share));

        if (shares.size() == groupKey.getK()) {
            byte[] toSign = name.getBytes();
            value = new BigInteger(ThreshsigUtils.combine(toSign, shares.values(), groupKey));
        }
    }

    public Boolean hasDecided() {
        return value != null;
    }

    public BigInteger getValue() {
        return this.value;
    }
}

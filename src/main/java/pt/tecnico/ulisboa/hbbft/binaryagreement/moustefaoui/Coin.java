package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;


import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;


/**
 * Threshold Coin-Tossing Scheme
 *
 * Reference: "Random Oracles in Constantinople",
 * Christian Cachin et all , IBM Research
 */
public class Coin {

    private final String name;
    private Long round;
    private final GroupKey groupKey;
    private final KeyShare keyShare;

    private Map<Integer, SigShare> shares = new TreeMap<>();
    private BigInteger signature;
    private Boolean decision;

    public Coin(String name, GroupKey groupKey, KeyShare keyShare) {
        this.name = name;
        this.round = 0L;
        this.groupKey = groupKey;
        this.keyShare = keyShare;
    }

    public void reset(Long round) {
        this.round = round;
        this.shares = new TreeMap<>();
        this.signature = null;
        this.decision = null;
    }

    public byte[] getMyShare() {
        byte[] toSign = this.getCoinName().getBytes();
        return ThreshsigUtils.sigShare(toSign, keyShare).getSig().toByteArray();
    }

    public void addShare(Integer replicaId, byte[] share) {
        if (this.hasDecided()) return;
        this.shares.putIfAbsent(replicaId, new SigShare(replicaId + 1, share));

        if (this.shares.size() == this.groupKey.getK()) {
            byte[] toSign = this.getCoinName().getBytes();
            this.signature = new BigInteger(ThreshsigUtils.combine(toSign, this.shares.values(), groupKey));
            // this.decision = (this.signature.mod(new BigInteger("2")).intValue() == 1);
            try {
                this.decision = (MessageDigest.getInstance("SHA-256").digest(this.signature.toByteArray())[0] & 1) == 1;
            }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    public Boolean hasDecided() {
        return decision != null;
    }

    public Boolean getValue() {
        return this.decision;
    }

    private String getCoinName() {
        return String.format("%s-%d", name, round);
    }
}

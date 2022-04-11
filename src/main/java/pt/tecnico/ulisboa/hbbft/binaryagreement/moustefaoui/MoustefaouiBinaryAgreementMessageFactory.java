package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.*;

public class MoustefaouiBinaryAgreementMessageFactory {

    private final String pid;
    private final Integer replicaId;

    private long round;

    public MoustefaouiBinaryAgreementMessageFactory(String pid, Integer replicaId, Long round) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.round = round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public BValMessage createBValMessage(Boolean value) {
        return new BValMessage(pid, replicaId, round, value);
    }

    public AuxMessage createAuxMessage(Boolean value) {
        return new AuxMessage(pid, replicaId, round, value);
    }

    public CoinMessage createCoinMessage(byte[] value) {
        return new CoinMessage(pid, replicaId, round, value);
    }

    public ConfMessage createConfMessage(BoolSet value) {
        return new ConfMessage(pid, replicaId, round, value);
    }

    public TermMessage createTermMessage(Boolean value) {
        return new TermMessage(pid, replicaId, round, value);
    }
}

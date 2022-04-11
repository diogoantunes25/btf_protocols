package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.utils;

import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;

public class DummyMessage extends BinaryAgreementMessage {

    public static final Integer DUMMY = 1337;

    private boolean canExpire;

    public DummyMessage(String pid, Integer sender, Long round) {
        super(pid, DUMMY, sender, round);
    }

    public DummyMessage(String pid, Integer sender, Long round, boolean canExpire) {
        super(pid, DUMMY, sender, round);
        this.canExpire = canExpire;
    }

    @Override
    public Boolean canExpire() {
        return canExpire;
    }
}

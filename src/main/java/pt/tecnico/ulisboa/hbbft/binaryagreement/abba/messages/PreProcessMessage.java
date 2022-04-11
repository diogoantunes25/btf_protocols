package pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages;

import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

public class PreProcessMessage extends BinaryAgreementMessage {

    public static final int PRE_PROCESS = 221;

    private final Boolean value;
    private final SigShare share;

    public PreProcessMessage(String pid, Integer sender, Long round, Boolean value, SigShare share) {
        super(pid, PRE_PROCESS, sender, round);
        this.value = value;
        this.share = share;
    }

    public Boolean getValue() {
        return value;
    }

    public SigShare getShare() {
        return share;
    }

    @Override
    public Boolean canExpire() {
        return false;
    }
}

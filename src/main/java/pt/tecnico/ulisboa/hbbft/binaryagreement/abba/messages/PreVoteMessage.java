package pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages;

import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

public class PreVoteMessage extends BinaryAgreementMessage {

    public static final int PRE_VOTE = 222;

    private final Boolean value;
    private final byte[] justification;
    private final SigShare share;

    public PreVoteMessage(String pid, Integer sender, Long round, Boolean value, byte[] justification, SigShare share) {
        super(pid, PRE_VOTE, sender, round);
        this.value = value;
        this.justification = justification;
        this.share = share;
    }

    public Boolean getValue() {
        return value;
    }

    public byte[] getJustification() {
        return justification;
    }

    public SigShare getShare() {
        return share;
    }

    @Override
    public Boolean canExpire() {
        return false;
    }
}

package pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages;

import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

public class CoinMessage extends BinaryAgreementMessage {

    public static final int COIN = 124;

    private final SigShare share;

    public CoinMessage(String pid, Integer sender, Long round, SigShare share) {
        super(pid, COIN, sender, round);
        this.share = share;
    }

    public SigShare getShare() {
        return share;
    }

    @Override
    public Boolean canExpire() {
        return false;
    }
}

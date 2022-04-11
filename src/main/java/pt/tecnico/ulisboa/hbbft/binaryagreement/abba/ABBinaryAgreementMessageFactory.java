package pt.tecnico.ulisboa.hbbft.binaryagreement.abba;

import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.CoinMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.MainVoteMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.PreProcessMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.PreVoteMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

public class ABBinaryAgreementMessageFactory {

    private final String pid;
    private final Integer replicaId;

    private long round;

    public ABBinaryAgreementMessageFactory(String pid, Integer replicaId, Long round) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.round = round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public PreProcessMessage createPreProcessMessage(Boolean value, SigShare share) {
        return new PreProcessMessage(pid, replicaId, round, value, share);
    }

    public PreVoteMessage createPreVoteMessage(Boolean value, byte[] justification, SigShare share) {
        return new PreVoteMessage(pid, replicaId, round, value, justification, share);
    }

    public MainVoteMessage createMainVoteMessage(Boolean value, byte[] justification, SigShare share) {
        return new MainVoteMessage(pid, replicaId, round, value, justification, share);
    }

    public CoinMessage createCoinMessage(SigShare share) {
        return new CoinMessage(pid, replicaId, round, share);
    }
}

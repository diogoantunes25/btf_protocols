package pt.tecnico.ulisboa.hbbft.binaryagreement.abba;

import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.CoinMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.MainVoteMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.PreVoteMessage;

import java.util.Map;
import java.util.TreeMap;

public class ReceivedMessages {

    // Received `PreVote` messages.
    private Map<Integer, PreVoteMessage> preVoteMessages = new TreeMap<>();

    // Received `MainVote` messages.
    private Map<Integer, MainVoteMessage> mainVoteMessages = new TreeMap<>();

    // Received `Coin` messages.
    private Map<Integer, CoinMessage> coinMessages = new TreeMap<>();

    public void insert(BinaryAgreementMessage message) {
        switch (message.getType()) {
            case PreVoteMessage.PRE_VOTE: {
                this.insert((PreVoteMessage) message); break;
            }
            case MainVoteMessage.MAIN_VOTE: {
                this.insert((MainVoteMessage) message); break;
            }
            case CoinMessage.COIN: {
                this.insert((CoinMessage) message); break;
            }
        }
    }

    public void insert(PreVoteMessage message) {
        this.preVoteMessages.putIfAbsent(message.getSender(), message);
    }

    public void insert(MainVoteMessage message) {
        this.mainVoteMessages.putIfAbsent(message.getSender(), message);
    }

    public void insert(CoinMessage message) {
        this.coinMessages.putIfAbsent(message.getSender(), message);
    }


}

package pt.tecnico.ulisboa.hbbft.binaryagreement.abba;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.MainVoteMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.PreProcessMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.abba.messages.PreVoteMessage;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class ABBinaryAgreement implements IBinaryAgreement {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String pid;

    // Our ID.
    private final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    private final KeyShare keyShare;

    private final GroupKey groupKey;

    // The current protocol round.
    private long round = 0L;

    // A cache for messages for future epochs that cannot be handled yet.
    private Map<Long, Map<Integer, ReceivedMessages>> incomingQueue = new TreeMap<>();

    private final ABBinaryAgreementMessageFactory messageFactory;

    public ABBinaryAgreement(
            String pid,
            Integer replicaId,
            NetworkInfo networkInfo,
            KeyShare keyShare,
            GroupKey groupKey
    ) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;

        this.keyShare = keyShare;
        this.groupKey = groupKey;

        this.messageFactory = new ABBinaryAgreementMessageFactory(pid, replicaId, 0L);
    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public Step<Boolean> handleInput(Boolean input) {
        // TODO if (!this.canPropose()) return new Step<>();

        byte[] toSign = String.format("%s-%d-%b", pid, PreProcessMessage.PRE_PROCESS, input).getBytes();
        SigShare share = ThreshsigUtils.sigShare(toSign, keyShare);
        PreProcessMessage preProcessMessage = messageFactory.createPreProcessMessage(input, share);

        return this.sendMessage(preProcessMessage);
    }

    @Override
    public Step<Boolean> handleMessage(BinaryAgreementMessage message) {
        Step<Boolean> step = new Step<>();

        // Check if the message PID matches this instance
        if (!message.getPid().equals(pid)) return step;

        final long round = message.getRound();

        if (round < this.round) {
            // TODO

        } else if (round > this.round) {
            // Message is for a later epoch. We can't handle that yet.
            Map<Integer, ReceivedMessages> epochState = incomingQueue.computeIfAbsent(round, r -> new TreeMap<>());
            ReceivedMessages received = epochState.computeIfAbsent(message.getSender(), s -> new ReceivedMessages());
            received.insert(message);

        } else {
            // Handle message content
            final Integer type = message.getType();
            switch (type) {
                case PreProcessMessage.PRE_PROCESS: {
                    step.add(handlePreProcessMessage((PreProcessMessage) message));
                    break;
                }
                case PreVoteMessage.PRE_VOTE: {
                    step.add(handlePreVoteMessage((PreVoteMessage) message));
                    break;
                }
                case MainVoteMessage.MAIN_VOTE: {
                    step.add(handleMainVoteMessage((MainVoteMessage) message));
                    break;
                }
            }
        }

        return step;
    }

    @Override
    public boolean hasTerminated() {
        return false;
    }

    @Override
    public Optional<Boolean> deliver() {
        return Optional.empty();
    }

    /**
     * Called by the protocol to indicate that
     * a {@link PreProcessMessage} has been received.
     *
     * @param preProcessMessage the received message
     */
    private Step<Boolean> handlePreProcessMessage(PreProcessMessage preProcessMessage) {
        Step<Boolean> step = new Step<>();
        // TODO
        return new Step<>();
    }

    /**
     * Called by the protocol to indicate that
     * a {@link PreVoteMessage} has been received.
     *
     * @param preVoteMessage the received message
     */
    private Step<Boolean> handlePreVoteMessage(PreVoteMessage preVoteMessage) {
        Step<Boolean> step = new Step<>();

        final int quorum = this.networkInfo.getN() - this.networkInfo.getF();

        return step; // TODO
    }

    /**
     * Called by the protocol to indicate that
     * a {@link MainVoteMessage} has been received.
     *
     * @param mainVoteMessage the received message
     */
    private Step<Boolean> handleMainVoteMessage(MainVoteMessage mainVoteMessage) {
        return new Step<>(); // TODO
    }

    private Step<Boolean> sendMessage(BinaryAgreementMessage message) {
        Step<Boolean> step = new Step<>();
        for (int id=0; id < this.networkInfo.getN(); id++) {
            if (id == this.replicaId) step.add(this.handleMessage(message));
            step.add(message, id);
        }
        return step;
    }
}

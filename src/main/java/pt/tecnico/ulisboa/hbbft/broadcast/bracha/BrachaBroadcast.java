package pt.tecnico.ulisboa.hbbft.broadcast.bracha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.broadcast.BroadcastMessage;
import pt.tecnico.ulisboa.hbbft.broadcast.IBroadcast;
import pt.tecnico.ulisboa.hbbft.broadcast.bracha.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.broadcast.bracha.messages.ReadyMessage;
import pt.tecnico.ulisboa.hbbft.broadcast.bracha.messages.SendMessage;

import java.util.*;
import java.util.stream.Collectors;

public class BrachaBroadcast implements IBroadcast {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // The protocol instance identifier.
    private final String pid;

    // The replica id.
    private final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    // The ID of the sending node.
    private  final Integer proposerId;

    // If we are the proposer: whether we have already sent the `Send` message.
    private Boolean sendSent = false;

    // Whether we have already multicast `Echo`.
    private Boolean echoSent = false;

    // Whether we have already multicast `Ready`.
    private Boolean readySent = false;

    // The `Echo` messages received, by sender ID.
    private Map<Integer, EchoMessage> echos = new TreeMap<>();

    // The `Ready` messages received, by sender ID.
    private Map<Integer, ReadyMessage> readies = new TreeMap<>();

    // Whether we have already output a value.
    private Boolean decided = false;

    // The value to output when ready.
    private byte[] decidedValue;

    private final BrachaBroadcastMessageFactory messageFactory;

    public BrachaBroadcast(
            String pid,
            Integer replicaId,
            NetworkInfo networkInfo,
            Integer proposerId
    ) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;
        this.proposerId = proposerId;
        this.messageFactory = new BrachaBroadcastMessageFactory(pid, replicaId);
    }

    @Override
    public String getPid() {
        return this.pid;
    }

    @Override
    public Step<byte[]> handleInput(byte[] input) {
        Step<byte[]> step = new Step<>();
        if (!this.replicaId.equals(proposerId) || this.sendSent) {
            return step;
        }
        this.sendSent = true;
        SendMessage sendMessage = messageFactory.createSendMessage(input);
        return this.sendMessage(sendMessage);
    }

    @Override
    public Step<byte[]> handleMessage(BroadcastMessage message) {
        // TODO check if BID matches this instance
        final int type = message.getType();
        switch (type) {
            case SendMessage.SEND: {
                return handleSendMessage((SendMessage) message);
            }
            case EchoMessage.ECHO: {
                return handleEchoMessage((EchoMessage) message);
            }
            case ReadyMessage.READY: {
                return handleReadyMessage((ReadyMessage) message);
            }
            default: {
                return new Step<>();
            }
        }
    }

    @Override
    public boolean hasTerminated() {
        return this.decided;
    }

    @Override
    public Optional<byte[]> deliver() {
        return Optional.ofNullable(this.decidedValue);
    }

    /**
     * Called by the replica to indicate that
     * a {@link SendMessage} has been received.
     *
     * @param sendMessage the received message
     */
    private Step<byte[]> handleSendMessage(SendMessage sendMessage) {
        if (!sendMessage.getSender().equals(this.proposerId) || this.echoSent) {
            return new Step<>();
        }
        this.echoSent = true;
        EchoMessage echoMessage = messageFactory.createEchoMessage(sendMessage.getValue());
        return this.sendMessage(echoMessage);
    }

    /**
     * Called by the replica to indicate that
     * a {@link EchoMessage} has been received.
     *
     * @param echoMessage the received message
     */
    private Step<byte[]> handleEchoMessage(EchoMessage echoMessage) {
        final int senderId = echoMessage.getSender();
        final byte[] value = echoMessage.getValue();

        Step<byte[]> step = new Step<>();
        if (this.echos.containsKey(senderId)) {
            return step;
        }

        // Save the `Echo` message
        this.echos.put(senderId, echoMessage);
        if (this.readySent) {
            return step;
        }

        // Upon receiving `(N + f + 1) / 2` `Echo`s for the same value
        int quorum = (int) Math.ceil((double) (networkInfo.getN() + networkInfo.getF() + 1) / 2);
        final List<EchoMessage> validEchos = this.echos.values().stream()
                .filter(e -> Arrays.equals(e.getValue(), value)).collect(Collectors.toList());
        if (validEchos.size() == quorum) {
            this.readySent = true;
            ReadyMessage readyMessage = messageFactory.createReadyMessage(value);
            step.add(this.sendMessage(readyMessage));
        }
        return step;
    }

    /**
     * Called by the replica to indicate that
     * a {@link ReadyMessage} has been received.
     *
     * @param readyMessage the received message
     */
    private Step<byte[]> handleReadyMessage(ReadyMessage readyMessage) {
        final int senderId = readyMessage.getSender();
        final byte[] value = readyMessage.getValue();

        Step<byte[]> step = new Step<>();
        if (this.readies.containsKey(senderId)) {
            return step;
        }

        // Save the `Ready` message
        this.readies.put(senderId, readyMessage);
        final List<ReadyMessage> validReadies = this.readies.values().stream()
                .filter(m -> Arrays.equals(m.getValue(), value)).collect(Collectors.toList());

        // Upon receiving `f + 1` `Ready` messages and not having sent a `Ready` message
        final int quorum1 = networkInfo.getF() + 1;
        if (validReadies.size() == quorum1 && !this.readySent) {
            // Send a `Ready` message to all
            this.readySent = true;
            ReadyMessage message = messageFactory.createReadyMessage(value);
            step.add(this.sendMessage(message));
        }

        // Upon receiving `2*f + 1` `Ready` messages
        final int quorum2 = 2*networkInfo.getF() + 1;
        if (validReadies.size() == quorum2) {
            // Deliver
            step.add(this.deliver(value));
        }

        return step;
    }

    private Step<byte[]> sendMessage(BroadcastMessage message) {
        Step<byte[]> step = new Step<>();
        for (int id=0; id < this.networkInfo.getN(); id++) {
            if (id == this.replicaId) step.add(this.handleMessage(message));
            else step.add(message, id);
        }
        return step;
    }

    private Step<byte[]> deliver(byte[] value) {
        Step<byte[]> step = new Step<>();
        if (decided) {
            return step;
        }

        this.decided = true;
        this.decidedValue = value;
        step.add(value);

        return step;
    }
}

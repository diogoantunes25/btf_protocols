package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtil;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.FinalMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public class EchoVBroadcast2 implements IEchoVBroadcast {

    private final Logger logger = LoggerFactory.getLogger("EchoVBroadcast2");

    // The protocol instance identifier.
    private final String pid;

    // The replica id.
    private final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    // The ID of the sending node.
    private final Integer proposerId;

    // Threshold signature utils.
    private final ThreshsigUtil threshsigUtil;

    private final EchoVBroadcastMessageFactory messageFactory;

    // If we are the proposer: whether we have already sent the `Send` message.
    private Boolean sendSent = false;

    // Whether we have already sent an `Echo` back to the proposer.
    private Boolean echoSent = false;

    // If we are the proposer: whether we have already sent the `Final` message.
    private Boolean finalSent = false;

    // The `Echo` messages received, by sender ID.
    private final Map<Integer, EchoMessage> echoMessages = new TreeMap<>();

    private byte[] input;

    private VOutput output;

    public EchoVBroadcast2(
            String pid,
            Integer replicaId,
            NetworkInfo networkInfo,
            Integer proposerId
    ) {
        this.pid = pid;
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;
        this.proposerId = proposerId;

        this.threshsigUtil = new ThreshsigUtil(networkInfo.getGroupKey(), networkInfo.getKeyShare());

        this.messageFactory = new EchoVBroadcastMessageFactory(pid, replicaId);
    }

    @Override
    public String getPid() {
        return this.pid;
    }

    @Override
    public Step<VOutput> handleInput(byte[] input) {
        Step<VOutput> step = new Step<>();

        // ignore if not proposer or already proposed
        if (!this.replicaId.equals(proposerId) || this.sendSent) {
            return step;
        }

        logger.info("handleInput called");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedInput = digest.digest(input);

            this.sendSent = true;
            this.input = input;

            SendMessage sendMessage = messageFactory.createSendMessage(hashedInput);
            return this.send(sendMessage);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return step;
    }

    @Override
    public Step<VOutput> handleMessage(VBroadcastMessage message) {
        // ignore messages with wrong pid
        if (!message.getPid().equals(pid)) return new Step<>();

        // logger.info("handleMessage - message:{}", message);

        switch (message.getType()) {
            case SendMessage.SEND: {
                return this.handleSendMessage((SendMessage) message);
            }
            case EchoMessage.ECHO: {
                return this.handleEchoMessage((EchoMessage) message);
            }
            case FinalMessage.FINAL: {
                return this.handleFinalMessage((FinalMessage) message);
            }
            default: {
                return new Step<>();
            }
        }
    }

    @Override
    public boolean hasTerminated() {
        return this.output != null;
    }

    @Override
    public Optional<VOutput> deliver() {
        return Optional.ofNullable(this.output);
    }

    @Override
    public Step<VOutput> handleSendMessage(SendMessage sendMessage) {
        Step<VOutput> step = new Step<>();

        // ignore `Send` message not from the proposer or if already echoed
        if (!sendMessage.getSender().equals(this.proposerId) || this.echoSent)
            return step;

        // set `Echo` flag
        this.echoSent = true;

        // compute signature share
        byte[] value = sendMessage.getValue();
        String toSign = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(value));
        SigShare share = threshsigUtil.sigShare(toSign.getBytes());

        // send `Echo` message back to proposer
        EchoMessage echoMessage = messageFactory.createEchoMessage(value, share);
        return this.send(echoMessage, this.proposerId);
    }

    @Override
    public Step<VOutput> handleEchoMessage(EchoMessage echoMessage) {
        Step<VOutput> step = new Step<>();

        // ignore if not proposer, not proposed or duplicate `Echo`
        final int senderId = echoMessage.getSender();
        if (!this.replicaId.equals(proposerId) || !this.sendSent || this.echoMessages.containsKey(senderId))
            return step;

        // verify the value
        final byte[] value = echoMessage.getValue();
        if (value == null || !Arrays.equals(value, this.hash(input))) {
            // logger.info("Invalid value");
            return step;
        }

        // verify threshold signature share
        final SigShare share = echoMessage.getShare();
        String toVerify = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(value));
        if (!threshsigUtil.verifyShare(toVerify.getBytes(), share)) {
            // logger.info("Invalid signature");
            return step;
        }

        // save `Echo` message
        this.echoMessages.put(senderId, echoMessage);

        // already sent `Final` message
        if (this.finalSent) {
            // logger.info("Already sent final message.");
            return step;
        }

        // upon receiving `2*f + 1` valid `Echo` messages
        int quorum = 2*networkInfo.getF() + 1;
        // logger.info("Quorum of {}", this.echoMessages.size());
        if (this.echoMessages.size() >= quorum) {
            // compute threshold signature from shares
            Set<SigShare> shares = this.echoMessages.values().stream()
                    .map(EchoMessage::getShare).collect(Collectors.toSet());
            byte[] signature = threshsigUtil.combine(toVerify.getBytes(), shares);
            // TODO verify signature validity

            // Send `Final` message to all replicas
            this.finalSent = true;
            FinalMessage finalMessage = messageFactory.createFinalMessage(input, signature);
            step.add(this.send(finalMessage));
        }

        return step;
    }

    @Override
    public Step<VOutput> handleFinalMessage(FinalMessage finalMessage) {
        Step<VOutput> step = new Step<>();

        // ignore if already decided
        if (this.hasTerminated())
            return step;

        // verify threshold signature
        final byte[] value = finalMessage.getValue();
        final byte[] hashedValue = this.hash(value);
        if (hashedValue == null) return step;
        final byte[] signature = finalMessage.getSignature();
        String toVerify = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(hashedValue));
        if (!threshsigUtil.verify(toVerify.getBytes(), signature))
            return step;

        // deliver
        this.output = new VOutput(value, signature);
        step.add(this.output);

        return step;
    }

    private byte[] hash(byte[] value) {
        byte[] hashed = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hashed =  digest.digest(value);
        } catch (NoSuchAlgorithmException ignore) {}
        return hashed;
    }

    private Step<VOutput> send(VBroadcastMessage message) {
        Step<VOutput> step = new Step<>();
        step.add(this.handleMessage(message));
        step.add(message, this.networkInfo.getValidatorSet().getAllIds().stream()
                .filter(id -> !id.equals(this.replicaId)).collect(Collectors.toList()));
        return step;
    }

    private Step<VOutput> send(VBroadcastMessage message, int target) {
        Step<VOutput> step = new Step<>();
        if (target == this.replicaId) step.add(this.handleMessage(message));
        else step.add(message, target);
        return step;
    }
}

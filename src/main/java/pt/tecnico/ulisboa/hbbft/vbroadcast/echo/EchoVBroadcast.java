package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtil;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.FinalMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;

import java.util.*;
import java.util.stream.Collectors;

public class EchoVBroadcast implements IEchoVBroadcast {

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

    // If we are the proposer: whether we have already sent the `Send` message.
    private Boolean sendSent = false;

    // Whether we have already sent an `Echo` back to the proposer.
    private Boolean echoSent = false;

    // If we are the proposer: whether we have already sent the `Final` message.
    private Boolean finalSent = false;

    // The `Echo` messages received, by sender ID.
    private final Map<Integer, EchoMessage> echos = new TreeMap<>();

    // Whether we have already output a value.
    private Boolean decided = false;

    // The value to output when ready.
    private VOutput decidedValue;

    private final EchoVBroadcastMessageFactory messageFactory;

    public EchoVBroadcast(
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
        if (!this.replicaId.equals(proposerId) || this.sendSent) {
            return step;
        }
        this.sendSent = true;
        SendMessage sendMessage = messageFactory.createSendMessage(input);
        return this.sendMessage(sendMessage);
    }

    @Override
    public Step<VOutput> handleMessage(VBroadcastMessage message) {
        if (!message.getPid().equals(pid)) return new Step<>();
        switch (message.getType()) {
            case SendMessage.SEND: {
                return handleSendMessage((SendMessage) message);
            }
            case EchoMessage.ECHO: {
                return handleEchoMessage((EchoMessage) message);
            }
            case FinalMessage.FINAL: {
                return handleFinalMessage((FinalMessage) message);
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
    public Optional<VOutput> deliver() {
        return Optional.ofNullable(this.decidedValue);
    }

    @Override
    public Step<VOutput> handleSendMessage(SendMessage sendMessage) {
        if (!sendMessage.getSender().equals(this.proposerId) || this.echoSent) {
            return new Step<>();
        }
        this.echoSent = true;

        // Compute signature share
        byte[] value = sendMessage.getValue();
        String toSign = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(value));
        SigShare share = threshsigUtil.sigShare(toSign.getBytes());

        // Send `Echo` message back to proposer
        EchoMessage echoMessage = messageFactory.createEchoMessage(value, share);
        return this.sendMessage(echoMessage, this.proposerId);
    }

    @Override
    public Step<VOutput> handleEchoMessage(EchoMessage echoMessage) {
        final int senderId = echoMessage.getSender();

        Step<VOutput> step = new Step<>();
        if (this.echos.containsKey(senderId)) {
            return step; // Duplicate `Echo` message
        }

        // Verify the threshold signature share
        final byte[] value = echoMessage.getValue();
        final SigShare share = echoMessage.getShare();
        String toVerify = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(value));
        if (!ThreshsigUtils.verifyShare(toVerify.getBytes(), share)) {
            return step; // Invalid share
        }

        // Save the `Echo` message
        this.echos.put(senderId, echoMessage);
        if (this.finalSent) {
            return step; // Already sent `Final` message
        }

        // Upon receiving `2*f + 1` valid `Echo`s for the same value
        int quorum = 2*networkInfo.getF() + 1;
        List<EchoMessage> validEchos = this.echos.values().stream()
                .filter(e -> Arrays.equals(e.getValue(), value)).collect(Collectors.toList());
        if (validEchos.size() == quorum) {
            this.finalSent = true;

            // Compute threshold signature
            Set<SigShare> shares = validEchos.stream()
                    .map(EchoMessage::getShare).collect(Collectors.toSet());
            byte[] signature = threshsigUtil.combine(toVerify.getBytes(), shares);
            // TODO verify signature validity

            // Send `Final` message to all replicas
            FinalMessage finalMessage = messageFactory.createFinalMessage(value, signature);
            step.add(this.sendMessage(finalMessage));
        }

        return step;
    }

    @Override
    public Step<VOutput> handleFinalMessage(FinalMessage finalMessage) {
        Step<VOutput> step = new Step<>();
        if (!finalMessage.getSender().equals(this.proposerId) || this.hasTerminated()) {
            return step; // Not proposer or already decided
        }

        // Verify threshold signature
        final byte[] value = finalMessage.getValue();
        final byte[] signature = finalMessage.getSignature();
        String toVerify = String.format("%s-%s", pid, Base64.getEncoder().encodeToString(value));
        if (!threshsigUtil.verify(toVerify.getBytes(), signature)) {
            return step; // Invalid signature
        }

        // Deliver
        this.decided = true;
        this.decidedValue = new VOutput(value, signature);
        step.add(this.decidedValue);

        return step;
    }

    private Step<VOutput> sendMessage(VBroadcastMessage message) {
        Step<VOutput> step = new Step<>();
        for (int id=0; id < this.networkInfo.getN(); id++) {
            if (id == this.replicaId) step.add(this.handleMessage(message));
            else step.add(message, id);
        }
        return step;
    }

    private Step<VOutput> sendMessage(VBroadcastMessage message, int target) {
        Step<VOutput> step = new Step<>();
        if (target == this.replicaId) step.add(this.handleMessage(message));
        else step.add(message, target);
        return step;
    }
}

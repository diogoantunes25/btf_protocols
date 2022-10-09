package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.MoustefaouiBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillGapMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillerMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.Slot;
import pt.tecnico.ulisboa.hbbft.abc.byzness.state.AgreementState;
import pt.tecnico.ulisboa.hbbft.abc.byzness.state.ProposingState;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtil;
import pt.tecnico.ulisboa.hbbft.vbroadcast.IVBroadcast;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.EchoVBroadcast;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Byzness implements IByzness {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Our ID.
    public final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    // Threshold signature utils.
    private final ThreshsigUtil threshsigUtil;

    // The local priority value
    private final AtomicLong priority = new AtomicLong();

    // The priority queues (one per replica)
    private Map<Integer, PriorityQueue> queues = new TreeMap<>();

    // A map of validated consistent broadcast instances by pid
    private Map<BcPid, IVBroadcast> bcInstances = new HashMap<>();

    // A map of binary agreement instances by pid
    private Map<BaPid, IBinaryAgreement> baInstances = new HashMap<>();

    // The current agreement state
    private AgreementState agreementState;

    // The set of executed commands
    private Set<byte[]> executed = new HashSet<>();

    public Byzness(
            Integer replicaId,
            NetworkInfo networkInfo
    ) {
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;

        this.threshsigUtil = new ThreshsigUtil(networkInfo.getGroupKey(), networkInfo.getKeyShare());

        // Initialize the priority queues
        for (int id=0; id<networkInfo.getN(); id++)
            this.queues.put(id, new PriorityQueue(id));

        this.agreementState = new ProposingState(this, 0L);
    }

    @Override
    public Integer getReplicaId() {
        return replicaId;
    }

    @Override
    public NetworkInfo getNetworkInfo() {
        return networkInfo;
    }

    @Override
    public Long getPriority() {
        return priority.get();
    }

    @Override
    public Map<Integer, PriorityQueue> getQueues() {
        return this.queues;
    }

    @Override
    public IVBroadcast getVBroadcastInstance(BcPid bcPid) {
        return this.bcInstances.computeIfAbsent(bcPid,
                p -> new EchoVBroadcast(
                        p.toString(),
                        replicaId,
                        networkInfo,
                        p.getQueueId()
                )
        );
    }

    @Override
    public IBinaryAgreement getBinaryAgreementInstance(BaPid baPid) {
        return this.baInstances.computeIfAbsent(baPid,
                p -> new MoustefaouiBinaryAgreement(
                        p.toString(),
                        replicaId,
                        networkInfo
                )
        );
    }

    @Override
    public Set<byte[]> getExecuted() {
        return executed;
    }

    public Step<Block> setAgreementState(AgreementState agreementState) {
        this.agreementState = agreementState;
        return this.agreementState.tryProgress();
    }

    @Override
    public Step<Block> handleInput(byte[] input) {
        logger.debug("handleInput - input:{}", new String(input, StandardCharsets.UTF_8));

        Step<Block> step = new Step<>();

        // Check if the input command was previously executed
        if (this.getExecuted().contains(input)) {
            step.addFault("BIZ", "CMD ALREADY EXECUTED");
            return step;
        }

        // Check if f+1 queues already contain the input command
        int matches = (int) this.getQueues().values().stream().filter(q -> q.contains(input)).count();
        if (matches > this.getNetworkInfo().getF()) {
            step.addFault("BIZ", "COMMAND ALREADY QUEUED");
            return step;
        }

        BcPid bcPid = new BcPid("vCBC", replicaId, priority.getAndIncrement());
        IVBroadcast instance = this.getVBroadcastInstance(bcPid);

        Step<VOutput> bcStep = instance.handleInput(input);
        return this.handleVBroadcastStep(bcStep, bcPid);
    }

    @Override
    public Step<Block> handleMessage(ProtocolMessage message) {
        logger.debug("handleMessage - msg:{}", message);

        if (message instanceof VBroadcastMessage) {
            // Route a verifiable broadcast message
            return this.handleVBroadcastMessage((VBroadcastMessage) message);

        } else if (message instanceof BinaryAgreementMessage) {
            // Route a binary agreement message
            return this.handleBinaryAgreementMessage((BinaryAgreementMessage) message);

        } else {
            // Route sub-protocol messages
            return this.handleProtocolMessage(message);
        }
    }

    @Override
    public boolean hasTerminated() {
        return false;
    }

    @Override
    public Optional<Block> deliver() {
        return Optional.empty();
    }

    @Override
    public Step<Block> handleVBroadcastMessage(VBroadcastMessage message) {
        BcPid bcPid = new BcPid(message.getPid());
        IVBroadcast instance = this.getVBroadcastInstance(bcPid);
        Step<VOutput> bsStep = instance.handleMessage(message);
        return this.handleVBroadcastStep(bsStep, bcPid);
    }

    @Override
    public Step<Block> handleBinaryAgreementMessage(BinaryAgreementMessage message) {
        BaPid baPid = new BaPid(message.getPid());
        IBinaryAgreement instance = this.getBinaryAgreementInstance(baPid);
        Step<Boolean> baStep = instance.handleMessage(message);
        return this.handleBinaryAgreementStep(baStep, baPid);
    }

    @Override
    public Step<Block> handleProtocolMessage(ProtocolMessage message) {
        switch (message.getType()) {
            case FillGapMessage.FILL_GAP: {
                return handleFillGapMessage((FillGapMessage) message);
            }
            case FillerMessage.FILLER: {
                return handleFillerMessage((FillerMessage) message);
            }
            default: {
                return new Step<>();
            }
        }
    }

    @Override
    public Step<Block> handleFillGapMessage(FillGapMessage fillGapMessage) {
        Step<Block> step = new Step<>();

        final Integer senderId = fillGapMessage.getSender();
        final Integer queueId = fillGapMessage.getQueue();
        final Long slotId = fillGapMessage.getSlot();

        PriorityQueue queue = this.getQueues().get(queueId);
        Optional<Slot> optionalSlot = queue.get(slotId);

        if (optionalSlot.isPresent()) {
            Slot slot = optionalSlot.get();
            FillerMessage fillerMessage = new FillerMessage(
                    "BIZ", replicaId, queueId, slotId, slot.getValue(), slot.getProof());
            step.add(fillerMessage, senderId);
        }
        return step;
    }

    @Override
    public Step<Block> handleFillerMessage(FillerMessage fillerMessage) {
        Step<Block> step = new Step<>();

        final Integer queueId = fillerMessage.getQueue();
        final Long slotId = fillerMessage.getSlot();
        final byte[] value = fillerMessage.getValue();
        final byte[] proof = fillerMessage.getProof();

        // Check if already contains the value
        PriorityQueue queue = this.getQueues().get(queueId);
        if (queue.get(slotId).isPresent()) {
            step.addFault("BIZ", "UNEXPECTED FILLER MESSAGE");
            return step;
        }

        // Verify the threshold signature proof
        String toVerify = String.format("BC-%d-%d-%s", queueId, slotId, Base64.getEncoder().encodeToString(value));
        if (!threshsigUtil.verify(toVerify.getBytes(), proof)) {
            step.addFault("BIZ", "INVALID PROOF");
            return step;
        }

        // Place the (value, proof) pair in the corresponding queue slot
        queue.enqueue(slotId, value, proof);

        return this.tryProgress();
    }

    @Override
    public Step<Block> handleVBroadcastStep(Step<VOutput> bcStep, BcPid bcPid) {
        Step<Block> step = new Step<>(bcStep.getMessages());
        if (bcStep.getOutput().isEmpty()) return step;

        PriorityQueue queue = queues.get(bcPid.getQueueId());
        VOutput output = bcStep.getOutput().firstElement();
        queue.enqueue(bcPid.getSlotId(), output.getValue(), output.getSignature());

        if (this.executed.contains(output.getValue()))
            queue.dequeue(bcPid.getSlotId());

        step.add(this.tryProgress());
        return step;
    }

    @Override
    public Step<Block> handleBinaryAgreementStep(Step<Boolean> baStep, BaPid baPid) {
        Step<Block> step = new Step<>(baStep.getMessages());
        step.add(this.tryProgress());
        return step;
    }

    private Step<Block> tryProgress() {
        Step<Block> step = this.agreementState.tryProgress();
        for (Block block: step.getOutput())
            executed.add(block.getContent());
        return step;
    }

    public void reset() {
        this.agreementState = new ProposingState(this, 0L);
        queues = new TreeMap<>();
        bcInstances = new HashMap<>();
        baInstances = new HashMap<>();
        executed = new HashSet<>();

        for (int id=0; id<networkInfo.getN(); id++)
            this.queues.put(id, new PriorityQueue(id));
    }
}
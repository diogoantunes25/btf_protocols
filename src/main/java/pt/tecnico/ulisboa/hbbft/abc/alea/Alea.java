package pt.tecnico.ulisboa.hbbft.abc.alea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.alea.benchmark.ExecutionLog;
import pt.tecnico.ulisboa.hbbft.abc.alea.messages.FillGapMessage;
import pt.tecnico.ulisboa.hbbft.abc.alea.messages.FillerMessage;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.Slot;
import pt.tecnico.ulisboa.hbbft.abc.alea.state.AgreementState;
import pt.tecnico.ulisboa.hbbft.abc.alea.state.ProposingState;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.MoustefaouiBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtil;
import pt.tecnico.ulisboa.hbbft.vbroadcast.IVBroadcast;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.EchoVBroadcast2;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Alea implements IAlea {

    private final static Logger logger = LoggerFactory.getLogger(Alea.class);

    // Our ID.
    public final Integer replicaId;

    // The network configuration.
    private final NetworkInfo networkInfo;

    // Parameters controlling Alea's behavior and performance.
    private final Params params;

    // Threshold signature utils.
    private final ThreshsigUtil threshsigUtil;

    // The local priority value
    private AtomicLong priority = new AtomicLong();

    // Queue of pending requests (ignored when batching is inactive)
    private Queue<byte[]> pendingQueue = new ConcurrentLinkedQueue<>();

    // The priority queues (one per replica)
    private Map<Integer, PriorityQueue> queues = new ConcurrentHashMap<>();

    // A map of validated consistent broadcast instances by pid
    private Map<BcPid, IVBroadcast> bcInstances = new ConcurrentHashMap<>();

    // A map of binary agreement instances by pid
    private Map<BaPid, IBinaryAgreement> baInstances = new ConcurrentHashMap<>();

    // The current number of active broadcast instances
    private AtomicInteger broadcastState;

    // The current agreement state
    private AgreementState agreementState;

    // The set of executed commands
    private Set<byte[]> executed = new HashSet<>();

    private final Lock broadcastComponentLock = new ReentrantLock();
    private final Lock agreementComponentLock = new ReentrantLock();
    private ExecutionLog<Block> executionLog = new ExecutionLog<>("ALEA");

    // Map from broadcast instances id to execution logs
    // An execution log for a broadcast instance
    private Map<BcPid, ExecutionLog<byte[]>> bcLog = new ConcurrentHashMap<>();
    // Map from binary agreement ids to execution logs
    private Map<BaPid, ExecutionLog<Boolean>> baLog = new ConcurrentHashMap<>();

    public Alea(Integer replicaId, NetworkInfo networkInfo, Params params) {
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;
        this.params = params;

        this.threshsigUtil = new ThreshsigUtil(networkInfo.getGroupKey(), networkInfo.getKeyShare());

        // Initialize the priority queues
        for (int id=0; id<networkInfo.getN(); id++)
            this.queues.put(id, new PriorityQueue(id));

        this.broadcastState = new AtomicInteger(0);
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

    public Params getParams() {
        return params;
    }

    @Override
    public Long getPriority() {
        return priority.get();
    }

    @Override
    public Map<Integer, PriorityQueue> getQueues() {
        return queues;
    }

    @Override
    public IVBroadcast getVBroadcastInstance(BcPid bcPid) {
        this.bcLog.computeIfAbsent(bcPid, p -> new ExecutionLog<>(p.toString()));
        return this.bcInstances.computeIfAbsent(bcPid,
                p -> new EchoVBroadcast2(
                        p.toString(),
                        replicaId,
                        networkInfo,
                        p.getQueueId()
                )
        );
    }

    @Override
    public IBinaryAgreement getBinaryAgreementInstance(BaPid baPid) {
        this.baLog.computeIfAbsent(baPid, p -> new ExecutionLog<>(p.toString()));
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
        // if (agreementState instanceof ProposingState) System.out.println(printState());
        return this.agreementState.tryProgress();
    }

    public ExecutionLog<Block> getExecutionLog() {
        // children = list of all broadcast component logs + all binary agreement logs
        List<ExecutionLog<?>> children = Stream.concat(bcLog.values().stream(), baLog.values().stream())
                .collect(Collectors.toList());

        this.executionLog.setChildren(children);
        return this.executionLog;
    }

    @Override
    public Step<Block> handleInput(byte[] input) {
        logger.info("Alea received input");

        Step<Block> step = new Step<>();
        if (params.getFault(replicaId) == Params.Fault.CRASH) return step;

        // Check if the input command was previously executed
        if (this.getExecuted().contains(input)) {
            step.addFault("ABC", "CMD ALREADY EXECUTED");
            return step;
        }

        // TODO check if replicaId is one of the matches (never order the same request twice)
        /*int matches = (int) this.getQueues().values().stream().filter(q -> q.contains(input)).count();
        if (matches > this.getNetworkInfo().getF()) {
            step.addFault("ABC", "COMMAND ALREADY QUEUED");
            return step;
        }*/

        // add input to queue of pending requests
        // logger.info("adding {} to pending queue {}", input, this.pendingQueue);
        this.pendingQueue.add(input);
        // logger.info("added to pending queue - {}", this.pendingQueue);

        return this.tryPropose();
    }

    @Override
    public Step<Block> handleMessage(ProtocolMessage message) {
        // logger.info("handleMessage - msg:{}", message);
        if (params.getFault(replicaId) == Params.Fault.CRASH) return new Step<>();

        Step<Block> step;

        if (message instanceof VBroadcastMessage) {
            // Route a verifiable broadcast message
            broadcastComponentLock.lock();
            step = this.handleVBroadcastMessage((VBroadcastMessage) message);
            broadcastComponentLock.unlock();

        } else if (message instanceof BinaryAgreementMessage) {
            // Route a binary agreement message
            agreementComponentLock.lock();
            step = this.handleBinaryAgreementMessage((BinaryAgreementMessage) message);
            agreementComponentLock.unlock();

        } else {
            // Route sub-protocol messages
            agreementComponentLock.lock();
            step = this.handleProtocolMessage(message);
            agreementComponentLock.unlock();
        }

        return step;
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
    public Step<Block> handleBinaryAgreementMessage(BinaryAgreementMessage message){
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
        // logger.info(fillGapMessage.toString());
        Step<Block> step = new Step<>();

        final Integer senderId = fillGapMessage.getSender();
        final Integer queueId = fillGapMessage.getQueue();
        final Long slotId = fillGapMessage.getSlot();

        PriorityQueue queue = this.getQueues().get(queueId);
        Optional<Slot> optionalSlot = queue.get(slotId);

        if (optionalSlot.isPresent()) {
            Slot slot = optionalSlot.get();
            FillerMessage fillerMessage = new FillerMessage(
                    "ABC", replicaId, queueId, slotId, slot.getValue(), slot.getProof());
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
            step.addFault("ABC", "UNEXPECTED FILLER MESSAGE");
            return step;
        }

        // Verify the threshold signature proof
        String toVerify = String.format("BC-%d-%d-%s", queueId, slotId, Base64.getEncoder().encodeToString(value));
        if (!params.isBenchmark() && !threshsigUtil.verify(toVerify.getBytes(), proof)) {
            step.addFault("ABC", "INVALID PROOF");
            return step;
        }

        // Place the (value, proof) pair in the corresponding queue slot
        queue.enqueue(slotId, value, proof);

        return this.tryProgress();
    }

    @Override
    public Step<Block> handleVBroadcastStep(Step<VOutput> bcStep, BcPid bcPid) {

        Step<Block> step = new Step<>(bcStep.getMessages());
        // If broadcast produced no output, then don't move on
        if (bcStep.getOutput().isEmpty()) {
            return step;
        }

        // set finish time for duration benchmark
        // TODO: (dsa) don't know what this is for
        this.bcLog.get(bcPid).setResult(null);

        // logger.info("Delivered proposal from replica-{} for slot-{}", bcPid.getQueueId(), bcPid.getSlotId());

        // add pre-ordered request to the corresponding priority queue
        logger.info("New command from VCBC -> enqueuing (queue from replica: {}, priority: {})", bcPid.getQueueId(), bcPid.getSlotId());
        PriorityQueue queue = queues.get(bcPid.getQueueId());
        VOutput output = bcStep.getOutput().firstElement();
        queue.enqueue(bcPid.getSlotId(), output.getValue(), output.getSignature());
        if (this.executed.contains(output.getValue())) {
            logger.info("Command had already been executed -> dequeuing");
            queue.dequeue(bcPid.getSlotId());
        }

        // remove VCBC instance
        this.bcInstances.remove(bcPid);

        if (bcPid.getQueueId().equals(replicaId)) {
            // decrease the number of active broadcast instances
            int bcActive = this.broadcastState.decrementAndGet();
            if (bcActive < this.params.getMaxConcurrentBroadcasts())
                step.add(this.tryPropose());
        }

        // Move to ABA
        step.add(this.tryProgress());

        return step;
    }

    @Override
    public Step<Block> handleBinaryAgreementStep(Step<Boolean> baStep, BaPid baPid) {
        Step<Block> step = new Step<>(baStep.getMessages());

        // set finish time for duration benchmark
        if (!baStep.getOutput().isEmpty()) {
            this.baLog.get(baPid).setResult(baStep.getOutput().firstElement());
        }

        step.add(this.tryProgress());

        return step;
    }

    /**
     * Try to propose a value to be included in PriorityQueues
     * @return
     */
    private synchronized Step<Block> tryPropose() {
        // do not propose if there is nothing to propose
        if (this.pendingQueue.isEmpty()) {
            return new Step<>();
        }

        // do not propose if the number of concurrent VCBC instances exceeds the maximum value in params
        if (this.broadcastState.get() >= this.params.getMaxConcurrentBroadcasts()) {
            logger.info("broadcast maximum reached");
            return new Step<>();
        }

        // do not propose if the pipeline offset exceed the maximum value in the params
        final long pipelineOffset = this.priority.get() - this.getQueues().get(replicaId).getHead();
        if (pipelineOffset >= params.getMaxPipelineOffset()) {
            logger.info("pipeline offset exceed");
            return new Step<>();
        }

        // group pending entries into a byte encoded batch
        List<byte[]> entries = new ArrayList<>();

        logger.info("tryPropose started");
        // select entries from pending queue
        synchronized (this.pendingQueue) {
            if (this.pendingQueue.size() < params.getBatchSize()) {
                logger.info("batch is not big enough");
                return new Step<>();
            }
            for (int i=0; i < Math.min(params.getBatchSize(), this.pendingQueue.size()); i++) {
                entries.add(this.pendingQueue.poll());
            }
        }

        if (entries.isEmpty()) {
            logger.info("tryPropose aborted - not enough pending requests to form a batch");
            return new Step<>();
        }

        byte[] batch = encodeBatchEntries(entries);

        // input batch to VCBC
        BcPid bcPid = new BcPid("VCBC", replicaId, priority.getAndIncrement());
        IVBroadcast instance = this.getVBroadcastInstance(bcPid);
        Step<VOutput> bcStep = instance.handleInput(batch);

        // increment the concurrent VCBC instances counter
        broadcastState.incrementAndGet();

        return this.handleVBroadcastStep(bcStep, bcPid);
    }

    private Step<Block> tryProgress() {
        logger.info("tryProgress called");
        Step<Block> step = this.agreementState.tryProgress();

        if (!step.getOutput().isEmpty()) {
            logger.info("tryProgress was successful - there's something to output");
            for (Block block: step.getOutput()) {
                executed.add(block.getContent());
            }
            step.add(this.tryPropose().getMessages());
        }
        return step;
    }

    // Parameters controlling Alea's behavior and performance.
    public static class Params {

        public enum Fault {
            FREE,
            CRASH,
            BYZANTINE
        }

        // batch size
        private Integer batchSize;

        // maximum number of VCBC concurrent instances
        private Integer maxConcurrentBroadcasts;

        // maximum offset between broadcast and agreement components
        private Integer maxPipelineOffset;

        private Map<Integer, Fault> faults;

        private boolean benchmark;

        private Integer maxPayloadSize;

        public static class Builder {
            private int batchSize = 8;
            private int maxConcurrentBroadcasts = 3;
            private int maxPipelineOffset = 5;
            private Map<Integer, Fault> faults = new HashMap<>();
            private boolean benchmark = false;
            private int maxPayloadSize = 250;

            public Builder() {}

            public Builder batchSize(int batchSize) {
                this.batchSize = batchSize;
                return this;
            }

            public Builder maxConcurrentBroadcasts(int maxConcurrentBroadcasts) {
                this.maxConcurrentBroadcasts = maxConcurrentBroadcasts;
                return this;
            }

            public Builder maxPipelineOffset(int maxPipelineOffset) {
                this.maxPipelineOffset = maxPipelineOffset;
                return this;
            }

            public Builder faults(Map<Integer, Fault> faults) {
                this.faults = faults;
                return this;
            }

            public Builder benchmark(boolean benchmark) {
                this.benchmark = benchmark;
                return this;
            }

            public Builder maxPayloadSize(int maxPayloadSize) {
                this.maxPayloadSize = maxPayloadSize;
                return this;
            }

            public Params build() {
                Params params = new Params();

                params.batchSize = this.batchSize;
                params.maxConcurrentBroadcasts = this.maxConcurrentBroadcasts;
                params.maxPipelineOffset = this.maxPipelineOffset;
                params.faults = this.faults;
                params.benchmark = this.benchmark;
                params.maxPayloadSize = this.maxPayloadSize;

                return params;
            }
        }

        private Params() {
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public Integer getMaxConcurrentBroadcasts() {
            return maxConcurrentBroadcasts;
        }

        public Integer getMaxPipelineOffset() {
            return maxPipelineOffset;
        }

        public Integer getMaxPayloadSize() {
            return maxPayloadSize;
        }

        public Fault getFault(Integer replicaId) {
            return faults.getOrDefault(replicaId, Fault.FREE);
        }

        public boolean isBenchmark() {
            return this.benchmark;
        }
    }

    public static byte[] encodeBatchEntries(Collection<byte[]> entries) {
        // logger.info("encodeBatchEntries started - {}", entries);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        try {
            out.writeInt(entries.size());
            for (byte[] entry : entries) {
                out.writeInt(entry.length);
                out.write(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // logger.info("encodeBatchEntries ended - {}", baos.toByteArray());
        return baos.toByteArray();
    }

    public static Collection<byte[]> decodeBatchEntries(byte[] encoded) {
        // logger.info("decodeBatchEntries started - {}", encoded);
        ByteArrayInputStream bais = new ByteArrayInputStream(encoded);
        DataInputStream in = new DataInputStream(bais);

        Set<byte[]> entries = new HashSet<>();
        try {
            int count = in.readInt();
            for (int i=0; i < count; i++) {
                int size = in.readInt();
                entries.add(in.readNBytes(size));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // logger.info("decodeBatchEntries ended - {}", entries);
        return entries;
    }

    public String printState() {
        StringBuilder sb = new StringBuilder();

        // add queue headers
        for (Integer replicaId: this.networkInfo.getAllIds())
            sb.append(String.format("| Q_%d |", replicaId));
        sb.append("\n");

        Collection<Long> priorities = this.getQueues().values().stream().map(PriorityQueue::getHead).collect(Collectors.toList());
        long minPriority = Collections.min(priorities);
        // long minPriority = Math.max(Collections.min(priorities)-3, 0L);
        // long maxPriority = Collections.max(priorities);
        long maxPriority = Math.max(Collections.max(priorities)+3, minPriority+3);;

        // add queue bodies
        for (long i=0; i<(maxPriority - minPriority); i++) {
            final long p = minPriority + i;
            for (Integer replicaId: this.networkInfo.getAllIds()) {
                PriorityQueue pq = this.getQueues().get(replicaId);
                Optional<Slot> slot = pq.get(p);
                sb.append(p == pq.getHead() ? "|>" : "| ");
                sb.append(slot.isPresent() ? "[X] |" : "[ ] |");

            }
            sb.append(String.format(" (%d) \n", p));
        }

        // add round pointer
        for (long i=0; i<this.agreementState.getQueue().getId(); i++) sb.append("       ");
        sb.append("   *\n\n");

        return sb.toString();
    }

    public void reset() {
        broadcastState = new AtomicInteger(0);
        agreementState = new ProposingState(this, 0L);
        priority = new AtomicLong();
        pendingQueue = new ConcurrentLinkedQueue<>();
        bcInstances = new ConcurrentHashMap<>();
        baInstances = new ConcurrentHashMap<>();
        executed = new HashSet<>();
        executionLog = new ExecutionLog<>("ALEA");
        bcLog = new ConcurrentHashMap<>();
        baLog = new ConcurrentHashMap<>();

        for (int id=0; id<networkInfo.getN(); id++) {
            this.queues.put(id, new PriorityQueue(id));
        }
    }
}

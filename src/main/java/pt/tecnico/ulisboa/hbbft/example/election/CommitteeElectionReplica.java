package pt.tecnico.ulisboa.hbbft.example.election;

import pt.tecnico.ulisboa.hbbft.*;
import pt.tecnico.ulisboa.hbbft.election.CommitteeElection;
import pt.tecnico.ulisboa.hbbft.election.CommitteeElectionMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class CommitteeElectionReplica {

    private final Integer replicaId;
    private final NetworkInfo networkInfo;
    private final GroupKey groupKey;
    private final KeyShare keyShare;
    private final Integer committeeSize;
    private final MessageEncoder<String> encoder;
    private final Transport<String> transport;

    private final AtomicLong count = new AtomicLong();
    private final Map<String, CommitteeElection> instances = new HashMap<>();

    public CommitteeElectionReplica(
            Integer replicaId,
            NetworkInfo networkInfo,
            GroupKey groupKey,
            KeyShare keyShare,
            Integer committeeSize,
            MessageEncoder<String> encoder,
            Transport<String> transport
    ) {
        this.replicaId = replicaId;
        this.networkInfo = networkInfo;
        this.groupKey = groupKey;
        this.keyShare = keyShare;
        this.committeeSize = committeeSize;
        this.encoder = encoder;
        this.transport = transport;
    }

    public Integer getId() {
        return replicaId;
    }

    public void propose() {
        // Create committee election instance
        String pid = String.format("CE-%d", count.getAndIncrement());
        CommitteeElection instance = instances.computeIfAbsent(pid, this::createInstance);

        // Propose
        Step<Set<Integer>> step = instance.handleInput(true);
        this.handleStep(step);
    }

    public synchronized void handleMessage(String data) {
        CommitteeElectionMessage message = (CommitteeElectionMessage) encoder.decode(data);
        final String pid = message.getPid();

        CommitteeElection instance = instances.computeIfAbsent(pid, this::createInstance);
        Step<Set<Integer>> step = instance.handleMessage(message);
        this.handleStep(step);
    }

    private void handleStep(Step<Set<Integer>> step) {
        for (TargetedMessage message : step.getMessages()) {
            String encoded = this.encoder.encode(message.getContent());
            this.transport.sendToReplica(message.getTarget(), encoded);
        }

        // Output if terminated
        for (Set<Integer> output : step.getOutput())
            System.out.println(String.format("(%d) Delivered: %s", replicaId, output.toString()));
    }

    private CommitteeElection createInstance(String pid) {
        return new CommitteeElection(pid, replicaId, networkInfo, committeeSize);
    }
}

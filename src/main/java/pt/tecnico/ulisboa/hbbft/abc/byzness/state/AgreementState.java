package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.byzness.BaPid;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.Byzness;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;

public abstract class AgreementState {

    protected Byzness byzness;
    protected Long epoch;

    public AgreementState(Byzness byzness, Long epoch) {
        this.byzness = byzness;
        this.epoch = epoch;
    }

    public PriorityQueue getQueue() {
        int queueId = (int) (epoch % this.byzness.getNetworkInfo().getN());
        return this.byzness.getQueues().get(queueId);
    }

    public IBinaryAgreement getBaInstance() {
        PriorityQueue queue = this.getQueue();
        BaPid baPid = new BaPid("BA", queue.getId(), queue.getHead(), epoch);
        return this.byzness.getBinaryAgreementInstance(baPid);
    }

    public abstract Step<Block> tryProgress();
}

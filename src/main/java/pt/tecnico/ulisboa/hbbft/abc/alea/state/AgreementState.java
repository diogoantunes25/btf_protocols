package pt.tecnico.ulisboa.hbbft.abc.alea.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.alea.Alea;
import pt.tecnico.ulisboa.hbbft.abc.alea.BaPid;
import pt.tecnico.ulisboa.hbbft.abc.alea.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;

public abstract class AgreementState {

    protected Logger logger = LoggerFactory.getLogger(AgreementState.class);

    protected Alea alea;
    protected Long epoch;

    public AgreementState(Alea alea, Long epoch) {
        this.alea = alea;
        this.epoch = epoch;
    }

    public PriorityQueue getQueue() {
        int queueId = (int) (epoch % this.alea.getNetworkInfo().getN());
        return this.alea.getQueues().get(queueId);
    }

    public IBinaryAgreement getBaInstance() {
        BaPid baPid = new BaPid("BA", epoch);
        return this.alea.getBinaryAgreementInstance(baPid);
    }

    public abstract Step<Block> tryProgress();
}

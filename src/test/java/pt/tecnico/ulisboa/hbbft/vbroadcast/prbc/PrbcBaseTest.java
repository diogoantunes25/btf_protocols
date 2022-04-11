package pt.tecnico.ulisboa.hbbft.vbroadcast.prbc;

import org.junit.jupiter.api.BeforeEach;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolTest;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.util.Map;
import java.util.TreeMap;

public class PrbcBaseTest extends ProtocolTest {

    protected final Integer PROPOSER = 0;

    protected Map<Integer, IProvableReliableBroadcast> instances;

    @BeforeEach
    public void init() {
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);

        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        this.instances = new TreeMap<>();
        for (int replicaId: validatorSet.getAllIds()) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet, groupKey, keyShares[replicaId]);
            IProvableReliableBroadcast instance = new ProvableReliableBroadcast("PRBC-0", replicaId, networkInfo, PROPOSER);
            this.instances.put(replicaId, instance);
        }

        this.populate4Test();
    }

    public void populate4Test() {}
}

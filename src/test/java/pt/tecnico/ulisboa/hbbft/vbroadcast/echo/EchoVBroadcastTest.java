package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolTest;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.util.*;

public abstract class EchoVBroadcastTest extends ProtocolTest {

    protected Map<Integer, EchoVBroadcast> instances;
    protected EchoVBroadcast proposerInstance;

    @BeforeAll
    static void setup() {
        // TODO setup keys
    }

    @BeforeEach
    public void init() {
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);

        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        String pid = String.format("vCBV-%d-%d", REPLICA_ID, 0);

        this.instances = new TreeMap<>();
        for (int replicaId = 0; replicaId < NUM_REPLICAS; replicaId++) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet, groupKey, keyShares[replicaId]);
            EchoVBroadcast instance = new EchoVBroadcast(pid, replicaId, networkInfo, REPLICA_ID);
            this.instances.put(replicaId, instance);
        }
        this.proposerInstance = instances.get(REPLICA_ID);
    }
}

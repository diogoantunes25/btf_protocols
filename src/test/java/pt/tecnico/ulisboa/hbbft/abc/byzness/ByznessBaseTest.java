package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolTest;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.util.Map;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
public abstract class ByznessBaseTest extends ProtocolTest {

    protected final byte[] COMMAND = "COMMAND".getBytes();

    protected Map<Integer, Byzness> instances;

    @Mock
    protected Byzness instance;

    @BeforeEach
    public void init() {
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);

        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        this.instances = new TreeMap<>();
        for (int replicaId = 0; replicaId < NUM_REPLICAS; replicaId++) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet, groupKey, keyShares[replicaId]);
            Byzness instance = new Byzness(replicaId, networkInfo);
            this.instances.put(replicaId, instance);
        }
        this.instance = Mockito.spy(instances.get(0));

        this.populate4Test();
    }

    public void populate4Test() {}
}

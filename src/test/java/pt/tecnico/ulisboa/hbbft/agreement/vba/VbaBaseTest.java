package pt.tecnico.ulisboa.hbbft.agreement.vba;

import pt.tecnico.ulisboa.hbbft.ProtocolTest;

public abstract class VbaBaseTest extends ProtocolTest {

    /*protected Map<Integer, IValidatedBinaryAgreement> instances;

    @BeforeEach
    public void init() {
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);

        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        this.instances = new TreeMap<>();
        for (int replicaId = 0; replicaId < NUM_REPLICAS; replicaId++) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet, groupKey, keyShares[replicaId]);
            IValidatedBinaryAgreement instance = new ValidatedBinaryAgreement("VBA", replicaId, networkInfo);
            this.instances.put(instance);
        }

        this.populate4Test();
    }

    public void populate4Test() {}*/
}

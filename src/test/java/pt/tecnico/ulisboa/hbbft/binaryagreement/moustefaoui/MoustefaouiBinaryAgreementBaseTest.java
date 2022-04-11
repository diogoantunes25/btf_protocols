package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolTest;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public abstract class MoustefaouiBinaryAgreementBaseTest extends ProtocolTest {

    @Mock
    protected IMoustefaouiBinaryAgreement instance;

    @BeforeEach
    public void init() {
        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);
        NetworkInfo networkInfo = new NetworkInfo(REPLICA_ID, validatorSet, groupKey, keyShares[REPLICA_ID]);
        this.instance = spy(new MoustefaouiBinaryAgreement("BA-0", REPLICA_ID, networkInfo));

        this.populate4Test();
    }

    public void populate4Test() {};
}

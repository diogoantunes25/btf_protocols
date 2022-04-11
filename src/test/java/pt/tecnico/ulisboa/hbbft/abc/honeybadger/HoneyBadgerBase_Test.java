package pt.tecnico.ulisboa.hbbft.abc.honeybadger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.ProtocolTest;
import pt.tecnico.ulisboa.hbbft.abc.honeybadger.crypto.NeverEncrypt;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementFactory;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.MoustefaouiBinaryAgreementFactory;
import pt.tecnico.ulisboa.hbbft.broadcast.BroadcastFactory;
import pt.tecnico.ulisboa.hbbft.broadcast.bracha.BrachaBroadcastFactory;
import pt.tecnico.ulisboa.hbbft.subset.hbbft.HoneyBadgerSubsetFactory;

import java.util.Map;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
public abstract class HoneyBadgerBase_Test extends ProtocolTest {

    protected Map<Integer, IHoneyBadger> instances;

    @Mock
    protected IHoneyBadger honeyBadger;

    @BeforeEach
    public void init() {
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(VALIDATORS, TOLERANCE);

        this.instances = new TreeMap<>();
        for (int replicaId = 0; replicaId < NUM_REPLICAS; replicaId++) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet);

            Params params = new Params(100L, new NeverEncrypt(), 8);

            BroadcastFactory bcFactory = new BrachaBroadcastFactory(replicaId, networkInfo);
            BinaryAgreementFactory baFactory = new MoustefaouiBinaryAgreementFactory(replicaId, networkInfo);
            HoneyBadgerSubsetFactory subsetFactory = new HoneyBadgerSubsetFactory(replicaId, networkInfo, bcFactory, baFactory);

            IHoneyBadger instance = new HoneyBadger(replicaId, networkInfo, params, subsetFactory);
            this.instances.put(replicaId, instance);
            if (replicaId == 0) {
                this.honeyBadger = instance;
            }
        }

        this.populate4Test();
    }

    public void populate4Test() {}
}

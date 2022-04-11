package pt.tecnico.ulisboa.hbbft.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

public class ThreshsigUtilsTest {

    public static final int K = 3;
    public static final int L = 5;
    public static final int KEY_SIZE = 512;

    private Dealer dealer;

    @BeforeEach
    public void init() {
        this.dealer = ThreshsigUtils.sigSetup(K, L, KEY_SIZE);
    }

    @Test
    public void sigSetupTest() {
        // When
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        // Then
        Assertions.assertNotNull(groupKey);
        Assertions.assertEquals(K, groupKey.getK());
        Assertions.assertEquals(L, groupKey.getL());
        // And
        Assertions.assertEquals(L, keyShares.length);
    }

    @Test
    public void sigShareTest() {
        // TODO
    }

    @Test
    public void givenValidSigShare_whenVerifyShare_thenSuccess() {
        // Given
        byte[] value = "Hello World".getBytes();
        //SigShare share = ThreshsigUtils.sigShare(value, keyShares.get(0));
    }
}

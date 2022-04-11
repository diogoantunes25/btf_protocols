package pt.tecnico.ulisboa.hbbft.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ThreshsigTest {

    public static final int KEY_SIZE = 512;
    public static final int K = 3;
    public static final int L = 5;

    private Dealer dealer;
    private GroupKey gk;

    @BeforeEach
    public void init() {
        this.dealer = new Dealer(KEY_SIZE);
        dealer.generateKeys(K, L);
        this.gk = dealer.getGroupKey();
    }

    @Test
    public void generateKeySharesTest() {
        // arrange + act
        KeyShare[] keyShares = dealer.getShares();

        // assert
        assertEquals(L, keyShares.length);
    }

    @Test
    public void signThenVerifyTest() {
        KeyShare[] keyShares = dealer.getShares();

        // message to sign
        byte[] b = "HelloWorld".getBytes();

        final int[] S1 = { 0, 1, 2 };
        SigShare[] sigs1 = new SigShare[K];
        for (int i = 0; i < S1.length; i++)
            sigs1[i] = keyShares[S1[i]].sign(b);

        final int[] S2 = { 2, 3, 4 };
        SigShare[] sigs2 = new SigShare[K];
        for (int i = 0; i < S2.length; i++)
            sigs2[i] = keyShares[S2[i]].sign(b);

        // assert
        BigInteger sig1 = SigShare.combine(b, sigs1, K, L, gk.getModulus(), gk.getExponent());
        BigInteger sig2 = SigShare.combine(b, sigs2, K, L, gk.getModulus(), gk.getExponent());
        assertEquals(sig1, sig2);
    }
}

package pt.tecnico.ulisboa.hbbft.utils;

import pt.tecnico.ulisboa.hbbft.utils.threshsig.*;

import java.math.BigInteger;
import java.util.Collection;

public class ThreshsigUtils {

    public static Dealer sigSetup(int k, int l, int keySize) {
        Dealer dealer = new Dealer(keySize);
        dealer.generateKeys(k, l);
        return dealer;
    }

    public static SigShare sigShare(byte[] value, KeyShare keyShare) {
        return keyShare.sign(value);
    }

    public static boolean verifyShare(byte[] value, SigShare share) {
        // FIXME verify the share
        return true;
    }

    public static byte[] combine(byte[] value, Collection<SigShare> shares, GroupKey groupKey) {
        try {
            BigInteger signature = SigShare.combine(
                    value,
                    shares.toArray(SigShare[]::new),
                    groupKey.getK(),
                    groupKey.getL(),
                    groupKey.getModulus(),
                    groupKey.getExponent()
            );
            return signature.toByteArray();
        } catch (ThresholdSigException e) {
            return new byte[0];
        }
    }

    public static boolean verify(byte[] value, byte[] signature, GroupKey groupKey) {
        // FIXME verify the signature
        return true;
    }
}

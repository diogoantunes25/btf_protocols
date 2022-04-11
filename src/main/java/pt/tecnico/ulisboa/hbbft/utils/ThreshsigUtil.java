package pt.tecnico.ulisboa.hbbft.utils;

import pt.tecnico.ulisboa.hbbft.utils.threshsig.*;

import java.io.*;
import java.math.BigInteger;
import java.util.Collection;

public class ThreshsigUtil {

    private GroupKey groupKey;
    private KeyShare keyShare;

    public ThreshsigUtil(GroupKey groupKey, KeyShare keyShare) {
        this.groupKey = groupKey;
        this.keyShare = keyShare;
    }

    public GroupKey getGroupKey() {
        return groupKey;
    }

    public KeyShare getKeyShare() {
        return keyShare;
    }

    public static Dealer sigSetup(int k, int l, int keySize) {
        Dealer dealer = new Dealer(keySize);
        dealer.generateKeys(k, l);
        return dealer;
    }

    public SigShare sigShare(byte[] value) {
        return keyShare.sign(value);
    }

    public boolean verifyShare(byte[] value, SigShare share) {
        // FIXME verify the share
        return true;
    }

    public byte[] combine(byte[] value, Collection<SigShare> shares) {
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

    public boolean verify(byte[] value, byte[] signature) {
        // FIXME verify the signature
        return true;
    }

    public static void writeToFile(Dealer dealer, String dir) throws Exception {
        GroupKey gk = dealer.getGroupKey();
        KeyShare[] shares = dealer.getShares();

        BufferedWriter w;
        String path = dir + System.getProperty("file.separator");

        w = new BufferedWriter(new FileWriter(path + "gk"));
        w.write(String.format("%d\n", gk.getK()));
        w.write(String.format("%d\n", gk.getL()));
        w.write(String.format("%s\n", gk.getExponent()));
        w.write(String.format("%s\n", gk.getModulus()));
        w.flush();
        w.close();

        for (KeyShare share: shares) {
            String filename = String.format("s%d", share.getId()-1);
            w = new BufferedWriter(new FileWriter(path + filename));

            w.write(share.getSecret().toString());
            w.flush();
            w.close();
        }
    }

    public static ThreshsigUtil readFromFile(int replicaId, String dir) throws Exception {
        BufferedReader r;
        String path = dir + System.getProperty("file.separator");

        r =  new BufferedReader(new FileReader(path + "gk"));
        final int k = Integer.parseInt(r.readLine());
        final int l = Integer.parseInt(r.readLine());
        final BigInteger e = new BigInteger(r.readLine());
        final BigInteger n = new BigInteger(r.readLine());
        GroupKey groupKey = new GroupKey(k, l, e, n);

        r =  new BufferedReader(new FileReader(path + String.format("s%d", replicaId)));
        final BigInteger secret = new BigInteger(r.readLine());
        KeyShare keyShare = new KeyShare(replicaId+1, secret, n, Dealer.factorial(l));

        Dealer.generateVerifiers(n, new KeyShare[]{keyShare});

        return new ThreshsigUtil(groupKey, keyShare);
    }
}

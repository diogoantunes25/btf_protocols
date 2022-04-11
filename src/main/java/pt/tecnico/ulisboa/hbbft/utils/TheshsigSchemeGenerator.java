package pt.tecnico.ulisboa.hbbft.utils;

import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class TheshsigSchemeGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Use: TheshsigSchemeGenerator <k> <l> <key size> [config dir]");
        }

        int k = (args.length > 0) ? Integer.parseInt(args[0]) : 2;
        int l = (args.length > 1) ? Integer.parseInt(args[1]) : 4;
        int keySize = (args.length > 2) ? Integer.parseInt(args[2]) : 256;

        String dir = (args.length > 3) ? args[3] : "src/main/resources";

        Dealer dealer = ThreshsigUtil.sigSetup(k, l, keySize);
        ThreshsigUtil.writeToFile(dealer, dir);
    }
}

package pt.tecnico.ulisboa.hbbft.example.vbroadcast;

import pt.tecnico.ulisboa.hbbft.MessageEncoder;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Transport;
import pt.tecnico.ulisboa.hbbft.broadcast.BroadcastFactory;
import pt.tecnico.ulisboa.hbbft.broadcast.bracha.BrachaBroadcastFactory;
import pt.tecnico.ulisboa.hbbft.broadcast.echo.utils.SignatureUtils;
import pt.tecnico.ulisboa.hbbft.example.LocalTransport;
import pt.tecnico.ulisboa.hbbft.example.broadcast.BroadcastReplica;
import pt.tecnico.ulisboa.hbbft.example.broadcast.bracha.BrachaBroadcastMessageEncoder;
import pt.tecnico.ulisboa.hbbft.example.vbroadcast.prbc.PrbcMessageEncoder;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    /*private enum Protocol {
        ECHO,
        PRBC,
    }

    private static final int TOLERANCE = 1;
    private static final int NUM_REPLICAS = 3 * TOLERANCE + 1;
    private static final int KEY_SIZE = 256;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        Map<Integer, ValidatedBroadcastReplica> replicas = setupReplicas();
    }

    private static Map<Integer, ValidatedBroadcastReplica> setupReplicas() {
        CountDownLatch readyLatch = new CountDownLatch(NUM_REPLICAS - 1);

        Set<Integer> validators = IntStream.range(0, NUM_REPLICAS)
                .boxed().collect(Collectors.toSet());
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(validators, TOLERANCE);

        // Generate key pairs
        Map<Integer, KeyPair> keyPairs = new TreeMap<>();
        try {
            for (int i=0; i < NUM_REPLICAS; i++)
                keyPairs.put(i, SignatureUtils.generateKeyPair());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Initialize message encoder
        MessageEncoder<String> rbcEncoder = new BrachaBroadcastMessageEncoder();
        MessageEncoder<String> encoder = new PrbcMessageEncoder(rbcEncoder);

        // Initialize transport layer
        Map<Integer, BlockingQueue<String>> messageQueue = new ConcurrentHashMap<>();
        Transport<String> transport = new LocalTransport(messageQueue);

        Map<Integer, BroadcastReplica> replicas = new TreeMap<>();
        for (int replicaId: validatorSet.getAllIds()) {
            NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet);

            BroadcastFactory rbcFactory = new BrachaBroadcastFactory(replicaId, networkInfo);
        }
    }*/
}

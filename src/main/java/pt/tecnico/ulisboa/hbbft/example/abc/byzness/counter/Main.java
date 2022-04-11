package pt.tecnico.ulisboa.hbbft.example.abc.byzness.counter;

import pt.tecnico.ulisboa.hbbft.MessageEncoder;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Transport;
import pt.tecnico.ulisboa.hbbft.example.RedisTransport;
import pt.tecnico.ulisboa.hbbft.example.binaryagreement.moustefaoui.MoustefaouiBinaryAgreementMessageEncoder;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.ByznessMessageEncoder;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.ByznessReplica;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.EchoVBroadcastMessageEncoder;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtils;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.Dealer;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.GroupKey;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.KeyShare;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {

    private static final int TOLERANCE = 1;
    private static final int NUM_REPLICAS = 3 * TOLERANCE + 1;
    private static final int KEY_SIZE = 256;

    public static void main(String[] args) throws InterruptedException {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");

        try (JedisPool pool = new JedisPool("192.168.1.117")) {
            // Wait for PubSub listeners to setup
            Thread.sleep(1000);

            Map<Integer, ByznessReplica> replicas = setupReplicas(pool);
            //replicas.get(0).propose("ByzNESS".getBytes());
            //replicas.get(3).propose("test-0".getBytes());
            replicas.get(2).propose("test-1".getBytes());
            //replicas.get(3).propose("test-2".getBytes());
            //replicas.get(3).propose("test-3".getBytes());
            //replicas.get(1).propose("test-4".getBytes());

            while (true) {}
        }
    }

    private static Map<Integer, ByznessReplica> setupReplicas(JedisPool pool) {
        CountDownLatch readyLatch = new CountDownLatch(NUM_REPLICAS - 1);

        Set<Integer> validators = IntStream.rangeClosed(0, NUM_REPLICAS)
                .boxed().collect(Collectors.toSet());
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(validators, TOLERANCE);

        // Initialize message encoders
        MessageEncoder<String> vbcEncoder = new EchoVBroadcastMessageEncoder();
        MessageEncoder<String> baEncoder = new MoustefaouiBinaryAgreementMessageEncoder();
        MessageEncoder<String> encoder = new ByznessMessageEncoder(vbcEncoder, baEncoder);

        // Initialize communication layer
        Transport<String> transport = new RedisTransport(pool, NUM_REPLICAS);

        // Generate threshold keys and shares
        Dealer dealer = ThreshsigUtils.sigSetup(TOLERANCE + 1, NUM_REPLICAS, KEY_SIZE);
        GroupKey groupKey = dealer.getGroupKey();
        KeyShare[] keyShares = dealer.getShares();

        Map<Integer, ByznessReplica> replicas = new TreeMap<>();
        for (int i=0; i < NUM_REPLICAS; i++) {
            NetworkInfo networkInfo = new NetworkInfo(i, validatorSet, groupKey, keyShares[i]);

            // Setup replica
            ByznessReplica replica = new CounterReplica(i, networkInfo, encoder, transport);
            replicas.put(replica.getId(), replica);

            // Start message listener thread
            Thread listenerThread = new Thread(() -> {
                try (Jedis jedis = pool.getResource()) {
                    String channel = "replica-" + replica.getId();
                    JedisPubSub listener = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            replica.handleMessage(message);
                        }
                    };
                    readyLatch.countDown();
                    jedis.subscribe(listener, channel);
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();
        }

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }
        return replicas;
    }
}

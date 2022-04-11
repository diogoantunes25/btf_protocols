package pt.tecnico.ulisboa.hbbft.example.abc.byzness.counter;

import pt.tecnico.ulisboa.hbbft.*;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.example.RedisTransport;
import pt.tecnico.ulisboa.hbbft.example.binaryagreement.moustefaoui.MoustefaouiBinaryAgreementMessageEncoder;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.ByznessMessageEncoder;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.ByznessReplica;
import pt.tecnico.ulisboa.hbbft.example.abc.byzness.EchoVBroadcastMessageEncoder;
import pt.tecnico.ulisboa.hbbft.utils.ThreshsigUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CounterReplica extends ByznessReplica {

    public static int NUM_REPLICAS = 4;
    public static int TOLERANCE = 1;

    private final LinkedBlockingQueue<Block> blocks = new LinkedBlockingQueue<>();

    public CounterReplica(
            Integer replicaId,
            NetworkInfo networkInfo,
            MessageEncoder<String> encoder,
            Transport<String> transport
    ) {
        super(replicaId, networkInfo, encoder, transport);
    }

    @Override
    public void deliver(Block block) {
        blocks.add(block);
        System.out.println(String.format("(p%d) Delivered: %s",
                this.getId(), new String(block.getContent(), StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java ... CounterClient <replica id>");
            System.exit(-1);
        }

        final int replicaId = Integer.parseInt(args[0]);

        ThreshsigUtil tsUtil = ThreshsigUtil.readFromFile(replicaId,"assets");

        Set<Integer> validators = IntStream.rangeClosed(0, NUM_REPLICAS)
                .boxed().collect(Collectors.toSet());
        NetworkInfo.ValidatorSet validatorSet = new NetworkInfo.ValidatorSet(validators, TOLERANCE);
        NetworkInfo networkInfo = new NetworkInfo(replicaId, validatorSet, tsUtil.getGroupKey(), tsUtil.getKeyShare());

        // Initialize message encoders
        MessageEncoder<String> vbcEncoder = new EchoVBroadcastMessageEncoder();
        MessageEncoder<String> baEncoder = new MoustefaouiBinaryAgreementMessageEncoder();
        MessageEncoder<String> encoder = new ByznessMessageEncoder(vbcEncoder, baEncoder);

        try (JedisPool pool = new JedisPool("192.168.1.93")) {
            // Initialize communication layer
            Transport<String> transport = new RedisTransport(pool, NUM_REPLICAS);

            // Setup replica
            CounterReplica replica = new CounterReplica(replicaId, networkInfo, encoder, transport);

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
                    jedis.subscribe(listener, channel);
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            while (true) {}
        }
    }
}

package pt.tecnico.ulisboa.hbbft.example.abc.byzness;

import pt.tecnico.ulisboa.hbbft.*;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.Byzness;

public abstract class ByznessReplica {

    // Our ID.
    private final Integer replicaId;

    // The ByzNESS protocol instance.
    protected final Byzness byzness;

    protected final MessageEncoder<String> encoder;
    protected final Transport<String> transport;

    public ByznessReplica(
            Integer replicaId,
            NetworkInfo networkInfo,
            MessageEncoder<String> encoder,
            Transport<String> transport
    ) {
        this.replicaId = replicaId;

        // Instantiate ByzNESS
        this.byzness = new Byzness(replicaId, networkInfo);

        this.encoder = encoder;
        this.transport = transport;
    }

    public Integer getId() {
        return this.replicaId;
    }

    public synchronized void propose(byte[] value) {
        Step<Block> step = byzness.handleInput(value);
        this.handleStep(step);
    }

    public synchronized void handleMessage(String data) {
        ProtocolMessage message = this.encoder.decode(data);
        Step<Block> step = byzness.handleMessage(message);
        this.handleStep(step);
    }

    private void handleStep(Step<Block> step) {
        // Send messages generated during this step
        for (TargetedMessage message : step.getMessages()) {
            String encoded = this.encoder.encode(message.getContent());
            this.transport.sendToReplica(message.getTarget(), encoded);
        }

        // Output batches delivered for each step
        for (Block block : step.getOutput()) deliver(block);
    }

    public abstract void deliver(Block block);
}

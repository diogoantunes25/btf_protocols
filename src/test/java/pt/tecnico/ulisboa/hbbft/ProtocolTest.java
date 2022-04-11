package pt.tecnico.ulisboa.hbbft;

import org.junit.jupiter.api.Assertions;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class ProtocolTest {

    protected final int TOLERANCE = 1;
    protected final int NUM_REPLICAS = 3 * TOLERANCE + 1;
    protected final int KEY_SIZE = 256;


    protected final Set<Integer> VALIDATORS = IntStream.range(0, NUM_REPLICAS)
            .boxed().collect(Collectors.toSet());

    protected final int REPLICA_ID = 0;

    public void assertEmptyStep(Step<?> step) {
        Assertions.assertAll("empty step",
                () -> Assertions.assertNotNull(step),
                () -> Assertions.assertTrue(step.getMessages().isEmpty()),
                () -> Assertions.assertTrue(step.getOutput().isEmpty())
        );
    }
}

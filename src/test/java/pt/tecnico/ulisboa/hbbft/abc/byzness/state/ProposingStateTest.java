package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.abc.byzness.ByznessBaseTest;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;

import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.*;

public class ProposingStateTest extends ByznessBaseTest {

    private ProposingState proposingState;
    private Map<Integer, PriorityQueue> queues;

    @Override
    public void populate4Test() {
        this.proposingState = spy(new ProposingState(instance, 0L));
        this.queues = new TreeMap<>();
    }

    @Test
    public void tryProgress_whenCannotProgress_thenDoNotProgress() {

    }

    @Test
    public void tryProgressState_Test() {
        // TODO
        /*Map<Integer, PriorityQueue> queues = new TreeMap<>();
        for (int id=0; id < NUM_REPLICAS; id++)
            queues.put(id, new PriorityQueue(id));

        // Given
        queues.get(0).enqueue(0, new byte[0], new byte[0]);
        doReturn(queues, queues).when(instance).getQueues();
        // And
        AgreementState proposingState = new ProposingState(instance, 0L);
        // And
        IBinaryAgreement binaryAgreement = mock(IBinaryAgreement.class);
        when(binaryAgreement.handleInput(anyBoolean())).thenReturn(new Step<>());

        // When
        Step<Block> step = proposingState.tryProgress();

        // Then*/
    }
}

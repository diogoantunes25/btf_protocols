package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillerMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.Slot;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class HandleFillerMessage_Test extends ByznessBaseTest {

    private final String PID = "BIZ"; // TODO move to ByznessBaseTest
    private final int QUEUE_ID = 2;
    private final long SLOT_ID = 42L;
    private final byte[] PROOF = "PROOF".getBytes();

    private final FillerMessage message = new FillerMessage(
            PID, REPLICA_ID, QUEUE_ID, SLOT_ID, COMMAND, PROOF);

    private PriorityQueue mQueue;

    @Override
    public void populate4Test() {
        this.mQueue = mock(PriorityQueue.class);

        Map<Integer, PriorityQueue> queues = new TreeMap<>();
        for (int id=0; id < NUM_REPLICAS; id++) {
            if (id == QUEUE_ID) queues.put(id, mQueue);
            else queues.put(id, new PriorityQueue(id));
        }

        doReturn(queues).when(instance).getQueues();
    }

    @Test
    public void handleFillerMessage_whenMissingValue_thenEnqueue() {
        // When
        Step<Block> step = instance.handleMessage(message);

        // Then
        assertEmptyStep(step);
        // And
        verify(mQueue, times(1)).enqueue(SLOT_ID, COMMAND, PROOF);
    }

    @Test
    public void handleFillerMessage_whenHasValue_thenIgnore() {
        // Given
        when(mQueue.get(SLOT_ID)).thenReturn(Optional.of(new Slot(COMMAND, PROOF)));

        // When
        Step<Block> step = instance.handleMessage(message);

        // Then
        assertNotNull(step);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getFaults().contains("BIZ:UNEXPECTED FILLER MESSAGE"));
        // And
        verify(mQueue, times(0)).enqueue(anyLong(), any(byte[].class), any(byte[].class));
    }

    //@Test
    public void handleFillerMessage_whenInvalidProof_thenIgnore() {
        // TODO
    }
}

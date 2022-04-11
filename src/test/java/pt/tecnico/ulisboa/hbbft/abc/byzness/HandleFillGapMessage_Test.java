package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillGapMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillerMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.Slot;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HandleFillGapMessage_Test extends ByznessBaseTest {

    private final Long SLOT = 42L;
    private final byte[] PROOF = "PROOF".getBytes();

    @Test
    public void handleFillGapMessage_whenHasValue_thenSendFillerMessage() {
        // Given
        FillGapMessage message = new FillGapMessage("BIZ", REPLICA_ID, 2, SLOT);
        // And
        PriorityQueue mQueue = mock(PriorityQueue.class);
        when(mQueue.get(SLOT)).thenReturn(Optional.of(new Slot(COMMAND, PROOF)));
        // And
        Map<Integer, PriorityQueue> queues = new TreeMap<>();
        for (int id=0; id < NUM_REPLICAS; id++) {
            if (id == 2) queues.put(id, mQueue);
            else queues.put(id, new PriorityQueue(id));
        }
        // And
        doReturn(queues).when(instance).getQueues();

        // When
        Step<Block> step = instance.handleMessage(message);

        // Then
        assertNotNull(step);
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getFaults().isEmpty());
        // And
        assertEquals(1, step.getMessages().size());
        TargetedMessage targetedMessage = step.getMessages().firstElement();
        assertEquals(REPLICA_ID, targetedMessage.getTarget());
        // And
        FillerMessage fillerMessage = (FillerMessage) targetedMessage.getContent();
        assertEquals(2, fillerMessage.getQueue());
        assertEquals(SLOT, fillerMessage.getSlot());
        assertEquals(COMMAND, fillerMessage.getValue());
        assertEquals(PROOF, fillerMessage.getProof());
    }

    @Test
    public void handleFillGapMessage_whenMissingValue_thenIgnore() {
        // Given
        FillGapMessage message = new FillGapMessage("BIZ", REPLICA_ID, 2, SLOT);

        // When
        Step<Block> step = instance.handleMessage(message);

        // Then
        assertNotNull(step);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getFaults().isEmpty());
    }
}

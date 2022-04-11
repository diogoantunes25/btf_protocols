package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.vbroadcast.IVBroadcast;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.EchoVBroadcast;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HandleInputTest extends ByznessBaseTest {

    @Test
    public void handleInputTest() {
        // Given
        IVBroadcast broadcast = mock(EchoVBroadcast.class);
        when(broadcast.handleInput(COMMAND)).thenReturn(new Step<>());
        // And
        BcPid bcPid = new BcPid("vCBC", instance.getReplicaId(), instance.getPriority());
        doReturn(broadcast).when(instance).getVBroadcastInstance(bcPid);
        // And
        doReturn(new Step<>()).when(instance).handleVBroadcastStep(any(), eq(bcPid));

        // When
        Step<Block> step = instance.handleInput(COMMAND);

        // Then
        verify(instance, times(1)).getVBroadcastInstance(bcPid);
        verify(broadcast, times(1)).handleInput(COMMAND);
        verify(instance, times(1)).handleVBroadcastStep(any(), eq(bcPid));
        // And
        assertEquals(1L, instance.getPriority());
        // And
        assertEmptyStep(step);
    }

    @Test
    public void handleInput_whenAlreadyExecuted_thenIgnore() {
        // Given
        doReturn(new HashSet<>(Collections.singletonList(COMMAND))).when(instance).getExecuted();

        // When
        Step<Block> step = instance.handleInput(COMMAND);

        // Then
        assertNotNull(step);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("BIZ:%s", "CMD ALREADY EXECUTED");
        assertTrue(step.getFaults().contains(fault));
    }

    @Test
    public void handleInput_whenAlreadyQueuedByCorrectReplica_thenIgnore() {
        // Given
        PriorityQueue mQueue = mock(PriorityQueue.class);
        when(mQueue.contains(COMMAND)).thenReturn(true);
        // And
        Map<Integer, PriorityQueue> queues = new TreeMap<>();
        for (int id=0; id < NUM_REPLICAS; id++) {
            if (id <= TOLERANCE) queues.put(id, mQueue);
            else queues.put(id, new PriorityQueue(id));
        }
        // And
        doReturn(queues).when(instance).getQueues();

        // When
        Step<Block> step = instance.handleInput(COMMAND);

        // Then
        assertNotNull(step);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("BIZ:%s", "COMMAND ALREADY QUEUED");
        assertTrue(step.getFaults().contains(fault));
    }
}

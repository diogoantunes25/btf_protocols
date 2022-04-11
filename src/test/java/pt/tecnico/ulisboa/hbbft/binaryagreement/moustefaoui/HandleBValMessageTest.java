package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.BValMessage;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

public class HandleBValMessageTest extends MoustefaouiBinaryAgreementBaseTest {

    /** TODO
     * Ignore multiple BVAL messages for the same valeu by the same replica
     *
     *
      */

    @Test
    public void shouldSend() {

    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void handleBValMessage_ShouldEchoBVAlIfNotSentYet_Test(Boolean value) {
        // Given
        for (int id=0; id < TOLERANCE; id++) {
            BValMessage message = new BValMessage(instance.getPid(), id, 0L, value);
            instance.handleBValMessage(message);
        }

        // When
        BValMessage message = new BValMessage(instance.getPid(), TOLERANCE, 0L, value);
        Step<Boolean> step = instance.handleBValMessage(message);

        // Then
        assertNotNull(step);
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getFaults().isEmpty());
        // And
        Vector<TargetedMessage> messages = step.getMessages();
        assertEquals(NUM_REPLICAS - 1, messages.size());
        assertTrue(messages.stream()
                .map(TargetedMessage::getContent)
                .allMatch(m -> m instanceof BValMessage && ((BValMessage) m).getValue().equals(value)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void handleBValMessage_whenDuplicateMessage_thenIgnore_Test(Boolean value) {
        // Given
        BValMessage message = new BValMessage(instance.getPid(), REPLICA_ID, instance.getRound(), value);

        // When
        instance.handleBValMessage(message);
        // And
        Step<Boolean> step = instance.handleMessage(message);

        // Then
        assertNotNull(step);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "DUPLICATE BVAL MESSAGE");
        assertTrue(step.getFaults().contains(fault));
    }
}

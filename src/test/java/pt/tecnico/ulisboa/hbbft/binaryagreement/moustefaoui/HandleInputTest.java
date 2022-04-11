package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.BValMessage;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

public class HandleInputTest extends MoustefaouiBinaryAgreementBaseTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void handleInput_Test(Boolean input) {
        // When
        Step<Boolean> step = instance.handleInput(input);

        // Then
        assertNotNull(step);
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getFaults().isEmpty());
        // And
        Vector<TargetedMessage> messages = step.getMessages();
        assertEquals(NUM_REPLICAS - 1, messages.size());
        assertTrue(messages.stream().allMatch(m -> m.getContent() instanceof BValMessage));
        // And
        assertEquals(input, instance.getEstimate());
        assertFalse(instance.hasTerminated());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void handleInput_whenAlreadyProposed_thenIgnore_Test(Boolean input) {
        // When
        instance.handleInput(input);
        Step<Boolean> step = instance.handleInput(!input);

        // Then
        assertNotNull(step);
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getMessages().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "CANNOT PROPOSE");
        Assertions.assertTrue(step.getFaults().contains(fault));
        // And
        assertEquals(input, instance.getEstimate()); // input matches the initial proposal
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void handleInput_whenNonInitialRound_thenIgnore_Test(Boolean input) {
        // Given
        doReturn(1L).when(instance).getRound();

        // When
        Step<Boolean> step = instance.handleInput(input);

        // Then
        // Then
        assertNotNull(step);
        assertTrue(step.getOutput().isEmpty());
        assertTrue(step.getMessages().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "CANNOT PROPOSE");
        Assertions.assertTrue(step.getFaults().contains(fault));
    }

    // TODO
    // 1) should not send BVAL message if already sent, after receiving f + 1 BVAL for that value
}

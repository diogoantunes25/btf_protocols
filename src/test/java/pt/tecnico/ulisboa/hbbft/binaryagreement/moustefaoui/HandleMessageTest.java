package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.utils.DummyMessage;

import static org.mockito.Mockito.doReturn;

public class HandleMessageTest extends MoustefaouiBinaryAgreementBaseTest {

    @ParameterizedTest
    @ValueSource(strings = {"BA-42", "", " "})
    public void shouldIgnoreMessagesWithInvalidPid(String pid) {
        // Given
        BinaryAgreementMessage message = new DummyMessage(pid, REPLICA_ID, 0L);

        // When
        Step<Boolean> step = instance.handleMessage(message);

        // Then
        Assertions.assertNotNull(step);
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "INVALID PID");
        Assertions.assertTrue(step.getFaults().contains(fault));
    }

    @Test
    public void shouldIgnoreMessagesAfterTerminating() {
        // Given
        doReturn(true).when(instance).hasTerminated();
        // And
        BinaryAgreementMessage message = new DummyMessage(instance.getPid(), REPLICA_ID, 0L);

        // When
        Step<Boolean> step = instance.handleMessage(message);

        // Then
        Assertions.assertNotNull(step);
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "MESSAGE OBSOLETE");
        Assertions.assertTrue(step.getFaults().contains(fault));
    }

    @Test
    public void shouldIgnoreObsoleteMessages() {
        // Given
        doReturn(42L).when(instance).getRound();
        // And
        BinaryAgreementMessage message = new DummyMessage(instance.getPid(), REPLICA_ID, 0L, true);

        // When
        Step<Boolean> step = instance.handleMessage(message);

        // Then
        Assertions.assertNotNull(step);
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "MESSAGE OBSOLETE");
        Assertions.assertTrue(step.getFaults().contains(fault));
    }

    @Test
    public void shouldIgnoreMessagesOutsideAgreementRange() {
        // Given
        Long round = 1337L;
        BinaryAgreementMessage message = new DummyMessage(instance.getPid(), REPLICA_ID, round);

        // When
        Step<Boolean> step = instance.handleMessage(message);

        // Then
        Assertions.assertNotNull(step);
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
        // And
        String fault = String.format("%s:%s", instance.getPid(), "MESSAGE OUTSIDE ROUND RANGE");
        Assertions.assertTrue(step.getFaults().contains(fault));
    }

    @Test
    public void shouldAddFutureMessagesToPendingQueue() {
        // Given
        BinaryAgreementMessage message = new DummyMessage(instance.getPid(), REPLICA_ID, 1L);
        // TODO
    }
}

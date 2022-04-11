package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;

import java.util.Vector;

public class HandleInputTest extends EchoVBroadcastTest {

    @Test
    public void givenInput_whenHandleInput_thenSucceed() {
        // Given
        byte[] input = "Hello World".getBytes(); // An input value

        // When
        Step<VOutput> step = proposerInstance.handleInput(input); // Proposing

        // Then
        Vector<TargetedMessage> messages = step.getMessages();
        Assertions.assertEquals(3, messages.size());
        Assertions.assertTrue(messages.stream()
                .allMatch(m -> m.getContent() instanceof SendMessage));

        // And
        Assertions.assertTrue(step.getOutput().isEmpty()); // No output was generated
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void handleInput_whenNotProposer_Test(Integer replicaId) {
        // Given
        byte[] input = "Hello World".getBytes();
        EchoVBroadcast instance = instances.get(replicaId);

        // When
        Step<VOutput> step = instance.handleInput(input);

        // Then
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
    }

    @Test
    public void handleInput_whenAlreadyProposed_Test() {
        // arrange
        byte[] input = "Hello World".getBytes();
        EchoVBroadcast instance = instances.get(REPLICA_ID + 1);

        // act
        instance.handleInput(input);
        Step<VOutput> step = instance.handleInput(input);

        // assert
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
    }
}

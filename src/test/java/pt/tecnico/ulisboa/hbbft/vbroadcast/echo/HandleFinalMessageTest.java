package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.FinalMessage;

import java.util.Vector;

public class HandleFinalMessageTest extends EchoVBroadcastTest {

    @Test
    public void shouldTerminateUponReceivingValidFinalMessage() {
        // Given
        EchoVBroadcast instance = instances.get(0);
        // And
        String pid = instance.getPid();
        byte[] input = "Hello World".getBytes();
        byte[] signature = "Proof".getBytes();
        FinalMessage finalMessage = new FinalMessage(pid, REPLICA_ID, input, signature);

        // When
        Step<VOutput> step = instance.handleMessage(finalMessage);

        // Then
        Assertions.assertNotNull(step);
        Assertions.assertTrue(step.getMessages().isEmpty());
        // And
        Vector<VOutput> outputs = step.getOutput();
        Assertions.assertEquals(1, outputs.size());
        // And
        VOutput output = outputs.firstElement();
        Assertions.assertAll("correct output",
                () -> Assertions.assertEquals(output.getValue(), input),
                () -> Assertions.assertEquals(output.getSignature(), signature)
        );
        // And
        Assertions.assertTrue(instance.hasTerminated());
    }

    @Test
    public void shouldIgnoreDuplicateMessages() {
        // Given
        EchoVBroadcast instance = instances.get(0);
        // And
        String pid = instance.getPid();
        byte[] input = "Hello World".getBytes();
        byte[] signature = "Proof".getBytes();
        FinalMessage finalMessage = new FinalMessage(pid, REPLICA_ID, input, signature);

        // When
        instance.handleMessage(finalMessage);
        // And
        Step<VOutput> step = instance.handleMessage(finalMessage);

        // Then
        assertEmptyStep(step);
        // And
        Assertions.assertTrue(instance.hasTerminated());
    }

    public void shouldIgnoreInvalidMessages() {
        // TODO
    }
}

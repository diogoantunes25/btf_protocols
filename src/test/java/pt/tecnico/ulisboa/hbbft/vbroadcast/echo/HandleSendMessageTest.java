package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;

import java.util.Vector;

public class HandleSendMessageTest extends EchoVBroadcastTest {

    @Test
    public void shouldEchoValidMessage() {
        // Given an instance that is not the proposer
        EchoVBroadcast instance = instances.get(REPLICA_ID + 1);
        // And a valid SEND message
        String pid = instance.getPid();
        byte[] input = "Hello World".getBytes();
        SendMessage message = new SendMessage(pid, REPLICA_ID, input);

        // When
        Step<VOutput> step = instance.handleMessage(message);

        // Then
        Assertions.assertNotNull(step);
        // And
        Vector<TargetedMessage> messages = step.getMessages();
        Assertions.assertEquals(1, messages.size());
        Assertions.assertTrue(messages.stream()
                .allMatch(m -> m.getContent() instanceof EchoMessage));
        // And
        Assertions.assertTrue(step.getOutput().isEmpty());
    }

    @Test
    public void shouldIgnoreDuplicateMessages() {
        // Given an instance that is not the proposer
        EchoVBroadcast instance = instances.get(REPLICA_ID + 1);
        // And a valid SEND message
        String pid = instance.getPid();
        byte[] input = "Hello World".getBytes();
        SendMessage message = new SendMessage(pid, REPLICA_ID, input);

        // When
        instance.handleMessage(message);
        // And
        Step<VOutput> step = instance.handleMessage(message);

        // Then
        assertEmptyStep(step);
    }

    @Test
    public void shouldIgnoreNonProposerMessages() {
        // Given an instance that is not the proposer
        EchoVBroadcast instance = instances.get(1);
        // And a SEND message from a non proposing replica
        String pid = instance.getPid();
        byte[] input = "Hello World".getBytes();
        SendMessage message = new SendMessage(pid, 2, input);

        // When
        Step<VOutput> step = instance.handleMessage(message);

        // Then
        assertEmptyStep(step);
    }
}

package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.utils.FakeMessage;

public class HandleMessageTest extends EchoVBroadcastTest {

    @ParameterizedTest
    @ValueSource(strings = {"vCBC-X-X", "", " "})
    public void givenMessageWithInvalidPid_whenHandleMessage_thenFail(String pid) {
        // Given
        IEchoVBroadcast instance = instances.get(1);
        // And
        VBroadcastMessage message = new SendMessage(pid, 0, new byte[0]);

        // act
        Step<VOutput> step = instance.handleMessage(message);

        // assert
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
    }

    @Test
    public void handleMessage_whenInvalidMessageType_Test() {
        // arrange
        EchoVBroadcast instance = instances.get(REPLICA_ID);
        // and
        VBroadcastMessage message = new FakeMessage(instance.getPid(), 0);

        // act
        Step<VOutput> step = instance.handleMessage(message);

        // assert
        Assertions.assertTrue(step.getMessages().isEmpty());
        Assertions.assertTrue(step.getOutput().isEmpty());
    }
}

package pt.tecnico.ulisboa.hbbft.vbroadcast.echo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.TargetedMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VOutput;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.FinalMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HandleEchoMessageTest extends EchoVBroadcastTest {

    @Test
    public void shouldSendFinalAfterReceivingAQuorumOfMessages() {
        // Given
        String pid = proposerInstance.getPid();
        byte[] input = "Hello World".getBytes();
        byte[] share = "Share".getBytes();
        // And
        int quorum = 2*TOLERANCE + 1;

        // When
        List<Step<VOutput>> steps = new ArrayList<>();
        for (int i=0; i < quorum - 1; i++) {
            SigShare sigShare = new SigShare(i+1, share);
            EchoMessage echoMessage = new EchoMessage(pid, i, input, sigShare);
            steps.add(proposerInstance.handleMessage(echoMessage));
        }

        // Then
        for (Step<VOutput> step: steps) assertEmptyStep(step);

        // When
        SigShare sigShare = new SigShare(quorum, share);
        EchoMessage echoMessage = new EchoMessage(pid, quorum-1, input, sigShare);
        Step<VOutput> step = proposerInstance.handleMessage(echoMessage);

        // Then
        Assertions.assertNotNull(step);
        // And
        Vector<TargetedMessage> messages = step.getMessages();
        Assertions.assertEquals(NUM_REPLICAS-1, messages.size());
        // And
        FinalMessage finalMessage = (FinalMessage) messages.firstElement().getContent();
        Assertions.assertAll(
                () -> Assertions.assertEquals(pid, finalMessage.getPid()),
                () -> Assertions.assertEquals(REPLICA_ID, finalMessage.getSender()),
                () -> Assertions.assertEquals(input, finalMessage.getValue())
                // TODO mock threshsig utils () -> Assertions.assertEquals(new byte[0], finalMessage.getSignature())
        );
    }

    @Test
    public void shouldIgnoreDuplicateMessages() {
        // TODO
    }

    @Test
    public void shouldIgnoreInvalidSignatureMessages() {
        // TODO
    }

    @Test
    public void shouldOnlySendFinalMessageOnce() {
        // TODO
    }
}

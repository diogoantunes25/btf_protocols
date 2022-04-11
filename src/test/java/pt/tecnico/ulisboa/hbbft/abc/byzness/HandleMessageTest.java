package pt.tecnico.ulisboa.hbbft.abc.byzness;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.binaryagreement.BinaryAgreementMessage;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.BoolSet;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.*;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillGapMessage;
import pt.tecnico.ulisboa.hbbft.abc.byzness.messages.FillerMessage;
import pt.tecnico.ulisboa.hbbft.utils.threshsig.SigShare;
import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.EchoMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.FinalMessage;
import pt.tecnico.ulisboa.hbbft.vbroadcast.echo.messages.SendMessage;

import java.util.stream.Stream;

import static org.mockito.Mockito.*;

public class HandleMessageTest extends ByznessBaseTest {

    @ParameterizedTest
    @MethodSource("vBroadcastMessageProvider")
    public void handleMessage_givenVBroadcastMessage_thenRouteToHandler(VBroadcastMessage msg) {
        // Given
        doReturn(new Step<>()).when(instance).handleVBroadcastMessage(msg);

        // When
        Step<Block> step = instance.handleMessage(msg);

        // Then
        verify(instance, times(1)).handleVBroadcastMessage(msg);
        assertEmptyStep(step);
    }

    @ParameterizedTest
    @MethodSource("binaryAgreementMessageProvider")
    public void handleMessage_givenBinaryAgreementMessage_thenRouteToHandler(BinaryAgreementMessage msg) {
        // Given
        doReturn(new Step<>()).when(instance).handleBinaryAgreementMessage(msg);

        // When
        Step<Block> step = instance.handleMessage(msg);

        // Then
        verify(instance, times(1)).handleBinaryAgreementMessage(msg);
        assertEmptyStep(step);
    }

    @ParameterizedTest
    @MethodSource("byznessMessageProvider")
    public void handleMessage_givenProtocolMessage_thenRouteToHandler(ProtocolMessage msg) {
        // Given
        doReturn(new Step<>()).when(instance).handleProtocolMessage(msg);

        // When
        Step<Block> step = instance.handleMessage(msg);

        // Then
        verify(instance, times(1)).handleProtocolMessage(msg);
        assertEmptyStep(step);
    }

    private static Stream<Arguments> vBroadcastMessageProvider() {
        return Stream.of(
                Arguments.of(new SendMessage("vCBC-0-0", 0, new byte[0])),
                Arguments.of(new EchoMessage("vCBC-0-0", 0, new byte[0], new SigShare(1, "sig".getBytes()))),
                Arguments.of(new FinalMessage("vCBC-0-0", 0, new byte[0], new byte[0]))
        );
    }

    private static Stream<Arguments> binaryAgreementMessageProvider() {
        return Stream.of(
                Arguments.of(new BValMessage("BA-0", 0, 0L, true)),
                Arguments.of(new AuxMessage("BA-0", 0, 0L, true)),
                Arguments.of(new ConfMessage("BA-0", 0, 0L, BoolSet.NONE())),
                Arguments.of(new CoinMessage("BA-0", 0, 0L, new byte[0])),
                Arguments.of(new TermMessage("BA-0", 0, 0L, true))
        );
    }

    private static Stream<Arguments> byznessMessageProvider() {
        return Stream.of(
                Arguments.of(new FillGapMessage("BIZ", 0, 0, 0L)),
                Arguments.of(new FillerMessage("BIZ", 0, 0, 0L, new byte[0], new byte[0]))
        );
    }
}

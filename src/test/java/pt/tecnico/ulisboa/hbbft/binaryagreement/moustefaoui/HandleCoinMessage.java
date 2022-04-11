package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui.messages.CoinMessage;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HandleCoinMessage extends MoustefaouiBinaryAgreementBaseTest {

    @Mock
    private Coin coin;

    private CoinMessage coinMessage;

    @Override
    public void populate4Test() {
        this.coin = mock(Coin.class);
        this.coinMessage = new CoinMessage(
                instance.getPid(), REPLICA_ID, instance.getRound(), "SHARE".getBytes());
    }

    @Test
    public void handleCoinMessage_whenAlreadyDecided_thenProgress() {
        // Given
        when(coin.hasDecided()).thenReturn(false).thenReturn(true);
        // And
        doReturn(coin).when(instance).getCoin();
        // And
        doReturn(new Step<>()).when(instance).tryUpdateRound();

        // When
        Step<Boolean> step = instance.handleMessage(coinMessage);

        // Then
        assertEmptyStep(step);
        verify(instance, times(1)).tryUpdateRound();
    }

    @Test
    public void handleCoinMessage_whenAlreadyDecided_thenIgnore() {
        // Given
        when(coin.hasDecided()).thenReturn(true);
        // And
        doReturn(coin).when(instance).getCoin();

        // When
        Step<Boolean> step = instance.handleMessage(coinMessage);

        // Then
        assertEmptyStep(step);
        verify(instance, times(0)).tryUpdateRound();
    }
}

package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.ByznessBaseTest;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class OngoingStateTest extends ByznessBaseTest {

    private OngoingState ongoingState;

    @Override
    public void populate4Test() {
        this.ongoingState = spy(new OngoingState(instance, 0L));
    }

    @Test
    public void tryProgress_whenCannotProgress_thenDoNotProgress() {
        // Given
        IBinaryAgreement baInstance = mock(IBinaryAgreement.class);
        when(baInstance.hasTerminated()).thenReturn(false);
        // And
        doReturn(baInstance).when(ongoingState).getBaInstance();

        // When
        Step<Block> step = ongoingState.tryProgress();

        // Then
        assertEmptyStep(step);
        // And
        verify(instance, times(0)).setAgreementState(any());
    }

    @Test
    public void tryProgress_whenYesDecision_thenProgressToWaitingState() {
        // Given
        IBinaryAgreement baInstance = mock(IBinaryAgreement.class);
        when(baInstance.hasTerminated()).thenReturn(true);
        when(baInstance.deliver()).thenReturn(Optional.of(true));
        // And
        doReturn(baInstance, baInstance).when(ongoingState).getBaInstance();
        // And
        doReturn(new Step<>()).when(instance).setAgreementState(any(WaitingState.class));

        // When
        ongoingState.tryProgress();

        // Then
        verify(instance, times(1)).setAgreementState(any(WaitingState.class));
    }

    @Test
    public void tryProgress_whenNoDecision_thenProgressToProposingState() {
        // Given
        IBinaryAgreement baInstance = mock(IBinaryAgreement.class);
        when(baInstance.hasTerminated()).thenReturn(true);
        when(baInstance.deliver()).thenReturn(Optional.of(false));
        // And
        doReturn(baInstance, baInstance).when(ongoingState).getBaInstance();
        // And
        doReturn(new Step<>()).when(instance).setAgreementState(any(ProposingState.class));

        // When
        ongoingState.tryProgress();

        // Then
        verify(instance, times(1)).setAgreementState(any(ProposingState.class));
    }
}

package pt.tecnico.ulisboa.hbbft.binaryagreement.moustefaoui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HandleTermMessageTest extends MoustefaouiBinaryAgreementBaseTest {

    @Test
    public void shouldIgnoreDuplicateMessages() {
        // TODO
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldTerminateUponReceivingAQuorumOfTermMessages() {
        // TODO
    }
}

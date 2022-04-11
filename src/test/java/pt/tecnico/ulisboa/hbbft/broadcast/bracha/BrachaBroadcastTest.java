package pt.tecnico.ulisboa.hbbft.broadcast.bracha;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.tecnico.ulisboa.hbbft.NetworkInfo;
import pt.tecnico.ulisboa.hbbft.Step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BrachaBroadcastTest {

    private String pid;
    private NetworkInfo networkInfo;
    private BrachaBroadcast broadcast;

    /*@BeforeEach
    public void init() {
        pid = "BC-0";
        networkInfo = new NetworkInfo(0, );
        broadcast = new BrachaBroadcast(pid, 0, networkInfo, 0);
    }

    @Test
    public void getPid_Test() {
        assertEquals(pid, broadcast.getPid());
    }

    @Test
    public void handleInput_whenNotProposer_Test() {
        final byte[] input = "Hello World".getBytes();
        Step<byte[]> step = broadcast.handleInput(input);
        assertTrue(step.getMessages().isEmpty());
        assertTrue(step.getOutput().isEmpty());
    }

    public void handleInput_whenProposer_Test() {
        final byte[] input = "Hello World".getBytes();
        Step<byte[]> step = broadcast.handleInput(input);
    }*/
}

package pt.tecnico.ulisboa.hbbft;

import java.util.Optional;

public interface IProtocol<I, O, M extends ProtocolMessage> {

    Step<O> handleInput(I input);

    Step<O> handleMessage(M message);

    boolean hasTerminated();

    Optional<O> deliver();
}

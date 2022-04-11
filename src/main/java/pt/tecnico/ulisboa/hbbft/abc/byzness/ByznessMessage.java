package pt.tecnico.ulisboa.hbbft.abc.byzness;

import pt.tecnico.ulisboa.hbbft.ProtocolMessage;

public class ByznessMessage extends ProtocolMessage {
    public ByznessMessage(String pid, Integer type, Integer sender) {
        super(pid, type, sender);
    }
}

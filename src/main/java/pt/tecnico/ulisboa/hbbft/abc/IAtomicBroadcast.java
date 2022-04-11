package pt.tecnico.ulisboa.hbbft.abc;

import pt.tecnico.ulisboa.hbbft.IProtocol;
import pt.tecnico.ulisboa.hbbft.ProtocolMessage;

public interface IAtomicBroadcast extends IProtocol<byte[], Block, ProtocolMessage> {
}

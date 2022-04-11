package pt.tecnico.ulisboa.hbbft.subset;

import pt.tecnico.ulisboa.hbbft.ProtocolMessage;
import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.broadcast.IBroadcast;
import pt.tecnico.ulisboa.hbbft.subset.state.OngoingState;

public class Proposal {

    private final Integer instance;
    private ProposalState state;

    private byte[] result;

    public Proposal(Integer instance, IBroadcast bc, IBinaryAgreement ba) {
        this.instance = instance;
        this.state = new OngoingState(this, bc, ba);
    }

    public Integer getInstance() {
        return instance;
    }

    public Boolean received() {
        return state.received();
    }

    public Boolean accepted() {
        return state.accepted();
    }

    public Boolean complete() {
        return state.complete();
    }

    public Step<byte[]> propose(byte[] value) {
        return state.propose(value);
    }

    public Step<byte[]> vote(boolean value) {
        return state.vote(value);
    }

    public Step<byte[]> handleMessage(ProtocolMessage message) {
        return state.handleMessage(message);
    }

    public byte[] getResult() {
        return this.result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public void setState(ProposalState state) {
        this.state = state;
    }
}

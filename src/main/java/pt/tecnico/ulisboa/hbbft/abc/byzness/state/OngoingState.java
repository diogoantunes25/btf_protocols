package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.Byzness;

public class OngoingState extends AgreementState {

    public OngoingState(Byzness byzness, Long epoch) {
        super(byzness, epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        Step<Block> step = new Step<>();;

        if (this.canProgress()) {
            IBinaryAgreement instance = this.getBaInstance();
            Boolean decision = instance.deliver().orElseThrow();

            AgreementState nextState;
            if (decision) nextState = new WaitingState(byzness, epoch);
            else nextState = new ProposingState(byzness, epoch + 1);

            step.add(this.byzness.setAgreementState(nextState));
        }
        return step;
    }

    private boolean canProgress() {
        return this.getBaInstance().hasTerminated();
    }
}

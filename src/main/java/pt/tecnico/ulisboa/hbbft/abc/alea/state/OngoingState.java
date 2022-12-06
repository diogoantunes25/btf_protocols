package pt.tecnico.ulisboa.hbbft.abc.alea.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.alea.Alea;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;

public class OngoingState extends AgreementState {

    public OngoingState(Alea alea, Long epoch) {
        super(alea, epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        // logger.info("tryPropose on Ongoingstate");
        Step<Block> step = new Step<>();;

        if (this.canProgress()) {
            IBinaryAgreement instance = this.getBaInstance();
            Boolean decision = instance.deliver().orElseThrow();

            AgreementState nextState;

            // Agreement succeeded
            if (decision) {
                nextState = new WaitingState(alea, epoch);
                // logger.info("moving to Waiting state");
            }
            // Not enough people had the slot so agreement failed
            else {
                nextState = new ProposingState(alea, epoch + 1);
                // logger.info("moving to Proposing state");
            }

            step.add(this.alea.setAgreementState(nextState));
        } else {
            // logger.info("tryPropose on OnGoingState aborted - ABA did not finish");
        }
        return step;
    }

    private boolean canProgress() {
        return this.getBaInstance().hasTerminated();
    }

    public String toString() {
        return "OnGoingState";
    }
}

package pt.tecnico.ulisboa.hbbft.abc.byzness.state;

import pt.tecnico.ulisboa.hbbft.Step;
import pt.tecnico.ulisboa.hbbft.binaryagreement.IBinaryAgreement;
import pt.tecnico.ulisboa.hbbft.abc.Block;
import pt.tecnico.ulisboa.hbbft.abc.byzness.Byzness;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.PriorityQueue;
import pt.tecnico.ulisboa.hbbft.abc.byzness.queue.Slot;

import java.util.Optional;

public class ProposingState extends AgreementState {

    public ProposingState(Byzness byzness, Long epoch) {
        super(byzness, epoch);
    }

    @Override
    public Step<Block> tryProgress() {
        Step<Block> step = new Step<>();

        if (this.canProgress()) {
            PriorityQueue queue = this.getQueue();
            Optional<Slot> slot = queue.peek();

            IBinaryAgreement instance = this.getBaInstance();
            Step<Boolean> baStep = instance.handleInput(slot.isPresent());
            step.add(baStep.getMessages());

            AgreementState nextState = new OngoingState(byzness, epoch);
            step.add(this.byzness.setAgreementState(nextState));
        }
        return step;
    }

    private boolean canProgress() {
        return this.byzness.getQueues().values().stream()
                .anyMatch(queue -> queue.peek().isPresent());
    }
}
